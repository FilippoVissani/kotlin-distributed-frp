package io.github.filippovissani.kotlin_distributed_frp

import io.github.filippovissani.kotlin_distributed_dfrp.*
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics.branch
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics.constant
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics.loop
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics.neighbour
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics.selfID
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics.sense
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.*
import kotlin.test.*

class SemanticsSpec : FreeSpec({
    val selfID = 1
    val localSensor = "sensor" to 1
    val initialSensorsValues = mapOf(localSensor)
    val neighbours = setOf(1, 2, 3, 4)
    val path = emptyList<Nothing>()
    val thenValue = 1
    val elseValue = 2

    fun runProgramOnNeighbours(selfContext: Context, aggregateExpression: AggregateExpression<*>, neighbors: Iterable<DeviceID> = neighbours) {
        runBlocking {
            neighbors.forEach{ neighbour ->
                val neighbourContext = Context(neighbour, initialSensorsValues)
                selfContext.receiveExport(neighbour, aggregateExpression.compute(path, neighbourContext).first())
            }
        }
    }

    "The constant construct" - {
        "should be a constant flow with the given value" {
            runBlocking {
                val selfContext = Context(selfID)
                val value = 10
                val program = constant(value)
                program
                    .compute(path, selfContext)
                    .collect{ export ->
                        export shouldBe ExportTree(value)
                    }
            }
        }
    }

    "The selfID construct" - {
        "should be a constant flow with the device ID" {
            runBlocking {
                val selfContext = Context(selfID)
                val program = selfID()
                program
                    .compute(path, selfContext)
                    .collect{export ->
                        export shouldBe ExportTree(selfID)
                    }
            }
        }
    }

    "The branch construct" - {
        "should include only the 'then' branch when the condition is true" {
            runBlocking {
                val selfContext = Context(selfID)
                val program = branch(constant(true), constant(thenValue), constant(elseValue))
                program.compute(path, selfContext).collect{ export ->
                    assertEquals(export, ExportTree(thenValue, mapOf(Condition to ExportTree(true), Then to ExportTree(thenValue))))
                }
            }
        }

        "should include only the 'else' branch when the condition is false" {
            runBlocking {
                val selfContext = Context(selfID)
                val program = branch(constant(false), constant(thenValue), constant(elseValue))
                program.compute(path, selfContext).collect{ export ->
                    assertEquals(export, ExportTree(elseValue, mapOf(Condition to ExportTree(false), Else to ExportTree(elseValue))))
                }
            }
        }

        "should react to changes in the condition" {
            runBlocking {
                val selfContext = Context(selfID)
                val condition = MutableStateFlow(true)
                val program = branch(AggregateExpression.fromFlow { _ -> condition }, constant(thenValue), constant(elseValue))
                val exports = program.compute(path, selfContext)
                condition.update { false }
                exports.first().root shouldBe elseValue
            }
        }

        "should react to changes in the selected branch" {
            runBlocking {
                val selfContext = Context(selfID)
                val thenBranch = MutableStateFlow(thenValue)
                val program = branch(constant(true), AggregateExpression.fromFlow { thenBranch }, constant(elseValue))
                val exports = program.compute(path, selfContext)
                val newValue = 100
                thenBranch.emit(newValue)
                exports.take(1).collectLatest{ export ->
                    export.root shouldBe newValue
                }
            }
        }
    }
    
    "The neighbour construct" - {
        "should collect values from aligned neighbors" {
            runBlocking {
                val selfContext = Context(selfID)
                val program = branch(selfID().map { it < 3 }, neighbour(selfID()), neighbour(constant(0)))
                runProgramOnNeighbours(selfContext, program)
                val expectedNeighborField = neighbours.associateWith { if (it < 3) it else null  }
                program.compute(path, selfContext).first().followPath(listOf(Then)) shouldBe ExportTree(expectedNeighborField, mapOf(Neighbour to ExportTree(selfID)))
            }
        }

        "should react to changes in the neighborhood state" {
            runBlocking {
                val selfContext = Context(selfID, initialSensorsValues)
                val program = neighbour(sense<Int>(localSensor.first))
                val export = program.compute(path, selfContext)
                runProgramOnNeighbours(selfContext, program)
                export.first().root shouldBe neighbours.associateWith { localSensor.second }
            }
        }
    }

    "The loop construct" - {
        "should return a self-dependant flow" {
            runBlocking {
                val selfContext = Context(selfID, initialSensorsValues)
                val program = loop(0){ value -> combine(value, sense<Int>(localSensor.first)){ x, y -> x + y } }
                val export = program.compute(path, selfContext)
                export.first().root shouldBe localSensor.second
            }
        }

        "should react to updates in its past state" {
            runBlocking {
                val selfContext = Context(selfID, initialSensorsValues)
                val program = loop(0){ value -> combine(value, sense<Int>(localSensor.first)){ x, y -> x + y } }
                val export = program.compute(path, selfContext)
                selfContext.receiveExport(selfID, export.first())
                export.first().root shouldBe localSensor.second + 1
                selfContext.receiveExport(selfID, export.first())
                export.first().root shouldBe localSensor.second + 2
            }
        }

        "should react to updates in dependencies in the looping function" {
            runBlocking {
                val selfContext = Context(selfID, initialSensorsValues)
                val program = loop(0){ value -> combine(value, sense<Int>(localSensor.first)){ x, y -> x + y } }
                val export = program.compute(path, selfContext)
                val newValue = 1
                selfContext.updateLocalSensor(localSensor.first, newValue)
                export.first().root shouldBe 1
            }
        }
    }

    "The sense construct" - {
        "should evaluate to the initial sensor value" {
            runBlocking {
                val selfContext = Context(selfID, initialSensorsValues)
                val program = sense<Int>(localSensor.first)
                program.compute(path, selfContext).first().root shouldBe localSensor.second
            }
        }

        "should react to sensor changes" {
            runBlocking {
                val selfContext = Context(selfID, initialSensorsValues)
                val program = sense<Int>(localSensor.first)
                val newValue = 10
                selfContext.updateLocalSensor(localSensor.first, newValue)
                program.compute(path, selfContext).first().root shouldBe newValue
            }
        }
    }
})

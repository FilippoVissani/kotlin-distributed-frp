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
    val localSensor = "sensor"
    val localSensorValue = 1
    val initialSensorsValues = mapOf(localSensor to localSensorValue)
    val neighbours = setOf(1, 2, 3, 4)
    val path = emptyList<Nothing>()
    val thenValue = 1
    val elseValue = 2
    fun selfContext() = Context(selfID, initialSensorsValues)
    fun neighboursContexts() = neighbours.map { Context(it, initialSensorsValues) }

    fun runProgramOnNetwork(selfContext: Context, neighbors: Iterable<Context>, aggregateExpression: AggregateExpression<*>) {
        runBlocking {
            neighbors.forEach{ neighbour ->
                selfContext.receiveExport(neighbour.selfID, aggregateExpression.compute(path, neighbour).first())
            }
        }
    }

    "The constant construct" - {
        "should be a constant flow with the given value" {
            runBlocking {
                val selfContext = selfContext()
                val value = 10
                val program = constant(value)
                val exports = program.compute(path, selfContext)
                exports.first().root shouldBe value
            }
        }
    }

    "The selfID construct" - {
        "should be a constant flow with the device ID" {
            runBlocking {
                val selfContext = selfContext()
                val program = selfID()
                val exports = program.compute(path, selfContext)
                exports.first().root shouldBe selfID
            }
        }
    }

    "The branch construct" - {
        "should include only the 'then' branch when the condition is true" {
            runBlocking {
                val selfContext = selfContext()
                val program = branch(constant(true), constant(thenValue), constant(elseValue))
                val exports = program.compute(path, selfContext)
                exports.first() shouldBe ExportTree(thenValue, mapOf(Condition to ExportTree(true), Then to ExportTree(thenValue)))
            }
        }

        "should include only the 'else' branch when the condition is false" {
            runBlocking {
                val selfContext = selfContext()
                val program = branch(constant(false), constant(thenValue), constant(elseValue))
                val exports = program.compute(path, selfContext)
                exports.first() shouldBe ExportTree(elseValue, mapOf(Condition to ExportTree(false), Else to ExportTree(elseValue)))
            }
        }

        "should react to changes in the condition" {
            runBlocking {
                val selfContext = selfContext()
                val condition = MutableStateFlow(true)
                val program = branch(AggregateExpression.fromFlow { _ -> condition }, constant(thenValue), constant(elseValue))
                val exports = program.compute(path, selfContext)
                condition.update { false }
                exports.first().root shouldBe elseValue
            }
        }

        "should react to changes in the selected branch" {
            runBlocking {
                val selfContext = selfContext()
                val thenBranch = MutableStateFlow(thenValue)
                val program = branch(constant(true), AggregateExpression.fromFlow { thenBranch }, constant(elseValue))
                val exports = program.compute(path, selfContext)
                val newValue = 100
                thenBranch.update { newValue }
                exports.first().root shouldBe newValue
            }
        }
    }

    "The neighbour construct" - {
        "should collect values from aligned neighbors" {
            runBlocking {
                val selfContext = selfContext()
                val neighboursContexts = neighboursContexts()
                val program = branch(selfID().map { it < 3 }, neighbour(selfID()), neighbour(constant(0)))
                val exports = program.compute(path, selfContext)
                runProgramOnNetwork(selfContext, neighboursContexts, program)
                val expectedNeighborField = neighbours.associateWith { if (it < 3) it else null  }
                exports.first().followPath(listOf(Then)) shouldBe ExportTree(expectedNeighborField, mapOf(Neighbour to ExportTree(selfID)))
            }
        }

        "should react to changes in the neighborhood state" {
            runBlocking {
                val selfContext = selfContext()
                val neighboursContexts = neighboursContexts()
                val program = neighbour(sense<Int>(localSensor))
                val export = program.compute(path, selfContext)
                runProgramOnNetwork(selfContext, neighboursContexts, program)
                export.first().root shouldBe neighbours.associateWith { localSensorValue }
            }
        }
    }

    "The loop construct" - {
        "should return a self-dependant flow" {
            runBlocking {
                val selfContext = selfContext()
                val program = loop(0){ value -> combine(value, sense<Int>(localSensor)){ x, y -> x + y } }
                val export = program.compute(path, selfContext)
                export.first().root shouldBe localSensorValue
            }
        }

        "should react to updates in its past state" {
            runBlocking {
                val selfContext = selfContext()
                val program = loop(0){ value -> combine(value, sense<Int>(localSensor)){ x, y -> x + y } }
                val export = program.compute(path, selfContext)
                selfContext.receiveExport(selfID, export.first())
                export.first().root shouldBe localSensorValue + 1
                selfContext.receiveExport(selfID, export.first())
                export.first().root shouldBe localSensorValue + 2
            }
        }

        "should react to updates in dependencies in the looping function" {
            runBlocking {
                val selfContext = selfContext()
                val program = loop(0){ value -> combine(value, sense<Int>(localSensor)){ x, y -> x + y } }
                val export = program.compute(path, selfContext)
                val newValue = 10
                selfContext.updateLocalSensor(localSensor, newValue)
                export.first().root shouldBe newValue
            }
        }
    }

    "The sense construct" - {
        "should evaluate to the initial sensor value" {
            runBlocking {
                val selfContext = selfContext()
                val program = sense<Int>(localSensor)
                val export = program.compute(path, selfContext)
                export.first().root shouldBe localSensorValue
            }
        }

        "should react to sensor changes" {
            runBlocking {
                val selfContext = selfContext()
                val program = sense<Int>(localSensor)
                val export = program.compute(path, selfContext)
                val newValue = 10
                selfContext.updateLocalSensor(localSensor, newValue)
                export.first().root shouldBe newValue
            }
        }
    }
})

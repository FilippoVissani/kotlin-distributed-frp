package io.github.filippovissani.kotlin_distributed_frp

import io.github.filippovissani.kotlin_distributed_dfrp.*
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics.branch
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics.constant
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics.neighbour
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics.selfID
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.*
import kotlin.test.*

class SemanticsSpec : FreeSpec({
    val selfID = 1
    val context = Context(selfID)
    val neighbours = setOf(1, 2, 3, 4)
    val path = emptyList<Nothing>()
    val thenValue = 1
    val elseValue = 2

    fun runProgramOnNeighbours(selfContext: Context, computation: Computation<*>, neighbors: Iterable<DeviceID> = neighbours) {
        runBlocking {
            neighbors.forEach{ neighbour ->
                val neighbourContext = Context(neighbour)
                selfContext.receiveExport(neighbour, computation.run(path, neighbourContext).last())
            }
        }
    }

    "The constant construct" - {
        "should be a constant flow with the given value" {
            runBlocking {
                val value = 10
                val program = constant(value)
                program
                    .run(path, context)
                    .collect{ export ->
                        export shouldBe ExportTree(value)
                    }
            }
        }
    }

    "The selfID construct" - {
        "should be a constant flow with the device ID" {
            runBlocking {
                val program = selfID()
                program
                    .run(path, context)
                    .collect{export ->
                        export shouldBe ExportTree(selfID)
                    }
            }
        }
    }

    "The branch construct" - {
        "should include only the 'then' branch when the condition is true" {
            runBlocking {
                val program = branch(constant(true), constant(thenValue), constant(elseValue))
                program.run(path, context).collect{ export ->
                    assertEquals(export, ExportTree(thenValue, mapOf(Condition to ExportTree(true), Then to ExportTree(thenValue))))
                }
            }
        }

        "should include only the 'else' branch when the condition is false" {
            runBlocking {
                val program = branch(constant(false), constant(thenValue), constant(elseValue))
                program.run(path, context).collect{ export ->
                    assertEquals(export, ExportTree(elseValue, mapOf(Condition to ExportTree(false), Else to ExportTree(elseValue))))
                }
            }
        }

        "should react to changes in the condition" {
            runBlocking {
                val condition = MutableStateFlow(true)
                val program = branch(Computation.fromFlow { _ -> condition }, constant(thenValue), constant(elseValue))
                val exports = program.run(path, context)
                condition.emit(false)
                exports.take(1).collectLatest{ export ->
                    export.root shouldBe elseValue
                }
            }
        }

        "should react to changes in the selected branch" {
            runBlocking {
                val thenBranch = MutableStateFlow(thenValue)
                val program = branch(constant(true), Computation.fromFlow { thenBranch }, constant(elseValue))
                val exports = program.run(path, context)
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
                val program = branch(Computation.fromFlow { ctx -> flowOf(ctx.selfID < 3) }, neighbour(selfID()), neighbour(constant(0)))
                runProgramOnNeighbours(context, program)
                val expectedNeighborField = neighbours.associateWith { if (it < 3) it else null  }
                program.run(path, context).collect{ export ->
                    export.followPath(listOf(Then)) shouldBe ExportTree(expectedNeighborField, mapOf(Neighbour to ExportTree(selfID)))
                }
            }
        }
    }
})

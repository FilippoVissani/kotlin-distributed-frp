package io.github.filippovissani.kotlin_distributed_frp

import io.github.filippovissani.kotlin_distributed_dfrp.*
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics.branch
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics.constant
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics.selfID
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.test.*

class SemanticsSpec : FreeSpec({
    val selfID = DeviceID(1)
    val context = Context(selfID)
    val neighbours = setOf(DeviceID(1), DeviceID(2), DeviceID(3), DeviceID(4))
    val path = emptyList<Nothing>()
    val thenValue = 1
    val elseValue = 2

    fun runFlowOnNeighbors(computation: Computation<Any>, neighbors: Iterable<DeviceID> = neighbours) {
        runBlocking {
            neighbors.forEach{ neighbour ->
                val neighbourContext = Context(neighbour)
                computation.run(path, neighbourContext).collect{ export ->
                    context.receiveExport(neighbour, export)
                }
            }
        }
    }

    "The constant construct" - {
        "should be a constant flow with the given value" {
            runBlocking {
                val value = 10
                val computation = constant(value)
                computation
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
                val computation = selfID()
                computation
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
                val computation = branch(constant(true), constant(thenValue), constant(elseValue))
                computation.run(path, context).collect{ export ->
                    assertEquals(export, ExportTree(thenValue, sequenceOf(Condition to ExportTree(true), Then to ExportTree(thenValue))))
                }
            }
        }

        "should include only the 'else' branch when the condition is false" {
            runBlocking {
                val computation = branch(constant(false), constant(thenValue), constant(elseValue))
                computation.run(path, context).collect{ export ->
                    assertEquals(export, ExportTree(elseValue, sequenceOf(Condition to ExportTree(false), Else to ExportTree(elseValue))))
                }
            }
        }

        "should react to changes in the condition" {
            val condition = flowOf(true).map { _ -> false }
            val computation = branch(Computation.fromFlow { _ -> condition }, constant(thenValue), constant(elseValue))
            val exports = computation.run(path, context)
            exports.collectLatest { export ->
                export.root shouldBe elseValue }
        }
    }
})

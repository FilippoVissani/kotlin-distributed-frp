package io.github.filippovissani.dfrp

import io.github.filippovissani.dfrp.DefaultConfiguration.initialSensorsValues
import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.Context
import io.github.filippovissani.dfrp.core.NeighborField
import io.github.filippovissani.dfrp.core.Semantics.branch
import io.github.filippovissani.dfrp.core.Semantics.constant
import io.github.filippovissani.dfrp.core.Semantics.neighbor
import io.github.filippovissani.dfrp.core.Semantics.selfID
import io.github.filippovissani.dfrp.core.extensions.map
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class AlignmentSpec : FreeSpec({
    "With branch" - {
        "Devices with different conditions should not be aligned" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                val context0 = Context(0, initialSensorsValues)
                val context1 = Context(1, initialSensorsValues)
                val aggregateProgram: () -> AggregateExpression<NeighborField<Boolean>> = {
                    branch(
                        selfID().map { it < 1 },
                        neighbor(constant(true)),
                        neighbor(constant(false))
                    )
                }
                val simulation0 = DeviceSimulation(
                    context0,
                    listOf(context0, context1),
                    aggregateProgram,
                    {},
                    { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe mapOf(1 to null, 0 to true)
                    },
                )
                val simulation1 = DeviceSimulation(
                    context1,
                    listOf(context0, context1),
                    aggregateProgram,
                    {},
                    { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe mapOf(1 to false, 0 to null)
                    },
                )
                MockSimulator.runSimulation(MockSimulation(testName, listOf(simulation0, simulation1)))
            }
        }

        "Devices with different conditions should not be aligned" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                val context0 = Context(0, initialSensorsValues)
                val context1 = Context(1, initialSensorsValues)
                val aggregateProgram: () -> AggregateExpression<NeighborField<Boolean>> = {
                    neighbor(
                        branch(
                            selfID().map { it < 1 },
                            constant(true),
                            constant(false)
                        )
                    )
                }
                val simulation0 = DeviceSimulation(
                    context0,
                    listOf(context0, context1),
                    aggregateProgram,
                    {},
                    { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe mapOf(1 to null, 0 to true)
                    },
                )
                val simulation1 = DeviceSimulation(
                    context1,
                    listOf(context0, context1),
                    aggregateProgram,
                    {},
                    { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe mapOf(1 to false, 0 to null)
                    },
                )
                MockSimulator.runSimulation(MockSimulation(testName, listOf(simulation0, simulation1)))
            }
        }
    }
})

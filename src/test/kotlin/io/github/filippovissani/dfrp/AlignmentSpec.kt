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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class AlignmentSpec : FreeSpec({

    suspend fun runAggregateProgram(
        testName: String,
        initialCondition0: Boolean,
        initialCondition1: Boolean,
        finalCondition0: Boolean = initialCondition0,
        finalCondition1: Boolean = initialCondition1,
        assertion0: (Context) -> Unit,
        assertion1: (Context) -> Unit,
    ) {
        val context0 = Context(0, initialSensorsValues)
        val context1 = Context(1, initialSensorsValues)
        val reactiveCondition0 = MutableStateFlow(initialCondition0)
        val reactiveCondition1 = MutableStateFlow(initialCondition1)
        val aggregateProgram0: () -> AggregateExpression<NeighborField<Boolean>> = {
            branch(
                AggregateExpression.fromStateFlow { reactiveCondition0 },
                neighbor(constant(true)),
                neighbor(constant(false))
            )
        }
        val aggregateProgram1: () -> AggregateExpression<NeighborField<Boolean>> = {
            branch(
                AggregateExpression.fromStateFlow { reactiveCondition1 },
                neighbor(constant(true)),
                neighbor(constant(false))
            )
        }
        val simulation0 = DeviceSimulation(
            context0,
            listOf(context0, context1),
            aggregateProgram0,
            { reactiveCondition0.update { finalCondition0 } },
            assertion0,
        )
        val simulation1 = DeviceSimulation(
            context1,
            listOf(context0, context1),
            aggregateProgram1,
            { reactiveCondition1.update { finalCondition1 } },
            assertion1,
        )
        MockSimulator.runSimulation(MockSimulation(testName, listOf(simulation0, simulation1)))
    }

    "With branch" - {

        "Devices with same condition should be aligned" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                runAggregateProgram(
                    testName,
                    initialCondition0 = true,
                    initialCondition1 = true,
                    assertion0 = { context -> context.neighborsStates.value[context.selfID]?.root shouldBe mapOf(0 to true, 1 to true) },
                    assertion1 = { context -> context.neighborsStates.value[context.selfID]?.root shouldBe mapOf(0 to true, 1 to true) },
                )
            }
        }

        "Devices with different conditions should not be aligned" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                runAggregateProgram(
                    testName,
                    initialCondition0 = true,
                    initialCondition1 = false,
                    assertion0 = { context -> context.neighborsStates.value[context.selfID]?.root shouldBe mapOf(0 to true, 1 to null) },
                    assertion1 = { context -> context.neighborsStates.value[context.selfID]?.root shouldBe mapOf(0 to null, 1 to false) },
                )
            }
        }

        "Devices should not align if the condition becomes the different" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                runAggregateProgram(
                    testName,
                    initialCondition0 = true,
                    initialCondition1 = true,
                    finalCondition1 = false,
                    assertion0 = { context -> context.neighborsStates.value[context.selfID]?.root shouldBe mapOf(0 to true, 1 to null) },
                    assertion1 = { context -> context.neighborsStates.value[context.selfID]?.root shouldBe mapOf(0 to null, 1 to false) },
                )
            }
        }

        "Devices should align if the condition becomes the same" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                runAggregateProgram(
                    testName,
                    initialCondition0 = true,
                    initialCondition1 = false,
                    finalCondition1 = true,
                    assertion0 = { context -> context.neighborsStates.value[context.selfID]?.root shouldBe mapOf(0 to true, 1 to true) },
                    assertion1 = { context -> context.neighborsStates.value[context.selfID]?.root shouldBe mapOf(0 to true, 1 to true) },
                )
            }
        }
    }
})

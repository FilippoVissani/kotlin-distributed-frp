package io.github.filippovissani.dfrp

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.Condition
import io.github.filippovissani.dfrp.core.Else
import io.github.filippovissani.dfrp.core.ExportTree
import io.github.filippovissani.dfrp.core.Semantics.branch
import io.github.filippovissani.dfrp.core.Semantics.constant
import io.github.filippovissani.dfrp.core.Semantics.loop
import io.github.filippovissani.dfrp.core.Semantics.mux
import io.github.filippovissani.dfrp.core.Semantics.neighbor
import io.github.filippovissani.dfrp.core.Semantics.selfID
import io.github.filippovissani.dfrp.core.Semantics.sense
import io.github.filippovissani.dfrp.core.Then
import io.github.filippovissani.dfrp.DefaultConfiguration.ELSE_VALUE
import io.github.filippovissani.dfrp.DefaultConfiguration.LOCAL_SENSOR
import io.github.filippovissani.dfrp.DefaultConfiguration.LOCAL_SENSOR_VALUE
import io.github.filippovissani.dfrp.DefaultConfiguration.N_DEVICES
import io.github.filippovissani.dfrp.DefaultConfiguration.THEN_VALUE
import io.github.filippovissani.dfrp.Simulator.runSimulation
import io.github.filippovissani.dfrp.core.extensions.combine
import io.github.filippovissani.dfrp.core.extensions.map
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class SemanticsSpec : FreeSpec({

    "The constant construct" - {
        "should be a constant flow with the given value" {
            val testName = this.testScope.testCase.name.testName
            val simpleValue = 100
            runBlocking {
                runSimulation(
                    testName = testName,
                    aggregateExpression = { constant(simpleValue) },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe simpleValue
                    }
                )
            }
        }
    }

    "The selfID construct" - {
        "should be a constant flow with the device ID" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                runSimulation(
                    testName = testName,
                    aggregateExpression = { selfID() },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe context.selfID
                    }
                )
            }
        }
    }

    "The branch construct" - {
        "should include only the 'then' branch when the condition is true" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                runSimulation(
                    testName = testName,
                    aggregateExpression = { branch(constant(true), constant(THEN_VALUE), constant(ELSE_VALUE)) },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID] shouldBe (ExportTree(
                            THEN_VALUE,
                            mapOf(Condition to ExportTree(true), Then to ExportTree(THEN_VALUE))
                        ))
                    }
                )
            }
        }

        "should include only the 'else' branch when the condition is false" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                runSimulation(
                    testName = testName,
                    aggregateExpression = { branch(constant(false), constant(THEN_VALUE), constant(ELSE_VALUE)) },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID] shouldBe (ExportTree(
                            ELSE_VALUE,
                            mapOf(Condition to ExportTree(false), Else to ExportTree(ELSE_VALUE))
                        ))
                    }
                )
            }
        }

        "should react to changes in the condition" {
            val testName = this.testScope.testCase.name.testName
            val condition = MutableStateFlow(true)
            runBlocking {
                runSimulation(
                    testName = testName,
                    aggregateExpression = {
                        branch(
                            AggregateExpression.fromStateFlow { condition },
                            constant(THEN_VALUE),
                            constant(ELSE_VALUE)
                        )
                    },
                    runAfter = { condition.update { false } },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe ELSE_VALUE
                    }
                )
            }
        }

        "should react to changes in the selected branch" {
            val testName = this.testScope.testCase.name.testName
            val thenBranch = MutableStateFlow(THEN_VALUE)
            val newValue = 100
            runBlocking {
                runSimulation(
                    testName = testName,
                    aggregateExpression = {
                        branch(
                            constant(true),
                            AggregateExpression.fromStateFlow { thenBranch },
                            constant(ELSE_VALUE)
                        )
                    },
                    runAfter = { thenBranch.update { newValue } },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe newValue
                    }
                )
            }
        }
    }

    "The neighbor construct" - {
        "should collect values from aligned neighbors" {
            val testName = this.testScope.testCase.name.testName
            val simpleValue = 100
            runBlocking {
                runSimulation(
                    testName = testName,
                    aggregateExpression = {
                        neighbor(
                            branch(
                                selfID().map { it < 2 },
                                selfID(),
                                constant(simpleValue)
                            )
                        )
                    },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe
                                (0..<N_DEVICES).associateWith { if (it < 2) it else simpleValue }
                    }
                )
            }
        }

        "should react to changes in the neighborhood state" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                runSimulation(
                    testName = testName,
                    aggregateExpression = { neighbor(sense<Int>(LOCAL_SENSOR)) },
                    runAfter = { contexts ->
                        contexts.forEach {
                            it.updateLocalSensor(
                                LOCAL_SENSOR,
                                LOCAL_SENSOR_VALUE + 10
                            )
                        }
                    },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe
                                (0..<N_DEVICES).associateWith { LOCAL_SENSOR_VALUE + 10 }
                    }
                )
            }
        }
    }

    "The loop construct" - {
        "should react to updates in its past state" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                runSimulation(
                    testName = testName,
                    aggregateExpression = {
                        loop(0) { value ->
                            combine(value, sense<Int>(LOCAL_SENSOR)) { x, y ->
                                x + y
                            }
                        }
                    },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root as Int % LOCAL_SENSOR_VALUE shouldBe 0
                    }
                )
            }
        }

        "should react to updates in dependencies in the looping function" {
            val testName = this.testScope.testCase.name.testName
            val flow = MutableStateFlow(12)
            val newValue = 5
            runBlocking {
                runSimulation(
                    testName = testName,
                    aggregateExpression = {
                        loop(0) { value -> combine(value, AggregateExpression.fromStateFlow { flow }) { _, y -> y } }
                    },
                    runAfter = { flow.update { newValue } },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe newValue
                    }
                )
            }
        }
    }

    "The sense construct" - {
        "should evaluate to the initial sensor value" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                runSimulation(
                    testName = testName,
                    aggregateExpression = { sense<Int>(LOCAL_SENSOR) },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe LOCAL_SENSOR_VALUE
                    }
                )
            }
        }

        "should react to sensor changes" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                runSimulation(
                    testName = testName,
                    aggregateExpression = { sense<Int>(LOCAL_SENSOR) },
                    runAfter = { contexts ->
                        contexts.forEach {
                            it.updateLocalSensor(
                                LOCAL_SENSOR,
                                LOCAL_SENSOR_VALUE + 10
                            )
                        }
                    },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe LOCAL_SENSOR_VALUE + 10
                    }
                )
            }
        }
    }

    "The mux construct" - {

        suspend fun computeMux(condition: Boolean, testName: String) = coroutineScope {
            runSimulation(
                testName = testName,
                aggregateExpression = { mux(constant(condition), constant(THEN_VALUE), constant(ELSE_VALUE)) },
                assertions = { context ->
                    context.neighborsStates.value[context.selfID] shouldBe ExportTree(
                        if (condition) THEN_VALUE else ELSE_VALUE,
                        mapOf(
                            Condition to ExportTree(condition),
                            Then to ExportTree(THEN_VALUE),
                            Else to ExportTree(ELSE_VALUE)
                        )
                    )
                }
            )
        }

        "should include both branches when the condition is true" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                computeMux(true, testName)
            }
        }

        "should include both branches when the condition is false" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                computeMux(false, testName)
            }
        }
    }
})

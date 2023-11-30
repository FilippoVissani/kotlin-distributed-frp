package io.github.filippovissani.dfrp

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.Condition
import io.github.filippovissani.dfrp.core.Context
import io.github.filippovissani.dfrp.core.Else
import io.github.filippovissani.dfrp.core.Execution.aggregate
import io.github.filippovissani.dfrp.core.ExportTree
import io.github.filippovissani.dfrp.core.Then
import io.github.filippovissani.dfrp.core.extensions.combine
import io.github.filippovissani.dfrp.core.extensions.map
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SemanticsSpec : FreeSpec({
    val localSensor = "sensor"
    val localSensorValue = 150
    val initialSensorsValues = mapOf(localSensor to localSensorValue)
    val nDevices = 4
    val thenValue = 200
    val elseValue = 300
    val delay: Long = 200
    val logger = KotlinLogging.logger {}

    suspend fun <T> computeResult(
        testName: String,
        aggregateExpression: Context.() -> AggregateExpression<T>,
        runAfter: (List<Context>) -> Unit = {},
        assertions: (contexts: Context) -> Unit = {},
    ) = coroutineScope {
        logger.info { testName }
        val contexts: List<Context> = (0..<nDevices).map { Context(it, initialSensorsValues) }
        val exports = aggregate(contexts, aggregateExpression)
        val exportsJobs = exports.withIndex().map { export ->
            contexts.map { deviceID ->
                launch(Dispatchers.Default) {
                    export.value.collect {
                        logger.debug { "(${export.index} -> ${it.root})" }
                        contexts[deviceID.selfID].receiveExport(export.index, it)
                    }
                }
            }
        }.flatten()
        delay(delay)
        runAfter(contexts)
        delay(delay)
        exportsJobs.forEach { it.cancelAndJoin() }
        logger.info { "#################################" }
        contexts.forEach { assertions(it) }
    }

    "The constant construct" - {
        "should be a constant flow with the given value" {
            val testName = this.testScope.testCase.name.testName
            val simpleValue = 100
            runBlocking {
                computeResult(
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
                computeResult(
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
                computeResult(
                    testName = testName,
                    aggregateExpression = { branch(constant(true), constant(thenValue), constant(elseValue)) },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID] shouldBe (ExportTree(
                            thenValue,
                            mapOf(Condition to ExportTree(true), Then to ExportTree(thenValue))
                        ))
                    }
                )
            }
        }

        "should include only the 'else' branch when the condition is false" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                computeResult(
                    testName = testName,
                    aggregateExpression = { branch(constant(false), constant(thenValue), constant(elseValue)) },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID] shouldBe (ExportTree(
                            elseValue,
                            mapOf(Condition to ExportTree(false), Else to ExportTree(elseValue))
                        ))
                    }
                )
            }
        }

        "should react to changes in the condition" {
            val testName = this.testScope.testCase.name.testName
            val condition = MutableStateFlow(true)
            runBlocking {
                computeResult(
                    testName = testName,
                    aggregateExpression = {
                        branch(
                            AggregateExpression.fromStateFlow { condition },
                            constant(thenValue),
                            constant(elseValue)
                        )
                    },
                    runAfter = { condition.update { false } },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe elseValue
                    }
                )
            }
        }

        "should react to changes in the selected branch" {
            val testName = this.testScope.testCase.name.testName
            val thenBranch = MutableStateFlow(thenValue)
            val newValue = 100
            runBlocking {
                computeResult(
                    testName = testName,
                    aggregateExpression = {
                        branch(
                            constant(true),
                            AggregateExpression.fromStateFlow { thenBranch },
                            constant(elseValue)
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
                computeResult(
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
                                (0..<nDevices).associateWith { if (it < 2) it else simpleValue }
                    }
                )
            }
        }

        "should react to changes in the neighborhood state" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                computeResult(
                    testName = testName,
                    aggregateExpression = { neighbor(sense<Int>(localSensor)) },
                    runAfter = { contexts ->
                        contexts.forEach {
                            it.updateLocalSensor(
                                localSensor,
                                localSensorValue + 10
                            )
                        }
                    },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe
                                (0..<nDevices).associateWith { localSensorValue + 10 }
                    }
                )
            }
        }
    }

    "The loop construct" - {
        "should react to updates in its past state" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                computeResult(
                    testName = testName,
                    aggregateExpression = {
                        loop(0) { value ->
                            combine(value, sense<Int>(localSensor)) { x, y ->
                                x + y
                            }
                        }
                    },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root as Int % localSensorValue shouldBe 0
                    }
                )
            }
        }

        "should react to updates in dependencies in the looping function" {
            val testName = this.testScope.testCase.name.testName
            val flow = MutableStateFlow(12)
            val newValue = 5
            runBlocking {
                computeResult(
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
                computeResult(
                    testName = testName,
                    aggregateExpression = { sense<Int>(localSensor) },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe localSensorValue
                    }
                )
            }
        }

        "should react to sensor changes" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                computeResult(
                    testName = testName,
                    aggregateExpression = { sense<Int>(localSensor) },
                    runAfter = { contexts ->
                        contexts.forEach {
                            it.updateLocalSensor(
                                localSensor,
                                localSensorValue + 10
                            )
                        }
                    },
                    assertions = { context ->
                        context.neighborsStates.value[context.selfID]?.root shouldBe localSensorValue + 10
                    }
                )
            }
        }
    }

    "The mux construct" - {

        suspend fun computeMux(condition: Boolean, testName: String) = coroutineScope {
            computeResult(
                testName = testName,
                aggregateExpression = { mux(constant(condition), constant(thenValue), constant(elseValue)) },
                assertions = { context ->
                    context.neighborsStates.value[context.selfID] shouldBe ExportTree(
                        if (condition) thenValue else elseValue,
                        mapOf(
                            Condition to ExportTree(condition),
                            Then to ExportTree(thenValue),
                            Else to ExportTree(elseValue)
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

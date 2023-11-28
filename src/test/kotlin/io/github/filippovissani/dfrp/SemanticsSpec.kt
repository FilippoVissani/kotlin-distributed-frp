package io.github.filippovissani.dfrp

import io.github.filippovissani.dfrp.core.Context
import io.github.filippovissani.dfrp.core.Slot
import io.github.filippovissani.dfrp.core.aggregate
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FreeSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SemanticsSpec : FreeSpec({

    val logger = KotlinLogging.logger {}
    val nDevices = 4
    val delay: Long = 200
    val intValue = 100
    val thenValue = 300
    val elseValue = 400

    suspend fun <T> computeResult(
        testName: String,
        aggregateExpression: suspend Context.() -> StateFlow<T>,
        runAfter: () -> Unit = {},
        assertions: (contexts: Context) -> Unit = {}
    ) =
        coroutineScope {
            logger.info { testName }
            val contexts = (0..<nDevices).map { Context(it) }
            contexts.forEach { it.neighbors = contexts.toSet() }
            val aggregateJob = launch(Dispatchers.Default) {
                aggregate(contexts) {
                    aggregateExpression()
                }
            }
            val exportsJobs = contexts.map { context ->
                launch(Dispatchers.Default) {
                    context.selfExports.collect { export ->
                        logger.info { "${context.selfID} -> $export" }
                    }
                }
            }
            delay(delay)
            runAfter()
            delay(delay)
            aggregateJob.cancelAndJoin()
            exportsJobs.forEach { it.cancelAndJoin() }
            logger.info { "#################################" }
            contexts.forEach { assertions(it) }
        }

    "The selfID construct" - {
        "Should be a constant flow with the device ID" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                computeResult(
                    testName = testName,
                    aggregateExpression = { selfID() },
                    assertions = { context ->
                        assertEquals(context.selfID, context.selfExports.value[emptyList()])
                    }
                )
            }
        }
    }

    "The constant construct" - {
        "Should be a constant flow with the given value" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                computeResult(
                    testName = testName,
                    aggregateExpression = { constant(intValue) },
                    assertions = { context ->
                        assertEquals(intValue, context.selfExports.value[emptyList()])
                    }
                )
            }
        }
    }

    "The neighbor construct" - {
        "Should collect values from aligned neighbors" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                computeResult(
                    testName = testName,
                    aggregateExpression = { neighbor { selfID() } },
                    assertions = { context ->
                        assertEquals(
                            context.neighbors.associate { it.selfID to it.selfID },
                            context.selfExports.value[emptyList()]
                        )
                    }
                )
            }
        }

        "Should react to changes in the neighborhood state" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                val reactiveValue = MutableStateFlow(intValue)
                val newValue = 200
                computeResult(
                    testName = testName,
                    { neighbor { reactiveValue.asStateFlow() } },
                    { reactiveValue.update { newValue } },
                    { context ->
                        assertEquals(
                            context.neighbors.associate { it.selfID to newValue },
                            context.selfExports.value[emptyList()]
                        )
                    }
                )
            }
        }
    }

    "The mux construct" - {
        "Should include both branches when the condition is true" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                computeResult(
                    testName = testName,
                    aggregateExpression = { mux(
                        { constant(true) },
                        { constant(thenValue) },
                        { constant(elseValue) }
                    ) },
                    assertions = { context ->
                        assertTrue { context.selfExports.value.containsKey(listOf(Slot.Condition)) }
                        assertTrue { context.selfExports.value.containsKey(listOf(Slot.Then)) }
                        assertTrue { context.selfExports.value.containsKey(listOf(Slot.Else)) }
                        assertEquals(
                            thenValue,
                            context.selfExports.value[emptyList()]
                        )
                    }
                )
            }
        }

        "Should include both branches when the condition is false" {
            val testName = this.testScope.testCase.name.testName
            runBlocking {
                computeResult(
                    testName = testName,
                    aggregateExpression = { mux(
                        { constant(false) },
                        { constant(thenValue) },
                        { constant(elseValue) }
                    ) },
                    assertions = { context ->
                        assertTrue { context.selfExports.value.containsKey(listOf(Slot.Condition)) }
                        assertTrue { context.selfExports.value.containsKey(listOf(Slot.Then)) }
                        assertTrue { context.selfExports.value.containsKey(listOf(Slot.Else)) }
                        assertEquals(
                            elseValue,
                            context.selfExports.value[emptyList()]
                        )
                    }
                )
            }
        }

        "Complex expressions" - {
            "Complicated expression 1" {
                val testName = this.testScope.testCase.name.testName
                runBlocking {
                    computeResult(
                        testName = testName,
                        aggregateExpression = {
                            mux(
                                { constant(selfID < 2) },
                                { neighbor { constant(thenValue) } },
                                { neighbor { constant(elseValue) } }
                            )
                    })
                }
            }

            "Complex expression 2" {
                val testName = this.testScope.testCase.name.testName
                runBlocking {
                    computeResult(
                        testName = testName,
                        aggregateExpression = {
                            neighbor {
                                mux(
                                    { constant(selfID < 2) },
                                    { constant(thenValue) },
                                    { constant(elseValue) }
                                )
                            }
                        })
                }
            }
        }
    }
})

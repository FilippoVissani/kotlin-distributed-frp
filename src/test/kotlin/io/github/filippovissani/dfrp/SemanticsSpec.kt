package io.github.filippovissani.dfrp

import io.github.filippovissani.dfrp.core.Context
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

class SemanticsSpec : FreeSpec({

    val logger = KotlinLogging.logger {}

    suspend fun <T> computeResult(aggregateExpression: suspend Context.() -> StateFlow<T>, runAfter: () -> Unit = {}) =
        coroutineScope {
            val contexts = (0..3).map { Context(it) }
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
            runAfter()
            delay(200)
            aggregateJob.cancelAndJoin()
            exportsJobs.forEach { it.cancelAndJoin() }
            logger.info { "#################################" }
        }

    "The selfID construct" - {
        "Should be a constant flow with the device ID" {
            runBlocking {
                logger.info { "Should be a constant flow with the device ID" }
                computeResult({ selfID() })
            }
        }
    }

    "The constant construct" - {
        "Should be a constant flow with the given value" {
            runBlocking {
                logger.info { "Should be a constant flow with the given value" }
                computeResult({ constant(100) })
            }
        }
    }

    "The neighbor construct" - {
        "Should collect values from aligned neighbors" {
            runBlocking {
                logger.info { "Should collect values from aligned neighbors" }
                computeResult({ neighbor(selfID()) })
            }
        }

        "Should react to changes in the neighborhood state" {
            runBlocking {
                logger.info { "Should react to changes in the neighborhood state" }
                val reactiveValue = MutableStateFlow(100)
                computeResult({ neighbor(reactiveValue.asStateFlow()) }, { reactiveValue.update { 200 } })
            }
        }
    }

    "The mux construct" - {
        "Should include both branches when the condition is true" {
            runBlocking {
                logger.info { "Should include both branches when the condition is true" }
                computeResult({ mux(constant(true), constant(0), constant(1)) })
            }
        }
    }

    "The mux construct" - {
        "Should include both branches when the condition is true" {
            runBlocking {
                logger.info { "Should include both branches when the condition is false" }
                computeResult({ mux(constant(false), constant(0), constant(1)) })
            }
        }
    }
})

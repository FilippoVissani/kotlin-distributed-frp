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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SemanticsSpec : FreeSpec({

    val logger = KotlinLogging.logger {}

    suspend fun <T> computeResult(aggregateExpression: suspend Context.() -> StateFlow<T>) = coroutineScope {
        val contexts = (0..3).map { Context(it) }
        contexts.forEach { it.neighbors = contexts.toSet() }
        val aggregateJob = launch(Dispatchers.Default) {
            aggregate(contexts) {
                aggregateExpression()
            }
        }
        logger.info { "#################################" }
        val exportsJobs = contexts.map { context ->
            launch(Dispatchers.Default) {
                context.selfExports.collect { export ->
                    logger.info { "${context.selfID} -> $export" }
                }
            }
        }
        delay(200)
        aggregateJob.cancelAndJoin()
        exportsJobs.forEach { it.cancelAndJoin() }
    }

    "The selfID construct" - {
        "Should be a constant flow with the device ID" {
            runBlocking {
                computeResult {
                    selfID()
                }
            }
        }
    }

    "The constant construct" - {
        "Should be a constant flow with the given value" {
            runBlocking {
                computeResult {
                    constant(100)
                }
            }
        }
    }

    "The neighbor construct" - {
        "Should collect values from aligned neighbors" {
            runBlocking {
                computeResult {
                    neighbor(selfID())
                }
            }
        }
    }

    "The mux construct" - {
        "Should include both branches when the condition is true" {
            runBlocking {
                computeResult {
                    mux(constant(true), constant(0), constant(1))
                }
            }
        }
    }
})

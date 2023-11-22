package io.github.filippovissani.dfrp

import io.github.filippovissani.dfrp.core.Context
import io.github.filippovissani.dfrp.core.aggregate
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FreeSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SemanticsSpec : FreeSpec({

    val logger = KotlinLogging.logger {}

    "The selfID construct" - {
        "Should be a constant flow with the device ID" {
            runBlocking {
                val contexts = (0..3).map { Context(it) }
                contexts.forEach { it.neighbors.update { contexts.toSet() } }
                aggregate(contexts) {
                    selfID()
                }
                contexts.forEach { context ->
                    logger.info { "${context.selfID} -> ${context.selfExports.value}" }
                }
            }
        }
    }

    "The constant construct" - {
        "Should be a constant flow with the given value" {
            runBlocking {
                val contexts = (0..3).map { Context(it) }
                contexts.forEach { it.neighbors.update { contexts.toSet() } }
                aggregate(contexts) {
                    constant(100)
                }
                contexts.forEach { context ->
                    logger.info { "${context.selfID} -> ${context.selfExports.value}" }
                }
            }
        }
    }

    "The neighbor construct" - {
        "Should collect values from aligned neighbors" {
            runBlocking {
                val contexts = (0..3).map { Context(it) }
                contexts.forEach { it.neighbors.update { contexts.toSet() } }
                val aggregateJob = launch(Dispatchers.Default) {
                    aggregate(contexts) {
                        neighbor(selfID())
                    }
                }
                val exportsJobs = contexts.map { context ->
                    launch(Dispatchers.Default) {
                        context.selfExports.collect { export ->
                            logger.info { "${context.selfID} -> $export" }
                        }
                    }
                }
                delay(1000)
                aggregateJob.cancelAndJoin()
                exportsJobs.forEach { it.cancelAndJoin() }
            }
        }
    }
})

package io.github.filippovissani.dfrp

import io.github.filippovissani.dfrp.core.Context
import io.github.filippovissani.dfrp.core.DeviceID
import io.github.filippovissani.dfrp.core.aggregate
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FreeSpec
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class SemanticsSpec : FreeSpec({
    "The selfID construct" - {
        "Should be a constant flow with the device ID" {
            runBlocking {
                val contexts = (0..3).map { Context(it) }
                contexts.forEach { it.neighbors.update { contexts.toSet() } }
                aggregate(contexts){
                    selfID()
                }
                contexts.forEach { context ->
                    context.selfExports.onEach { export ->
                        println("${context.selfID} -> ${export[emptyList()]?.value}")
                    }.launchIn(this)
                }
            }
        }
    }

    "The constant construct" - {
        "Should be a constant flow with the given value" {
            runBlocking {
                val contexts = (0..3).map { Context(it) }
                contexts.forEach { it.neighbors.update { contexts.toSet() } }
                aggregate(contexts){
                    constant(0)
                }
                contexts.forEach { context ->
                    context.selfExports.onEach { export ->
                        println("${context.selfID} -> ${export[emptyList()]?.value}")
                    }.launchIn(this)
                }
            }
        }
    }

    "The neighbor construct" - {
        "Should collect values from aligned neighbors" {
            runBlocking {
                val contexts = (0..3).map { Context(it) }
                contexts.forEach { it.neighbors.update { contexts.toSet() } }
                aggregate(contexts){
                    neighbor(selfID())
                }
                contexts.forEach { context ->
                    context.selfExports.onEach { export ->
                        (export[emptyList()]?.value as Map<DeviceID, SharedFlow<DeviceID>>)
                            .entries.forEach { (k, v) ->
                                v.onEach {
                                    println("${context.selfID} -> ($k, $it)")
                                }.launchIn(this)
                            }
                    }.launchIn(this)
                }
            }
        }
    }
})

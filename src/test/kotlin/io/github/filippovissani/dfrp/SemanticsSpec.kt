package io.github.filippovissani.dfrp

import io.github.filippovissani.dfrp.core.Context
import io.github.filippovissani.dfrp.core.aggregate
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FreeSpec
import kotlinx.coroutines.flow.MutableStateFlow

class SemanticsSpec : FreeSpec({

    "The selfID construct" - {
        "Should be a constant flow with the device ID" {
            runBlocking {
                val contexts = (0..3).map { Context(it) }
                contexts.forEach { it.neighbors.addAll(contexts) }
                aggregate(contexts){
                    selfID()
                }
                println(contexts.map { it.selfExport[emptyList()]?.value })
            }
        }
    }

    "The mux construct" - {
        "Should" {
            runBlocking {
                val contexts = (0..3).map { Context(it) }
                contexts.forEach { it.neighbors.addAll(contexts) }
                aggregate(contexts){
                    mux(MutableStateFlow(selfID().value < 2), selfID(), constant(100))
                }
                val result = contexts.map { it.selfExport[emptyList()]?.value }.toList()
                println(result)
            }
        }
    }
})

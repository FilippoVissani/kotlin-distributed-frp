package io.github.filippovissani.kotlin_distributed_frp

import io.github.filippovissani.kotlin_distributed_dfrp.Context
import io.github.filippovissani.kotlin_distributed_dfrp.DeviceID
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics
import io.github.filippovissani.kotlin_distributed_dfrp.Semantics.selfID
import io.kotest.common.runBlocking
import kotlinx.coroutines.flow.onCompletion
import kotlin.test.*

class SemanticsSpec {
    @Test
    fun constant(){
        runBlocking {
            val value = 10
            val context = Context(DeviceID(1))
            Semantics
                .constant(value)
                .run(emptyList(), context)
                .onCompletion { cause -> println("Flow completed with $cause") }
                .collect{ export ->
                    println("######")
                    println(export)
                    println("######")
                }
        }
    }

    @Test
    fun selfIDTest(){
        runBlocking {
            val context = Context(DeviceID(1))
            selfID().run(emptyList(), context).collect{export ->
                println("######")
                println(export)
                println("######")
            }
        }
    }
}

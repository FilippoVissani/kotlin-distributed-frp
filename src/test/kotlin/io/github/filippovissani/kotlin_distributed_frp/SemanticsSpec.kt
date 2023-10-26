package io.github.filippovissani.kotlin_distributed_frp

import io.github.filippovissani.kotlin_distributed_dfrp.*
import io.kotest.common.runBlocking
import kotlin.test.*

class SemanticsSpec {

    private val selfID = DeviceID(1)
    private val context = Context(selfID)

    @Test
    fun constant(){
        runBlocking {
            val value = 10
            Semantics
                .constant(value)
                .run(emptyList(), context)
                .collect{ export ->
                    assertEquals(export, ExportTree(value))
                }
        }
    }

    @Test
    fun selfID(){
        runBlocking {
            Semantics
                .selfID()
                .run(emptyList(), context)
                .collect{export ->
                    assertEquals(export, ExportTree(selfID))
                }
        }
    }
}

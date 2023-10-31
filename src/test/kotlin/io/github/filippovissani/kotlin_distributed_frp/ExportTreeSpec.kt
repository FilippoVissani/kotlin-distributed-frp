package io.github.filippovissani.kotlin_distributed_frp

import io.github.filippovissani.kotlin_distributed_dfrp.ExportTree
import io.github.filippovissani.kotlin_distributed_dfrp.Key
import io.github.filippovissani.kotlin_distributed_dfrp.Slot
import kotlin.test.*

class ExportTreeSpec {
    @Test
    fun root(){
        assertEquals(ExportTree(1).root, 1)
    }

    @Test
    fun children(){
        val children: List<Pair<Slot, ExportTree<*>>> = listOf(
            Pair(Key("a"), ExportTree(10)),
            Pair(Key("b"), ExportTree(20)),
        )
        assertEquals(ExportTree(1, children).children, children)
    }

    @Test
    fun traversable(){
        val exp = ExportTree(1)
        assertEquals(exp.followPath(emptyList()), exp)
    }
}

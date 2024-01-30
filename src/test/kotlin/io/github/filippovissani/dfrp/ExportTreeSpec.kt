package io.github.filippovissani.dfrp

import io.github.filippovissani.dfrp.core.Condition
import io.github.filippovissani.dfrp.core.Else
import io.github.filippovissani.dfrp.core.ExportTree
import io.github.filippovissani.dfrp.core.Key
import io.github.filippovissani.dfrp.core.Neighbor
import io.github.filippovissani.dfrp.core.Slot
import io.github.filippovissani.dfrp.core.Then
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExportTreeSpec {
    @Test
    fun root() {
        assertEquals(ExportTree(1).root, 1)
    }

    @Test
    fun children() {
        val children: Map<Slot, ExportTree<*>> = mapOf(
            Pair(Key("a"), ExportTree(10)),
            Pair(Key("b"), ExportTree(20))
        )
        assertEquals(ExportTree(1, children).children, children)
    }

    @Test
    fun traversable() {
        val exp = ExportTree(1)
        assertEquals(exp.followPath(emptyList()), exp)
    }

    @Test
    fun branchNotAlignedWithTrue() {
        val exp = ExportTree(
            true,
            mapOf(
                Condition to ExportTree(true),
                Then to ExportTree(true),
            ),
        )
        assertEquals(exp.followPath(listOf(Else)), null)
    }

    @Test
    fun branchNotAlignedWithFalse() {
        val exp = ExportTree(
            true,
            mapOf(
                Condition to ExportTree(false),
                Else to ExportTree(false),
            ),
        )
        assertEquals(exp.followPath(listOf(Then)), null)
    }
}

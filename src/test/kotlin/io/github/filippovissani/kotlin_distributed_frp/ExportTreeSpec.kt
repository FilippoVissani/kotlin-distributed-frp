package io.github.filippovissani.kotlin_distributed_frp

import io.github.filippovissani.ExportTree
import io.github.filippovissani.Key
import io.github.filippovissani.Slot
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.be
import io.kotest.matchers.should

class ExportTreeSpec : FreeSpec({
    "An export tree" - {
        "should have the given root" {
            ExportTree(1).root should be (1)
        }

        "should have the given children" {
            val children: Sequence<Pair<Slot, ExportTree<*>>> = sequenceOf(
                Pair(Key("a"), ExportTree(10)),
                Pair(Key("b"), ExportTree(20)),
            )
            ExportTree(1, children).children should be (children.toMap())
        }
    }
})

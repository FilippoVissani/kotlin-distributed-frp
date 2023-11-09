package io.github.filippovissani.dfrp.core.impl

import io.github.filippovissani.dfrp.core.ExportTree
import io.github.filippovissani.dfrp.core.Slot

data class ExportTreeImpl<T>(override val root: T, override val children: Map<Slot, ExportTree<*>>) : ExportTree<T> {
    private val indentAmount = "  "

    override fun followPath(path: List<Slot>): ExportTree<*>? = when {
        path.isNotEmpty() -> children[path.first()]?.followPath(path.drop(1))
        else -> this
    }

    override fun toString(): String {
        val sb = StringBuilder()
        format("", sb)
        return sb.toString()
    }

    private fun format(indent: String, sb: StringBuilder){
        sb.append("[").append(root).append("]\n")
        if (children.toMap().isNotEmpty()) {
            sb.append(indent).append("{\n")
            children.toMap().forEach { (k, v) ->
                sb.append(indent).append(indentAmount).append(k).append(" => ")
                (v as ExportTreeImpl).format(indent + indentAmount, sb)
            }
            sb.append(indent).append("}\n")
        }
    }
}
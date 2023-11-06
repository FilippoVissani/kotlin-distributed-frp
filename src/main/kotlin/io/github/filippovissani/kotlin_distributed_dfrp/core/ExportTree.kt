package io.github.filippovissani.kotlin_distributed_dfrp.core

data class ExportTree<T>(val root: T, val children: Map<Slot, ExportTree<*>> = emptyMap()) {
    private val indentAmount = "  "

    fun followPath(path: List<Slot>): ExportTree<*>? = when {
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
                v.format(indent + indentAmount, sb)
            }
            sb.append(indent).append("}\n")
        }
    }
}

fun <T, R> ExportTree<T>.map(transform: (T) -> R): ExportTree<R> {
    return ExportTree(transform(root), this.children)
}

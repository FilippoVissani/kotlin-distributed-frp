package io.github.filippovissani.kotlin_distributed_dfrp

data class ExportTree<T>(val root: T, val children: List<Pair<Slot, ExportTree<*>>> = emptyList()) {
    private val indentAmount = "  "

    fun followPath(path: List<Slot>): ExportTree<*>? = when {
            path.isNotEmpty() -> followPath(path.drop(1))
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
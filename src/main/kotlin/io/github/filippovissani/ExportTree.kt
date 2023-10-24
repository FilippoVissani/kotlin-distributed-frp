package io.github.filippovissani

interface ExportTree<T> {
    fun followPath(path: List<Slot>): ExportTree<*>?

    companion object {
        operator fun <T> invoke(root: T, children: Map<Slot, ExportTree<*>>): ExportTree<T> = ExportTreeImpl(root, children)
    }
}

internal class ExportTreeImpl<T>(val root: T, val children: Map<Slot, ExportTree<*>>) : ExportTree<T>{
    override fun followPath(path: List<Slot>): ExportTree<*>? = when {
            path.isNotEmpty() -> children[path.first()]?.followPath(path.drop(1))
            else -> this
        }
}
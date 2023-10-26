package io.github.filippovissani.kotlin_distributed_dfrp

interface ExportTree<T> {
    val root: T

    val children: Map<Slot, ExportTree<*>>
    fun followPath(path: List<Slot>): ExportTree<*>?

    companion object {
        operator fun <T> invoke(root: T, children: Sequence<Pair<Slot, ExportTree<*>>> = emptySequence()): ExportTree<T> = ExportTreeImpl(root, children.toMap())
    }
}

internal data class ExportTreeImpl<T>(override val root: T, override val children: Map<Slot, ExportTree<*>>) :
    ExportTree<T> {
    override fun followPath(path: List<Slot>): ExportTree<*>? = when {
            path.isNotEmpty() -> children[path.first()]?.followPath(path.drop(1))
            else -> this
        }
}
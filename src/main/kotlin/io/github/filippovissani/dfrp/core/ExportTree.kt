package io.github.filippovissani.dfrp.core

import io.github.filippovissani.dfrp.core.impl.ExportTreeImpl

interface ExportTree<T>{
    val root: T

    val children: Map<Slot, ExportTree<*>>
    fun followPath(path: List<Slot>): ExportTree<*>?

    companion object{
        operator fun <T> invoke(root: T, children: Map<Slot, ExportTree<*>> = emptyMap()): ExportTree<T>{
            return ExportTreeImpl(root, children)
        }
    }
}

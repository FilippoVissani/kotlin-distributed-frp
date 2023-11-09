package io.github.filippovissani.dfrp.core.extensions

import io.github.filippovissani.dfrp.core.ExportTree

fun <T, R> ExportTree<T>.map(transform: (T) -> R): ExportTree<R> {
    return ExportTree(transform(root), this.children)
}

package io.github.filippovissani.dfrp.core.impl

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.Export
import io.github.filippovissani.dfrp.core.Path
import kotlinx.coroutines.flow.StateFlow

internal class AggregateExpressionImpl<T>(private val f: (Path) -> StateFlow<Export<T>>) : AggregateExpression<T> {

    override fun compute(path: Path): StateFlow<Export<T>> {
        return f(path)
    }
}
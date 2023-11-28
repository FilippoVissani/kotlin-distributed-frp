package io.github.filippovissani.dfrp.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

interface AggregateExpression<T> {
    fun compute(path: Path): Flow<Export<T>>

    companion object {
        operator fun <T> invoke(f: (Path) -> Flow<Export<T>>): AggregateExpression<T> {
            return AggregateExpressionImpl(f)
        }

        fun <T> fromFlow(flow: () -> Flow<T>): AggregateExpression<T> {
            return AggregateExpression { _ -> flow().map { ExportTree(it) } }
        }

        fun <T> constant(value: () -> T): AggregateExpression<T> {
            return fromFlow { flowOf(value()) }
        }
    }
}

internal class AggregateExpressionImpl<T>(private val f: (Path) -> Flow<Export<T>>) : AggregateExpression<T> {

    override fun compute(path: Path): Flow<Export<T>> {
        return f(path).transform { emit(it) }
    }
}

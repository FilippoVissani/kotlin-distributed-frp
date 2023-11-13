package io.github.filippovissani.dfrp.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

interface AggregateExpression<T> {
    fun compute(path: Path, context: Context): Flow<Export<T>>

    companion object {
        operator fun <T> invoke(f: (Context, Path) -> Flow<Export<T>>): AggregateExpression<T> {
            return AggregateExpressionImpl(f)
        }

        fun <T> fromFlow(flow: (Context) -> Flow<T>): AggregateExpression<T> {
            return AggregateExpression { context, _ -> flow(context).map { ExportTree(it) } }
        }

        fun <T> constant(value: (Context) -> T): AggregateExpression<T> {
            return fromFlow { context -> flowOf(value(context)) }
        }
    }
}

internal class AggregateExpressionImpl<T>(private val f: (Context, Path) -> Flow<Export<T>>) : AggregateExpression<T> {

    override fun compute(path: Path, context: Context): Flow<Export<T>> {
        return f(context, path).transform { emit(it) }
    }
}

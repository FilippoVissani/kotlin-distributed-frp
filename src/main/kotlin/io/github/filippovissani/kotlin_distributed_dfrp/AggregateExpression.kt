package io.github.filippovissani.kotlin_distributed_dfrp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

interface AggregateExpression<T>{
    fun compute(path: Path, context: Context): Flow<Export<T>>

    companion object{
        fun <T> of(f: (Context, Path) -> Flow<Export<T>>): AggregateExpression<T> {
            return object : AggregateExpression<T> {
                override fun compute(path: Path, context: Context): Flow<Export<T>> {
                    return f(context, path).transform { export -> emit(export) }
                }
            }
        }

        fun <T> fromFlow(flow: (Context) -> Flow<T>): AggregateExpression<T> {
            return of { context, _ -> flow(context).map { ExportTree(it) } }
        }

        fun <T> constant(value: (Context) -> T): AggregateExpression<T> {
            return fromFlow { context -> flowOf(value(context)) }
        }
    }
}

fun <T, R> AggregateExpression<T>.map(transform: (T) -> R): AggregateExpression<R> {
    return AggregateExpression.of { context, path ->
        this.compute(path, context).map { export -> export.map(transform) }
    }
}
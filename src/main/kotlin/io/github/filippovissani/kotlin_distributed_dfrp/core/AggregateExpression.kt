package io.github.filippovissani.kotlin_distributed_dfrp.core

import kotlinx.coroutines.flow.*

interface AggregateExpression<T>{
    fun compute(path: Path, context: Context): Flow<Export<T>>

    companion object{
        fun <T> of(f: (Context, Path) -> Flow<Export<T>>): AggregateExpression<T> {
            return object : AggregateExpression<T> {
                override fun compute(path: Path, context: Context): Flow<Export<T>> {
                    return f(context, path).transform { emit(it) }
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

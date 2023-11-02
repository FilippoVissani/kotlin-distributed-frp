package io.github.filippovissani.kotlin_distributed_dfrp

import kotlinx.coroutines.flow.*

typealias DeviceID = Int
typealias Path = List<Slot>
typealias Export<T> = ExportTree<T>
typealias NeighbourField<T> = Map<DeviceID, T>

interface AggregateExpression<T>{
    fun run(path: Path, context: Context): Flow<Export<T>>

    companion object{
        fun <T> of(f: (Context, Path) -> Flow<Export<T>>): AggregateExpression<T> {
            return object : AggregateExpression<T> {
                override fun run(path: Path, context: Context): Flow<Export<T>> {
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

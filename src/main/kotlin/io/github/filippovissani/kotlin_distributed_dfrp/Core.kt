package io.github.filippovissani.kotlin_distributed_dfrp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

typealias DeviceID = ID
typealias Path = List<Slot>
typealias Export<T> = ExportTree<T>
typealias NeighbourField<T> = Map<DeviceID, T>

interface Computation<T>{
    fun run(path: Path, context: Context): Flow<Export<T>>

    companion object{
        fun <T> of(f: (Context, Path) -> Flow<Export<T>>): Computation<T> {
            return object : Computation<T> {
                override fun run(path: Path, context: Context): Flow<Export<T>> {
                    return f(context, path).transform { export ->
                        println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
                        emit(export)
                    }
                }
            }
        }

        fun <T> fromFlow(flow: (Context) -> Flow<T>): Computation<T> {
            return of { context, _ -> flow(context).map { ExportTree(it) } }
        }

        fun <T> constant(value: (Context) -> T): Computation<T> {
            return fromFlow { context -> flow { value(context) } }
        }
    }
}

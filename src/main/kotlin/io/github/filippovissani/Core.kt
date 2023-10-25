package io.github.filippovissani

import io.github.filippovissani.field.Field
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

typealias DeviceID = ID
typealias SensorID = String
typealias Path = List<Slot>
typealias Export<T> = ExportTree<T>
typealias NeighbourField<T> = Map<DeviceID, T>

interface Expression<T>{
    fun compute(path: Path, context: Context): Flow<Export<T>>

    companion object{
        fun <T> of(f: (Context, Path) -> Flow<Export<T>>): Expression<T> {
            return object : Expression<T>{
                override fun compute(path: Path, context: Context): Flow<Export<T>> {
                    return f(context, path)
                }
            }
        }

        fun <T> fromFlow(flow: (Context) -> Flow<T>): Expression<T> {
            return of { context, _ -> flow(context).map { ExportTree(it) } }
        }

        fun <T> constant(value: (Context) -> T): Expression<T> {
            return fromFlow { context -> flow { value(context) } }
        }
    }
}
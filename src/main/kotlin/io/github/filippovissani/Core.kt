package io.github.filippovissani

import io.github.filippovissani.field.Field
import kotlinx.coroutines.flow.Flow

typealias DeviceID = ID
typealias SensorID = String
typealias Path = List<Slot>
typealias Export<T> = ExportTree<T>
typealias NeighbourField<T> = Field<T>

fun <T> run(path: Path, context: Context): Flow<Export<T>> {
    TODO("Not yet implemented")
}
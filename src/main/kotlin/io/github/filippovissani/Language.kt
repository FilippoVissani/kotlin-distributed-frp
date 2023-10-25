package io.github.filippovissani

import kotlinx.coroutines.flow.Flow

interface Language {
    
    fun <T> constant(value: T): Flow<T>
    
    fun <T> neighbourSense(sensorID: SensorID): Expression<NeighbourField<T>>

    fun <T> neighbour(expression: Expression<T>): Expression<NeighbourField<T>>
    
    fun <T> branch(condition: Flow<Boolean>, th: Flow<T>, el: Flow<T>): Flow<T>
    
    fun <T> loop(initial: T, f: (Flow<T>) -> Flow<T>): Flow<T>
    fun selfID(context: Context): Flow<DeviceID>
    fun <T> sense(sensorID: SensorID, context: Context): Flow<T>
}
package io.github.filippovissani

import kotlinx.coroutines.flow.Flow

interface Language {
    fun selfID(): Flow<DeviceID>
    
    fun <T> constant(value: T): Flow<T>
    
    fun <T> sense(sensorID: SensorID): Flow<T>
    
    fun <T> neighbourSense(sensorID: SensorID): Flow<NeighbourField<T>>

    fun <T> neighbour(expression: Flow<T>): NeighbourField<T>
    
    fun <T> branch(condition: Flow<Boolean>, th: Flow<T>, el: Flow<T>): Flow<T>
    
    fun <T> loop(initial: T, f: (Flow<T>) -> Flow<T>): Flow<T>
}
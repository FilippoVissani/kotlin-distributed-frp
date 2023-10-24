package io.github.filippovissani

import kotlinx.coroutines.flow.Flow

interface Context {
    fun selfID(): DeviceID
    fun <T> sensor(sensorID: SensorID): Flow<T>
    fun neighbours(): Flow<Map<DeviceID, NeighbourState>>
}
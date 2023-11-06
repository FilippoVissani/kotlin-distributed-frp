package io.github.filippovissani.kotlin_distributed_dfrp.core

import io.github.filippovissani.kotlin_distributed_dfrp.core.impl.ContextImpl
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

interface Context {
    val selfID: DeviceID

    val neighbors: Flow<Map<DeviceID, Export<*>>>

    fun <T> sensor(sensorID: SensorID): Flow<T>

    fun receiveExport(neighborID: DeviceID, exported: Export<*>)

    fun <T> updateLocalSensor(sensorID: SensorID, newValue: T)

    companion object{
        operator fun invoke(selfID: DeviceID, sensors: Map<SensorID, *> = emptyMap<SensorID, Any>()): Context {
            return ContextImpl(selfID, sensors)
        }
    }
}

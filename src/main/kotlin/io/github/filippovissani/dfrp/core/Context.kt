package io.github.filippovissani.dfrp.core

import io.github.filippovissani.dfrp.core.impl.ContextImpl
import kotlinx.coroutines.flow.StateFlow

interface Context {
    val selfID: DeviceID
    val neighborsStates: StateFlow<Map<DeviceID, Export<*>>>
    val sensorsStates: StateFlow<Map<SensorID, *>>
    fun receiveExport(neighborID: DeviceID, exported: Export<*>)
    fun <T> updateLocalSensor(sensorID: SensorID, newValue: T)

    companion object {
        operator fun invoke(selfID: DeviceID, initialSensorsStates: Map<SensorID, *>): Context =
            ContextImpl(selfID, initialSensorsStates)
    }
}

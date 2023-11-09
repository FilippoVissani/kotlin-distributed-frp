package io.github.filippovissani.dfrp.core.impl

import io.github.filippovissani.dfrp.core.Context
import io.github.filippovissani.dfrp.core.DeviceID
import io.github.filippovissani.dfrp.core.Export
import io.github.filippovissani.dfrp.core.SensorID
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

internal class ContextImpl(override val selfID: DeviceID, sensors: Map<SensorID, *>) : Context {
    private val _neighborsStates = MutableStateFlow(emptyMap<DeviceID, Export<*>>())
    private val _sensorsStates = sensors.map { (k, v) -> k to MutableStateFlow(v) }.toMap()
    override val neighbors = _neighborsStates.asSharedFlow()

    override fun receiveExport(neighborID: DeviceID, exported: Export<*>) {
        runBlocking {
            _neighborsStates.update { it.plus(neighborID to exported) }
        }
    }

    override fun <T> updateLocalSensor(sensorID: SensorID, newValue: T) {
        runBlocking {
            _sensorsStates[sensorID]?.update { newValue }
        }
    }

    override fun <T> sensor(sensorID: SensorID): Flow<T> {
        return _sensorsStates[sensorID]?.asStateFlow() as Flow<T>
    }
}
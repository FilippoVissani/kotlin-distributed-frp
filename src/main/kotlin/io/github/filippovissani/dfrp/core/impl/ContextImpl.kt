package io.github.filippovissani.dfrp.core.impl

import io.github.filippovissani.dfrp.core.Context
import io.github.filippovissani.dfrp.core.DeviceID
import io.github.filippovissani.dfrp.core.Export
import io.github.filippovissani.dfrp.core.SensorID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class ContextImpl(override val selfID: DeviceID, sensors: Map<SensorID, *>) : Context {
    private val _neighborsStates = MutableStateFlow(emptyMap<DeviceID, Export<*>>())
    private val _sensorsStates = sensors.map { (k, v) -> k to MutableStateFlow(v) }.toMap()
    override val neighbors = _neighborsStates.asSharedFlow()

    override suspend fun receiveExport(neighborID: DeviceID, exported: Export<*>) {
        _neighborsStates.update { it.plus(neighborID to exported) }
    }

    override suspend fun <T> updateLocalSensor(sensorID: SensorID, newValue: T) {
        _sensorsStates[sensorID]?.update { newValue }
    }

    override fun <T> sense(sensorID: SensorID): Flow<T> {
        return _sensorsStates[sensorID]?.asStateFlow() as Flow<T>
    }
}

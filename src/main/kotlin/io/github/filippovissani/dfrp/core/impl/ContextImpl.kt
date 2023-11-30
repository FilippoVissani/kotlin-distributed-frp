package io.github.filippovissani.dfrp.core.impl

import io.github.filippovissani.dfrp.core.Context
import io.github.filippovissani.dfrp.core.DeviceID
import io.github.filippovissani.dfrp.core.Export
import io.github.filippovissani.dfrp.core.SensorID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ContextImpl(override val selfID: DeviceID, initialSensorsStates: Map<SensorID, *>) : Context {
    private val _neighborsStates = MutableStateFlow(emptyMap<DeviceID, Export<*>>())
    private val _sensorsStates = MutableStateFlow(initialSensorsStates)
    override val neighborsStates = _neighborsStates.asStateFlow()
    override val sensorsStates = _sensorsStates.asStateFlow()

    override fun receiveExport(neighborID: DeviceID, exported: Export<*>) {
        _neighborsStates.update { it.plus(neighborID to exported) }
    }

    override fun <T> updateLocalSensor(sensorID: SensorID, newValue: T) {
        _sensorsStates.update { it.plus(sensorID to newValue) }
    }
}

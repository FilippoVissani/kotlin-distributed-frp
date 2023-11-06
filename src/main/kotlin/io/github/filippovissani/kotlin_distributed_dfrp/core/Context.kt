package io.github.filippovissani.kotlin_distributed_dfrp.core

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

package io.github.filippovissani.kotlin_distributed_dfrp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking

interface Context {
    val selfID: DeviceID

    val neighboursStates: Flow<Map<DeviceID, NeighbourState>>

    fun receiveExport(neighborID: DeviceID, exported: Export<*>)

    companion object{
        operator fun invoke(selfID: DeviceID = DeviceID()
        ): Context{
            return ContextImpl(selfID)
        }
    }
}

internal class ContextImpl(override val selfID: DeviceID) : Context {
    private val _neighboursStates = MutableStateFlow(emptyMap<DeviceID, NeighbourState>())
    override val neighboursStates = _neighboursStates.asSharedFlow()

    override fun receiveExport(neighborID: DeviceID, exported: Export<*>) {
        runBlocking {
            _neighboursStates.emit(mapOf(neighborID to NeighbourState(neighborID, exported)))
        }
    }
}

package io.github.filippovissani.kotlin_distributed_dfrp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

interface Context {
    val selfID: DeviceID

    val neighboursStates: Flow<Map<DeviceID, NeighbourState>>

    fun receiveExport(neighborID: DeviceID, exported: Export<Any>): Context

    companion object{
        operator fun invoke(selfID: DeviceID = DeviceID(),
                            neighboursStates: Flow<Map<DeviceID, NeighbourState>> = emptyFlow(),
        ): Context{
            return ContextImpl(selfID, neighboursStates)
        }
    }
}

internal class ContextImpl(
    override val selfID: DeviceID,
    override val neighboursStates: Flow<Map<DeviceID, NeighbourState>>,
) : Context {
    override fun receiveExport(neighborID: DeviceID, exported: Export<Any>): Context {
        return Context(selfID, neighboursStates.map { it.plus(neighborID to NeighbourState(neighborID, exported)) })
    }
}

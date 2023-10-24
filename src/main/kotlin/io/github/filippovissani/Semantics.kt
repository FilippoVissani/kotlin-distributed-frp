package io.github.filippovissani

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface Semantics : Language {

    val context: Context

    fun <T> alignWithNeighbors(path: Path, extract: (Export<*>?, NeighbourState) -> T): Flow<Map<DeviceID, T>> {
        fun alignWith(neighbourID: DeviceID, neighbourState: NeighbourState): Pair<DeviceID, T> {
            val alignedExport = neighbourState.exported().followPath(path)
            return Pair(neighbourID, extract(alignedExport, neighbourState))
        }

        return context.neighbours().map { neighbours -> neighbours.map { alignWith(it.key, it.value) }.toMap() }
    }

    override fun selfID(): Flow<DeviceID> {
        return flow {emit(context.selfID())}
    }

    override fun <T> constant(value: T): Flow<T> {
        return flow { emit(value) }
    }

    override fun <T> sense(sensorID: SensorID): Flow<T> {
        return context.sensor(sensorID)
    }

    override fun <T> neighbourSense(sensorID: SensorID): Flow<NeighbourField<T>> {
        TODO("Not yet implemented")
    }

    override fun <T> neighbour(expression: Flow<T>): NeighbourField<T> {
        TODO("Not yet implemented")
    }

    override fun <T> branch(condition: Flow<Boolean>, th: Flow<T>, el: Flow<T>): Flow<T> {
        TODO("Not yet implemented")
    }

    override fun <T> loop(initial: T, f: (Flow<T>) -> Flow<T>): Flow<T> {
        TODO("Not yet implemented")
    }


}
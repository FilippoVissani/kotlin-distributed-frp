package io.github.filippovissani

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip

interface Semantics : Language {

    fun <T> alignWithNeighbors(path: Path, context: Context, extract: (Export<*>?, NeighbourState) -> T): Flow<Map<DeviceID, T>> {
        fun alignWith(neighbourID: DeviceID, neighbourState: NeighbourState): Pair<DeviceID, T> {
            val alignedExport = neighbourState.exported().followPath(path)
            return Pair(neighbourID, extract(alignedExport, neighbourState))
        }

        return context.neighbours().map { neighbours -> neighbours.map { alignWith(it.key, it.value) }.toMap() }
    }

    override fun selfID(context: Context): Flow<DeviceID> {
        return flow {emit(context.selfID())}
    }

    override fun <T> constant(value: T): Flow<T> {
        return flow { emit(value) }
    }

    override fun <T> sense(sensorID: SensorID, context: Context): Flow<T> {
        return context.sensor(sensorID)
    }

    override fun <T> neighbourSense(sensorID: SensorID): Expression<NeighbourField<T>> {
        return Expression.of{context, path ->
            alignWithNeighbors(path, context) { _, neighbourState ->
                neighbourState.sensor<T>(sensorID) }.map { x ->
                    ExportTree(x) }
        }
    }

    override fun <T> neighbour(expression: Expression<T>): Expression<NeighbourField<T>> {
        return Expression.of{ context, path ->
            val alignmentPath = path + Neighbour()
            val neighboringValues = alignWithNeighbors(alignmentPath, context){export, _ -> export?.root as T }
            expression.compute(alignmentPath, context).zip(neighboringValues){ exp, n ->
                val neighborField = n.plus(Pair(context.selfID(), exp.root))
                ExportTree(neighborField, sequenceOf(Pair(Neighbour(), exp)))
            }
        }
    }

    override fun <T> branch(condition: Flow<Boolean>, th: Flow<T>, el: Flow<T>): Flow<T> {
        TODO("Not yet implemented")
    }

    override fun <T> loop(initial: T, f: (Flow<T>) -> Flow<T>): Flow<T> {
        TODO("Not yet implemented")
    }


}
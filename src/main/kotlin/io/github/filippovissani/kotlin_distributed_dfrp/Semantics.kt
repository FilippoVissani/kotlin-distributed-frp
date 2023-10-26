package io.github.filippovissani.kotlin_distributed_dfrp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip

object Semantics : Language {

    private fun <T> alignWithNeighbors(path: Path, context: Context, extract: (Export<*>?, NeighbourState) -> T): Flow<Map<DeviceID, T>> {
        fun alignWith(neighbourID: DeviceID, neighbourState: NeighbourState): Pair<DeviceID, T> {
            val alignedExport = neighbourState.exported.followPath(path)
            return Pair(neighbourID, extract(alignedExport, neighbourState))
        }

        return context.neighboursStates.map { neighbours -> neighbours.map { alignWith(it.key, it.value) }.toMap() }
    }

    override fun selfID(): Computation<DeviceID> {
        return Computation.constant { context -> context.selfID }
    }

    override fun <T> constant(value: T): Computation<T> {
        return Computation.constant { _ -> value }
    }

    override fun <T> neighbour(computation: Computation<T>): Computation<NeighbourField<T>> {
        return Computation.of{ context, path ->
            val alignmentPath = path + Neighbour()
            val neighboringValues = alignWithNeighbors(alignmentPath, context){ export, _ -> export?.root as T }
            computation.run(path, context).zip(neighboringValues){ e, n ->
                val neighbourField = n.plus(context.selfID to e.root)
                ExportTree(neighbourField, sequenceOf(Neighbour() to e))
            }
        }
    }

    override fun <T> branch(condition: Computation<Boolean>, th: Computation<T>, el: Computation<T>): Computation<T> {
        TODO("Not yet implemented")
    }

    override fun <T> loop(initial: T, f: (Computation<T>) -> Computation<T>): Computation<T> {
        TODO("Not yet implemented")
    }
}
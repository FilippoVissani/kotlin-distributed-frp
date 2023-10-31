package io.github.filippovissani.kotlin_distributed_dfrp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip

object Semantics : Language {

    private fun <T> alignWithNeighbors(path: Path, context: Context, extract: (Export<*>?) -> T): Flow<Map<DeviceID, T>> {
        fun alignWith(neighbourID: DeviceID, export: Export<*>): Pair<DeviceID, T> {
            val alignedExport = export.followPath(path)
            return Pair(neighbourID, extract(alignedExport))
        }

        return context.neighbours.map { neighbours -> neighbours.map { alignWith(it.key, it.value) }.toMap() }
    }

    override fun selfID(): Computation<DeviceID> {
        return Computation.constant { context -> context.selfID }
    }

    override fun <T> constant(value: T): Computation<T> {
        return Computation.constant { _ -> value }
    }

    override fun <T> neighbour(computation: Computation<T>): Computation<NeighbourField<T>> {
        return Computation.of{ context, path ->
            val alignmentPath = path + Neighbour
            val neighboringValues = alignWithNeighbors(alignmentPath, context){ export -> export?.root as T }
            computation.run(path, context).zip(neighboringValues){ e, n ->
                val neighbourField = n.plus(context.selfID to e.root)
                ExportTree(neighbourField, listOf(Neighbour to e))
            }
        }
    }

    override fun <T> branch(condition: Computation<Boolean>, th: Computation<T>, el: Computation<T>): Computation<T> {
        return Computation.of{ context, path ->
            val conditionExport = condition.run(path.plus(Condition), context)
            val thenExport = th.run(path.plus(Then), context)
            val elseExport = el.run(path.plus(Else), context)
            combine(conditionExport, thenExport, elseExport){ c, t, e ->
                val selected = if (c.root) t else e
                val selectedSlot = if (c.root) Then else Else
                ExportTree(selected.root, listOf(Condition to c, selectedSlot to selected))
            }
        }
    }

    override fun <T> loop(initial: T, f: (Computation<T>) -> Computation<T>): Computation<T> {
        TODO("Not yet implemented")
    }
}
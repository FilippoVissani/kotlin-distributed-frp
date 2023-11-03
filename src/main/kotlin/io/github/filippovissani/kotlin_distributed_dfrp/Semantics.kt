package io.github.filippovissani.kotlin_distributed_dfrp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

object Semantics : Language {

    private fun <T> alignWithNeighbors(path: Path, context: Context, extract: (Export<*>?) -> T): Flow<Map<DeviceID, T>> {
        fun alignWith(neighbourID: DeviceID, export: Export<*>): Pair<DeviceID, T> {
            val alignedExport = export.followPath(path)
            return Pair(neighbourID, extract(alignedExport))
        }

        return context.neighbours.map { neighbours -> neighbours.map { alignWith(it.key, it.value) }.toMap() }
    }

    override fun selfID(): AggregateExpression<DeviceID> {
        return AggregateExpression.constant { context -> context.selfID }
    }

    override fun <T> constant(value: T): AggregateExpression<T> {
        return AggregateExpression.constant { _ -> value }
    }

    override fun <T> neighbour(aggregateExpression: AggregateExpression<T>): AggregateExpression<NeighbourField<T>> {
        return AggregateExpression.of{ context, path ->
            val alignmentPath = path + Neighbour
            val neighboringValues = alignWithNeighbors(alignmentPath, context){ export -> export?.root as T }
            combine(aggregateExpression.compute(path, context), neighboringValues){ export, values ->
                val neighbourField = values.plus(context.selfID to export.root)
                ExportTree(neighbourField, mapOf(Neighbour to export))
            }
        }
    }

    override fun <T> branch(condition: AggregateExpression<Boolean>, th: AggregateExpression<T>, el: AggregateExpression<T>): AggregateExpression<T> {
        return AggregateExpression.of{ context, path ->
            val conditionExport = condition.compute(path.plus(Condition), context)
            val thenExport = th.compute(path.plus(Then), context)
            val elseExport = el.compute(path.plus(Else), context)
            combine(conditionExport, thenExport, elseExport){ c, t, e ->
                val selected = if (c.root) t else e
                val selectedSlot = if (c.root) Then else Else
                ExportTree(selected.root, mapOf(Condition to c, selectedSlot to selected))
            }
        }
    }

    override fun <T : Any> loop(initial: T, f: (AggregateExpression<T>) -> AggregateExpression<T>): AggregateExpression<T> {
        return AggregateExpression.of{ context, path ->
            val previousExport = context.neighbours.map { neighbours ->
                    val previousValue = neighbours[context.selfID]?.followPath(path)?.root as T?
                    if(previousValue != null) ExportTree(previousValue) else ExportTree(initial)
            }
            f(AggregateExpression.of { _, _ -> previousExport }).compute(path, context)
        }
    }
}

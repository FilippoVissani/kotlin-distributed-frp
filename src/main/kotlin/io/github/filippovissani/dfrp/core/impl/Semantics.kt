package io.github.filippovissani.dfrp.core.impl

import io.github.filippovissani.dfrp.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

object Semantics : Language {

    private fun <T> alignWithNeighbors(path: Path, context: Context, extract: (Export<*>?) -> T): Flow<Map<DeviceID, T>> {
        fun alignWith(neighborID: DeviceID, export: Export<*>): Pair<DeviceID, T> {
            val alignedExport = export.followPath(path)
            return Pair(neighborID, extract(alignedExport))
        }

        return context.neighbors.map { neighbors -> neighbors.map { alignWith(it.key, it.value) }.toMap() }
    }

    private fun <T> conditional(condition: AggregateExpression<Boolean>, th: AggregateExpression<T>, el: AggregateExpression<T>, combiner: (Export<Boolean>, Export<T>, Export<T>) -> Export<T>): AggregateExpression<T>{
        return AggregateExpression.of{ context, path ->
            val conditionExport = condition.compute(path.plus(Condition), context)
            val thenExport = th.compute(path.plus(Then), context)
            val elseExport = el.compute(path.plus(Else), context)
            combine(conditionExport, thenExport, elseExport, combiner)
        }
    }

    override fun selfID(): AggregateExpression<DeviceID> {
        return AggregateExpression.constant { context -> context.selfID }
    }

    override fun <T> constant(value: T): AggregateExpression<T> {
        return AggregateExpression.constant { _ -> value }
    }

    override fun <T> neighbor(aggregateExpression: AggregateExpression<T>): AggregateExpression<NeighborField<T>> {
        return AggregateExpression.of { context, path ->
            val alignmentPath = path + Neighbor
            val neighboringValues = alignWithNeighbors(alignmentPath, context) { export -> export?.root as T }
            combine(aggregateExpression.compute(path, context), neighboringValues) { export, values ->
                val neighborField = values.plus(context.selfID to export.root)
                ExportTree(neighborField, mapOf(Neighbor to export))
            }
        }
    }

    override fun <T> branch(condition: AggregateExpression<Boolean>, th: AggregateExpression<T>, el: AggregateExpression<T>): AggregateExpression<T> {
        return conditional(condition, th, el){ c, t, e ->
            val selected = if(c.root) t else e
            val selectedSlot = if(c.root) Then else Else
            ExportTree(selected.root, mapOf(Condition to c, selectedSlot to selected))
        }
    }

    override fun <T> mux(
        condition: AggregateExpression<Boolean>,
        th: AggregateExpression<T>,
        el: AggregateExpression<T>
    ): AggregateExpression<T> {
        return conditional(condition, th, el){ c, t, e ->
            val selected = if(c.root) t.root else e.root
            ExportTree(selected, mapOf(Condition to c, Then to t, Else to e))
        }
    }

    override fun <T : Any> loop(initial: T, f: (AggregateExpression<T>) -> AggregateExpression<T>): AggregateExpression<T> {
        return AggregateExpression.of { context, path ->
            val previousExport = context.neighbors.map { neighbors ->
                val previousValue = neighbors[context.selfID]?.followPath(path)?.root as T?
                if (previousValue != null) ExportTree(previousValue) else ExportTree(initial)
            }
            f(AggregateExpression.of { _, _ -> previousExport }).compute(path, context)
        }
    }

    override fun <T> sense(sensorID: SensorID): AggregateExpression<T> {
        return AggregateExpression.fromFlow { context -> context.sensor(sensorID) }
    }
}

package io.github.filippovissani.dfrp.core

import io.github.filippovissani.dfrp.flow.extensions.combineStates
import io.github.filippovissani.dfrp.flow.extensions.mapStates
import kotlinx.coroutines.flow.StateFlow

object Semantics {

    private fun <T> alignWithNeighbors(
        path: Path,
        context: Context,
        extract: (Export<*>?) -> T
    ): StateFlow<Map<DeviceID, T>> {
        fun alignWith(neighborID: DeviceID, export: Export<*>): Pair<DeviceID, T> {
            val alignedExport = export.followPath(path)
            return Pair(neighborID, extract(alignedExport))
        }

        return mapStates(context.neighborsStates){
                neighbors -> neighbors.map { alignWith(it.key, it.value) }.toMap()
        }
    }

    private fun <T> conditional(
        condition: AggregateExpression<Boolean>,
        th: AggregateExpression<T>,
        el: AggregateExpression<T>,
        combiner: (Export<Boolean>, Export<T>, Export<T>) -> Export<T>
    ): AggregateExpression<T> {
        return AggregateExpression { path, context ->
            val conditionExport = condition.compute(path.plus(Condition), context)
            val thenExport = th.compute(path.plus(Then), context)
            val elseExport = el.compute(path.plus(Else), context)
            combineStates(conditionExport, thenExport, elseExport, combiner)
        }
    }

    fun selfID(): AggregateExpression<DeviceID> {
        return AggregateExpression.constant { context -> context.selfID }
    }

    inline fun <reified T> constant(value: T): AggregateExpression<T> {
        return AggregateExpression.constant { _ -> value }
    }

    fun <T> neighbor(aggregateExpression: AggregateExpression<T>): AggregateExpression<NeighborField<T>> {
        return AggregateExpression { path, context ->
            val alignmentPath = path + Neighbor
            val neighboringValues = alignWithNeighbors(alignmentPath, context) { export -> export?.root as T }
            combineStates(aggregateExpression.compute(path, context), neighboringValues) { export, values ->
                val neighborField = values.plus(context.selfID to export.root)
                ExportTree(neighborField, mapOf(Neighbor to export))
            }
        }
    }

    fun <T> branch(
        condition: AggregateExpression<Boolean>,
        th: AggregateExpression<T>,
        el: AggregateExpression<T>
    ): AggregateExpression<T> {
        return conditional(condition, th, el) { c, t, e ->
            val selected = if (c.root) t else e
            val selectedSlot = if (c.root) Then else Else
            ExportTree(selected.root, mapOf(Condition to c, selectedSlot to selected))
        }
    }

    fun <T> mux(
        condition: AggregateExpression<Boolean>,
        th: AggregateExpression<T>,
        el: AggregateExpression<T>
    ): AggregateExpression<T> {
        return conditional(condition, th, el) { c, t, e ->
            val selected = if (c.root) t.root else e.root
            ExportTree(selected, mapOf(Condition to c, Then to t, Else to e))
        }
    }

    fun <T> loop(
        initial: T,
        f: (AggregateExpression<T>) -> AggregateExpression<T>
    ): AggregateExpression<T> {
        return AggregateExpression { path, context ->
            val previousExport = mapStates(context.neighborsStates) { neighbors ->
                val previousValue = neighbors[context.selfID]?.followPath(path)?.root as T
                if (previousValue != null) ExportTree(previousValue) else ExportTree(initial)
            }
            f(AggregateExpression { _, _ -> previousExport as StateFlow<Export<T>> }).compute(path, context)
        }
    }

    inline fun <reified T> sense(sensorID: SensorID): AggregateExpression<T> {
        return AggregateExpression.fromStateFlow { context -> mapStates(context.sensorsStates) { sensors -> sensors[sensorID] as T } }
    }
}
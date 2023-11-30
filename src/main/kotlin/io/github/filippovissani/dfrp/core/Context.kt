package io.github.filippovissani.dfrp.core

import io.github.filippovissani.dfrp.core.extensions.combineStates
import io.github.filippovissani.dfrp.core.extensions.mapStates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class Context(val selfID: DeviceID, sensors: Map<SensorID, *>) {
    val _neighborsStates = MutableStateFlow(emptyMap<DeviceID, Export<*>>())
    val _sensorsStates = sensors.map { (k, v) -> k to MutableStateFlow(v) }.toMap()
    val neighbors = _neighborsStates.asStateFlow()

    fun receiveExport(neighborID: DeviceID, exported: Export<*>) {
        _neighborsStates.update { it.plus(neighborID to exported) }
    }

    fun <T> updateLocalSensor(sensorID: SensorID, newValue: T) {
        _sensorsStates[sensorID]?.update { newValue }
    }

    private fun <T> alignWithNeighbors(
        path: Path,
        extract: (Export<*>?) -> T,
    ): StateFlow<Map<DeviceID, T>> {
        fun alignWith(neighborID: DeviceID, export: Export<*>): Pair<DeviceID, T> {
            val alignedExport = export.followPath(path)
            return Pair(neighborID, extract(alignedExport))
        }
        return mapStates(neighbors) { neighbors -> neighbors.map { alignWith(it.key, it.value) }.toMap() }
    }

    private fun <T> conditional(
        condition: AggregateExpression<Boolean>,
        th: AggregateExpression<T>,
        el: AggregateExpression<T>,
        combiner: (Export<Boolean>, Export<T>, Export<T>) -> Export<T>,
    ): AggregateExpression<T> {
        return AggregateExpression { path ->
            val conditionExport = condition.compute(path.plus(Condition))
            val thenExport = th.compute(path.plus(Then))
            val elseExport = el.compute(path.plus(Else))
            combineStates(conditionExport, thenExport, elseExport, combiner)
        }
    }

    fun selfID(): AggregateExpression<DeviceID> {
        return AggregateExpression.constant { selfID }
    }

    inline fun <reified T> constant(value: T): AggregateExpression<T> {
        return AggregateExpression.constant { value }
    }

    fun <T> neighbor(aggregateExpression: AggregateExpression<T>): AggregateExpression<NeighborField<T>> {
        return AggregateExpression { path ->
            val alignmentPath = path + Neighbor
            val neighboringValues = alignWithNeighbors(alignmentPath) { export -> export?.root as T }
            val result = combineStates(aggregateExpression.compute(path), neighboringValues) { export, values ->
                val neighborField = values.plus(selfID to export.root)
                ExportTree(neighborField, mapOf(Neighbor to export))
            }
            result
        }
    }

    fun <T> branch(
        condition: AggregateExpression<Boolean>,
        th: AggregateExpression<T>,
        el: AggregateExpression<T>,
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
        el: AggregateExpression<T>,
    ): AggregateExpression<T> {
        return conditional(condition, th, el) { c, t, e ->
            val selected = if (c.root) t.root else e.root
            ExportTree(selected, mapOf(Condition to c, Then to t, Else to e))
        }
    }

    fun <T> loop(
        initial: T,
        f: (AggregateExpression<T>) -> AggregateExpression<T>,
    ): AggregateExpression<T> {
        return AggregateExpression { path ->
            val previousExport = mapStates(neighbors) { neighbors ->
                val previousValue = neighbors[selfID]?.followPath(path)?.root as T?
                if (previousValue != null) ExportTree(previousValue) else ExportTree(initial)
            }
            f(AggregateExpression { previousExport as StateFlow<Export<T>> }).compute(path)
        }
    }

    inline fun <reified T> sense(sensorID: SensorID): AggregateExpression<T> {
        return AggregateExpression.fromStateFlow { _sensorsStates[sensorID]?.asStateFlow() as StateFlow<T> }
    }
}

fun <T> aggregate(
    contexts: Iterable<Context>,
    aggregateExpression: Context.() -> AggregateExpression<T>,
): List<Flow<Export<T>>> {
    return contexts.map {
        with(it) {
            aggregateExpression().compute(emptyList())
        }
    }
}

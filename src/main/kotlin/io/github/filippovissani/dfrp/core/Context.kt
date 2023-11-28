package io.github.filippovissani.dfrp.core

import io.github.filippovissani.dfrp.core.extensions.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class Context(val selfID: DeviceID, sensors: Map<SensorID, *>) : Language {
    private val _neighborsStates = MutableStateFlow(emptyMap<DeviceID, Export<*>>())
    private val _sensorsStates = sensors.map { (k, v) -> k to MutableStateFlow(v) }.toMap()
    val neighbors = _neighborsStates.asSharedFlow()

    fun receiveExport(neighborID: DeviceID, exported: Export<*>) {
        _neighborsStates.update { it.plus(neighborID to exported) }
    }

    fun <T> updateLocalSensor(sensorID: SensorID, newValue: T) {
        _sensorsStates[sensorID]?.update { newValue }
    }

    private fun <T> alignWithNeighbors(
        path: Path,
        extract: (Export<*>?) -> T
    ): Flow<Map<DeviceID, T>> {
        fun alignWith(neighborID: DeviceID, export: Export<*>): Pair<DeviceID, T> {
            val alignedExport = export.followPath(path)
            return Pair(neighborID, extract(alignedExport))
        }

        return neighbors.map { neighbors -> neighbors.map { alignWith(it.key, it.value) }.toMap() }
    }

    private fun <T> conditional(
        condition: AggregateExpression<Boolean>,
        th: AggregateExpression<T>,
        el: AggregateExpression<T>,
        combiner: (Export<Boolean>, Export<T>, Export<T>) -> Export<T>
    ): AggregateExpression<T> {
        return AggregateExpression { path ->
            val conditionExport = condition.compute(path.plus(Condition))
            val thenExport = th.compute(path.plus(Then))
            val elseExport = el.compute(path.plus(Else))
            combine(conditionExport, thenExport, elseExport, combiner)
        }
    }

    override fun selfID(): AggregateExpression<DeviceID> {
        return AggregateExpression.constant { selfID }
    }

    override fun <T> constant(value: T): AggregateExpression<T> {
        return AggregateExpression.constant { value }
    }

    override fun <T> neighbor(aggregateExpression: AggregateExpression<T>): AggregateExpression<NeighborField<T>> {
        return AggregateExpression { path ->
            val alignmentPath = path + Neighbor
            val neighboringValues = alignWithNeighbors(alignmentPath) { export -> export?.root as T }
            combine(aggregateExpression.compute(path), neighboringValues) { export, values ->
                val neighborField = values.plus(selfID to export.root)
                ExportTree(neighborField, mapOf(Neighbor to export))
            }
        }
    }

    override fun <T> branch(
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

    override fun <T> mux(
        condition: AggregateExpression<Boolean>,
        th: AggregateExpression<T>,
        el: AggregateExpression<T>
    ): AggregateExpression<T> {
        return conditional(condition, th, el) { c, t, e ->
            val selected = if (c.root) t.root else e.root
            ExportTree(selected, mapOf(Condition to c, Then to t, Else to e))
        }
    }

    override fun <T> loop(
        initial: T,
        f: (AggregateExpression<T>) -> AggregateExpression<T>
    ): AggregateExpression<T> {
        return AggregateExpression { path ->
            val previousExport = neighbors.map { neighbors ->
                val previousValue = neighbors[selfID]?.followPath(path)?.root as T?
                if (previousValue != null) ExportTree(previousValue) else ExportTree(initial)
            }
            f(AggregateExpression { previousExport as Flow<Export<T>> }).compute(path)
        }
    }

    override fun <T> sense(sensorID: SensorID): AggregateExpression<T> {
        return AggregateExpression.fromFlow { _sensorsStates[sensorID]?.asSharedFlow() as SharedFlow<T> }
    }
}

fun <T> aggregate(
    contexts: Iterable<Context>,
    aggregateExpression: Language.() -> AggregateExpression<T>
): List<Flow<Export<T>>> {
    return contexts.map {
        with(it) {
            aggregateExpression().compute(emptyList())
        }
    }
}

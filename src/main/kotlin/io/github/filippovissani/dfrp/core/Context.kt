package io.github.filippovissani.dfrp.core

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class Context(val selfID: DeviceID) {
    val neighbors: MutableStateFlow<Set<Context>> = MutableStateFlow(emptySet())
    val selfExports: MutableStateFlow<Map<Path, MutableStateFlow<*>>> = MutableStateFlow(emptyMap())
    private val currentPath: MutableStateFlow<List<Slot>> = MutableStateFlow(emptyList())

    fun selfID(): StateFlow<DeviceID> {
        selfExports.update { it.plus(currentPath.value to MutableStateFlow(selfID)) }
        return selfExports.value[currentPath.value] as StateFlow<DeviceID>
    }

    fun <T> constant(value: T): StateFlow<T> {
        selfExports.update { it.plus(currentPath.value to MutableStateFlow(value)) }
        return selfExports.value[currentPath.value] as StateFlow<T>
    }

    fun <T> neighbor(expression: StateFlow<T>): StateFlow<Map<DeviceID, T>> {
        val oldPath = currentPath.value
        val alignmentPath = currentPath.value + Slot.Neighbor
        val neighborField: MutableStateFlow<Map<DeviceID, T>> = MutableStateFlow(emptyMap())
        selfExports.update { it.plus(alignmentPath to expression as MutableStateFlow<T>) }
        selfExports.update { it.plus(oldPath to neighborField) }
        neighbors.value.forEach { neighbor ->
            neighbor.selfExports.onEach { export ->
                neighborField.update { it.plus(neighbor.selfID to export[alignmentPath] as T) }
            }.launchIn(GlobalScope)
        }
        currentPath.update { alignmentPath }
        return neighborField
    }

    fun <T> branch(
        condition: StateFlow<Boolean>,
        th: StateFlow<T>,
        el: StateFlow<T>
    ): StateFlow<T> {
        TODO()
    }

    fun <T> mux(
        condition: StateFlow<Boolean>,
        th: StateFlow<T>,
        el: StateFlow<T>
    ): StateFlow<T> {
        TODO()
    }

    fun <T> loop(
        initial: T,
        f: (StateFlow<T>) -> StateFlow<T>
    ): StateFlow<T> {
        TODO()
    }

    fun <T> sense(sensorID: SensorID): StateFlow<T> {
        TODO()
    }
}

fun <T> aggregate(
    contexts: Iterable<Context>,
    aggregateExpression: Context.() -> StateFlow<T>
) {
    contexts.forEach {
        with(it){
            aggregateExpression()
        }
    }
}

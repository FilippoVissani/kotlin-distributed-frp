package io.github.filippovissani.dfrp.core

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class Context(val selfID: DeviceID) {
    val neighbors: MutableStateFlow<Set<Context>> = MutableStateFlow(emptySet())
    val selfExports: MutableStateFlow<Map<Path, StateFlow<*>>> = MutableStateFlow(emptyMap())
    private val currentPath: MutableStateFlow<List<Slot>> = MutableStateFlow(emptyList())

    fun selfID(): StateFlow<DeviceID> {
        val result = MutableStateFlow(selfID)
        selfExports.update { it.plus(currentPath.value to result) }
        return result.asStateFlow()
    }

    fun <T> constant(value: T): StateFlow<T> {
        val result = MutableStateFlow(value)
        selfExports.update { it.plus(currentPath.value to result) }
        return result.asStateFlow()
    }

    fun <T> neighbor(expression: StateFlow<T>): StateFlow<Map<DeviceID, T>> {
        val oldPath = currentPath.value
        val alignmentPath = currentPath.value + Slot.Neighbor
        val result: MutableStateFlow<Map<DeviceID, T>> = MutableStateFlow(mapOf(selfID to expression.value))
        neighbors.value.forEach { neighbor ->
            neighbor.selfExports.onEach { export ->
                result.update { it.plus(neighbor.selfID to export[alignmentPath] as T) }
            }.launchIn(GlobalScope)
        }
        selfExports.update { it.plus(oldPath to result) }
        currentPath.update { alignmentPath }
        selfExports.update { it.plus(alignmentPath to expression) }
        return result
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

package io.github.filippovissani.dfrp.core

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

typealias DeviceID = Int
typealias Path = List<Slot>
typealias SensorID = String
typealias Export<T> = Map<Path, T>

class Context(val selfID: DeviceID) {
    val neighbors: MutableStateFlow<Set<Context>> = MutableStateFlow(emptySet())
    val selfExports: MutableStateFlow<Export<*>> = MutableStateFlow(emptyMap<Path, Any>())
    private val currentPath: MutableStateFlow<Path> = MutableStateFlow(emptyList())

    fun selfID(): StateFlow<DeviceID> {
        val result = MutableStateFlow(selfID)
        selfExports.update { it.plus(currentPath.value to selfID) }
        return result.asStateFlow()
    }

    fun <T> constant(value: T): StateFlow<T> {
        val result = MutableStateFlow(value)
        selfExports.update { it.plus(currentPath.value to value) }
        return result.asStateFlow()
    }

    fun <T> neighbor(expression: StateFlow<T>): StateFlow<Map<DeviceID, T>> {
        val oldPath = currentPath.value
        val alignmentPath = currentPath.value + Slot.Neighbor
        val neighborField: MutableStateFlow<Map<DeviceID, T>> = MutableStateFlow(emptyMap())
        selfExports.update { it.plus(alignmentPath to expression.value) }
        selfExports.update { it.plus(oldPath to mapOf(selfID to expression.value)) }
        expression.onEach { value ->
            selfExports.update { export -> export.plus(alignmentPath to value) }
        }.launchIn(GlobalScope)
        neighbors.value.forEach { neighbor ->
            neighbor.selfExports.onEach { newNeighborExport ->
                selfExports.update { selfExport ->
                    val newValue = newNeighborExport[alignmentPath] as T
                    val actualNeighborField = selfExport[oldPath] as Map<DeviceID, T>
                    selfExport.plus(oldPath to actualNeighborField.plus(neighbor.selfID to newValue))
                }
            }.launchIn(GlobalScope)
        }
        currentPath.update { alignmentPath }
        return neighborField.asStateFlow()
        TODO("Update neighborField")
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
        with(it) {
            aggregateExpression()
        }
    }
}

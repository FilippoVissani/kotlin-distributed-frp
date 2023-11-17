package io.github.filippovissani.dfrp.core

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    fun selfID(): DeviceID {
        selfExports.update { it.plus(currentPath.value to selfID) }
        return selfID
    }

    fun <T> constant(value: T): T {
        selfExports.update { it.plus(currentPath.value to value) }
        return value
    }

    fun <T> neighbor(expression: () -> T): Map<DeviceID, T> {
        val oldPath = currentPath.value
        val alignmentPath = currentPath.value + Slot.Neighbor
        val initialValue = expression()
        val initialNeighborField: Map<DeviceID, T> = mapOf(selfID to initialValue)
        selfExports.update { it.plus(alignmentPath to initialValue) }
        selfExports.update { it.plus(oldPath to initialNeighborField) }
        neighbors.value.forEach { neighbor ->
            neighbor.selfExports.onEach { export ->
                selfExports.update { selfExport ->
                    val actualNeighborField = selfExport[oldPath] as Map<DeviceID, T>
                    val updatedNeighborField = actualNeighborField.plus(neighbor.selfID to export[alignmentPath] as T)
                    selfExport.plus(oldPath to updatedNeighborField)
                }
            }.launchIn(GlobalScope)
        }
        currentPath.update { alignmentPath }
        return initialNeighborField
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
    aggregateExpression: Context.() -> T
) {
    contexts.forEach {
        with(it){
            aggregateExpression()
        }
    }
}

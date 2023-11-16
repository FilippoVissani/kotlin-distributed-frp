package io.github.filippovissani.dfrp.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class Context(val selfID: DeviceID) {
    val neighbors: MutableSet<Context> = mutableSetOf()
    val selfExport: MutableMap<Path, MutableStateFlow<*>> = mutableMapOf()
    private val currentPath: MutableList<Slot> = mutableListOf()

    fun selfID(): MutableStateFlow<DeviceID> {
        val result = MutableStateFlow(selfID)
        selfExport[currentPath] = result
        return result
    }

    fun <T> constant(value: T): MutableStateFlow<T> {
        val result = MutableStateFlow(value)
        selfExport[currentPath] = result
        return result
    }

    fun <T> neighbor(expression: MutableStateFlow<T>): MutableStateFlow<Map<DeviceID, T>> {
        val alignmentPath = currentPath + Slot.Neighbor
        val neighborValues: Map<DeviceID, MutableStateFlow<T>?> =
            neighbors.associate { it.selfID to it.selfExport[alignmentPath] as MutableStateFlow<T>? }
                .plus(selfID to expression)
        val result: MutableStateFlow<Map<DeviceID, T>> = MutableStateFlow(emptyMap())
        neighborValues.forEach{ (neighborID, flow) ->
            flow?.onEach { newValue ->
                result.update { it.plus(neighborID to newValue) }
            }
        }
        selfExport[currentPath] = result
        currentPath += Slot.Neighbor
        return result
    }

    fun <T> branch(
        condition: MutableStateFlow<Boolean>,
        th: MutableStateFlow<T>,
        el: MutableStateFlow<T>
    ): MutableStateFlow<T> {
        TODO()
    }

    fun <T> mux(
        condition: MutableStateFlow<Boolean>,
        th: MutableStateFlow<T>,
        el: MutableStateFlow<T>
    ): MutableStateFlow<T> {
        val initialSelected = if(condition.value) th.value else el.value
        val result = MutableStateFlow(initialSelected)
        condition.onEach {
            val selected = if(it) th else el
            selected.onEach { newValue ->
                result.update { newValue }
            }
        }
        selfExport[currentPath] = result
        currentPath += Slot.Condition
        selfExport[currentPath] = condition
        currentPath += Slot.Then
        selfExport[currentPath] = th
        currentPath += Slot.Else
        selfExport[currentPath] = el
        return result
    }

    fun <T> loop(
        initial: T,
        f: (MutableStateFlow<T>) -> MutableStateFlow<T>
    ): MutableStateFlow<T> {
        TODO()
    }

    fun <T> sense(sensorID: SensorID): MutableStateFlow<T> {
        TODO()
    }
}

fun <T> aggregate(
    contexts: Iterable<Context>,
    aggregateExpression: Context.() -> MutableStateFlow<T>
) {
    contexts.forEach {
        with(it){
            aggregateExpression()
        }
    }
}

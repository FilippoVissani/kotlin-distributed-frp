package io.github.filippovissani.dfrp.core

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias DeviceID = Int
typealias Path = List<Slot>
typealias SensorID = String
typealias Export<T> = Map<Path, T>

class Context(val selfID: DeviceID) {
    var neighbors: Set<Context> = emptySet()
    val selfExports: MutableStateFlow<Export<*>> = MutableStateFlow(emptyMap<Path, Any>())
    private val currentPath: MutableStateFlow<Path> = MutableStateFlow(emptyList())
    private val logger = KotlinLogging.logger {}

    suspend fun selfID(): StateFlow<DeviceID> = coroutineScope {
        val result = MutableStateFlow(selfID)
        selfExports.update { it.plus(currentPath.value to selfID) }
        result.asStateFlow()
    }

    suspend fun <T> constant(value: T): StateFlow<T> = coroutineScope {
        val result = MutableStateFlow(value)
        selfExports.update { it.plus(currentPath.value to value) }
        result.asStateFlow()
    }

    suspend fun <T> neighbor(expression: StateFlow<T>): StateFlow<Map<DeviceID, T>> = coroutineScope {
        val oldPath = currentPath.value
        val alignmentPath = currentPath.value + Slot.Neighbor
        val initialExpressionValue = expression.value
        val neighborField: MutableStateFlow<Map<DeviceID, T>> =
            MutableStateFlow(mapOf(selfID to initialExpressionValue))
        currentPath.update { alignmentPath }
        selfExports.update {
            it.plus(alignmentPath to initialExpressionValue).plus(oldPath to neighborField.value)
        }
        launch(Dispatchers.Default) {
            expression.collect { value ->
                logger.debug { "$value from expression" }
                selfExports.update { export ->
                    export.plus(alignmentPath to value)
                }
            }
        }
        neighbors.forEach { neighbor ->
            launch(Dispatchers.Default) {
                neighbor.selfExports.collect { newNeighborExport ->
                    val newValue = newNeighborExport[alignmentPath] as T
                    neighborField.update { it.plus(neighbor.selfID to newValue) }
                    selfExports.update { selfExport ->
                        logger.debug { "$selfID received: $newNeighborExport from ${neighbor.selfID}" }
                        selfExport.plus(oldPath to neighborField.value)
                    }
                }
            }
        }
        neighborField.asStateFlow()
    }

    fun <T> branch(
        condition: StateFlow<Boolean>,
        th: StateFlow<T>,
        el: StateFlow<T>
    ): StateFlow<T> {
        TODO()
    }

    suspend fun <T> mux(
        condition: StateFlow<Boolean>,
        th: StateFlow<T>,
        el: StateFlow<T>
    ): StateFlow<T> = coroutineScope {
        val oldPath = currentPath.value
        val conditionPath = currentPath.value + Slot.Condition
        val thenPath = currentPath.value + Slot.Then
        val elsePath = currentPath.value + Slot.Else
        val initialConditionValue = condition.value
        val initialThenValue = th.value
        val initialElseValue = el.value
        val result: MutableStateFlow<T> =
            MutableStateFlow(if (initialConditionValue) initialThenValue else initialElseValue)
        selfExports.update {
            it.plus(oldPath to result.value)
                .plus(conditionPath to initialConditionValue)
                .plus(thenPath to initialThenValue)
                .plus(elsePath to initialElseValue)
        }
        launch(Dispatchers.Default) {
            condition.collect { newCondition ->
                result.update { if (newCondition) th.value else el.value }
                selfExports.update {
                    it.plus(conditionPath to newCondition)
                        .plus(oldPath to if (newCondition) th.value else el.value)
                }
            }
        }
        launch(Dispatchers.Default) {
            th.collect { newTh ->
                result.update { if (condition.value) newTh else el.value }
                selfExports.update {
                    it
                        .plus(thenPath to newTh)
                        .plus(oldPath to if (condition.value) newTh else el.value)
                }
            }
        }
        launch(Dispatchers.Default) {
            el.collect { newEl ->
                result.update { if (condition.value) th.value else newEl }
                selfExports.update {
                    it
                        .plus(elsePath to newEl)
                        .plus(oldPath to if (condition.value) th.value else newEl)
                }
            }
        }
        result
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

suspend fun <T> aggregate(
    contexts: Iterable<Context>,
    aggregateExpression: suspend Context.() -> StateFlow<T>
) = coroutineScope {
    contexts.forEach {
        with(it) {
            launch {
                aggregateExpression()
            }
        }
    }
}

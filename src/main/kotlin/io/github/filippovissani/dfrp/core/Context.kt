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

    suspend fun <T> neighbor(expression: suspend () -> StateFlow<T>): StateFlow<Map<DeviceID, T>> = coroutineScope {
        val oldPath = currentPath.value
        val alignmentPath = oldPath + Slot.Neighbor
        currentPath.update { alignmentPath }
        val expressionResult = expression()
        val initialExpressionValue = expressionResult.value
        val neighborField: MutableStateFlow<Map<DeviceID, T>> =
            MutableStateFlow(mapOf(selfID to initialExpressionValue))
        selfExports.update {
            it.plus(alignmentPath to initialExpressionValue).plus(oldPath to neighborField.value)
        }
        launch(Dispatchers.Default) {
            expressionResult.collect { value ->
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
        condition: suspend () -> StateFlow<Boolean>,
        th: suspend () -> StateFlow<T>,
        el: suspend () -> StateFlow<T>
    ): StateFlow<T> = coroutineScope {
        // TODO THERE IS A BUG, ONLY FIRST BRANCH (THEN) IS EVALUATED
        // THERE IS NO ROOT IN THE FINAL EXPORT OF DEVICES
        val oldPath = currentPath.value
        val conditionPath = oldPath + Slot.Condition
        val thenPath = oldPath + Slot.Then
        val elsePath = oldPath + Slot.Else
        currentPath.update { conditionPath }
        val conditionResult = condition()
        logger.debug { "Condition evaluated" }
        currentPath.update { thenPath }
        val thenResult = th()
        logger.debug { "Then evaluated" }
        currentPath.update { elsePath }
        val elseResult = el()
        logger.debug { "Else evaluated" }
        val initialConditionValue = conditionResult.value
        val initialThenValue = thenResult.value
        val initialElseValue = elseResult.value
        val result: MutableStateFlow<T> =
            MutableStateFlow(if (initialConditionValue) initialThenValue else initialElseValue)
        selfExports.update {
            it.plus(oldPath to result.value)
                .plus(conditionPath to initialConditionValue)
                .plus(thenPath to initialThenValue)
                .plus(elsePath to initialElseValue)
        }
        launch(Dispatchers.Default) {
            conditionResult.collect { newCondition ->
                val newResult = if (newCondition) thenResult.value else elseResult.value
                result.update { newResult }
                selfExports.update {
                    it.plus(oldPath to newResult)
                        .plus(conditionPath to newCondition)
                }
            }
        }
        launch(Dispatchers.Default) {
            thenResult.collect { newTh ->
                val newResult = if (conditionResult.value) newTh else elseResult.value
                result.update { newResult }
                selfExports.update {
                    it.plus(oldPath to newResult)
                        .plus(thenPath to newTh)
                }
            }
        }
        launch(Dispatchers.Default) {
            elseResult.collect { newEl ->
                val newResult = if (conditionResult.value) thenResult.value else newEl
                result.update { newResult }
                selfExports.update {
                    it.plus(oldPath to newResult)
                        .plus(elsePath to newEl)
                }
            }
        }
        result.asStateFlow()
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

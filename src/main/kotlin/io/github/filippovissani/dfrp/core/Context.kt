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
    val neighbors: MutableStateFlow<Set<Context>> = MutableStateFlow(emptySet())
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
        val neighborField: MutableStateFlow<Map<DeviceID, T>> = MutableStateFlow(emptyMap())
        selfExports.update { it.plus(alignmentPath to expression.value) }
        selfExports.update { it.plus(oldPath to mapOf(selfID to expression.value)) }
        currentPath.update { alignmentPath }
        logger.debug { "$selfID line 43" }
        launch(Dispatchers.Default) {
            expression.collect { value ->
                logger.debug { "$value from expression" }
                selfExports.update { export ->
                    export.plus(alignmentPath to value)
                }
            }
        }
        logger.debug { "$selfID line 55" }
        neighbors.value.forEach { neighbor ->
            launch(Dispatchers.Default) {
                neighbor.selfExports.collect { newNeighborExport ->
                    selfExports.update { selfExport ->
                        logger.debug { "$selfID received: $newNeighborExport from ${neighbor.selfID}" }
                        val newValue = newNeighborExport[alignmentPath] as T
                        val actualNeighborField = selfExport[oldPath] as Map<DeviceID, T>
                        selfExport.plus(oldPath to actualNeighborField.plus(neighbor.selfID to newValue))
                    }
                }
            }
        }
        logger.debug { "$selfID line 68" }
        neighborField.asStateFlow()
        // TODO("update neighborField")
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

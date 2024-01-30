package io.github.filippovissani.dfrp.core.extensions

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.Context
import io.github.filippovissani.dfrp.core.extensions.DefaultConfiguration.initialSensorsValues
import io.github.filippovissani.dfrp.core.extensions.DefaultConfiguration.N_DEVICES
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Simulator {
    private const val DELAY: Long = 200
    private val logger = KotlinLogging.logger {}

    suspend fun <T> runSimulation(
        testName: String,
        aggregateExpression: () -> AggregateExpression<T>,
        runAfter: (List<Context>) -> Unit = {},
        assertions: (contexts: Context) -> Unit = {},
    ) = coroutineScope {
        logger.info { testName }
        val contexts: List<Context> = (0..<N_DEVICES).map { Context(it, initialSensorsValues) }
        val exports = contexts.map { context ->
            (context.selfID to aggregateExpression().compute(emptyList(), context))
        }
        val exportsJobs = exports.map { (id, export) ->
            contexts.map { neighbor ->
                launch(Dispatchers.Default) {
                    export.collect {
                        println("(${id} -> ${it.root})")
                        logger.debug { "(${id} -> ${it.root})" }
                        neighbor.receiveExport(id, it)
                    }
                }
            }
        }.flatten()
        delay(DELAY)
        runAfter(contexts)
        delay(DELAY)
        exportsJobs.forEach { it.cancelAndJoin() }
        logger.info { "#################################" }
        contexts.forEach { assertions(it) }
    }
}

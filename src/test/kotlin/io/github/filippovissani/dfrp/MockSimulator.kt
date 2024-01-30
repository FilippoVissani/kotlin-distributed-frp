package io.github.filippovissani.dfrp

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.Context
import io.github.filippovissani.dfrp.DefaultConfiguration.initialSensorsValues
import io.github.filippovissani.dfrp.DefaultConfiguration.N_DEVICES
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object MockSimulator {
    private const val DELAY: Long = 200
    private val logger = KotlinLogging.logger {}

    suspend fun <T> runDefaultSimulation(
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

    suspend fun <T> runSimulation(simulation: Simulation<T>) = coroutineScope {
        logger.info { simulation.testName }
        val results = simulation.deviceSimulations.map { deviceSimulation ->
            DeviceSimulationResult(
                deviceSimulation.context,
                deviceSimulation.neighbors,
                deviceSimulation.aggregateExpression().compute(emptyList(), deviceSimulation.context),
            )
        }
        val jobs = results.map { result ->
            result.neighbors.map { neighbor ->
                launch(Dispatchers.Default) {
                    result.export.collect {
                        logger.debug { "(${result.context.selfID} -> ${it.root})" }
                        neighbor.receiveExport(result.context.selfID, it)
                    }
                }
            }
        }.flatten()
        delay(DELAY)
        simulation.deviceSimulations.forEach{ it.runAfter() }
        delay(DELAY)
        jobs.forEach { it.cancelAndJoin() }
        logger.info { "#################################" }
        simulation.deviceSimulations.forEach { it.assertions() }
    }
}

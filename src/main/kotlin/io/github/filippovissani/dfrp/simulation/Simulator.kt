package io.github.filippovissani.dfrp.simulation

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.Context
import io.github.filippovissani.dfrp.core.Execution.aggregate
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class Simulator(private val simulation: Simulation) {
    private val logger = KotlinLogging.logger {}

    suspend fun <T> start(aggregateExpression: Context.() -> AggregateExpression<T>) = coroutineScope {
        val exports = aggregate(simulation.contexts, aggregateExpression)
        exports.withIndex().forEach { export ->
            simulation.environment.neighbors(export.index).forEach { neighborID ->
                launch(Dispatchers.Default) {
                    export.value.collect {
                        println("(${export.index} -> ${it.root})")
                        logger.debug { "(${export.index} -> ${it.root})" }
                        simulation.contexts[neighborID].receiveExport(export.index, it)
                    }
                }
            }
        }
    }
}

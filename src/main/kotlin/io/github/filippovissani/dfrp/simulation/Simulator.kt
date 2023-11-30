package io.github.filippovissani.dfrp.simulation

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class Simulator(private val simulation: Simulation) {
    private val logger = KotlinLogging.logger {}

    suspend fun <T> start(aggregateExpression: AggregateExpression<T>) = coroutineScope {
        val exports =
            simulation.contexts.map { context ->
                (context.selfID to aggregateExpression.compute(emptyList(), context))
            }
        exports.forEach { (id, export) ->
            simulation.environment.neighbors(id).forEach { neighborID ->
                launch(Dispatchers.Default) {
                    export.collect {
                        logger.debug { "(${id} -> ${it.root})" }
                        simulation.contexts[neighborID].receiveExport(id, it)
                    }
                }
            }
        }
    }
}

package io.github.filippovissani.dfrp.simulation

import io.github.filippovissani.dfrp.core.AggregateExpression
import kotlinx.coroutines.flow.first
import org.slf4j.LoggerFactory

class Simulator(private val simulation: Simulation) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun <T> start(aggregateExpression: AggregateExpression<T>) {
        val exports =
            simulation.contexts.map { context -> (context.selfID to aggregateExpression.compute(emptyList(), context)) }
        exports.forEach { (id, export) ->
            logger.debug("Device $id exported:\n${export.first()}")
            simulation.environment.neighbors(id).forEach { n ->
                simulation.contexts[n].receiveExport(id, export.first())
            }
        }
    }
}

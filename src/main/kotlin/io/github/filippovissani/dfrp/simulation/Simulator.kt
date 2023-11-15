package io.github.filippovissani.dfrp.simulation

import io.github.filippovissani.dfrp.core.AggregateExpression
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class Simulator(private val simulation: Simulation) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun <T> start(aggregateExpression: AggregateExpression<T>) {
        runBlocking {
            val exports =
                simulation.contexts.map { context ->
                    (context.selfID to aggregateExpression.compute(
                        emptyList(),
                        context
                    ))
                }
            exports.forEach { (id, export) ->
                simulation.environment.neighbors(id).forEach { neighborID ->
                    export.onEach {
                        logger.info("($id -> ${it.root})")
                        simulation.contexts[neighborID].receiveExport(id, it)
                    }.launchIn(this)
                }
            }
        }
    }
}

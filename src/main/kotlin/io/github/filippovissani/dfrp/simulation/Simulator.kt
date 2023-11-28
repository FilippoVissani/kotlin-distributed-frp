package io.github.filippovissani.dfrp.simulation

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.Language
import io.github.filippovissani.dfrp.core.aggregate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class Simulator(private val simulation: Simulation) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun <T> start(aggregateExpression: Language.() -> AggregateExpression<T>) = coroutineScope {
        val exports = aggregate(simulation.contexts, aggregateExpression)
        exports.withIndex().forEach { export ->
            simulation.environment.neighbors(export.index).forEach { neighborID ->
                launch(Dispatchers.Default) {
                    export.value.collect {
                        logger.info("(${export.index} -> ${it.root})")
                        simulation.contexts[neighborID].receiveExport(export.index, it)
                    }
                }
            }
        }
    }
}

package io.github.filippovissani.dfrp.samples

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.Semantics.constant
import io.github.filippovissani.dfrp.core.Semantics.loop
import io.github.filippovissani.dfrp.core.Semantics.mux
import io.github.filippovissani.dfrp.core.Semantics.neighbor
import io.github.filippovissani.dfrp.core.Semantics.sense
import io.github.filippovissani.dfrp.core.extensions.map
import io.github.filippovissani.dfrp.core.extensions.min
import io.github.filippovissani.dfrp.core.extensions.withoutSelf
import io.github.filippovissani.dfrp.simulation.Environment
import io.github.filippovissani.dfrp.simulation.Sensors
import io.github.filippovissani.dfrp.simulation.Simulation
import io.github.filippovissani.dfrp.simulation.Simulator
import kotlinx.coroutines.runBlocking

suspend fun runGradientSimulation(environment: Environment, source: Int) {
    val simulation = Simulation(environment, source)
    val simulator = Simulator(simulation)

    fun gradient(): AggregateExpression<Double> {
        return loop(Double.POSITIVE_INFINITY) { distance ->
            mux(
                sense(Sensors.IS_SOURCE.sensorID),
                constant(0.0),
                neighbor(distance)
                    .withoutSelf()
                    .map { neighborField -> neighborField.mapValues { it.value + 1 } }
                    .min()
            )
        }
    }

    simulator.start(gradient())
}

fun main() {
    runBlocking {
        val environment = Environment.manhattanGrid(2, 2)
        runGradientSimulation(environment, 3)
    }
}

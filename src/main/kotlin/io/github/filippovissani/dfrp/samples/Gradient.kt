package io.github.filippovissani.dfrp.samples

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.extensions.map
import io.github.filippovissani.dfrp.core.extensions.minOrNull
import io.github.filippovissani.dfrp.core.extensions.withoutSelf
import io.github.filippovissani.dfrp.core.impl.Semantics.constant
import io.github.filippovissani.dfrp.core.impl.Semantics.loop
import io.github.filippovissani.dfrp.core.impl.Semantics.mux
import io.github.filippovissani.dfrp.core.impl.Semantics.neighbor
import io.github.filippovissani.dfrp.core.impl.Semantics.sense
import io.github.filippovissani.dfrp.simulation.Environment
import io.github.filippovissani.dfrp.simulation.Sensors
import io.github.filippovissani.dfrp.simulation.Simulation
import io.github.filippovissani.dfrp.simulation.Simulator
import kotlinx.coroutines.runBlocking

fun runGradientSimulation(environment: Environment, source: Int) {
    val simulation = Simulation(environment, source)
    val simulator = Simulator(simulation)

    fun gradient(): AggregateExpression<Double?> {
        return loop(Double.POSITIVE_INFINITY) { distance ->
            mux(
                sense(Sensors.IS_SOURCE.sensorID),
                constant(0.0),
                neighbor(distance).map { field -> field.map { it.key to (it.value?.plus(1) ?: it.value) }.toMap() }
                    .withoutSelf().minOrNull()
            )
        }
    }

    simulator.start(gradient())
}

fun main() {
    runBlocking {
        val environment = Environment.manhattanGrid(5, 5)
        runGradientSimulation(environment, 0)
    }
}

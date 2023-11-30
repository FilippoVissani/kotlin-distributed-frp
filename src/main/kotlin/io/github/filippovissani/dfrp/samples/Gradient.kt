package io.github.filippovissani.dfrp.samples

import io.github.filippovissani.dfrp.core.extensions.map
import io.github.filippovissani.dfrp.core.extensions.min
import io.github.filippovissani.dfrp.simulation.Environment
import io.github.filippovissani.dfrp.simulation.Sensors
import io.github.filippovissani.dfrp.simulation.Simulation
import io.github.filippovissani.dfrp.simulation.Simulator
import kotlinx.coroutines.runBlocking

suspend fun runGradientSimulation(environment: Environment, source: Int) {
    val simulation = Simulation(environment, source)
    val simulator = Simulator(simulation)
    simulator.start {
        loop(Double.POSITIVE_INFINITY) { distance ->
            mux(
                sense(Sensors.IS_SOURCE.sensorID),
                constant(0.0),
                withoutSelf(neighbor(distance))
                    .map { neighborField -> neighborField.mapValues { it.value + 1 } }
                    .min()
            )
        }
    }
}

fun main() {
    runBlocking {
        val environment = Environment.manhattanGrid(10, 10)
        runGradientSimulation(environment, 99)
    }
}

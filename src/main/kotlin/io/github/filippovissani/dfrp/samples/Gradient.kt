package io.github.filippovissani.dfrp.samples

import io.github.filippovissani.dfrp.core.extensions.map
import io.github.filippovissani.dfrp.simulation.Environment
import io.github.filippovissani.dfrp.simulation.Sensors
import io.github.filippovissani.dfrp.simulation.Simulation
import io.github.filippovissani.dfrp.simulation.Simulator
import kotlinx.coroutines.runBlocking

suspend fun runGradientSimulation(environment: Environment, source: Int) {
    val simulation = Simulation(environment, source)
    val simulator = Simulator(simulation)
    simulator.start<Double?> {
        loop(Double.POSITIVE_INFINITY) { distance ->
            mux(
                sense(Sensors.IS_SOURCE.sensorID),
                constant(0.0),
                neighbor(distance).map { field ->
                    field.minus(selfID).values.fold(Double.POSITIVE_INFINITY) { x, y ->
                        if (x < y!!) x else y
                    }.plus(1)
                }
            )
        }
    }
}

fun main() {
    runBlocking {
        val environment = Environment.manhattanGrid(3, 3)
        runGradientSimulation(environment, 4)
    }
}

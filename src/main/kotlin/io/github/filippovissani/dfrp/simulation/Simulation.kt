package io.github.filippovissani.dfrp.simulation

import io.github.filippovissani.dfrp.core.Context

class Simulation(val environment: Environment, private val source: Int) {
    val contexts = (0 until environment.devicesNumber).map {
        if (it == source) Context(
            it,
            mapOf(Sensors.IS_SOURCE.sensorID to true)
        ) else Context(it, mapOf(Sensors.IS_SOURCE.sensorID to false))
    }
}

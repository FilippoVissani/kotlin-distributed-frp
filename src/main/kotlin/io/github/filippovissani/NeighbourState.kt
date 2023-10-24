package io.github.filippovissani

interface NeighbourState {
    fun <T> sensor(sensorID: SensorID): T
    fun exported(): Export<*>
}
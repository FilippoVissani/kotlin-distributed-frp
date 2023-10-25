package io.github.filippovissani.kotlin_distributed_dfrp

interface NeighbourState {
    fun <T> sensor(sensorID: SensorID): T
    fun exported(): Export<*>
}
package io.github.filippovissani.dfrp

object DefaultConfiguration {
    const val LOCAL_SENSOR = "sensor"
    const val LOCAL_SENSOR_VALUE = 150
    val initialSensorsValues = mapOf(LOCAL_SENSOR to LOCAL_SENSOR_VALUE)
    const val N_DEVICES = 4
    const val THEN_VALUE = 200
    const val ELSE_VALUE = 300
}
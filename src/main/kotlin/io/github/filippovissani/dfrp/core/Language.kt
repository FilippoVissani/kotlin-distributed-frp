package io.github.filippovissani.dfrp.core

interface Language {

    fun <T> constant(value: T): AggregateExpression<T>

    fun <T> neighbor(aggregateExpression: AggregateExpression<T>): AggregateExpression<NeighborField<T>>

    fun <T> branch(
        condition: AggregateExpression<Boolean>,
        th: AggregateExpression<T>,
        el: AggregateExpression<T>
    ): AggregateExpression<T>

    fun <T> mux(
        condition: AggregateExpression<Boolean>,
        th: AggregateExpression<T>,
        el: AggregateExpression<T>
    ): AggregateExpression<T>

    fun <T : Any> loop(initial: T, f: (AggregateExpression<T>) -> AggregateExpression<T>): AggregateExpression<T>

    fun selfID(): AggregateExpression<DeviceID>

    fun <T> sense(sensorID: SensorID): AggregateExpression<T>
}

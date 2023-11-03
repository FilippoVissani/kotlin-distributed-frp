package io.github.filippovissani.kotlin_distributed_dfrp

interface Language {
    
    fun <T> constant(value: T): AggregateExpression<T>

    fun <T> neighbour(aggregateExpression: AggregateExpression<T>): AggregateExpression<NeighbourField<T>>
    
    fun <T> branch(condition: AggregateExpression<Boolean>, th: AggregateExpression<T>, el: AggregateExpression<T>): AggregateExpression<T>
    
    fun <T : Any> loop(initial: T, f: (AggregateExpression<T>) -> AggregateExpression<T>): AggregateExpression<T>

    fun selfID(): AggregateExpression<DeviceID>

    fun <T> sense(sensorID: SensorID): AggregateExpression<T>
}
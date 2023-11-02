package io.github.filippovissani.kotlin_distributed_dfrp

interface Language {
    
    fun <T> constant(value: T): Computation<T>

    fun <T> neighbour(computation: Computation<T>): Computation<NeighbourField<T>>
    
    fun <T> branch(condition: Computation<Boolean>, th: Computation<T>, el: Computation<T>): Computation<T>
    
    fun <T> loop(initial: T, f: (Computation<T>) -> Computation<T>): Computation<T>

    fun selfID(): Computation<DeviceID>
}
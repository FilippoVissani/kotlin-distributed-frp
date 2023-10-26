package io.github.filippovissani.kotlin_distributed_dfrp

data class NeighbourState(
    val neighbourID: DeviceID,
    val exported: Export<Any>
)

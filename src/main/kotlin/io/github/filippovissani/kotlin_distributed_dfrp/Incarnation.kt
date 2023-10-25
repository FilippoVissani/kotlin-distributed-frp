package io.github.filippovissani.kotlin_distributed_dfrp

interface Incarnation : Semantics {
    fun context(selfID: DeviceID): Context
}
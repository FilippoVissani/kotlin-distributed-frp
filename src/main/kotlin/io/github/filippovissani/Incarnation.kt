package io.github.filippovissani

interface Incarnation : Semantics {
    fun context(selfID: DeviceID): Context
}
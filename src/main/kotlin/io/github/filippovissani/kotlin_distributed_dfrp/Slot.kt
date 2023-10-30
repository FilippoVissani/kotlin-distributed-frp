package io.github.filippovissani.kotlin_distributed_dfrp

interface Slot

data class Operand(val index: Int) : Slot

object Neighbour : Slot

object Condition : Slot

object Then : Slot

object Else : Slot

data class Key<T>(val value: T) : Slot

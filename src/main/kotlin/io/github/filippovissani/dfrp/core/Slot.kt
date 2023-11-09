package io.github.filippovissani.dfrp.core

interface Slot

data class Operand(val index: Int) : Slot

object Neighbor : Slot

object Condition : Slot

object Then : Slot

object Else : Slot

data class Key<T>(val value: T) : Slot

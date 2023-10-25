package io.github.filippovissani.kotlin_distributed_dfrp

interface Slot

data class Operand(val index: Int) : Slot

class Neighbour : Slot

class Condition : Slot

class Then : Slot

class Else : Slot

data class Key<T>(val value: T) : Slot

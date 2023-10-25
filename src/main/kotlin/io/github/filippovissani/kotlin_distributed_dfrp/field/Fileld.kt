package io.github.filippovissani.kotlin_distributed_dfrp.field

import io.github.filippovissani.kotlin_distributed_dfrp.ID

interface Field<T> : Map<ID, T> {

    val selfID: ID

    val localValue: T?

    fun excludeSelf(): Field<T> = Field(selfID, filter { it.key != selfID })

    fun <B> map(f: (T) -> B): Field<B> = Field(selfID, mapValues { (_, value) -> f(value) })

    companion object {

        operator fun <T> invoke(selfID: ID, messages: Map<ID, T> = emptyMap()): Field<T> = FieldImpl(selfID, messages)
    }
}

fun <T> Map<ID, T>.toField(selfID: ID): Field<T> = Field(selfID, this)

internal data class FieldImpl<T>(
    override val selfID: ID,
    private val messages: Map<ID, T>,
) : Field<T>, Map<ID, T> by messages { override val localValue: T? = this[selfID] }
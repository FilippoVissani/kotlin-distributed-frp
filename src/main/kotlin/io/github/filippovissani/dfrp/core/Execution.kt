package io.github.filippovissani.dfrp.core

import kotlinx.coroutines.flow.Flow

object Execution {
    fun <T> aggregate(
        contexts: Iterable<Context>,
        aggregateExpression: Context.() -> AggregateExpression<T>,
    ): List<Flow<Export<T>>> {
        return contexts.map {
            with(it) {
                aggregateExpression().compute(emptyList())
            }
        }
    }
}
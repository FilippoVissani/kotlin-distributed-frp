package io.github.filippovissani.dfrp.core

import io.github.filippovissani.dfrp.flow.extensions.mapStates
import io.github.filippovissani.dfrp.core.impl.AggregateExpressionImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AggregateExpression<T> {
    fun compute(path: Path): StateFlow<Export<T>>

    companion object {
        operator fun <T> invoke(f: (Path) -> StateFlow<Export<T>>): AggregateExpression<T> {
            return AggregateExpressionImpl(f)
        }

        inline fun <reified T> fromStateFlow(crossinline flow: () -> StateFlow<T>): AggregateExpression<T> {
            return AggregateExpression { _ -> mapStates(flow()) { state -> ExportTree(state) } }
        }

        inline fun <reified T> constant(crossinline value: () -> T): AggregateExpression<T> {
            return fromStateFlow { MutableStateFlow(value()) }
        }
    }
}


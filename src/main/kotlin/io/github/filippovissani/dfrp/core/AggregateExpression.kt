package io.github.filippovissani.dfrp.core

import io.github.filippovissani.dfrp.core.extensions.mapStates
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

internal class AggregateExpressionImpl<T>(private val f: (Path) -> StateFlow<Export<T>>) : AggregateExpression<T> {

    override fun compute(path: Path): StateFlow<Export<T>> {
        return f(path)
    }
}

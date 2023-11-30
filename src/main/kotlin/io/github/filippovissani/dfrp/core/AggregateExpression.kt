package io.github.filippovissani.dfrp.core

import io.github.filippovissani.dfrp.core.impl.AggregateExpressionImpl
import io.github.filippovissani.dfrp.flow.extensions.mapStates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface AggregateExpression<T> {
    fun compute(path: Path, context: Context): StateFlow<Export<T>>

    companion object {
        operator fun <T> invoke(f: (Path, Context) -> StateFlow<Export<T>>): AggregateExpression<T> {
            return AggregateExpressionImpl(f)
        }

        inline fun <reified T> fromStateFlow(crossinline flow: (Context) -> StateFlow<T>): AggregateExpression<T> {
            return AggregateExpression { _, context -> mapStates(flow(context)) { state -> ExportTree(state) } }
        }

        inline fun <reified T> constant(crossinline value: (Context) -> T): AggregateExpression<T> {
            return fromStateFlow { context -> MutableStateFlow(value(context)) }
        }
    }
}

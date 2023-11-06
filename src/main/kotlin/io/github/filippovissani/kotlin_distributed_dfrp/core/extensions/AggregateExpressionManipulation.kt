package io.github.filippovissani.kotlin_distributed_dfrp.core.extensions

import io.github.filippovissani.kotlin_distributed_dfrp.core.AggregateExpression
import io.github.filippovissani.kotlin_distributed_dfrp.core.ExportTree
import kotlinx.coroutines.flow.map

fun <T, R> AggregateExpression<T>.map(transform: (T) -> R): AggregateExpression<R> {
    return AggregateExpression.of { context, path ->
        this.compute(path, context).map { export -> export.map(transform) }
    }
}

fun <T1, T2, R> combine(exp1: AggregateExpression<T1>, exp2: AggregateExpression<T2>, transform: (T1, T2) -> R): AggregateExpression<R> {
    return AggregateExpression.of { context, path ->
        kotlinx.coroutines.flow.combine(exp1.compute(path, context), exp2.compute(path, context)) { result1, result2 ->
            ExportTree(transform(result1.root, result2.root))
        }
    }
}
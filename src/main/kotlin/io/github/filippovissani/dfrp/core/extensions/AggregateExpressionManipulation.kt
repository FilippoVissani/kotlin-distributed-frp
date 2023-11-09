package io.github.filippovissani.dfrp.core.extensions

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.ExportTree
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

fun <T1, T2, T3, R> combine(exp1: AggregateExpression<T1>, exp2: AggregateExpression<T2>, exp3: AggregateExpression<T3>, transform: (T1, T2, T3) -> R): AggregateExpression<R> {
    return AggregateExpression.of { context, path ->
        kotlinx.coroutines.flow.combine(exp1.compute(path, context), exp2.compute(path, context), exp3.compute(path, context)) { result1, result2, result3 ->
            ExportTree(transform(result1.root, result2.root, result3.root))
        }
    }
}
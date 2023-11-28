package io.github.filippovissani.dfrp.core.extensions

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.ExportTree
import io.github.filippovissani.dfrp.core.NeighborField
import kotlinx.coroutines.flow.map

fun <T, R> AggregateExpression<T>.map(transform: (T) -> R): AggregateExpression<R> {
    return AggregateExpression { path ->
        this.compute(path).map { export -> export.map(transform) }
    }
}

fun AggregateExpression<NeighborField<Double?>>.minOrNull(): AggregateExpression<Double?> {
    return AggregateExpression { path ->
        this.compute(path).map { export -> export.map { it.values.minByOrNull { x -> x != null } } }
    }
}

fun <T1, T2, R> combine(
    exp1: AggregateExpression<T1>,
    exp2: AggregateExpression<T2>,
    transform: (T1, T2) -> R
): AggregateExpression<R> {
    return AggregateExpression { path ->
        kotlinx.coroutines.flow.combine(exp1.compute(path), exp2.compute(path)) { result1, result2 ->
            ExportTree(transform(result1.root, result2.root))
        }
    }
}

fun <T1, T2, T3, R> combine(
    exp1: AggregateExpression<T1>,
    exp2: AggregateExpression<T2>,
    exp3: AggregateExpression<T3>,
    transform: (T1, T2, T3) -> R
): AggregateExpression<R> {
    return AggregateExpression { path ->
        kotlinx.coroutines.flow.combine(
            exp1.compute(path),
            exp2.compute(path),
            exp3.compute(path)
        ) { result1, result2, result3 ->
            ExportTree(transform(result1.root, result2.root, result3.root))
        }
    }
}

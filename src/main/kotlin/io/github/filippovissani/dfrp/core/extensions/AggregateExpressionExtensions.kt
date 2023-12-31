package io.github.filippovissani.dfrp.core.extensions

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.ExportTree
import io.github.filippovissani.dfrp.core.NeighborField
import io.github.filippovissani.dfrp.flow.extensions.combineStates
import io.github.filippovissani.dfrp.flow.extensions.mapStates

fun <T, R> AggregateExpression<T>.map(transform: (T) -> R): AggregateExpression<R> {
    return AggregateExpression { path, context ->
        mapStates(this.compute(path, context)) { it.map(transform) }
    }
}

fun <T> AggregateExpression<NeighborField<T>>.withoutSelf(): AggregateExpression<NeighborField<T>> {
    return AggregateExpression { path, context ->
        mapStates(this.compute(path, context)) { neighborField ->
            neighborField.map { it.minus(context.selfID) }
        }
    }
}

fun AggregateExpression<NeighborField<Double>>.min(): AggregateExpression<Double> {
    return AggregateExpression { path, context ->
        mapStates(
            this.compute(
                path,
                context
            )
        ) { export -> export.map { it.values.fold(Double.POSITIVE_INFINITY) { x, y -> if (x < y) x else y } } }
    }
}

fun <T1, T2, R> combine(
    exp1: AggregateExpression<T1>,
    exp2: AggregateExpression<T2>,
    transform: (T1, T2) -> R
): AggregateExpression<R> {
    return AggregateExpression { path, context ->
        combineStates(exp1.compute(path, context), exp2.compute(path, context)) { result1, result2 ->
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
    return AggregateExpression { path, context ->
        combineStates(
            exp1.compute(path, context),
            exp2.compute(path, context),
            exp3.compute(path, context)
        ) { result1, result2, result3 ->
            ExportTree(transform(result1.root, result2.root, result3.root))
        }
    }
}

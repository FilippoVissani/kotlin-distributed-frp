package io.github.filippovissani.dfrp

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.Context
import io.github.filippovissani.dfrp.core.Export
import kotlinx.coroutines.flow.StateFlow

data class MockSimulation<T>(
    val testName: String,
    val deviceSimulations: List<DeviceSimulation<T>>,
)

data class DeviceSimulation<T>(
    val context: Context,
    val neighbors: List<Context>,
    val aggregateExpression: () -> AggregateExpression<T>,
    val runAfter: (Context) -> Unit,
    val assertions: (Context) -> Unit,
)

data class DeviceSimulationResult<T>(
    val context: Context,
    val neighbors: List<Context>,
    val export: StateFlow<Export<T>>,
)

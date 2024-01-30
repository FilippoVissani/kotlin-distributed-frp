package io.github.filippovissani.dfrp

import io.github.filippovissani.dfrp.core.AggregateExpression
import io.github.filippovissani.dfrp.core.Context
import io.github.filippovissani.dfrp.core.DeviceID
import io.github.filippovissani.dfrp.core.Export
import kotlinx.coroutines.flow.StateFlow

data class Simulation<T>(
    val testName: String,
    val deviceSimulations: List<DeviceSimulation<T>>,
)

data class DeviceSimulation<T>(
    val context: Context,
    val neighbors: List<Context>,
    val aggregateExpression: () -> AggregateExpression<T>,
    val runAfter: () -> Unit,
    val assertions: () -> Unit,
)

data class DeviceSimulationResult<T>(
    val context: Context,
    val neighbors: List<Context>,
    val export: StateFlow<Export<T>>,
)

package io.github.filippovissani.dfrp.core.extensions

import io.github.filippovissani.dfrp.FlowExtensions.combineStates
import io.kotest.core.spec.style.FreeSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.test.assertEquals

class CombinedStateFlowSpec : FreeSpec({
    "CombinedStateFlow" - {
        "Should update its combined value" {
            val value1 = 1
            val value2 = 2
            val newValue = 5
            val stateFlow1 = MutableStateFlow(value1)
            val stateFlow2 = MutableStateFlow(value2)
            val stateFlowResult = combineStates(stateFlow1, stateFlow2) { states ->
                states.sum()
            }
            assertEquals(value1 + value2, stateFlowResult.value)
            stateFlow1.update { newValue }
            assertEquals(newValue + value2, stateFlowResult.value)
        }
    }
})
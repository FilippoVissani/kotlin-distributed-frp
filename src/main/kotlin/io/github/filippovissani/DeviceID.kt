package io.github.filippovissani

import kotlin.random.Random

data class ID(val id: Int = Random.nextInt(Int.MIN_VALUE, Int.MAX_VALUE))

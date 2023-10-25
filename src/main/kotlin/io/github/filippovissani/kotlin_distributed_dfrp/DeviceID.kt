package io.github.filippovissani.kotlin_distributed_dfrp

import kotlin.random.Random

data class ID(val id: Int = Random.nextInt(Int.MIN_VALUE, Int.MAX_VALUE))

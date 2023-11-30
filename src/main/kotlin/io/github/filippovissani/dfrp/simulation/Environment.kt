package io.github.filippovissani.dfrp.simulation

interface Environment {
    val devicesNumber: Int
    fun position(device: Int): Pair<Double, Double>
    fun neighbors(device: Int): Iterable<Int>

    companion object {
        fun manhattanGrid(columns: Int, rows: Int): Environment {
            return grid(columns, rows) { column, row ->
                listOf(
                    Pair(column, row),
                    Pair(column + 1, row),
                    Pair(column - 1, row),
                    Pair(column, row + 1),
                    Pair(column, row - 1),
                )
            }
        }

        private fun grid(
            columns: Int,
            rows: Int,
            candidateNeighbors: (Int, Int) -> Iterable<Pair<Int, Int>>,
        ): Environment = object : Environment {
            private fun row(device: Int): Int = device / columns

            private fun col(device: Int): Int = device % columns

            override val devicesNumber: Int = rows * columns

            override fun position(device: Int): Pair<Double, Double> =
                Pair(col(device).toDouble(), row(device).toDouble())

            override fun neighbors(device: Int): Iterable<Int> {
                val deviceCol = col(device)
                val deviceRow = row(device)
                val candidates = candidateNeighbors(deviceCol, deviceRow)
                return candidates
                    .filter { (c, r) -> c >= 0 && r >= 0 && c < columns && r < rows }
                    .map { (c, r) -> r * columns + c }
            }
        }
    }
}

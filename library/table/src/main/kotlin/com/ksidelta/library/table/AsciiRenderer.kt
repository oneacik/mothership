package com.ksidelta.library.table

class AsciiRenderer : Renderer {
    override fun render(
        xs: List<String>,
        ys: List<String>,
        values: Map<Renderer.XY, String>
    ): String {
        val xLengths = xs.toLengths(values) { current, xy -> current == xy.x }

        return ys.map { y ->
            xs.map { x ->
                val value = values[Renderer.XY(x, y)] ?: ""
                val padding = xLengths[x] ?: 0

                val cellValue = " ${value.padEnd(padding)} "
                cellValue
            }.joinToString("|", "|", "|")
        }.joinToString("\n")
    }

    fun List<String>.toLengths(
        values: Map<Renderer.XY, String>,
        predicate: (String, Renderer.XY) -> Boolean
    ): Map<String, Int> =
        this.map { current ->
            values.entries
                .filter { predicate(current, it.key) }
                .map { (_, value) -> value.length }
                .max()
                .let { Pair(current, it) }
        }.toMap()


}
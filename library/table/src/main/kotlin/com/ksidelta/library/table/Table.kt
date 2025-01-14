package com.ksidelta.library.table

data class Table(val cells: List<Cell>) {

    fun render(renderer: Renderer): String {
        val xs = cells.map { it.x }.toNames().reversed()
        val ys = cells.map { it.y }.toNames()
        val cells = cells.map {
            Pair(
                Renderer.XY(it.x.toString(), it.y.toString()),
                it.content
            )
        }.toMap()

        val headerValues = (xs.map {
            Pair(
                Renderer.XY(it, "HEADER"),
                it
            )
        } + ys.map {
            Pair(
                Renderer.XY("HEADER", it),
                it
            )
        }).toMap()

        return renderer.render(listOf("HEADER") + xs, listOf("HEADER") + ys, headerValues + cells)
    }


    data class Cell(val x: Comparable<Any>, val y: Comparable<Any>, val content: String)
    data class Pos(val order: Int = 0, val name: String) : Comparable<Any> {
        override fun toString(): String = name
        override fun compareTo(other: Any): Int =
            compareValuesBy(this, other, { (it as Pos).order }, { (it as Pos).name })


    }

    fun List<Comparable<Any>>.toNames(): List<String> =
        this
            .sorted()
            .map { it.toString() }
            .distinct()


}
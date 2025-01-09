package com.ksidelta.library.table

data class Table(val cells: List<Cell>) {

    fun render(renderer: Renderer): String {
        val xs = cells.map { it.x }.toNames().reversed()
        val ys = cells.map { it.y }.toNames()
        val cells = cells.map {
            Pair(
                Renderer.XY(it.x.name, it.y.name),
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


    data class Cell(val x: Pos, val y: Pos, val content: String)
    data class Pos(val name: String, val order: Int = 0)

    fun List<Pos>.toNames(): List<String> =
        this
            .sortedWith(compareBy({ it.order }, { it.name }))
            .map { it.name }
            .distinct()


}
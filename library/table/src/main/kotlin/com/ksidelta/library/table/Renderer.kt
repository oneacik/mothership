package com.ksidelta.library.table

interface Renderer {
    fun render(xs: List<String>, ys: List<String>, values: Map<XY, String>): String

    data class XY(val x: String, val y: String)
}
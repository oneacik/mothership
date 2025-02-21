package com.ksidelta.library.table

import com.ksidelta.library.table.Renderer.XY

class HtmlRenderer : Renderer {
    override fun render(xs: List<String>, ys: List<String>, values: Map<XY, String>): String {
        val headers = xs.map { x -> "<td>${x}</td>" }.joinToString("", "<tr><td></td>", "</tr>")

        val content = ys.map { y ->
            xs.map { x -> "<td>${values[XY(x, y)] ?: ""}</td>" }
                .joinToString("", "<tr>\n", "</tr>\n")
        }.joinToString("\n")

        return """
            <html>
            <head>
            <style>
               table {
                overflow: scroll; 
                white-space: nowrap;
                border-collapse: collapse;
                border: 1px solid black;
               }
               tr, td {
                padding: 0;
                border: 0;
                margin: 0;
               }
               td {
                padding: 1px 2px;
                border-right: 0.5px solid black;
                border-left: 0.5px solid black;
               }
               tr:nth-child(odd) {
                background-color: #dddddd;
               }
               tr:nth-child(even) {
                background-color: #eeeeee;
               }
            </style>
            </head>
            <body>
            <table>
            ${content.prependIndent()}
            </table>
            </body></html>
        """.trimIndent()
    }
}
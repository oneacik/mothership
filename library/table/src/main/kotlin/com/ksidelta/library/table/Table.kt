package com.ksidelta.library.http

import java.net.URLEncoder

object Table {
    fun queryUrl(base: String, parameters: Map<String, String>): String =
        base + "?" + parameters.entries.stream()
            .map { entry -> entry.key + "=" + URLEncoder.encode(entry.value, "UTF-8") }
            .reduce { l, r -> l + "&" + r }
            .orElse("")

}
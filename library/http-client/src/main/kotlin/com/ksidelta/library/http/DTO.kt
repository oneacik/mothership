package com.ksidelta.library.http

data class DTO(
    val headers: Map<String, String> = mapOf(
        "Accept" to "application/json",
    ),
    val body: Any? = null
) {
    fun body(content: Any) = copy(headers + mapOf("Content-Type" to "application/json"), content)
    fun setToken(token: String) = copy(headers + mapOf("Authorization" to "Bearer $token"))
}


package com.ksidelta.library.http

interface HttpClient {
    fun <T : Any> get(url: String, klass: Class<T>, configure: (DTO) -> DTO = { it }): T
    fun <T : Any> post(url: String, klass: Class<T>, configure: (DTO) -> DTO = { it }): T
}
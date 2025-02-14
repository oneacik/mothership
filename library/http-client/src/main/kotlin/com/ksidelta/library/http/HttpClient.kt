package com.ksidelta.library.http

interface HttpClient {
    fun <T : Any> get(url: String, klass: Class<T>, configure: (DTO) -> DTO = { it }): Response<T>
    fun <T : Any> post(url: String, klass: Class<T>, configure: (DTO) -> DTO = { it }): Response<T>

    data class Response<T>(val data: T, val status: Int)
}
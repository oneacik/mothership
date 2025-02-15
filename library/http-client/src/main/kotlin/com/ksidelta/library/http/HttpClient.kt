package com.ksidelta.library.http

interface HttpClient {
    fun <T : Any> get(url: String, klass: Class<T>, configure: (DTO) -> DTO = { it }): Response<T>
    fun <T : Any> post(url: String, klass: Class<T>, configure: (DTO) -> DTO = { it }): Response<T>

    data class Response<T>(val data: T, val status: Int)
}

fun <T> HttpClient.fallbackExecute(executions: List<() -> HttpClient.Response<T>>): HttpClient.Response<T> {
    var first: HttpClient.Response<T>? = null
    for (execution in executions) {
        var x = execution()
        if (first == null) first = x
        if (x.status == 200) return x;
    }

    return checkNotNull(first)
}


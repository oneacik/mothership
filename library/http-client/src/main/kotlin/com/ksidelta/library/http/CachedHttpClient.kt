package com.ksidelta.library.http

import com.ksidelta.library.store.Store

class CachedHttpClient(val httpClient: HttpClient, val cache: Store) : HttpClient {
    override fun <T : Any> get(url: String, klass: Class<T>, configure: (DTO) -> DTO): HttpClient.Response<T> =
        cache.get(url, klass)?.let { HttpClient.Response(it, 200) }
            ?: httpClient.get(url, klass, configure)
                .also {
                    if (it.status == 200) cache.store(url, it.data)
                }

    override fun <T : Any> post(url: String, klass: Class<T>, configure: (DTO) -> DTO): HttpClient.Response<T> =
        httpClient.post(url, klass, configure)
}
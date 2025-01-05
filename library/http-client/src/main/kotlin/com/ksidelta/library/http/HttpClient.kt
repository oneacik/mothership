package com.ksidelta.library.http

interface HttpClient {
    fun <T : Any> get(url: String, klass: Class<T>): T
    fun <T : Any> post(url: String, klass: Class<T>): T
}
package com.ksidelta.library.serialization


interface Serializer {
    fun encode(obj: Any): String
    fun <T> decode(obj: String, klass: Class<T>): T
}
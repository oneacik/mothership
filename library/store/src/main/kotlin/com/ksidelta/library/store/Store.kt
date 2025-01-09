package com.ksidelta.library.store

import java.net.URLEncoder

interface Store {
    fun store(id: String, obj: Any)
    fun <T> get(id: String, klass: Class<T>): T?
    fun keys(): List<String>
}
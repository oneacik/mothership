package com.ksidelta.library.session

interface Session {
    fun <T> fetch(klass: Class<T>): T?
    fun <T> store(klass: Class<T>, obj: T)
}
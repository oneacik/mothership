package com.ksidelta.library.memoize

interface Memoize {
    fun <T : Any> execute(key: String, klass: Class<T>, run: () -> T): T
}

inline fun <reified T : Any> Memoize.execute(key: String, noinline run: () -> T): T =
    this.execute(key, T::class.java, run);
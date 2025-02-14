package com.ksidelta.library.cache

import com.ksidelta.library.store.Store

interface Cache : Store {
}

inline fun <reified T> Store.passthrough(key: String, compute: () -> T?): T? =
    this.get(key, T::class.java) ?: compute()?.also { value -> this.store(key, value) }

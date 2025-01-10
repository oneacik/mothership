package com.ksidelta.library.memoize

import com.ksidelta.library.store.Store

class StoreMemoize(val store: Store) : Memoize {
    private val prefix = "memoize-"
    override fun <T : Any> execute(key: String, klass: Class<T>, run: () -> T) =
        store.get(prefix + key, klass) ?: run()
            .also {
                store.store(prefix + key, it)
            }

}
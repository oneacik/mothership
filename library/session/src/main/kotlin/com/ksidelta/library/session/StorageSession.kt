package com.ksidelta.library.session

import com.ksidelta.library.store.Store
import java.util.UUID

class StorageSession(val store: Store, val id: UUID) : Session {
    override fun <T> fetch(klass: Class<T>): T? =
        store.get("session-" + id.toString(), SessionModel::class.java)
            ?.let { it.associations[klass as Class<*>] }
            ?.also {
                if (!klass.isAssignableFrom(it.javaClass))
                    throw IllegalStateException("NOT CLASS")
            } as T?


    override fun <T> store(klass: Class<T>, obj: T) {
        (store.get("session-" + id.toString(), SessionModel::class.java) ?: SessionModel())
            .let { it.copy(it.associations + mapOf(Pair(klass, obj)) as Map<Class<Any>, Any>) }
            .also { store.store("session-" + id.toString(), it) }
    }
}
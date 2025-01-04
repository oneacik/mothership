package com.ksidelta.library.cache

import com.ksidelta.library.store.Store
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class StoreCache(val store: Store, val secondsFresh: Int) : Cache {
    init {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
            {
                runCatching { refresh() }
                    .onFailure { it.printStackTrace() }
            },
            0,
            1, TimeUnit.MINUTES
        )
    }

    fun refresh() {
        keys()
            .forEach() { id ->
                runCatching {
                    if (store.get(id, TimeHasCome::class.java)?.until?.isFresh() == false)
                        remove(id)
                }.onFailure { println("KURWAAA: DLA ${id} ::: ${it.toString()}")
                }

            }
    }

    override fun store(id: String, obj: Any) =
        store.store(id, freeze(obj))

    override fun <T> get(id: String, klass: Class<T>): T? =
        store.get(id, TimeHasCome::class.java)
            ?.let {
                if (it.until.isFresh())
                    it.value as T?
                else
                    remove(id).let { null }
            }


    override fun remove(id: String) =
        store.remove(id)

    override fun keys(): List<String> =
        store.keys()

    fun freeze(value: Any) = TimeHasCome(keepFresh(secondsFresh), value)

    data class TimeHasCome<Any>(val until: Instant, val value: Any)

    fun keepFresh(forSeconds: Int) = Instant.now().plus(forSeconds.toLong(), ChronoUnit.SECONDS)

    fun Instant.isFresh() = Instant.now() < this
}
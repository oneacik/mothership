package com.ksidelta.library.store

import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FileStoreTest {
    lateinit var store: Store

    @BeforeTest
    fun before() {
        File("./storage/").apply {
            deleteRecursively()
            mkdir()
        }
        store = FileStore("./storage")
    }

    @Test
    fun storeAndRead() {
        store.store("A", "TEST")
        assertEquals(
            store.get<String>("A", String::class.java),
            "TEST"
        )
    }

    @Test
    fun listKeys() {
        store.store("A", "x")
        store.store("B", "x")
        assertEquals(store.keys(), listOf("A", "B"))
    }

}
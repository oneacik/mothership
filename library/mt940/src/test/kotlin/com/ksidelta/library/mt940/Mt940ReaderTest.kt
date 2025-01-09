package com.ksidelta.library.mt940

import com.ksidelta.library.mt940.ModelParser.Entry
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class Mt940ReaderTest {
    val sut = Mt940Reader.createForBNPParibas()

    @Test
    fun shouldReadFullExample() {
        val input = Mt940ReaderTest::class.java
            .getResourceAsStream("/full-example-utf8.mt940")!!
            .bufferedReader().readText()
        sut.read(input)
    }


}
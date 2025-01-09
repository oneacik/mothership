package com.ksidelta.library.mt940

import kotlin.test.Test

class ReducerTest {
    val example = """
        :20:1
        :25:/PL50160014591890544930000001
        :28C:01/2023/M
        :60F:C230101PLN000000024853,76
        :61:2301020102CN000000000100,00N723112 20230101 51//CEN2301020646240
        :86:723^00PRZELEW OTRZYMANY ELIXIR   ^34000
        ^3010501764  
        ^20Składka Norbert Szulc
        ^32SZULC NORBERT ŚW. DUCHA 596^331/17 80-834 GDAŃSK
        ^3822105017641000009215291726
        :61:2301020102CN000000000100,00N723112 20230102 51//CEN2301020646331
        :86:723^00PRZELEW OTRZYMANY ELIXIR   ^34000
        ^3011602202  
        ^20Składka Omer Sakarya
        ^32SAKARYA OMER UL SEJMOWA 21 ^3305-071    SULEJÓWEK
        ^3815116022020000000118438407
        :61:2301020102CN000000000100,00N723112 20230101 51//CEN2301020677678
        :86:723^00PRZELEW OTRZYMANY ELIXIR   ^34000
    """.trimIndent()

    val tokenizer = Tokenizer()
    val sut = ModelParser()

    @Test
    fun tokenize(){
        val tokens = tokenizer.tokenize(example)
        val reduced = sut.reduce(tokens)
        // I just ant to copy everything
    }


}
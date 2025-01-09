package com.ksidelta.library.mt940

import java.math.BigDecimal

class Reducer(val reducers: List<Reducer> = listOf()) {
    fun reduce(tokens: List<Tokenizer.Token>) =
        tokens.fold(State()) { state, token ->
            reducers
                .find { reducer -> reducer.prefix == token.prefix }
                ?.let { reducer -> reducer.transform(state, token.content) }
                ?: throw IllegalStateException("Did not find reducer for ${token.prefix}${token.content}")

        }

    data class Entry(
        val date: String,
        val account: String,
        val title: String,
        val amount: BigDecimal
    )

    class Reducer(val prefix: String, val transform: (State, String) -> State)

    data class State(
        val entries: List<Entry> = listOf(),
        val current: Entry? = null
    )
}
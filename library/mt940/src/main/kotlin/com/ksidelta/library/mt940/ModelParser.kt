package com.ksidelta.library.mt940

import java.math.BigDecimal

class ModelParser(
    val reducers: List<Reducer> = listOf(
        Reducer(":20:", identityTransform),
        Reducer(":25:", identityTransform), // Could store
        Reducer(":28C:", identityTransform), // Could store
        Reducer(":60F:", identityTransform), // Starting Balance
        Reducer(":62F:", identityTransform), // Ending Balance
        Reducer(":61:") { state, content ->
            Regex("""(\d{6})(\d{4})(.).([\d,]{15})(N\w{3})(.*)""")
                .matchEntire(content)!!
                .let {
                    val date = it.groups[1]!!.value
                    // val shortDate = it.groups[2]!!.value
                    val creditOrDebit = it.groups[3]!!.value
                    val amount = it.groups[4]!!.value
                    // val transferType = it.groups[5]!!.value
                    // val nobodyCares = it.groups[6]!!.value

                    state.run {
                        state.copy(
                            entries = entries + current.toListIfExists(),
                            current = Entry(
                                date,
                                null,
                                "",
                                "",
                                BigDecimal.valueOf(
                                    amount
                                        .replace(Regex("^0*"), "")
                                        .replace(",", ".")
                                        .toDouble()
                                            * (if (creditOrDebit == "C") 1.0 else -1.0)
                                )
                            )
                        )
                    }
                }
        }, // Starter Balance
        Reducer(":86:", identityTransform), // Operation Code
        Reducer("\\^00", identityTransform), // PRZELEW OTRZYMANY ELIXIR
        Reducer("\\^34", identityTransform), // Operation Code Too?
        Reducer("\\^30", identityTransform), // Person Bank Code
        Reducer("\\^20", addToTitle), // Title
        Reducer("\\^21", addToTitle), // Title
        Reducer("\\^22", addToTitle), // Title
        Reducer("\\^23", addToTitle), // Title
        Reducer("\\^24", addToTitle), // Title
        Reducer("\\^25", addToTitle), // Title
        Reducer("\\^26", addToTitle), // Title

        Reducer("\\^32", addToSender), // Nazwa Kontrahenta
        Reducer("\\^33", addToSender), // Nazwa Kontrahenta
        Reducer("\\^62", addToSender),
        Reducer("\\^63", addToSender),

        Reducer("\\^38") { state, contents ->
            state.copy(current = state.current!!.copy(account = contents))
        }, // account num

    )
) {
    fun reduce(tokens: List<Tokenizer.Token>): State =
        tokens.fold(State()) { state, token ->
            reducers
                .find { reducer -> reducer.prefix == token.prefix }
                ?.let { reducer -> reducer.transform(state, token.content) }
                ?: throw IllegalStateException("Did not find reducer for ${token.prefix}${token.content}")
        }.let { State(it.entries + it.current.toListIfExists()) }

    data class Entry(
        val date: String?,
        val account: String?,
        val sender: String?,
        val title: String?,
        val amount: BigDecimal?
    )

    class Reducer(val prefix: String, val transform: (State, String) -> State)

    data class State(
        val entries: List<Entry> = listOf(),
        val current: Entry? = null
    )

    companion object {
        val identityTransform: (State, String) -> State = { state, _ -> state }
        val addToTitle: (State, String) -> State = { state, content ->
            state.run {
                copy(current = current!!.run {
                    copy(title = title + content)
                })
            }
        }

        val addToSender: (State, String) -> State = { state, content ->
            state.run {
                copy(current = current!!.run {
                    copy(sender = sender + content)
                })
            }
        }

    }
}

fun <T> T?.toListIfExists(): List<T> =
    this?.let { listOf(this) } ?: listOf()
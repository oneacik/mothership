package com.ksidelta.library.mt940

import com.ksidelta.library.banking.Model
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Mt940Reader internal constructor(internal val tokenizer: Tokenizer, internal val modelParser: ModelParser) {
    fun read(contents: String): Model =
        tokenizer.tokenize(contents)
            .let { modelParser.reduce(it) }
            .let {
                createFromState(it)
            }

    companion object {
        fun createForBNPParibas() = Mt940Reader(
            Tokenizer(),
            ModelParser()
        )
    }

    val LONG_DATE = DateTimeFormatter.ofPattern("yyMMdd")

    fun createFromState(state: ModelParser.State): Model =
        state.let { (it.entries + it.current.toListIfExists()) }
            .map { model ->
                model.run {
                    Model.Entry(
                        LocalDate.parse(date!!, LONG_DATE),
                        account,
                        sender,
                        title!!,
                        amount!!
                    )
                }
            }
            .let { Model.createFromList(it) }
}


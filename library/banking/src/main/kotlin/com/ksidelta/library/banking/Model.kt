package com.ksidelta.library.mt940

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.SortedSet

data class Model(
    val entries: SortedSet<Entry>
) {
    data class Entry(
        val date: LocalDate,
        val account: String?,
        val sender: String?,
        val title: String,
        val amount: BigDecimal
    )

    companion object {
        val LONG_DATE = DateTimeFormatter.ofPattern("yyMMdd")

        fun createFromState(state: ModelParser.State): Model =
            state.let { (it.entries + it.current.toListIfExists()) }
                .map { model ->
                    model.run {
                        Entry(
                            LocalDate.parse(date!!, LONG_DATE),
                            account,
                            sender,
                            title!!,
                            amount!!
                        )
                    }
                }
                .let { createFromList(it) }

        fun createFromList(list: List<Entry>) =
            list.toSortedSet { a, b ->
                compareValuesBy(
                    a,
                    b,
                    { it.date },
                    { it.account },
                    { it.amount },
                    { it.title })
            }
                .let { Model(it) }

    }

}

fun Collection<Model>.mergeModels(): Model = this.reduce { a, b ->
    Model.createFromList((a.entries + b.entries).toList())
}

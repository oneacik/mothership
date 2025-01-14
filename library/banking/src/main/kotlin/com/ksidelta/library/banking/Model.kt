package com.ksidelta.library.banking

import java.math.BigDecimal
import java.time.LocalDate
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
    ) : Comparable<Entry> {
        override fun compareTo(other: Entry): Int =
            compareValuesBy(
                this, other,
                { it.date },
                { it.account },
                { it.amount },
                { it.title }
            )
    }

    companion object {
        fun createFromList(list: List<Entry>) =
            list.toSortedSet()
                .let { Model(it) }
    }

}

fun Collection<Model>.mergeModels(): Model = this.reduce { a, b ->
    Model.createFromList((a.entries + b.entries).toList())
}

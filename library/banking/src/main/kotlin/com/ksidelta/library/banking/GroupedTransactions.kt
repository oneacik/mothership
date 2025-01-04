package com.ksidelta.library.banking

data class GroupedTransactions<T>(val key: T, val transactions: List<Transaction>) {

    data class SenderAndTitle(val sender: String, val title: String)

    companion object {
        fun Model.groupBySenderAndTitle(): List<GroupedTransactions<SenderAndTitle>> =
            this.entries
                .groupBy { SenderAndTitle(it.sender ?: "unknown", it.title) }
                .entries
                .map { (key, value) ->
                    value
                        .map { it.run { Transaction(amount, date, title) } }
                        .let {
                            GroupedTransactions(key, it)
                        }
                }

    }
}
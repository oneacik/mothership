package com.ksidelta.library.banking

import java.math.BigDecimal
import java.time.LocalDate

data class Transaction(val amount: BigDecimal, val date: LocalDate, val title: String)

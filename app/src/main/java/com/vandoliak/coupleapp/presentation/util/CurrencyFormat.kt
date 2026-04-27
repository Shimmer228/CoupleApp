package com.vandoliak.coupleapp.presentation.util

import com.vandoliak.coupleapp.data.local.AppCurrency
import java.util.Locale

fun formatCurrency(amount: Double, currency: AppCurrency): String {
    val formatted = String.format(Locale.US, "%.2f", amount)
    return if (currency.suffix) {
        "$formatted ${currency.symbol}"
    } else {
        "${currency.symbol}$formatted"
    }
}

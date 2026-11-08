package com.adhamelgendy.nixura.presentation.platform

import java.util.Currency
import java.util.Locale

actual fun localeCurrencyCode(): String? = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull()

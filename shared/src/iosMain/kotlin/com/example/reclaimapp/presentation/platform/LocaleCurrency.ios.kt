package com.example.reclaimapp.presentation.platform

import platform.Foundation.NSLocale
import platform.Foundation.currencyCode
import platform.Foundation.currentLocale

actual fun localeCurrencyCode(): String? = NSLocale.currentLocale.currencyCode

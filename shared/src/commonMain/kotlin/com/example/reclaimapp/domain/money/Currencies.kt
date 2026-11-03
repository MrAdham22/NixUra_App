package com.example.reclaimapp.domain.money

import kotlin.math.abs
import kotlin.math.roundToLong

/** A currency the money tracker offers: ISO code, digits after the decimal point, and how it is written. */
data class CurrencyInfo(val code: String, val decimals: Int, val symbol: String, val symbolFirst: Boolean = true)

object Currencies {
    const val DEFAULT = "EGP"

    val all: List<CurrencyInfo> = listOf(
        CurrencyInfo("EGP", 2, "EGP"),
        CurrencyInfo("USD", 2, "$"),
        CurrencyInfo("EUR", 2, "€"),
        CurrencyInfo("GBP", 2, "£"),
        CurrencyInfo("SAR", 2, "SAR"),
        CurrencyInfo("AED", 2, "AED"),
        CurrencyInfo("KWD", 3, "KWD"),
        CurrencyInfo("QAR", 2, "QAR"),
        CurrencyInfo("BHD", 3, "BHD"),
        CurrencyInfo("OMR", 3, "OMR"),
        CurrencyInfo("JOD", 3, "JOD"),
        CurrencyInfo("MAD", 2, "MAD"),
        CurrencyInfo("TRY", 2, "₺"),
        CurrencyInfo("INR", 2, "₹"),
        CurrencyInfo("CAD", 2, "CA$"),
        CurrencyInfo("AUD", 2, "A$"),
    )

    fun of(code: String): CurrencyInfo = all.firstOrNull { it.code == code } ?: CurrencyInfo(code, 2, code)

    /** The device's currency when it is one we offer, otherwise [DEFAULT]. */
    fun defaultFor(localeCurrency: String?): String = all.firstOrNull { it.code == localeCurrency }?.code ?: DEFAULT

    /** Parses what the user typed ("35", "35.5", "35,50") into minor units; `null` when it is not a price. */
    fun parseMinor(text: String, code: String): Long? {
        val cleaned = text.trim().replace(',', '.')
        if (cleaned.isEmpty() || cleaned.count { it == '.' } > 1) return null
        val value = cleaned.toDoubleOrNull()?.takeIf { it >= 0 && it < 1e9 } ?: return null
        return (value * pow10(of(code).decimals)).roundToLong()
    }

    /** Minor units back to an editable number ("35.5"), without trailing zeros. */
    fun toInput(minor: Long, code: String): String {
        val d = of(code).decimals
        val whole = minor / pow10(d).toLong()
        val frac = (minor % pow10(d).toLong()).toString().padStart(d, '0').trimEnd('0')
        return if (frac.isEmpty()) whole.toString() else "$whole.$frac"
    }

    /** "EGP 1,234.50", "$12.40": grouped thousands, the currency's own decimals. */
    fun format(minor: Long, code: String): String {
        val info = of(code)
        val unit = pow10(info.decimals).toLong()
        val whole = abs(minor) / unit
        val frac = (abs(minor) % unit).toString().padStart(info.decimals, '0')
        val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
        val number = (if (minor < 0) "-" else "") + grouped + if (info.decimals > 0) ".$frac" else ""
        val tight = info.symbol.length == 1 || info.symbol.endsWith("$")
        return if (tight) "${info.symbol}$number" else "${info.symbol} $number"
    }

    fun minorPerMajor(code: String): Long = pow10(of(code).decimals).toLong()

    /** Whole units only ("EGP 340"), for places that cannot update every second. */
    fun formatWhole(minor: Long, code: String): String {
        val info = of(code)
        val whole = (minor / minorPerMajor(code)).toString().reversed().chunked(3).joinToString(",").reversed()
        val tight = info.symbol.length == 1 || info.symbol.endsWith("$")
        return if (tight) "${info.symbol}$whole" else "${info.symbol} $whole"
    }

    private fun pow10(n: Int): Double {
        var r = 1.0
        repeat(n) { r *= 10 }
        return r
    }
}

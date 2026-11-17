package com.adhamelgendy.nixura.presentation.strings

/** The six plural forms Arabic uses (CLDR): ٠ أيام، يوم، يومان، ٣ أيام، ١١ يومًا، ١٠٠ يوم. */
enum class PluralCategory { ZERO, ONE, TWO, FEW, MANY, OTHER }

/** Arabic plural rules (CLDR). Pure Kotlin, shared with iOS. */
object ArabicPlurals {

    fun category(count: Long): PluralCategory {
        val n = if (count < 0) -count else count
        val mod100 = n % 100
        return when {
            n == 0L -> PluralCategory.ZERO
            n == 1L -> PluralCategory.ONE
            n == 2L -> PluralCategory.TWO
            mod100 in 3..10 -> PluralCategory.FEW
            mod100 in 11..99 -> PluralCategory.MANY
            else -> PluralCategory.OTHER
        }
    }

    /** Picks the right form for [count]. Forms are written with `%d` where the number goes. */
    fun format(count: Long, zero: String, one: String, two: String, few: String, many: String, other: String): String {
        val form = when (category(count)) {
            PluralCategory.ZERO -> zero
            PluralCategory.ONE -> one
            PluralCategory.TWO -> two
            PluralCategory.FEW -> few
            PluralCategory.MANY -> many
            PluralCategory.OTHER -> other
        }
        return form.replace("%d", count.toString())
    }
}

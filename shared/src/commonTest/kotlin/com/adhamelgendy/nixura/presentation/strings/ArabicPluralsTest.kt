package com.adhamelgendy.nixura.presentation.strings

import kotlin.test.Test
import kotlin.test.assertEquals

class ArabicPluralsTest {
    private fun days(n: Long) = ArabicPlurals.format(n, "%d يوم", "يوم", "يومان", "%d أيام", "%d يومًا", "%d يوم")

    @Test
    fun followsTheSixArabicForms() {
        assertEquals(PluralCategory.ZERO, ArabicPlurals.category(0))
        assertEquals("يوم", days(1))
        assertEquals("يومان", days(2))
        assertEquals("3 أيام", days(3))
        assertEquals("10 أيام", days(10))
        assertEquals("11 يومًا", days(11))
        assertEquals("99 يومًا", days(99))
        assertEquals("100 يوم", days(100))
        assertEquals("103 أيام", days(103))
        assertEquals("111 يومًا", days(111))
    }
}

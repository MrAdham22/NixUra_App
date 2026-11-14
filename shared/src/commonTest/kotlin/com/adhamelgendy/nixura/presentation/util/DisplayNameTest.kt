package com.adhamelgendy.nixura.presentation.util

import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayNameTest {
    @Test
    fun firstLetterIsCapitalized() = assertEquals("Smoking", "smoking".forDisplay())

    @Test
    fun theRestStaysAsTyped() = assertEquals("Junk Food", "Junk Food".forDisplay())

    @Test
    fun aNameStartingWithADigitIsLeftAlone() = assertEquals("5 am walk", "5 am walk".forDisplay())

    @Test
    fun aPersonGetsEveryWordCapitalized() = assertEquals("Adham Ahmed Elgendy", "adham  ahmed elgendy ".personForDisplay())

    @Test
    fun arabicIsUnchanged() = assertEquals("تدخين", "تدخين".forDisplay())
}

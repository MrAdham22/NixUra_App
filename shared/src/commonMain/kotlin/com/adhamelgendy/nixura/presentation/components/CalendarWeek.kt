package com.adhamelgendy.nixura.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import com.adhamelgendy.nixura.presentation.locale.LocalAppLocale

/** The first day of a week in the month calendar. [isoDay]: 1 = Monday … 7 = Sunday. */
enum class WeekStart(val isoDay: Int) {
    SUNDAY(7),
    SATURDAY(6),
    ;

    /** Column (0..6) of [dayIndex] in a week that starts on this day. */
    fun columnOf(dayIndex: Long): Int = (CivilCalendar.isoWeekday(dayIndex) - isoDay).mod(7)

    /** [isoLetters] (Monday first, as `Strings.weekdayLetters`) reordered to start on this day. */
    fun <T> reorder(isoLetters: List<T>): List<T> = List(7) { isoLetters[(isoDay - 1 + it) % 7] }

    companion object {
        /**
         * NixUra's weeks start on Sunday (owner decision, 2026-11-14); Arabic weeks start on Saturday. A right-to-left
         * layout also counts as Arabic, so RTL previews that do not set the language show it too.
         */
        fun forLocale(languageCode: String?, rtl: Boolean): WeekStart =
            if (rtl || languageCode?.lowercase()?.substringBefore('-')?.substringBefore('_') == "ar") SATURDAY else SUNDAY
    }
}

/** The week start for the app's language (the grid itself runs right to left on its own in an RTL layout). */
@Composable
fun rememberWeekStart(): WeekStart =
    WeekStart.forLocale(LocalAppLocale.current.language, LocalLayoutDirection.current == LayoutDirection.Rtl)

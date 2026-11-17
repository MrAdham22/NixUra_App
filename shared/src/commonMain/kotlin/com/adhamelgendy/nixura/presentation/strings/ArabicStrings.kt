package com.adhamelgendy.nixura.presentation.strings

/**
 * Arabic text (friendly Modern Standard Arabic). Anything not overridden here falls back to English.
 * Not selectable yet: the language switch, RTL checks and the remaining screens come in the localization round
 * (see HANDOFF.md, Session 4). Until then every new string is added here as well as in [Strings].
 */
object ArabicStrings : Strings() {
    // Starter guide
    override val guideStartHereTitle: String = "ابدأ من هنا"
    override val guideStartHereBody: String = "اختر شيئًا واحدًا تبدأ به: عادة تتركها، أو عادة تبنيها، أو مناسبة تعدّ الأيام حتى موعدها. ستظهر هنا حلقات تقدّمك بعد أن تضيفه."

    // Home
    override val homeLoading: String = "جارٍ تحميل عاداتك"

    // ---- Session 2 (Add/Edit form). Only Session 2 adds lines here. ----

    // ---- end of Session 2 ----

    // ---- Session 3 (details, calendar, cards). Only Session 3 adds lines here. ----
    override val waterProgressLiters: (String, String) -> String = { drunk, goal -> "$drunk / $goal لتر" }
    override val waterProgressCups: (String, String, Int) -> String = { drunk, goal, n -> "$drunk / $goal ${cupNoun(n)}" }
    override val waterLeftLiters: (String) -> String = { "باقٍ $it لتر" }
    override val waterLeftCups: (String, Int) -> String = { amount, n -> "باقٍ $amount ${cupNoun(n)}" }
    override val waterAddCup: String = "+1 كوب"
    override val waterAddMl: (Int) -> String = { "+$it مل" }
    override val waterAddA11y: (Int) -> String = { "أضف كوبًا بحجم $it مل" }
    override val calendarLess: String = "أقل"
    override val calendarMore: String = "أكثر"
    override val calendarLegendA11y: String = "يدلّ اللون على مقدار ما أُنجز في كل يوم، من الأقل إلى الأكثر"

    /** The noun after a written number: 3–10 أكواب, 11–99 كوبًا, otherwise كوب. */
    private fun cupNoun(n: Int): String = when (ArabicPlurals.category(n.toLong())) {
        PluralCategory.FEW -> "أكواب"
        PluralCategory.MANY -> "كوبًا"
        else -> "كوب"
    }

    // ---- end of Session 3 ----
}

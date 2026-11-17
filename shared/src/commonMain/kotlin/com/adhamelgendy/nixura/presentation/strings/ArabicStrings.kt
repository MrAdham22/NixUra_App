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

    // ---- end of Session 3 ----
}

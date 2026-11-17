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
    override val waterGoalTitle: String = "هدف الماء اليومي"
    override val waterGoalHint: String = "حدّده باللتر أو بالأكواب، والتبديل بينهما لا يغيّر الهدف."
    override val waterUnitLiters: String = "لتر"
    override val waterUnitCups: String = "أكواب"
    override fun waterLiters(amount: String): String = "$amount لتر"
    override fun waterCups(amount: String, wholeCups: Int): String =
        if (amount != wholeCups.toString()) "$amount كوب"
        else ArabicPlurals.format(wholeCups.toLong(), "%d كوب", "كوب واحد", "كوبان", "%d أكواب", "%d كوبًا", "%d كوب")
    override val waterCupSize: String = "حجم الكوب"
    override val waterCupSizeHint: String = "كل لمسة على البطاقة تضيف كوبًا واحدًا."
    override fun waterMl(ml: Int): String = "$ml مل"
    override fun waterGoalSummary(cups: Int, servingMl: Int): String =
        ArabicPlurals.format(cups.toLong(), "%d كوب", "كوب واحد", "كوبان", "%d أكواب", "%d كوبًا", "%d كوب") + " بحجم $servingMl مل يوميًا"
    override val waterCupSizeChanged: String = "الأيام التي سجّلتها ستُحسب بحجم الكوب الجديد."
    override val waterGoalDecrease: String = "خفّض الهدف"
    override val waterGoalIncrease: String = "ارفع الهدف"

    // ---- end of Session 2 ----

    // ---- Session 3 (details, calendar, cards). Only Session 3 adds lines here. ----

    // ---- end of Session 3 ----
}

package com.adhamelgendy.nixura.presentation.strings

import com.adhamelgendy.nixura.domain.locale.ArabicPlurals
import com.adhamelgendy.nixura.domain.locale.PluralCategory
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.money.CostUnit
import com.adhamelgendy.nixura.domain.notification.ReminderError
import com.adhamelgendy.nixura.domain.reminder.Prayer
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.coach.arDays
import com.adhamelgendy.nixura.presentation.util.formatClockMinutes

/**
 * Arabic text: friendly Modern Standard Arabic, written for the meaning rather than word for word. Counts go through
 * [ArabicPlurals] (يوم / يومان / 3 أيام / 11 يومًا / 100 يوم). Anything not overridden here falls back to English, so
 * every new string in [Strings] needs its Arabic line here too. Strings the owner should review are listed in
 * HANDOFF.md (Session 4, "Arabic to review").
 *
 * Numbers are written with Western digits here; Settings → Digits turns them into Arabic-Indic digits when drawn.
 */
object ArabicStrings : Strings() {

    /** Left-to-right mark: keeps a sign with its number ("+300 مل", not "300+ مل") inside right-to-left text. */
    private const val LRM = "\u200E"

    /**
     * Right-to-left mark: put first on a line that can start with a typed name, so a Latin name ("Reading") does not
     * flip the whole line to left to right. A second one after the name keeps "0 \u0645\u0646 2" in order.
     */
    private const val RLM = "\u200F"

    // ---------- plural helpers ----------
    private fun pl(n: Long, one: String, two: String, few: String, many: String, other: String = many.replace("ًا", "")): String =
        ArabicPlurals.format(n, "%d $other", one, two, "%d $few", "%d $many", "%d $other")

    internal fun nDays(n: Long) = pl(n, "يوم واحد", "يومان", "أيام", "يومًا", "يوم")
    internal fun nHours(n: Long) = pl(n, "ساعة واحدة", "ساعتان", "ساعات", "ساعة", "ساعة")
    internal fun nMinutes(n: Long) = pl(n, "دقيقة واحدة", "دقيقتان", "دقائق", "دقيقة", "دقيقة")
    internal fun nSeconds(n: Long) = pl(n, "ثانية واحدة", "ثانيتان", "ثوانٍ", "ثانية", "ثانية")
    internal fun nWeeks(n: Long) = pl(n, "أسبوع واحد", "أسبوعان", "أسابيع", "أسبوعًا", "أسبوع")
    internal fun nMonths(n: Long) = pl(n, "شهر واحد", "شهران", "أشهر", "شهرًا", "شهر")
    internal fun nYears(n: Long) = pl(n, "سنة واحدة", "سنتان", "سنوات", "سنة", "سنة")
    private fun times(n: Long) = pl(n, "مرة واحدة", "مرتان", "مرات", "مرة", "مرة")
    private fun items(n: Long) = pl(n, "عنصر واحد", "عنصران", "عناصر", "عنصرًا", "عنصر")
    private fun messages(n: Long) = pl(n, "رسالة واحدة", "رسالتان", "رسائل", "رسالة", "رسالة")
    private fun slips(n: Long) = pl(n, "تعثّر واحد", "تعثّران", "تعثّرات", "تعثّرًا", "تعثّر")
    private fun milestones(n: Long) = pl(n, "محطة واحدة", "محطتان", "محطات", "محطة", "محطة")

    // Navigation
    override val navHome: String = "الرئيسية"
    override val navQuit: String = "الإقلاع"
    override val navBuild: String = "البناء"
    override val navEvents: String = "المناسبات"
    override val navCoach: String = "المدرّب"

    // Common
    override val cancel: String = "إلغاء"
    override val save: String = "حفظ"
    override val saveChanges: String = "حفظ التغييرات"
    override val done: String = "تم"
    override val add: String = "إضافة"
    override val delete: String = "حذف"
    override val remove: String = "إزالة"
    override val seeAll: String = "عرض الكل"
    override val reached: String = "تحقّق"
    override val habitNotFound: String = "هذه العادة لم تعد موجودة."
    override val am: String = "ص"
    override val pm: String = "م"

    /** Known English messages from the use cases, in Arabic. Unknown ones get a general message. */
    override fun errorText(message: String): String {
        val m = message.trim()
        return when {
            m.startsWith("Habit name cannot be empty") -> "اكتب اسم العادة."
            m.startsWith("Habit name must be at most") -> "اسم العادة طويل جدًا. اختصره قليلًا."
            m.startsWith("Event name is required") -> "اكتب اسم المناسبة."
            m.startsWith("Event name must be at most") -> "اسم المناسبة طويل جدًا. اختصره قليلًا."
            m.startsWith("Choose a date and time") -> "اختر التاريخ والوقت."
            m.startsWith("Daily target must be between") -> "اختر هدفًا يوميًا بين 1 و99 مرة."
            m.startsWith("Name is too long") -> "الاسم طويل جدًا."
            m.startsWith("Enter a valid email") -> "اكتب بريدًا إلكترونيًا صحيحًا."
            m.startsWith("Enter a valid age") -> "اكتب عمرًا صحيحًا."
            m.startsWith("Units per day") -> "اكتب كم مرة في اليوم (أكثر من صفر)."
            m.startsWith("Price can't be negative") -> "لا يمكن أن يكون السعر بالسالب."
            m.startsWith("Water goal must be between") -> "اختر هدفًا للماء بين 250 مل و6 لترات."
            m.startsWith("Cup size must be between") -> "اختر حجم كوب بين 100 و1000 مل."
            m.startsWith("Message is empty") -> "اكتب رسالتك أولًا."
            m.startsWith("Message is too long") -> "رسالتك طويلة جدًا. اختصرها قليلًا."
            m.startsWith("Habit not found") || m.startsWith("Event") && m.endsWith("no longer exists") -> "هذا العنصر لم يعد موجودًا."
            m.startsWith("Cannot record relapse on archived habit") -> "لا يمكن تسجيل تعثّر على عادة مؤرشفة."
            m.startsWith("Could not save settings") -> "تعذّر حفظ الإعدادات."
            m.startsWith("Could not erase data") -> "تعذّر مسح البيانات."
            m.startsWith("Could not create habit") -> "تعذّرت إضافة العادة."
            m.startsWith("Could not save changes") || m.startsWith("Could not save") -> "تعذّر حفظ التغييرات."
            m.startsWith("Could not record relapse") -> "تعذّر تسجيل التعثّر."
            else -> "حدث خطأ ما. حاول مرة أخرى."
        }
    }

    // Units
    override val pickerHour: String = "الساعة"
    override val pickerMinute: String = "الدقيقة"
    override val unitDays: String = "أيام"
    override val unitHours: String = "ساعات"
    override val unitMinutes: String = "دقائق"
    override val unitSeconds: String = "ثوانٍ"
    // Monday first, like the English list: الاثنين، الثلاثاء، الأربعاء، الخميس، الجمعة، السبت، الأحد.
    override val weekdayLetters: List<String> = listOf("ن", "ث", "ر", "خ", "ج", "س", "ح")
    override val weekdayShort: List<String> = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
    override val monthNames: List<String> = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
    )
    override val monthShort: List<String> = monthNames
    override val daysCount: (Int) -> String = { nDays(it.toLong()) }
    override fun durationWords(duration: kotlin.time.Duration): String = com.adhamelgendy.nixura.domain.coach.durationText(duration, arabic = true)
    override fun dateText(weekday: String?, day: Int, month: String, year: Int?): String {
        val base = if (year != null) "$day $month $year" else "$day $month"
        return if (weekday != null) "$weekday، $base" else base
    }
    override fun clockText(hour12: Int, minute: String, suffix: String): String = "$hour12:$minute $suffix"
    override fun dateTimeText(date: String, time: String): String = "$date · $time"
    override fun monthYear(month: String, year: Int): String = "$month $year"

    // Home
    override val greetingMorning: String = "صباح الخير"
    override val greetingAfternoon: String = "طاب يومك"
    override val greetingEvening: String = "مساء الخير"
    override val todaysFocus: String = "تركيز اليوم"
    override val momentum: String = "الزخم"
    override val momentumDelta: (Int) -> String = { if (it >= 0) "$LRM+$it نقطة عن الأسبوع الماضي" else "$LRM$it نقطة عن الأسبوع الماضي" }
    override val quittingSection: String = "أقلع عنها"
    override val buildingSection: String = "أبنيها"
    override val nextEventSection: String = "المناسبة القادمة"

    // Bad habits
    override val badEyebrow: String = "تحرّر"
    override val badTitle: String = "عادات تقلع عنها"
    override val badSubtitle: String = "تصبح بطاقاتك أفتح كلما طالت سلسلتك."
    override val badEmptyTitle: String = "لا شيء تقلع عنه بعد"
    override val badEmptyBody: String = "أخبر NixUra بعادة تريد تركها، وسيبدأ العدّاد فورًا."
    override val badAdd: String = "أضف عادة تقلع عنها"
    override val statHabitsQuitting: String = "أقلع عنها"
    override val statBestActive: String = "أطول سلسلة الآن"
    override val clearFor: String = "صامد منذ"
    override val longestLabel: (String) -> String = { "أطول سلسلة: $it" }
    override val longestYet: String = "أطول سلسلة لك حتى الآن"
    override fun moneySavedShort(amount: String): String = "وفّرت $amount"
    override val cravingAction: String = "رغبة قوية؟ تحدّث عنها"
    override val cravingShort: String = "رغبة قوية؟"
    override fun counterSpoken(days: Long?, hours: Long?, minutes: Long?, seconds: Long?): String = listOfNotNull(
        days?.let { nDays(it) },
        hours?.let { nHours(it) },
        minutes?.let { nMinutes(it) },
        seconds?.let { nSeconds(it) },
    ).joinToString("، ")
    override fun passedAgo(duration: String): String = "منذ $duration"
    override val slipAction: String = "تعثّرت"
    override val cardMoreOptions: String = "خيارات أخرى"
    override fun cravingPrompt(habit: String): String = "أشعر برغبة قوية في $habit الآن. ساعدني أتجاوزها."
    override val statNextWin: String = "الإنجاز القادم"
    override val statSaved: String = "وفّرت"
    override val infoNextWin: String = "أقرب محطة بين عاداتك التي تقلع عنها. اصمد حتى تصلها: كل محطة تبلغها إنجاز، والتعثّر لا يمحو ما حقّقته قبله."
    override fun nextWinLine(habit: String, left: String): String = "$habit · بعد $left"
    override fun nextMilestone(milestone: String, left: String): String = "التالي: $milestone · $left"
    override fun stageOf(stage: Int, count: Int): String = "المرحلة $stage من $count"
    override fun milestoneName(days: Int): String = when {
        days < 7 -> nDays(days.toLong())
        days == 7 -> "أسبوع"
        days < 30 && days % 7 == 0 -> nWeeks((days / 7).toLong())
        days == 30 -> "شهر"
        days < 365 && days % 30 == 0 -> nMonths((days / 30).toLong())
        days == 180 -> "6 أشهر"
        days == 365 -> "سنة"
        days % 365 == 0 -> nYears((days / 365).toLong())
        else -> nDays(days.toLong())
    }
    override fun timeLeftShort(days: Long, hours: Long, minutes: Long): String = when {
        days > 0 -> "${days}ي ${hours}س"
        hours > 0 -> "${hours}س ${minutes}د"
        minutes > 0 -> "${minutes}د"
        else -> "أقل من دقيقة"
    }

    // Bad habit form
    override val newBadHabit: String = "عادة جديدة تقلع عنها"
    override val editHabit: String = "تعديل العادة"
    override val habitNameLabel: String = "ما الذي تريد التوقف عنه؟"
    override val goodHabitNameLabel: String = "ما الذي تريد بناءه؟"
    override val habitNamePlaceholder: String = "مثل: التدخين، المشروبات الغازية، التصفّح ليلًا"
    override val coachUnderstood: String = "فهم مدرّبك"
    override val firstMilestone: String = "أول محطة"
    override val cardColor: String = "لون البطاقة"
    override val colorAuto: String = "تلقائي"
    override val cardColorHint: String = "تبدأ بطاقتك بدرجة داكنة من هذا اللون، وتفتح يومًا بعد يوم."
    override val shadeDayZero: String = "اليوم 0"
    override val shadeDayThirty: String = "اليوم 30+"
    override val lastTimeLabel: String = "متى كانت آخر مرة؟"
    override val startNow: String = "الآن"
    override val startHourAgo: String = "قبل ساعة"
    override val startYesterday: String = "أمس"
    override val startThreeDays: String = "قبل 3 أيام"
    override val startWeek: String = "قبل أسبوع"
    override val startCustom: String = "وقت آخر"
    override val lastTimeHint: String = "كن صادقًا مع نفسك، فالعدّاد يبدأ من هذه اللحظة."
    override val startTracking: String = "ابدأ العدّ"

    // Bad habit detail
    override val nextShadeIn: (String) -> String = { "الدرجة التالية للبطاقة بعد $it" }
    override val shadeMax: String = "وصلت بطاقتك إلى أفتح درجاتها."
    override val statCurrent: String = "السلسلة الحالية"
    override val statBest: String = "أطول سلسلة"
    override val statRelapses: String = "مرات التعثّر"
    override val statTrackingSince: String = "تتابعها منذ"
    override val coachNote: String = "ملاحظة المدرّب"
    override val askCoach: String = "تحدّث مع مدرّبك"
    override val timelineTitle: String = "كيف يتعافى جسمك وذهنك"
    override val timelineMilestones: String = "المحطات"
    override fun timelineReachedCount(reached: Int, total: Int): String = "بلغت $reached من $total"
    override val timelineUpNext: String = "التالي"
    override fun timelineIn(time: String): String = "بعد $time"
    override fun timelineAfter(time: String): String = "بعد $time"
    override val timelineAllReached: String = "بلغت كل المحطات. واصل."
    override fun timelineShowEarlier(count: Int): String = "اعرض ${milestones(count.toLong())} سابقة"
    override val timelineHideEarlier: String = "أخفِ المحطات السابقة"
    override fun timelineEarlierCount(count: Int): String = "$LRM+$count"
    override val medicalDisclaimer: String = "معلومات عامة عن الصحة والعافية، وليست نصيحة طبية. استشر طبيبًا في أي أمر يخص صحتك."
    override val relapseHistory: String = "سجلّ التعثّر"
    override val recordRelapse: String = "سجّل تعثّرًا"
    override val relapseHint: String = "التعثّر ليس فشلًا. تسجيله يُبقي تقدّمك صادقًا ويعيد العدّاد من جديد."
    override val relapseConfirmTitle: String = "تسجيل تعثّر؟"
    override val relapseConfirmBody: String = "سيبدأ العدّاد من الآن، وتبقى أطول سلسلة لك محفوظة في سجلّك."

    // Smart recognition
    override val looksGoodBanner: String = "تبدو هذه عادة جيدة. هل تضيفها إلى البناء بدلًا من ذلك؟"
    override val moveToBuild: String = "أضفها إلى البناء"
    override val looksBadBanner: String = "تبدو هذه عادة للإقلاع عنها. هل تتابعها كعادة تقلع عنها؟"
    override val moveToQuit: String = "تابعها للإقلاع"
    override val didYouMean: (String) -> String = { "هل تقصد «$it»؟" }

    // Good habits
    override val goodEyebrow: String = "ارتقِ"
    override val goodTitle: String = "عادات تبنيها"
    override val goodSubtitle: String = "خطوات صغيرة تتكرّر كل يوم."
    override val goodEmptyTitle: String = "لا عادات جيدة بعد"
    override val goodEmptyBody: String = "أضف شيئًا صغيرًا، كوب ماء أو مشيًا قصيرًا، وسجّله كل يوم."
    override val goodAdd: String = "أضف عادة جيدة"
    override val newGoodHabit: String = "عادة جيدة جديدة"
    override val goodNamePlaceholder: String = "مثل: اشرب الماء، المشي صباحًا"
    override val quickPicks: String = "اختيارات سريعة"
    override val goodSuggestions: List<Pair<String, Int>> = listOf(
        "اشرب الماء" to 8,
        "المشي صباحًا" to 1,
        "تمرين رياضي" to 1,
        "تأمّل" to 1,
        "الصلاة" to 5,
    )
    override val prayerNames: List<String> = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
    override val prayerHint: String = "مضبوطة على الصلوات الخمس. غيّر العدد متى شئت."
    override val dailyTarget: String = "الهدف اليومي"
    override val dailyTargetHint: String = "كم مرة تكفي ليُحسب اليوم منجزًا"
    override val addHabit: String = "أضف العادة"
    override val todayLabel: String = "اليوم"
    override val habitsDoneToday: (Int, Int) -> String = { done, total -> "أنجزت $done من $total" }
    override val goodPlusA11y: (String) -> String = { name -> "سجّل مرة لـ $name" }
    override val goodMinusA11y: (String) -> String = { name -> "احذف مرة من $name" }
    override val todayProgress: (Int, Int) -> String = { count, target -> "$count / $target اليوم" }
    override val streakDays: (Int) -> String = { "${nDays(it.toLong())} على التوالي" }
    override val timesDoneTotal: (Int) -> String = { "${times(it.toLong())} إجمالًا" }
    override val goalReached: String = "حقّقت هدف اليوم"
    override val timesToGo: (Int) -> String = { "بقيت ${times(it.toLong())}" }
    override val statCurrentStreak: String = "السلسلة الحالية"
    override val statBestStreak: String = "أطول سلسلة"
    override val statTotalTimes: String = "مرات الإنجاز"
    override val statDaysCompleted: String = "أيام مكتملة"
    override val last7Days: String = "آخر 7 أيام"
    override val last30Days: String = "آخر 30 يومًا"
    override val calendarTitle: String = "التقويم"
    override val legendDone: String = "منجز"
    override val legendPartial: String = "جزئيًا"
    override val legendMissed: String = "فائت"
    override val editPastHint: String = "نسيت تسجيل يوم؟ عدّله من هنا."

    // Weekly good habits
    override val habitFrequency: String = "كم مرة"
    override val frequencyDaily: String = "كل يوم"
    override val frequencyWeekly: String = "أيام في الأسبوع"
    override val weeklyTarget: String = "أيام في الأسبوع"
    override val notDoneYet: String = "لم تُنجز بعد"
    override val weeklyTargetHint: String = "أي أيام من السبت إلى الجمعة. كل يوم يُحسب مرة واحدة."
    override val weekProgress: (Int, Int) -> String = { done, goal -> "$done / $goal أيام هذا الأسبوع" }
    override val weekGoalReached: String = "حقّقت هدف الأسبوع"
    override val daysToGoThisWeek: (Int) -> String = { "بقي ${nDays(it.toLong())} هذا الأسبوع" }
    override val thisWeekLabel: String = "هذا الأسبوع"
    override val last4Weeks: String = "آخر 4 أسابيع"
    override val weeksCount: (Int) -> String = { nWeeks(it.toLong()) }
    override val streakWeeks: (Int) -> String = { "${nWeeks(it.toLong())} على التوالي" }
    override val streakStartToday: String = "ابدأ سلسلة اليوم"
    override fun alreadyTracking(name: String): String = "أنت تتابع «$name» بالفعل. إضافتها مجددًا تنشئ بطاقة ثانية."
    override val streakStartWeek: String = "ابدأ سلسلة هذا الأسبوع"
    override val daysDoneTotal: (Int) -> String = { "${nDays(it.toLong())} إجمالًا" }
    override val statDaysDone: String = "أيام الإنجاز"
    override val statWeeksCompleted: String = "أسابيع مكتملة"
    override val infoCurrentStreakWeekly: String = "عدد الأسابيع المتتالية التي حقّقت فيها الهدف الأسبوعي. يُحسب هذا الأسبوع بمجرد أن تحقّقه، والأسبوع الفائت يعيد العدّ من البداية."
    override val infoBestStreakWeekly: String = "أكبر عدد من الأسابيع المتتالية حقّقت فيه الهدف الأسبوعي."
    override val infoDaysDoneWeekly: String = "كل يوم أنجزت فيه هذه العادة، في كل الأسابيع معًا. اليوم يُحسب مرة واحدة."
    override val infoWeeksCompleted: String = "عدد الأسابيع التي حقّقت فيها الهدف الأسبوعي كاملًا."
    override val disciplineWaiting: String = "تُحسب عاداتك الأسبوعية بعد أن يتحقّق هدف هذا الأسبوع."

    // Events
    override val eventsEyebrow: String = "تطلّع"
    override val eventsTitle: String = "مناسبات مهمة"
    override val eventsSubtitle: String = "عدّ تنازلي لما يهمّك."
    override val eventsEmptyTitle: String = "لا مناسبات بعد"
    override val eventsEmptyBody: String = "أضف رحلة أو موعدًا نهائيًا أو احتفالًا، وتابع العدّ التنازلي."
    override val eventsAdd: String = "أضف مناسبة"
    override val upcoming: String = "القادمة"
    override val pastEvents: String = "مناسبات مضت"
    override val startsIn: String = "تبدأ بعد"
    override val passedSince: String = "مضى عليها"
    override val newEvent: String = "مناسبة جديدة"
    override val editEvent: String = "تعديل المناسبة"
    override val eventNameLabel: String = "اسم المناسبة"
    override val eventNamePlaceholder: String = "مثل: رحلة إلى سيوة"
    override val eventCategoryLabel: String = "الفئة"
    override val eventWhenLabel: String = "التاريخ والوقت"
    override val eventPastWarning: String = "هذه اللحظة مضت بالفعل، فلن تصلك تذكيرات."
    override val remindMe: String = "ذكّرني"
    override val customReminder: String = "تذكير مخصّص"
    override val reminderHint: String = "اختر ما تشاء من التذكيرات. تصلك كإشعارات على هذا الجهاز."
    override val addOtherReminder: String = "وقت آخر"
    override val addEvent: String = "أضف المناسبة"
    override val deleteEvent: String = "حذف المناسبة"
    override val atEventTime: String = "في وقت المناسبة"

    // Event reminders
    override val remindersTitle: String = "التذكيرات"
    override val remindersEmpty: String = "لا تذكيرات بعد. أضف تذكيرًا ليصلك إشعار قبل هذه اللحظة."
    override val addReminder: String = "أضف تذكيرًا"
    override val editReminder: String = "تعديل التذكير"
    override val removeReminder: String = "إزالة التذكير"
    override val quickAdd: String = "إضافة سريعة"
    override val reminderFiresAt: (String) -> String = { "يصلك في $it" }
    override val reminderPassed: String = "مضى وقته، فلن يصلك"
    override val reminderRemoved: String = "أُزيل التذكير"
    override val undo: String = "تراجع"
    override val reminderUnitLabel: String = "قبلها بكم؟"
    override val reminderDialogSave: String = "احفظ التذكير"
    override val reminderNotificationsOff: String = "الإشعارات متوقفة، لذا لن تصلك التذكيرات."
    override val reminderOpenSettings: String = "افتح الإعدادات"
    override val reminderExactOff: String = "المنبّهات الدقيقة متوقفة. ستصلك التذكيرات، لكن قد تتأخر بضع دقائق."
    override val reminderAllowExact: String = "اسمح بالمنبّهات الدقيقة"
    override val reminderSaveFailed: String = "تعذّر تحديث التذكير. حاول مرة أخرى."
    override val reminderError: (ReminderError) -> String = {
        when (it) {
            ReminderError.NEGATIVE -> "اختر وقتًا قبل المناسبة."
            ReminderError.TOO_FAR_AHEAD -> "يمكن أن يكون التذكير قبل المناسبة بـ 365 يومًا على الأكثر."
            ReminderError.EVENT_PASSED -> "هذه المناسبة مضت بالفعل."
            ReminderError.IN_PAST -> "وقت هذا التذكير مضى. اختر مدة أقصر قبل المناسبة."
            ReminderError.DUPLICATE -> "لديك هذا التذكير بالفعل."
            ReminderError.TOO_MANY -> "يمكن أن يكون للمناسبة 10 تذكيرات على الأكثر."
        }
    }
    override val minutesBefore: (Int) -> String = { "قبلها بـ ${nMinutes(it.toLong())}" }
    override val hoursBefore: (Int) -> String = { "قبلها بـ ${nHours(it.toLong())}" }
    override val daysBefore: (Int) -> String = { "قبلها بـ ${nDays(it.toLong())}" }
    override val categoryLabel: (EventCategory) -> String = {
        when (it) {
            EventCategory.PERSONAL -> "شخصي"
            EventCategory.TRAVEL -> "سفر"
            EventCategory.WORK -> "عمل"
            EventCategory.HEALTH -> "صحة"
            EventCategory.CELEBRATION -> "احتفال"
            EventCategory.OTHER -> "أخرى"
            EventCategory.EVENT -> "مناسبة"
            EventCategory.BIRTHDAY -> "عيد ميلاد"
            EventCategory.TASK -> "مهمة"
            EventCategory.HOLIDAY -> "عطلة"
        }
    }

    // Coach
    override val coachEyebrow: String = "مدرّبك"
    override val coachTitle: String = "مدرّب الذكاء الاصطناعي"
    override val coachTitleOffline: String = "المدرّب"
    override val coachInputHint: String = "اطلب مساعدة أو خطة أو دفعة حماس…"
    override val coachOfflineNote: String = "تُكتب الردود على جهازك. خاصة بك، لكنها لا تغني عن الرعاية المتخصصة."
    override val coachModeOfflineShort: String = "دون اتصال · خاص، على جهازك"
    override val coachPickHabit: String = "اختر عادة تتحدث عنها"

    // What happens if I stop for ...
    override val coachQuickWhatHappens: String = "ماذا يحدث لو توقفت لمدة…"
    override val coachWhatHappensPrefix: String = "ماذا يحدث لو توقفت لمدة "
    override val stopExplorerTitle: String = "ماذا يحدث لو توقفت لمدة…"
    override val stopExplorerHint: String = "اختر مدة أو اكتبها بنفسك. تختلف المدد من شخص لآخر."
    override val stopExplorerNow: String = "سلسلتي"
    override val stopExplorerChipLabels: List<String> = listOf("1س", "3س", "6س", "12س", "1ي", "3ي", "1أ", "2أ", "1ش")
    override val stopExplorerCustomPlaceholder: String = "مثل: 6 ساعات، يومين، 3 أسابيع"
    override val stopExplorerShow: String = "اعرض"
    override val stopExplorerUnreadable: String = "لم أفهم هذه المدة. جرّب «6 ساعات» أو «يومين» (حتى 10 سنوات)."
    override val stopExplorerHappeningLabel: String = "ما يحدث الآن"
    override val stopExplorerNextLabel: String = "ما يأتي بعد ذلك"
    override val stopExplorerNextIn: (String, String) -> String = { title, time -> "$title، بعد نحو $time" }
    override val stopExplorerNothingNext: String = "تجاوزت المحطات الرئيسية في هذا المسار."
    override val stopExplorerTipsLabel: String = "إن جاءتك الرغبة"
    override val stopExplorerSafetyLabel: String = "مهم"

    // Profile
    override val profileTitle: String = "الملف الشخصي"
    override val profileNoName: String = "أضف اسمك"
    override val editProfile: String = "تعديل الملف"
    override val nameLabel: String = "الاسم"
    override val namePlaceholder: String = "بماذا نناديك؟"
    override val emailLabel: String = "البريد الإلكتروني"
    override val ageLabel: String = "العمر"
    override val ageYears: (Int) -> String = { "${nYears(it.toLong())} من العمر" }
    override val yourProgress: String = "تقدّمك"
    override val profileTotals: String = "المجموع"
    override val languageHint: String = "يُطبَّق فورًا، وتصبح الواجهة من اليمين إلى اليسار بالعربية."
    override val languageSystem: String = "لغة الجهاز"
    override val digitsTitle: String = "الأرقام"
    override val digitsHint: String = "طريقة عرض الأرقام. بياناتك المحفوظة لا تتغيّر."
    override val guideReplayHint: String = "جولة في التطبيق خلال 30 ثانية."
    override val privacyAndData: String = "الخصوصية والبيانات"
    override val quitScoreTitle: String = "التحرّر"
    override val quitScoreBody: String = "متوسط تقدّمك نحو 30 يومًا من الصمود"
    override val disciplineScoreTitle: String = "الانضباط"
    override val disciplineScoreBody: String = "إنجازك لعاداتك الجيدة في آخر 7 أيام"
    override val noBadHabitsYet: String = "أضف عادة تقلع عنها لترى هذه النتيجة."
    override val noGoodHabitsYet: String = "أضف عادة جيدة لترى هذه النتيجة."
    override val statClearDays: String = "أيام الصمود"
    override val statCheckIns: String = "مرات التسجيل"
    override val statHabitsTracked: String = "العادات المتابَعة"
    override val nearestEvent: String = "أقرب مناسبة"
    override val noUpcomingEvents: String = "لا مناسبات قادمة. المس لإضافة واحدة."

    // Settings
    override val settingsTitle: String = "الإعدادات"
    override val appearance: String = "المظهر"
    override val darkMode: String = "داكن"
    override val lightMode: String = "فاتح"
    override val language: String = "اللغة"
    override val notifications: String = "الإشعارات"
    override val notificationsToggle: String = "التذكيرات والتشجيع"
    override val notificationsHint: String = "تذكيرات المناسبات، ومحطات سلاسلك، ورسالة يومية قصيرة."
    override val dailyCheckIn: String = "الرسالة اليومية"
    override val dailyCheckInHint: String = "كلمة قصيرة من مدرّبك"
    override val dataTitle: String = "بياناتك"
    override val dataBody: String = "احذف كل عاداتك الجيدة والسيئة ومناسباتك ومحادثاتك مع المدرّب."
    override val eraseData: String = "امسح كل البيانات"
    override val eraseTitle: String = "مسح كل شيء؟"
    override val eraseBody: String = "ستُحذف كل عاداتك الجيدة والسيئة ومناسباتك ومحادثاتك مع المدرّب نهائيًا، ومن حسابك وأجهزتك الأخرى أيضًا إن كنت مسجّل الدخول. يبقى حسابك وإعداداتك."
    override val eraseConfirm: String = "امسح"
    override val about: String = "حول التطبيق"
    override val versionLabel: String = "الإصدار 1.0"
    override val aboutBody: String = "يساعدك NixUra على ترك العادات السيئة، وبناء العادات الجيدة، والاستعداد للحظات التي تهمّك."

    // Widgets
    override val widgetQuitCaption: String = "منذ آخر تعثّر"
    override val widgetDaysSuffix: String = "ي"
    override val widgetNoQuitHabits: String = "لا عادات تقلع عنها بعد"
    override val widgetNoGoodHabits: String = "لا عادات جيدة بعد"
    override val widgetNoEvents: String = "لا مناسبات بعد"
    override val widgetTapToAdd: String = "المس لإضافة واحدة"
    override val widgetTapToChoose: String = "المس لتختار"
    override val widgetTapToChooseAnother: String = "المس لتختار غيرها"
    override val widgetHabitRemoved: String = "حُذفت العادة"
    override val widgetEventRemoved: String = "حُذفت المناسبة"
    override val widgetGoodDone: String = "أنجزتها اليوم"
    override val widgetGoodToGo: String = "متبقٍ"
    override val widgetAddOne: String = "أضف واحدة"
    override val widgetRemoveOne: String = "أزل واحدة"
    override val widgetEventNext: String = "المناسبة القادمة"
    override val widgetEventUntil: String = "حتى"
    override val widgetEventSince: String = "منذ"
    override val widgetError: String = "تعذّر التحميل"
    override val widgetTapToOpen: String = "المس لفتح NixUra"
    override val widgetConfigTitleQuit: String = "اختر عادة تقلع عنها"
    override val widgetConfigTitleGood: String = "اختر عادة جيدة"
    override val widgetConfigTitleEvent: String = "اختر مناسبة"
    override val widgetConfigNearestEvent: String = "أقرب مناسبة قادمة"
    override val widgetConfigEmptyQuit: String = "ليست لديك عادات تقلع عنها بعد. أضف واحدة في NixUra أولًا، أو ضع الأداة الآن والمسها لاحقًا."
    override val widgetConfigEmptyGood: String = "ليست لديك عادات جيدة بعد. أضف واحدة في NixUra أولًا، أو ضع الأداة الآن والمسها لاحقًا."
    override val widgetConfigPlaceAnyway: String = "ضع الأداة على أي حال"
    override val widgetConfigCancel: String = "إلغاء"
    override val widgetAddToHome: String = "أضف إلى الشاشة الرئيسية"
    override val widgetAddToHomeAsk: String = "أكّد في النافذة، ثم ضع الأداة حيث تريد. يمكنك تغيير حجمها ونقلها بعد ذلك."
    override val widgetAddToHomeManual: String = "لا تسمح شاشتك الرئيسية بإضافة الأدوات من هنا. اضغط مطولًا على الشاشة الرئيسية، وافتح الأدوات، واختر NixUra."
    override val widgetAddToHomeFailed: String = "تعذّرت إضافة الأداة. جرّب الضغط مطولًا على الشاشة الرئيسية واختيار NixUra من الأدوات."
    override val widgetTitleQuit: String = "الإقلاع"
    override val widgetTitleBuild: String = "البناء"

    // Accounts
    // "Let's Nix the bad Aura": erase (نمحو, like "Nix") the negative aura (الهالة = aura); the aura words blow away.
    override val landingTagline: String = "لنمحُ"
    override val landingFadingWords: List<String> = listOf("الهالة", "السلبية")
    override val landingBody: String = "اترك ما يستنزفك، وابنِ ما يرفعك، وعُدّ الأيام لما يهمّك."
    override val landingGetStarted: String = "أنشئ حسابًا"
    override val landingHaveAccount: String = "لديّ حساب بالفعل"
    override val landingGuestNote: String = "كل شيء يعمل دون اتصال، وبياناتك تبقى على هذا الهاتف."
    override val authCreateAccount: String = "أنشئ حسابًا"
    override val authSignIn: String = "تسجيل الدخول"
    override val authContinueOffline: String = "المتابعة كضيف"
    override val authOfflineWarnTitle: String = "المتابعة كضيف؟"
    override val authOfflineWarnBody: String = "كضيف، يعمل التطبيق دون اتصال تمامًا. لن تُحفظ بياناتك في السحابة، وقد تضيع إن حذفت التطبيق أو مسحت بياناته أو فقدت هاتفك. يمكنك تسجيل الدخول في أي وقت من صورتك أعلى التطبيق."
    override val authOfflineWarnConfirm: String = "متابعة"
    override val authLeftoverTitle: String = "جلستك السابقة ما زالت هنا"
    override val authLeftoverBody: String = "سُجّل خروجك، لكن بيانات ذلك الحساب ما زالت على هذا الهاتف. سجّل الدخول لتحتفظ بها. الضيف يبدأ فارغًا، لذا تؤدي المتابعة إلى حذفها من هذا الهاتف، بما فيها التغييرات التي لم تُرفع بعد."
    override val authLeftoverStartFresh: String = "ابدأ من جديد"
    override val accountGuestName: String = "ضيف"
    override val accountGuestBody: String = "بياناتك على هذا الهاتف فقط"
    override val authRegisterTitle: String = "أنشئ حسابك"
    override val authRegisterSubtitle: String = "سنرسل إليك رابطًا للتأكد من أنك أنت."
    override val authEmailLabel: String = "البريد الإلكتروني"
    override val authPasswordLabel: String = "كلمة المرور"
    override val authNewPasswordLabel: String = "كلمة مرور جديدة"
    override val authCurrentPasswordLabel: String = "كلمة المرور الحالية"
    override val authPasswordHint: String = "10 أحرف على الأقل. بضع كلمات عشوائية تفي بالغرض."
    override val authShowPassword: String = "أظهر كلمة المرور"
    override val authHidePassword: String = "أخفِ كلمة المرور"
    override val authAgreeText: String = "بإنشاء حساب فأنت توافق على"
    override val authTermsLink: String = "شروط الاستخدام"
    override val authPrivacyLink: String = "سياسة الخصوصية"
    override val authAnd: String = "و"
    override val authLoginTitle: String = "أهلًا بعودتك"
    override val authLoginSubtitle: String = "سجّل الدخول لتكمل من حيث توقفت."
    override val authForgotLink: String = "نسيت كلمة المرور؟"
    override val authNewHere: String = "جديد هنا؟"
    override val authHaveAccount: String = "لديك حساب بالفعل؟"
    override val authVerifyTitle: String = "تفقّد بريدك"
    override fun authVerifySubtitle(email: String): String = "أرسلنا رابطًا إلى $email. المسه ثم عُد إلى هنا، وستنتقل هذه الشاشة وحدها."
    override val authVerifyButton: String = "أكّدته"
    override val authVerifyCheckSpam: String = "لم يصلك شيء؟ تفقّد مجلد الرسائل غير المرغوب فيها."
    override val authResend: String = "أعد الإرسال"
    override fun authResendIn(seconds: Int): String = "أعد الإرسال بعد ${seconds}ث"
    override val authLinkResent: String = "إن كان هذا العنوان يستقبل البريد، فرابط جديد في الطريق."
    override val authForgotTitle: String = "إعادة تعيين كلمة المرور"
    override val authForgotSubtitle: String = "اكتب بريدك وسنرسل إليك رابطًا لاختيار كلمة مرور جديدة."
    override val authSendLink: String = "أرسل الرابط"
    override val authForgotSentTitle: String = "تفقّد بريدك"
    override fun authForgotSent(email: String): String = "إن كان لـ $email حساب، فرابط اختيار كلمة مرور جديدة في الطريق. افتحه واختر كلمة المرور الجديدة، ثم سجّل الدخول هنا."
    override val authOrDivider: String = "أو"
    override val authContinueWithGoogle: String = "المتابعة عبر Google"
    override val authContinueWithFacebook: String = "المتابعة عبر Facebook"
    override val authBack: String = "رجوع"
    override val authStrengthWeak: String = "ضعيفة"
    override val authStrengthFair: String = "مقبولة"
    override val authStrengthGood: String = "جيدة"
    override val authStrengthStrong: String = "قوية"
    override fun authStrengthLabel(level: String): String = "قوة كلمة المرور: $level"
    override val authEmailEmpty: String = "اكتب بريدك الإلكتروني."
    override val authEmailInvalid: String = "اكتب بريدًا إلكترونيًا صحيحًا."
    override val authCodeIncomplete: String = "اكتب الأرقام الستة كلها."
    override val authPasswordRequired: String = "اكتب كلمة المرور."
    override val pwTooShort: String = "استخدم 10 أحرف على الأقل."
    override val pwTooLong: String = "استخدم 128 حرفًا على الأكثر."
    override val pwTooCommon: String = "كلمة المرور هذه شائعة جدًا."
    override val pwSameAsEmail: String = "لا تستخدم بريدك ككلمة مرور."
    override val pwLowVariety: String = "استخدم مزيجًا أكثر تنوّعًا من الأحرف."
    override val pwBreached: String = "ظهرت كلمة المرور هذه في تسريب بيانات. اختر غيرها."
    override val pwSameAsCurrent: String = "اختر كلمة مرور مختلفة عن الحالية."
    override val errInvalidCredentials: String = "البريد الإلكتروني أو كلمة المرور غير صحيحين."
    override val errNotVerified: String = "لم يُؤكَّد بريدك بعد. المس الرابط الذي أرسلناه ثم حاول مجددًا."
    override val errInvalidCode: String = "الرابط غير صحيح أو انتهت صلاحيته. اطلب رابطًا جديدًا."
    override val errNotAvailable: String = "طريقة الدخول هذه غير متاحة على هذا الهاتف بعد."
    override val errAccountExistsOther: String = "لهذا البريد حساب في NixUra يُسجَّل الدخول إليه بطريقة أخرى. استخدم تلك الطريقة."
    override val errEmailInUse: String = "لهذا البريد حساب في NixUra بالفعل. سجّل الدخول بدلًا من ذلك، بكلمة المرور أو عبر Google أو Facebook إن كنت استخدمت أحدهما."
    override val errRecentLogin: String = "حفاظًا على أمانك، سجّل الخروج ثم الدخول مجددًا، وحاول بعدها."
    override val usageStatsToggle: String = "شارك إحصاءات استخدام مجهولة"
    override val usageStatsHint: String = "مثل عدد مرات فتح التطبيق، لتحسين NixUra. لا تشمل عاداتك أو ملاحظاتك أو محادثاتك أبدًا. يمكنك إيقافها في أي وقت."
    override fun errLocked(seconds: Int?): String =
        if (seconds == null) "محاولات كثيرة. حاول لاحقًا." else "محاولات كثيرة. حاول بعد ${nMinutes(((seconds + 59) / 60).toLong())}."
    override val errRateLimited: String = "طلبات كثيرة. انتظر قليلًا."
    override val errNetwork: String = "تعذّر الوصول إلى NixUra. تحقّق من اتصالك وحاول مجددًا."
    override val errServer: String = "حدث خطأ من جهتنا. حاول بعد قليل."
    override val errSessionExpired: String = "انتهت جلستك. سجّل الدخول مجددًا."
    override val errInvalidInput: String = "راجع الحقول وحاول مجددًا."
    override val errUnknown: String = "حدث خطأ ما. حاول مرة أخرى."
    override val accountTitle: String = "الحساب"
    override val accountSignedInAs: String = "مسجّل الدخول باسم"
    override val accountSignedOutBody: String = "تستخدم NixUra على هذا الجهاز فقط. أنشئ حسابًا لتدخل من أي مكان."
    override val accountSignOut: String = "تسجيل الخروج"
    override val accountSignOutHint: String = "يزيل بيانات هذا الحساب من هذا الهاتف."
    override val accountSecurity: String = "الدخول والأمان"
    override val accountChangePasswordHint: String = "ستحتاج الهواتف الأخرى إلى كلمة المرور الجديدة."
    override val accountLegal: String = "قانوني"
    override val legalPrivacyHint: String = "كيف يتعامل NixUra مع بياناتك."
    override val legalTermsHint: String = "قواعد استخدام NixUra."
    override val accountDangerZone: String = "منطقة الخطر"
    override val accountDeleteHint: String = "يحذف حسابك نهائيًا."
    override val accountSignOutTitle: String = "تسجيل الخروج؟"
    override val accountSignOutBody: String = "سنحفظ آخر تغييراتك في حسابك، ثم نزيل بياناتك من هذا الهاتف. ستعود كلها عندما تسجّل الدخول مجددًا."
    override val accountSignOutUnsavedTitle: String = "بعض التغييرات لم تُحفظ"
    override val accountSignOutUnsavedBody: String = "تعذّر الوصول إلى الخادم، لذا لم تصل آخر تغييراتك إلى حسابك بعد. إن سجّلت الخروج الآن فستضيع. حاول مجددًا وأنت متصل."
    override val accountSignOutAnyway: String = "سجّل الخروج على أي حال"
    override val syncTitle: String = "المزامنة السحابية"
    override val syncNow: String = "زامن الآن"
    override val syncComingSoon: String = "قريبًا. حاليًا تُحفظ عاداتك على هذا الهاتف."
    override val syncSavedOnPhone: String = "محفوظة على هذا الهاتف"
    override val syncInProgress: String = "جارٍ المزامنة..."
    override val syncNeverYet: String = "لم تتم المزامنة بعد"
    override val syncJustNow: String = "تمت المزامنة للتو"
    override fun syncMinutesAgo(minutes: Long): String = "تمت المزامنة قبل ${nMinutes(minutes)}"
    override fun syncHoursAgo(hours: Long): String = "تمت المزامنة قبل ${nHours(hours)}"
    override fun syncDaysAgo(days: Long): String = "تمت المزامنة قبل ${nDays(days)}"
    override fun syncOffline(since: String): String = "غير متصل. $since"
    override fun syncFailed(since: String): String = "تعذّرت المزامنة. $since"
    override val accountChangePassword: String = "تغيير كلمة المرور"
    override val accountDelete: String = "حذف الحساب"
    override val accountEmailManaged: String = "بريدك جزء من حسابك ولا يمكن تعديله هنا."
    override val settingsAccountRow: String = "الحساب والدخول والخصوصية"
    override val cpTitle: String = "تغيير كلمة المرور"
    override val cpButton: String = "حدّث كلمة المرور"
    override val cpDone: String = "تم تحديث كلمة المرور، وسُجّل خروج أجهزتك الأخرى."
    override val delTitle: String = "حذف الحساب"
    override val delWarn: String = "سيحذف هذا حسابك في NixUra نهائيًا: بريده واسمه وطريقة دخوله. العادات المحفوظة على هذا الهاتف لن تُحذف. لا يمكن التراجع عن ذلك."
    override val delNoPasswordHint: String = "تدخل إلى حسابك عبر Google أو Facebook، فلا حاجة لكلمة مرور للتأكيد."
    override val delPasswordLabel: String = "اكتب كلمة المرور للتأكيد"
    override val delButton: String = "احذف حسابي"
    override val delConfirmTitle: String = "حذف حسابك؟"
    override val delConfirmBody: String = "لا يمكن التراجع عن ذلك."
    override val legalPrivacyTitle: String = "سياسة الخصوصية"
    override val legalTermsTitle: String = "شروط الاستخدام"
    override val legalDraftNotice: String = "مسودة: على المالك ملء التفاصيل بين الأقواس ومراجعتها قبل الإطلاق."
    override val legalEnglishOnly: String = "النص القانوني متاح بالإنجليزية فقط حاليًا."

    // Score and streak explanations
    override val scoreHowTitle: String = "كيف تُحسب"
    override val scoreRuleBreakingFree: String = "كل عادة تقلع عنها تُحسب حتى 100% خلال 30 يومًا من الصمود. التعثّر يعيد تلك العادة من البداية. هذه النتيجة متوسطها جميعًا."
    override val scoreRuleDiscipline: String = "لكل عادة جيدة: كم حقّقت من هدفها. العادة اليومية تُقاس بآخر 7 أيام. والأسبوعية تُقاس بهذا الأسبوع بعد تحقيق هدفه (أو حين يتعذّر تحقيقه)، وبالأسبوع الماضي قبل ذلك. هذه النتيجة متوسطها جميعًا."
    override val scoreRuleMomentum: String = "متوسط التحرّر والانضباط. إن كنت تتابع نوعًا واحدًا من العادات، فهي نتيجته."
    override val scoreTipBreakingFree: String = "كل ساعة صمود ترفعها."
    override val scoreTipDiscipline: String = "حقّق أهداف اليوم لترفعها. الأيام التي سبقت إنشاء العادة لا تُحسب ضدك، والعادة الأسبوعية لا تخفضها إلا حين يتعذّر تحقيق هدفها."
    override val scoreTipMomentum: String = "المس التحرّر أو الانضباط في الرئيسية لترى نصيب كل عادة."
    override val infoCurrentStreak: String = "عدد الأيام المتتالية التي حقّقت فيها الهدف اليومي لهذه العادة. يُحسب اليوم بمجرد أن تحقّقه، واليوم الفائت يعيد العدّ من البداية."
    override val infoBestStreak: String = "أكبر عدد من الأيام المتتالية حقّقت فيه الهدف اليومي."
    override val infoTotalTimes: String = "كل مرة سجّلت فيها هذه العادة، في كل الأيام معًا."
    override val infoDaysCompleted: String = "عدد الأيام التي حقّقت فيها الهدف اليومي كاملًا."
    override val infoClearFor: String = "الوقت منذ آخر تعثّر. يستمر العدّ وحده، وتصبح البطاقة أفتح كلما زاد: عند 1 و3 و7 و14 و30 يومًا."
    override val infoBestQuit: String = "أطول مدة صمدت فيها على هذه العادة. التعثّر لا يمحوها أبدًا."
    override val infoRelapses: String = "عدد مرات التعثّر التي سجّلتها. هي جزء من سجلّك ولا تنقص من أفضل إنجاز لك."
    override val infoTrackingSince: String = "اليوم الذي بدأت فيه متابعة هذه العادة."
    override val infoClearDays: String = "أيام الصمود الكاملة الآن، مجموعة من كل العادات التي تقلع عنها."
    override val infoBestAcross: String = "أطول مدة صمدت فيها على أي عادة تقلع عنها."
    override val infoCheckIns: String = "كل مرة سجّلت فيها عادة جيدة، مجموعة معًا."
    override val infoHabitsTracked: String = "عاداتك التي تقلع عنها وعاداتك الجيدة معًا."
    override val infoBestActive: String = "العادة التي صمدت عليها أطول مدة حتى الآن."

    // Dashboard
    override val guestBannerTitle: String = "تستخدم NixUra كضيف"
    override val guestBannerBody: String = "بياناتك على هذا الهاتف فقط. سجّل الدخول لتحفظ نسخة منها."
    override val homeLoading: String = "جارٍ تحميل عاداتك"
    override val homeStartTitle: String = "ابدأ بشيء واحد"
    override val homeStartQuit: String = "شيء تقلع عنه"
    override val homeStartBuild: String = "عادة تبنيها"
    override val homeStartEvent: String = "مناسبة تعدّ لها الأيام"
    override val homeThought: String = "فكرة اليوم"
    override val homeToday: String = "اليوم"

    // Custom habits
    override val customHabitPill: String = "+ اكتب عادتي"

    // Coach
    override val coachWelcomeSubtitle: String = "مدرّبك الخاص، يعمل دون اتصال."
    override val coachWelcomeSubtitleAi: String = "ردود حقيقية من الذكاء الاصطناعي. لا يُرسل اسمك ولا بريدك أبدًا."
    override val coachStatBest: String = "الأفضل الآن"
    override val coachStatHabits: String = "أقلع عنها"
    override val coachStatToday: String = "أُنجز اليوم"
    override val coachPickStart: String = "بماذا أساعدك؟"
    override val coachHistoryTitle: String = "المحادثات"
    override val coachHistoryEmpty: String = "ستظهر محادثاتك السابقة هنا."
    override val coachNewChat: String = "محادثة جديدة"
    override val coachUntitledChat: String = "محادثة جديدة"
    override val coachDeleteChatTitle: String = "حذف هذه المحادثة؟"
    override val coachDeleteChatBody: String = "ستُحذف من هذا الهاتف، ولا يمكن التراجع عن ذلك."

    // Starter guide
    override val guideSkip: String = "تخطَّ"
    override val guideNext: String = "التالي"
    override val guideWelcomeTitle: String = "أهلًا بك في NixUra"
    override val guideWelcomeBody: String = "اترك ما يستنزفك، وابنِ ما يرفعك. إليك جولة سريعة في التطبيق، لن تأخذ أكثر من 30 ثانية."
    override val guideStart: String = "خذني في جولة"
    override val guideScoresTitle: String = "تقدّمك بنظرة واحدة"
    override val guideScoresBody: String = "تمتلئ هذه الحلقات كلما صمدت وحقّقت أهدافك. المس أيًّا منها في أي وقت لترى كيف تُحسب."
    override val guideStartHereTitle: String = "ابدأ من هنا"
    override val guideStartHereBody: String = "اختر شيئًا واحدًا تبدأ به: عادة تتركها، أو عادة تبنيها، أو مناسبة تعدّ الأيام حتى موعدها. ستظهر هنا حلقات تقدّمك بعد أن تضيفه."
    override val guideQuitTitle: String = "اترك عادة سيئة"
    override val guideQuitBody: String = "المس + لتضيف واحدة. يبدأ عدّاد مباشر، وتصبح بطاقته أفتح كلما طال صمودك. وفي اللحظات الصعبة، المس «رغبة قوية؟» على البطاقة."
    override val guideBuildTitle: String = "ابنِ عادة جيدة"
    override val guideBuildBody: String = "أضف أشياء مثل شرب الماء، ثم المس + خلال اليوم. تمتلئ البطاقة بالأخضر حتى تُنجزها."
    override val guideEventsTitle: String = "عُدّ الأيام لما يهمّك"
    override val guideEventsBody: String = "أضف مناسبة، واضبط التذكيرات، وتابع العدّ التنازلي."
    override val guideBarTitle: String = "تنقّل بسهولة"
    override val guideBarBody: String = "المس هنا أو اسحب بين الشاشات. آخرها مدرّبك، للرغبات القوية واللحظات الصعبة."
    override val guideAccountTitle: String = "حسابك"
    override val guideAccountBody: String = "المس صورتك لتسجيل الدخول أو الخروج، أو لفتح الملف الشخصي والإعدادات."
    override val guideDoneTitle: String = "أنت جاهز"
    override val guideDoneBody: String = "أفضل خطوة أولى: عادة واحدة تريد أن تتركها خلفك."
    override val guideAddFirst: String = "أضف أول عادة"
    override val guideExplore: String = "سأتجوّل أولًا"
    override val guideReplay: String = "أعد الجولة التعريفية"

    // Long-press delete with undo
    override val deleteAction: String = "حذف"
    override fun deleteItemTitle(name: String): String = "حذف «$name»؟"
    override val deleteBadHabitBody: String = "ستُحذف سلسلتها وكل تعثّر سجّلته. وإن كنت مسجّل الدخول فستُحذف من أجهزتك الأخرى أيضًا."
    override val deleteGoodHabitBody: String = "سيُحذف كل يوم سجّلته. وإن كنت مسجّل الدخول فستُحذف من أجهزتك الأخرى أيضًا."
    override val deleteEventItemBody: String = "سيُحذف العدّ التنازلي وتذكيراته."
    override fun deletedItem(name: String): String = "حُذف «$name»"

    // Money saved
    override val moneyTitle: String = "المال الذي وفّرته"
    override val moneyOptIn: String = "تابع المال الذي توفّره"
    override val moneyOptInBody: String = "اختياري. أخبرنا كم كانت تكلّفك، وشاهد مدخراتك تزيد كلما صمدت."
    override val moneyWhatExactly: String = "ما هو بالتحديد؟"
    override val moneyWhatCigarettes: String = "الماركة، مثل كليوباترا (اختياري)"
    override val moneyWhatOther: String = "أيّ نوع، مثل بيبسي (اختياري)"
    override val moneyBoughtAs: String = "تشتريها"
    override val moneyCountBy: String = "تعدّها يوميًا بـ"
    override val moneyPerDay: String = "كم في اليوم في المتوسط؟"
    override val moneyPacksPerDay: String = "علب في اليوم"
    override val moneyPerPack: String = "كم في العلبة الواحدة؟"
    override val moneyPrice: String = "السعر"
    override val moneyCurrency: String = "العملة"
    override val moneyRemove: String = "أوقف متابعة المال"
    override val moneyInvalid: String = "اكتب الكمية في اليوم والسعر، أو أوقف المتابعة."
    override val moneySavedThisStreak: String = "وفّرته في هذه السلسلة"
    override val moneyDailyCost: String = "تكلفة اليوم العادي"
    override val moneyPerStreak: String = "ما وفّرته في كل سلسلة"
    override val moneyNow: String = "الآن"
    override val moneyEdit: String = "تعديل التكاليف"
    override val moneyAddCta: String = "أضف ما كانت تكلّفك لترى المال الذي توفّره."
    override fun moneyPricePer(unit: String): String = "سعر $unit"
    override fun moneyDailyCostLine(amount: String, perDay: String): String = "$amount في اليوم ($perDay)"
    override fun moneyCardLine(amount: String, avoided: String): String = "وفّرت $amount · $avoided"

    override fun costUnitName(unit: CostUnit, plural: Boolean): String = when (unit) {
        CostUnit.CIGARETTE -> if (plural) "سجائر" else "سيجارة"
        CostUnit.POD -> if (plural) "كبسولات" else "كبسولة"
        CostUnit.POUCH -> if (plural) "أكياس" else "كيس"
        CostUnit.SESSION -> if (plural) "جلسات" else "جلسة"
        CostUnit.CUP -> if (plural) "أكواب" else "كوب"
        CostUnit.CAN -> if (plural) "علب" else "علبة"
        CostUnit.BOTTLE -> if (plural) "زجاجات" else "زجاجة"
        CostUnit.DRINK -> if (plural) "مشروبات" else "مشروب"
        CostUnit.MEAL -> if (plural) "وجبات" else "وجبة"
    }

    /** "340 سيجارة لم تُدخَّن": the count and noun by the Arabic plural rules, then what did not happen. */
    override fun unitsAvoided(count: Long, unit: CostUnit): String {
        val noun = when (unit) {
            CostUnit.CIGARETTE -> pl(count, "سيجارة واحدة", "سيجارتان", "سجائر", "سيجارة", "سيجارة")
            CostUnit.POD -> pl(count, "كبسولة واحدة", "كبسولتان", "كبسولات", "كبسولة", "كبسولة")
            CostUnit.POUCH -> pl(count, "كيس واحد", "كيسان", "أكياس", "كيسًا", "كيس")
            CostUnit.SESSION -> pl(count, "جلسة واحدة", "جلستان", "جلسات", "جلسة", "جلسة")
            CostUnit.CUP -> pl(count, "كوب واحد", "كوبان", "أكواب", "كوبًا", "كوب")
            CostUnit.CAN -> pl(count, "علبة واحدة", "علبتان", "علب", "علبة", "علبة")
            CostUnit.BOTTLE -> pl(count, "زجاجة واحدة", "زجاجتان", "زجاجات", "زجاجة", "زجاجة")
            CostUnit.DRINK -> pl(count, "مشروب واحد", "مشروبان", "مشروبات", "مشروبًا", "مشروب")
            CostUnit.MEAL -> pl(count, "وجبة واحدة", "وجبتان", "وجبات", "وجبة", "وجبة")
        }
        val verb = when (unit) {
            CostUnit.CIGARETTE -> "لم تُدخّنها"
            CostUnit.CUP, CostUnit.CAN, CostUnit.BOTTLE, CostUnit.DRINK -> "لم تشربها"
            CostUnit.MEAL -> "لم تأكلها"
            CostUnit.SESSION -> "تجاوزتها"
            CostUnit.POD, CostUnit.POUCH -> "لم تستخدمها"
        }
        return "$noun $verb"
    }
    override val packWord: String = "العلبة"

    // Integrations
    override val integrationsTitle: String = "التقويمات"
    override val systemMode: String = "مثل الهاتف"
    override val integrationsRow: String = "التقويمات"
    override val integrationsRowHint: String = "استورد المناسبات من تقويم Google أو Notion"
    override val integrationsIntro: String = "تظهر المناسبات المرتبطة في المناسبات مع عدّها التنازلي، وتبقى محدّثة. NixUra يقرؤها فقط."
    override val integrationsNeedAccount: String = "سجّل الدخول لربط تقويم. الضيف يستخدم NixUra دون اتصال تمامًا."
    override val googleCalendar: String = "تقويم Google"
    override val notion: String = "Notion"
    override val integrationConnect: String = "اربط"
    override val integrationReconnect: String = "اربط مجددًا"
    override val integrationDisconnect: String = "افصل"
    override val integrationSyncNow: String = "زامن الآن"
    override val integrationNotConnected: String = "غير مربوط"
    override fun integrationConnectedAs(label: String): String = if (label.isBlank()) "مربوط" else "مربوط · $label"
    override val integrationNeverSynced: String = "لم تتم المزامنة بعد"
    override fun integrationSyncedAgo(ago: String): String = "آخر مزامنة $ago"
    override val integrationErrRevoked: String = "أُلغي الوصول أو انتهت صلاحيته. اربط مجددًا لمتابعة الاستيراد."
    override val integrationErrOffline: String = "أنت غير متصل. سيحاول NixUra لاحقًا."
    override val integrationErrNotConfigured: String = "اختر ما تريد استيراده أدناه."
    override val integrationErrFailed: String = "تعذّرت المزامنة هذه المرة. سيحاول NixUra لاحقًا."
    override val googleNotAvailable: String = "تقويم Google غير مُعدّ في هذا الإصدار بعد."
    override val googleCancelled: String = "لم يُربط تقويم Google. المس «اربط» للمحاولة مجددًا."
    override val googleDenied: String = "لم يُمنح الوصول إلى Google."
    override val notionNotConnected: String = "أكمل ربط Notion في المتصفح، ثم عُد إلى هنا."
    override val googlePickCalendars: String = "ما الذي تستورده"
    override val googlePickHint: String = "العناصر المميّزة تُستورد. المس أيًّا منها لتشغيله أو إيقافه."
    override val googleFeedEvents: String = "المناسبات"
    override val googleFeedBirthdays: String = "أعياد الميلاد"
    override val googleFeedTasks: String = "المهام"
    override val googleFeedHolidays: String = "العطلات"
    override val notionPickDatabase: String = "قاعدة البيانات المستوردة"
    override val notionNoDatabases: String = "لم تُشارك أي قاعدة بيانات بعد. في صفحة الربط في Notion، شارك قاعدة البيانات التي تضم مناسباتك."
    override val notionTitleProperty: String = "اسم المناسبة من"
    override val notionDateProperty: String = "تاريخ المناسبة من"
    override val notionNoDateProperty: String = "ليس في قاعدة البيانات هذه حقل تاريخ، فلا يمكن استيرادها."
    override fun disconnectTitle(name: String): String = "فصل $name؟"
    override val disconnectBody: String = "ستُحذف المناسبات المستوردة منه من NixUra (ومن أجهزتك الأخرى أيضًا). لا يتغيّر شيء في التقويم نفسه."
    override fun importedFrom(name: String): String = "مستوردة من $name. عدّلها هناك، وتذكيراتك هنا محفوظة."
    override val sourceGoogle: String = "Google"
    override val sourceNotion: String = "Notion"
    override val agoJustNow: String = "للتو"
    override fun agoMinutes(n: Long): String = "قبل ${nMinutes(n)}"
    override fun agoHours(n: Long): String = "قبل ${nHours(n)}"
    override fun agoDays(n: Long): String = "قبل ${nDays(n)}"

    // AI coach, progress cards and voice notes
    override val aiConsentTitle: String = "تشغيل مدرّب الذكاء الاصطناعي؟"
    override val aiConsentPoints: List<String> = listOf(
        "تُرسل رسائلك وملاحظاتك الصوتية وأسماء عاداتك وأرقامها وعناوين مناسباتك وتواريخها إلى خدمات ذكاء اصطناعي على الإنترنت لكتابة الرد.",
        "لا يُرسل اسمك ولا بريدك ولا حسابك أبدًا. ولا تُحفظ التسجيلات الصوتية على هاتفك.",
        "قد يخطئ الذكاء الاصطناعي، وهو ليس طبيبًا. يمكنك إيقافه من الإعدادات في أي وقت، والمدرّب دون اتصال يعمل دائمًا.",
    )
    override val aiConsentAgree: String = "شغّل مدرّب الذكاء الاصطناعي"
    override val aiConsentDecline: String = "أبقِ المدرّب دون اتصال"
    override val aiSettingTitle: String = "مدرّب الذكاء الاصطناعي"
    override val aiSettingHint: String = "ردود حقيقية وملاحظات صوتية. يرسل رسائلك وبيانات عاداتك إلى خدمات ذكاء اصطناعي على الإنترنت. يحتاج إلى حساب."
    override val coachModeAi: String = "متصل"
    override fun coachModeAiLeft(left: Int): String = "بقيت ${messages(left.toLong())} اليوم"
    override val coachModeGuest: String = "دون اتصال · سجّل الدخول لردود الذكاء الاصطناعي"
    override val coachNoticeLimit: String = "استخدمت رسائل الذكاء الاصطناعي لهذا اليوم. سيرد المدرّب دون اتصال حتى الغد."
    override val coachNoticeResting: String = "مدرّب الذكاء الاصطناعي يستريح الآن، فردّ المدرّب دون اتصال."
    override val coachNoticeOffline: String = "لا يوجد اتصال بالإنترنت، فردّ المدرّب دون اتصال."
    override val voiceCardTitle: String = "قلها وأنا أضيفها"
    override val voiceCardBody: String = "المس وأخبرني بالعادات والمناسبات التي تريدها."
    override val voiceSignInTitle: String = "الملاحظات الصوتية تحتاج إلى حساب"
    override val voiceSignInBody: String = "سجّل الدخول لتستخدم مدرّب الذكاء الاصطناعي وتضيف العادات والمناسبات بصوتك."
    override val voiceAllowMicTitle: String = "اسمح باستخدام الميكروفون"
    override val voiceAllowMicBody: String = "يسجّل NixUra فقط حين تكون هذه النافذة تستمع، ولا يحتفظ بالتسجيل أبدًا."
    override val voiceAllowMic: String = "اسمح بالميكروفون"
    override val voiceListening: String = "أستمع إليك…"
    override val voiceExample: String = "جرّب: «أريد أن أترك التدخين، وأشرب 5 أكواب ماء في اليوم، ولديّ اجتماع يوم الاثنين القادم الساعة 7 مساءً.»"
    override val voiceTapToStop: String = "المس الزر حين تنتهي"
    override val voiceUnderstanding: String = "أفهم ما قلته…"
    override val voiceUnderstandingHint: String = "أحدّد ما سأضيفه. يستغرق ذلك بضع ثوانٍ."
    override val voiceReviewTitle: String = "هذا ما فهمته"
    override val voiceSuggestTitle: String = "أضيف هذه لك؟"
    override val voiceReviewHint: String = "المس عنصرًا لتستبعده، أو القلم لتصحّحه. لن يُضاف شيء حتى تؤكّد."
    override val voiceEditItem: String = "صحّح هذا العنصر"
    override val voiceEditTitle: String = "صحّح ما أخطأت فيه"
    override val voiceEditHint: String = "غيّر أي شيء أسأت سماعه. لن يُضاف شيء حتى تؤكّد القائمة."
    override val voiceEditKind: String = "النوع"
    override val voiceEditKeep: String = "احفظ التغييرات"
    override val voiceNeedsFix: String = "يحتاج إلى تصحيح قبل إضافته"
    override val voiceProblemName: String = "أعطه اسمًا."
    override fun voiceProblemNameTooLong(max: Int): String = "اجعل الاسم $max حرفًا أو أقل."
    override val voiceProblemDailyTarget: String = "اختر من 1 إلى 99 مرة في اليوم."
    override val voiceProblemDaysAWeek: String = "اختر من 1 إلى 7 أيام في الأسبوع."
    override val voiceProblemBadDate: String = "هذا التاريخ غير موجود. اختر غيره."
    override val voiceProblemPast: String = "هذه اللحظة مضت بالفعل. اختر وقتًا في المستقبل."
    override fun voiceAdd(count: Int): String = "أضف ${items(count.toLong())}"
    override fun voiceDoneTitle(added: Int): String = "أُضيف ${items(added.toLong())}"
    override val voiceDoneBody: String = "بدأت العدّادات والعدّ التنازلي."
    override fun voiceDoneFailed(failed: Int): String = "تعذّرت إضافة ${items(failed.toLong())}."
    override val voiceRecordAnother: String = "سجّل ملاحظة أخرى"
    override val voiceTryAgain: String = "حاول مجددًا"
    override val voiceFailTitle: String = "لم ينجح ذلك"
    override val voiceFailMic: String = "تعذّر فتح الميكروفون. أغلق التطبيقات الأخرى التي تستخدمه وحاول مجددًا."
    override val voiceFailNothingHeard: String = "لم أسمع شيئًا. قرّب الهاتف قليلًا وحاول مجددًا."
    override val voiceFailNothingToAdd: String = "سمعتك، لكن لم أجد عادة أو مناسبة أضيفها. جرّب أن تقول ما تريد تركه أو بدءه أو حضوره."
    override val voiceFailLimit: String = "استخدمت ملاحظاتك الصوتية لهذا اليوم. تعود غدًا."
    override val voiceFailResting: String = "الذكاء الاصطناعي يستريح الآن. حاول بعد قليل."
    override val voiceFailOffline: String = "لا يوجد اتصال بالإنترنت. الملاحظات الصوتية تحتاج إلى اتصال."
    override val actionQuit: String = "إقلاع"
    override val actionQuitDetail: String = "يبدأ العدّاد الآن"
    override val actionBuild: String = "بناء"
    override fun actionTimesADay(count: Int): String = if (count == 1) "كل يوم" else "${times(count.toLong())} في اليوم"
    override fun actionDaysAWeek(count: Int): String = "${nDays(count.toLong())} في الأسبوع"
    override val actionEvent: String = "مناسبة"
    override val visualQuitEyebrow: String = "أقلع عنها"
    override val visualGoodEyebrow: String = "أبنيها"
    override fun visualToGoal(days: Int): String = "حتى ${nDays(days.toLong())}"
    override val visualClean: String = "صامد والعدّ مستمر"
    override fun visualBest(time: String): String = "أطول سلسلة: $time"
    override fun visualRelapses(count: Int): String = slips(count.toLong())
    override val visualToday: String = "اليوم"
    override val visualThisWeek: String = "هذا الأسبوع"
    override fun visualStreakDays(days: Int): String = "${nDays(days.toLong())} على التوالي · آخر 7 أيام"
    override fun visualStreakWeeks(weeks: Int): String = "${nWeeks(weeks.toLong())} على التوالي · آخر 7 أيام"
    override val visualOverviewTitle: String = "تقدّمك"
    override val visualWeek: String = "هذا الأسبوع"
    override val visualLongestClean: String = "أطول سلسلة صمود"
    override val visualQuitting: String = "عادات تقلع عنها"
    override val visualDoneToday: String = "عادات جيدة أُنجزت اليوم"

    // ---- Session 2 (Add/Edit form) ----
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
    override val waterGoalPerDay: String = "في اليوم"
    override val waterGoalDecrease: String = "خفّض الهدف"
    override val waterGoalIncrease: String = "ارفع الهدف"

    // ---- Session 3 (details, calendar, cards) ----
    override val waterProgressLiters: (String, String) -> String = { drunk, goal -> "$drunk / $goal لتر" }
    override val waterProgressCups: (String, String, Int) -> String = { drunk, goal, n -> "$drunk / $goal ${cupNoun(n)}" }
    override val waterLeftLiters: (String) -> String = { "باقٍ $it لتر" }
    override val waterLeftCups: (String, Int) -> String = { amount, n -> "باقٍ $amount ${cupNoun(n)}" }
    override val waterAddCup: String = "$LRM+1 كوب"
    override val waterAddMl: (Int) -> String = { "$LRM+$it مل" }
    override val waterAddA11y: (Int) -> String = { "أضف كوبًا بحجم $it مل" }
    override val calendarLess: String = "أقل"
    override val calendarMore: String = "أكثر"
    override val calendarLegendA11y: String = "يدلّ اللون على مقدار ما أُنجز في كل يوم، من الأقل إلى الأكثر"

    // ---- UX A2: launcher shortcut screens ----
    override val keepTitle: String = "لا تحذف NixUra!"
    override val keepHeadline: String = "لحظة… هل تنفصل عني؟ 🥺"
    override val keepIntro: String = "هل قلتُ شيئًا أزعجك؟ أم عددتُ شيئًا خطأ؟"
    override fun keepStreak(habit: String, days: Long): String =
        if (days < 1) "عدّاد «$habit» بدأ للتو، ويسأل من الآن: إلى أين ستذهب؟"
        else "سلسلة " + when (days) {
            1L -> "اليوم الواحد"
            2L -> "اليومين"
            else -> "الـ${nDays(days)}"
        } + " في «$habit» تسأل إن كانت أخطأت في شيء."
    override fun keepWater(glasses: Int): String =
        "أكواب الماء تبكي الآن. حرفيًا. هذه " +
            ArabicPlurals.format(glasses.toLong(), "%d كوب", "كوب واحد", "كوبان", "%d أكواب", "%d كوبًا", "%d كوب") +
            " من الدموع. 💧"
    override fun keepTracking(habits: Int): String =
        if (habits == 1) "عادتك تتظاهر بأنها بخير. وهي ليست بخير."
        else "عاداتك ($habits) متلاصقة الآن، تتظاهر بأنها بخير."
    override val keepNoProgress: String = "سلاسلك القادمة حزينة من الآن."
    override val keepGuestNote: String =
        "وأيضًا، أنت ضيف، فعاداتك محفوظة على هذا الهاتف فقط. إن رحلتُ رحلت معي. 💔"
    override val keepSignedInNote: String = "تقدّمك في أمان داخل حسابك. سينتظرك. كما كنت سأفعل."
    override val keepCanChange: String = "قل لي ما الذي حدث. يمكنني أن أتغيّر. (أنا تطبيق، أستطيع فعلًا، بتحديث.)"
    override val keepBack: String = "حسنًا، سأبقى"
    override val keepFeedback: String = "أخبرني ما المشكلة"
    override val keepUninstall: String = "احذفه على أي حال"
    override val keepUninstallNote: String = "لا ضغينة. تقريبًا."
    override val keepSettingsFailed: String = "تعذّر فتح صفحة التطبيق في النظام. يمكنك حذف NixUra من إعدادات الهاتف ← التطبيقات."

    override val bugTitle: String = "الإبلاغ عن مشكلة"
    override val bugIntro: String = "أخبرني بما حدث. الكلمات البسيطة تكفي تمامًا."
    override val bugCategoryLabel: String = "ما نوع المشكلة؟ (اختياري)"
    override fun bugCategory(category: com.adhamelgendy.nixura.domain.support.BugCategory): String = when (category) {
        com.adhamelgendy.nixura.domain.support.BugCategory.CRASH -> "أُغلق أو توقّف"
        com.adhamelgendy.nixura.domain.support.BugCategory.WRONG_DATA -> "أرقام خاطئة"
        com.adhamelgendy.nixura.domain.support.BugCategory.LOOKS_WRONG -> "الشكل غير صحيح"
        com.adhamelgendy.nixura.domain.support.BugCategory.NOTIFICATIONS -> "التذكيرات"
        com.adhamelgendy.nixura.domain.support.BugCategory.IDEA -> "فكرة"
        com.adhamelgendy.nixura.domain.support.BugCategory.OTHER -> "شيء آخر"
    }
    override val bugMessageLabel: String = "ماذا حدث؟"
    override val bugMessagePlaceholder: String = "مثلًا: بطاقة الماء لم تمتلئ بعد أن لمست ‎+1"
    override val bugStepsLabel: String = "كيف أرى المشكلة بنفسي؟ (اختياري)"
    override val bugStepsPlaceholder: String = "1. افتح…  2. المس…  3. ثم…"
    override val bugPrivacyNote: String = "نضيف إصدار التطبيق وطراز الهاتف وإصدار النظام. لا اسمك ولا بريدك ولا عاداتك أبدًا."
    override val bugSignedInNote: String = "يُرسل بالبريد، وتُحفظ نسخة مع حسابك لأتابع معك."
    override val bugGuestNote: String = "بصفتك ضيفًا، يُرسل بلاغك بالبريد فقط: يبقى NixUra بلا اتصال."
    override val bugSend: String = "أرسل البلاغ"
    override val bugEmpty: String = "اكتب بضع كلمات عن المشكلة أولًا."
    override fun bugTooShort(min: Int): String = "بعض التفاصيل الإضافية من فضلك (" + pl(min.toLong(), "حرف واحد", "حرفان", "أحرف", "حرفًا", "حرف") + " على الأقل)."
    override fun bugTooLong(max: Int): String = "هذا كثير! اجعله أقل من $max حرف من فضلك."
    override val bugSentTitle: String = "شكرًا لك! 💛"
    override val bugSentSaved: String = "حُفظ بلاغك."
    override val bugSentOffline: String = "يبدو أنك غير متصل، فلم تُحفظ النسخة. البريد سيصل."
    override val bugSentFailed: String = "لم تُحفظ النسخة، لكن البريد سيصل."
    override val bugSentEmailOpened: String = "فُتح تطبيق البريد وكل شيء مكتوب. اضغط «إرسال» هناك فقط."
    override fun bugSentNoEmailApp(address: String): String = "لا يوجد تطبيق بريد على هذا الهاتف. يمكنك مراسلتنا على $address."
    override val bugDone: String = "عودة إلى التطبيق"

    override val helpTitle: String = "المساعدة"
    override val helpIntro: String = "إجابات سريعة عن NixUra. المس السؤال لتفتحه."
    override val helpFaq: List<FaqItem> = listOf(
        FaqItem(
            "كيف أضيف عادة؟",
            "استخدم تبويب «الإقلاع» للعادات التي تريد تركها، وتبويب «البناء» للعادات الجيدة. المس +، واكتب الاسم (بأي لغة، " +
                "والأخطاء الإملائية لا تهم) واحفظ. عادة الإقلاع يبدأ عدّادها فورًا، ويمكنك أيضًا أن تحدّد متى فعلتها آخر مرة.",
        ),
        FaqItem(
            "تعثّرت. ماذا الآن؟",
            "يحدث هذا. افتح قائمة ⋯ في بطاقة الإقلاع (أو اضغط مطوّلًا على البطاقة) واختر «تعثّرت». يبدأ العدّاد من جديد، " +
                "وتبقى أطول سلسلة لك محفوظة، ويستطيع المدرّب مساعدتك على التخطيط للسلسلة القادمة.",
        ),
        FaqItem(
            "كيف أسجّل عادة جيدة؟",
            "المس + على بطاقتها في الرئيسية أو في تبويب «البناء»، أو على أداتها في الشاشة الرئيسية. لمستها بالخطأ؟ المس −، " +
                "أو افتح العادة والمس أي يوم سابق في التقويم لتصحيحه.",
        ),
        FaqItem(
            "كيف أضع عادة على الشاشة الرئيسية؟",
            "على أندرويد، افتح عادة أو مناسبة والمس «أضف إلى الشاشة الرئيسية»، أو اضغط مطوّلًا على مكان فارغ في الشاشة " +
                "الرئيسية واختر NixUra من الأدوات. هناك عدّاد للإقلاع، وتسجيل سريع للعادات الجيدة، وعدّ تنازلي للمناسبات.",
        ),
        FaqItem(
            "لماذا لا تصلني التذكيرات؟",
            "تأكّد أن الإشعارات مفعّلة في إعدادات NixUra وفي إعدادات الهاتف. بعض الهواتف (OPPO وXiaomi وHuawei وغيرها) " +
                "توقف التطبيقات لتوفير البطارية: من إعدادات الهاتف ← البطارية، اسمح لـNixUra بالعمل بلا قيود وبالتشغيل التلقائي.",
        ),
        FaqItem(
            "ماذا يعرف المدرّب الذكي؟ وهل هو خاص؟",
            "إن كنت مسجّلًا ووافقت، يجيبك مدرّب ذكاء اصطناعي حقيقي، مع بضع رسائل مجانية كل يوم. وإلا يجيبك مدرّب يعمل " +
                "على هاتفك بالكامل. الذكاء الاصطناعي لا يحصل أبدًا على اسمك أو بريدك أو حسابك.",
        ),
        FaqItem(
            "هل أحتاج إلى حساب؟ وهل بياناتي محفوظة احتياطيًا؟",
            "لا تحتاج إلى حساب. بصفتك ضيفًا، يبقى كل شيء على هذا الهاتف ويعمل NixUra بلا اتصال، لذا فحذف التطبيق يحذف " +
                "بياناتك. مع حساب، تتزامن عاداتك وتعود حين تسجّل الدخول على هاتف جديد.",
        ),
        FaqItem(
            "كيف أغيّر اللغة؟",
            "الإعدادات ← المظهر ← اللغة: لغة النظام أو English أو العربية. ويمكنك هناك أيضًا اختيار الأرقام 123 أو ١٢٣.",
        ),
        FaqItem(
            "لماذا يتغيّر لون بطاقة الإقلاع؟",
            "تصبح أفتح كلما طالت سلسلتك، عند 1 و3 و7 و14 و30 يومًا. ويمكنك اختيار لونك الخاص عند تعديل العادة.",
        ),
        FaqItem(
            "كيف أحذف عادة أو بياناتي أو حسابي؟",
            "عادة واحدة: اضغط مطوّلًا على بطاقتها واختر «حذف» (لديك 5 ثوانٍ للتراجع). كل عاداتك ومناسباتك ومحادثاتك: " +
                "الإعدادات ← الخصوصية والبيانات ← امسح كل البيانات (ومن حسابك أيضًا إن كنت مسجّلًا). الحساب نفسه: " +
                "الإعدادات ← الحساب ← حذف الحساب.",
        ),
    )
    override val helpStillStuck: String = "ما زلت عالقًا؟"
    override val helpStillStuckBody: String = "أخبرني بما يحدث وسأبحث فيه."
    override val helpReport: String = "الإبلاغ عن مشكلة"

    // ---- UX B ----
    override val notificationAskTitle: String = "السماح بالإشعارات؟"
    override val notificationAskEventReason: String = "حتى يذكّرك NixUra قبل هذه المناسبة. يمكنك إسكات كل نوع من الإشعارات لاحقًا."
    override val notificationAskSettingsReason: String = "حتى تصلك تذكيرات المناسبات والمحطات ورسالتك اليومية. أنت تختار الأنواع التي تبقى مفعّلة."
    override val notificationAskAllow: String = "اسمح"
    override val notNow: String = "ليس الآن"
    override val notificationBlockedTitle: String = "الإشعارات محظورة"
    override val notificationBlockedBody: String = "لن يسأل أندرويد مرة أخرى. لتصلك التذكيرات، فعّل إشعارات NixUra من إعدادات النظام."
    override val notificationsBlockedNotice: String = "إشعارات NixUra محظورة، لذا لن يصلك شيء."
    override val eventChannelOff: String = "تذكيرات المناسبات متوقفة من إعدادات النظام."
    override val turnOnInSettings: String = "فعّلها من الإعدادات"
    override val notificationTypes: String = "أنواع الإشعارات"
    override val notificationTypesHint: String = "الصوت أو الصمت للمناسبات والعادات والمدرب والتطبيق، من إعدادات النظام."
    // ---- end of UX B ----

    // ---- UX C ----
    private fun prayersMarked(n: Long) = pl(n, "صلاة واحدة", "صلاتين", "صلوات", "صلاة", "صلاة")
    private fun snoozeLength(minutes: Int): String =
        if (minutes % 60 == 0) nHours((minutes / 60).toLong()).let { if (minutes == 60) "ساعة" else it } else nMinutes(minutes.toLong())
    override fun prayerName(prayer: Prayer): String = when (prayer) {
        Prayer.FAJR -> "الفجر"
        Prayer.DHUHR -> "الظهر"
        Prayer.ASR -> "العصر"
        Prayer.MAGHRIB -> "المغرب"
        Prayer.ISHA -> "العشاء"
    }
    override fun reminderPrayerTitle(prayer: Prayer): String = when (prayer) {
        Prayer.FAJR -> "🌙 دخل وقت الفجر"
        Prayer.DHUHR -> "☀️ حان وقت الظهر"
        Prayer.ASR -> "🌤️ حان وقت العصر"
        Prayer.MAGHRIB -> "🌅 حان وقت المغرب"
        Prayer.ISHA -> "🌌 حان وقت العشاء"
    }
    override fun reminderPrayerBody(prayer: Prayer, marked: Int): String =
        if (marked <= 0) "سجّلها هنا بعد أن تصلّي."
        else "سجّلت ${prayersMarked(marked.toLong())} اليوم. سجّل ${prayerName(prayer)} هنا بعد أن تصلّي."
    override fun reminderWaterTitle(variant: Int): String = when (variant.mod(3)) {
        0 -> "💧 وقت كوب ماء"
        1 -> "💧 استراحة ماء"
        else -> "💧 كوب ماء؟"
    }
    override fun reminderWaterBody(count: Int, target: Int): String = "$count من $target حتى الآن اليوم. اضغط $LRM+1 عندما تشربه."
    override fun reminderWaterLastBody(count: Int, target: Int): String = "كوب واحد بعد ويكتمل ماء اليوم."
    override fun reminderHabitTitle(theme: HabitTheme, name: String, variant: Int): String = when (theme) {
        HabitTheme.READING -> "📖 وقت القراءة"
        HabitTheme.WALK -> "🚶 مشية قصيرة؟"
        HabitTheme.WORKOUT -> "💪 وقت الحركة"
        HabitTheme.SLEEP -> "🌙 وقت الاستعداد للنوم"
        HabitTheme.WATER -> "$RLM💧 $name"
        HabitTheme.PRAYER -> "$RLM🕌 $name"
        HabitTheme.DEFAULT -> if (variant.mod(2) == 0) "🌱 حان وقت $name" else "$RLM🌱 $name؟"
    }
    override fun reminderDailyBody(name: String, count: Int, target: Int): String =
        if (target <= 1) "$RLM$name: مرة واحدة اليوم تكفي." else "$RLM$name: $RLM$count من $target اليوم."
    override fun reminderWeeklyBody(name: String, doneDays: Int, goalDays: Int): String =
        "$RLM$name: $RLM$doneDays من ${nDays(goalDays.toLong())} هذا الأسبوع."
    override fun reminderActionGlass(servingMl: Int): String = "$LRM+1 كوب · $servingMl مل"
    override fun reminderActionPrayed(prayer: Prayer): String = "صلّيت ✓"
    override fun reminderActionDone(): String = "تمّ ✓"
    override fun reminderActionSnooze(minutes: Int): String = "أجّل ${snoozeLength(minutes)}"
    override fun reminderLoggedWater(count: Int, target: Int): String = "شربت $count من $target اليوم"
    override fun reminderWaterGoalDone(): String = "💧 اكتمل ماء اليوم. أحسنت."
    override fun reminderLoggedPrayer(prayer: Prayer, marked: Int): String = "سُجّلت صلاة ${prayerName(prayer)} · $marked من 5 اليوم"
    override fun reminderLoggedHabit(name: String, count: Int, target: Int): String =
        if (count >= target) "$RLM$name: اكتمل اليوم ✓" else "$RLM$name: $RLM$count من $target اليوم"
    override fun reminderSnoozed(minutes: Int): String = "حسنًا، سأذكّرك بعد ${snoozeLength(minutes)}."

    override fun reminderSheetRow(status: String): String = "التذكيرات: $status"
    override val reminderStatusOff: String = "متوقفة"
    override val reminderStatusWater: String = "مفعّلة · كل ساعتين"
    override val reminderStatusWaterSpread: String = "مفعّلة · حتى 6 في اليوم"
    override val reminderStatusPrayer: String = "مفعّلة · في أوقات الصلاة"
    override fun reminderStatusAt(time: String): String = "مفعّلة · $time"
    override val reminderStatusNeedsLocation: String = "تحتاج إلى موقعك"
    override fun reminderStatusPausedUntil(date: String): String = "متوقفة مؤقتًا حتى $date"
    override val reminderSwitch: String = "ذكّرني"
    override val reminderExplainPrayer: String =
        "بعد دخول وقت كل صلاة بنحو 10 دقائق، للصلاة التي لم تسجّلها بعد. الأوقات تُحسب على هاتفك من موقعك."
    override val reminderExplainWater: String =
        "كل ساعتين بين الوقتين أدناه، أو 6 تذكيرات موزّعة إن زادت المدة على 12 ساعة. يمكن أن تنتهي بعد منتصف الليل. فقط عندما تكون متأخرًا، وقد يتحرك التذكير قليلًا كي لا يتزامن مع غيره."
    override val reminderExplainDaily: String =
        "في الوقت أدناه مرة في اليوم (وتذكير ثانٍ بعد 3 ساعات إن كان هدفك أكثر من مرة، حتى بعد منتصف الليل). لا يصل إذا اكتمل هدف اليوم."
    override val reminderExplainWeekly: String =
        "في الوقت أدناه، في الأيام التي ما زال فيها هدف الأسبوع ممكنًا. لا يصل في يوم أنجزتها فيه."
    override val reminderFrom: String = "من"
    override val reminderUntil: String = "إلى"
    override val reminderTime: String = "الوقت"
    override val reminderPauseToday: String = "إيقاف لليوم"
    override val reminderPauseWeek: String = "إيقاف لأسبوع"
    override val reminderResume: String = "استئناف التذكيرات"
    override val reminderChannelOff: String = "تذكيرات العادات متوقفة من إعدادات النظام."
    override val habitReminderNotificationsOff: String = "إشعارات NixUra محظورة، لذا لن تصلك التذكيرات."
    override val notificationAskHabitReason: String =
        "حتى يذكّرك NixUra بهذه العادة في الوقت المناسب. يمكنك إيقافها من هنا متى شئت."
    override val reminderFormLine: String = "التذكيرات مفعّلة، غيّرها متى شئت"
    override val reminderFormHintPrayer: String = "بعد كل وقت صلاة بنحو 10 دقائق."
    override val reminderFormHintWater: String = "كل ساعتين من 9:00 إلى 21:00، فقط عندما تكون متأخرًا."
    override val reminderAllowNotifications: String = "اسمح بالإشعارات"

    override val locationTitle: String = "أوقات الصلاة تحتاج إلى موقعك"
    override val locationBody: String =
        "يُقرأ موقعك على مستوى المدينة مرة واحدة ويبقى على هذا الهاتف: لا يُزامَن ولا يُرسَل إلى أي مكان. بدونه تبقى تذكيرات الصلاة متوقفة."
    override val locationUse: String = "استخدم موقعي"
    override val locationUpdate: String = "حدّث الموقع"
    override val locationSaved: String = "أوقات الصلاة تتبع موقعك المحفوظ."
    override val locationBlocked: String = "الموقع متوقف لـ NixUra، فلا يمكن حساب أوقات الصلاة."
    override val locationFailed: String = "تعذّر تحديد موقعك. فعّل الموقع في هاتفك ثم حاول مجددًا."
    override val locationReading: String = "جارٍ تحديد موقعك…"

    override val pauseRemindersTitle: String = "إيقاف تذكيرات العادات مؤقتًا"
    override val pauseRemindersHint: String = "على هذا الهاتف فقط. تذكيرات المناسبات والمحطات والرسالة اليومية تستمر."
    override val pauseForToday: String = "لليوم"
    override val pauseForWeek: String = "لأسبوع"
    // ---- end of UX C ----

    // ---- UX D: lock screen, names hidden outside the app, the Today notification. ----
    override fun lockScreenEvent(inText: String?): String = if (inText == null) "مناسبة الآن" else "مناسبة بعد ${afterPreposition(inText)}"
    override fun lockScreenMilestone(): String = "وصلت إلى محطة جديدة"
    override fun lockScreenCoach(): String = "ملاحظة من مدربك"

    override val hideNameTitle: String = "إخفاء الاسم خارج التطبيق"
    override val hideNameHint: String =
        "تحتفظ الإشعارات والأدوات بأيامك وسلسلتك، دون الاسم. داخل NixUra لا يتغيّر شيء."
    override val hideNameLimits: String =
        "قد يرى الآخرون أنك تقلع عن شيء، دون أن يعرفوا ما هو. الإشعارات التي ظهرت سابقًا تبقى في سجل هاتفك. " +
            "تُخفى أيضًا معاينة التطبيقات الأخيرة (أندرويد 13 أو أحدث). يظل الاسم يُزامَن مع حسابك، ويصل إلى المدرب عبر الإنترنت إن سمحت بذلك."
    override val hideNameConfirm: String = "إخفاء الاسم"
    override fun hideNameSheetRow(on: Boolean): String = "$hideNameTitle: ${if (on) "مفعّل" else "متوقف"}"

    // A name may be in Latin letters: the leading RLM keeps the line right to left, the second keeps "3 من 8" in order.
    override fun todayTitle(done: Int, total: Int): String = "اليوم: أنجزت $done من $total"
    override fun todayTitleNoHabits(): String = "اليوم"
    override fun todayWater(count: Int, target: Int): String = "💧 $count من $target"
    override fun todayPrayers(count: Int, target: Int): String = "🕌 $count من $target"
    override fun todayHabit(name: String, count: Int, target: Int): String = "\u200f$name: \u200f$count من $target"
    override fun todayMore(count: Int): String = ArabicPlurals.format(
        count.toLong(), "", "وعادة أخرى", "وعادتان أخريان", "و%d عادات أخرى", "و%d عادة أخرى", "و%d عادة أخرى",
    )
    override fun todayNext(label: String, minutesOfDay: Int): String = "التالي: $label \u200f${formatClockMinutes(minutesOfDay, this)}"
    override fun todayEvent(daysAway: Long, minutesOfDay: Int?): String = when {
        daysAway <= 0L && minutesOfDay != null -> "مناسبة اليوم، ${formatClockMinutes(minutesOfDay, this)}"
        daysAway <= 0L -> "مناسبة اليوم"
        daysAway == 1L -> "مناسبة غدًا"
        else -> "مناسبة بعد ${afterPreposition(arDays(daysAway))}"
    }
    override fun todayStreak(days: Long): String =
        if (days == 0L) "سلسلة الإقلاع: أقل من يوم" else "سلسلة الإقلاع: ${arDays(days)}"
    override fun todayMetricDone(): String = "أُنجز"
    override fun todayMetricNext(label: String): String = "التالي: $label"
    override fun todayHide(): String = "أخفِ لليوم"
    override fun todayActionPrayed(prayer: Prayer): String = "صلّيت ${prayerName(prayer)} ✓"

    override val todaySettingTitle: String = "اليوم على شاشة القفل"
    override val todaySettingHint: String = "إشعار صامت واحد بتقدّم اليوم وما يلي. اسحبه ليختفي، ويعود غدًا."
    override val todayHiddenUntilTomorrow: String = "مخفي لليوم. يعود غدًا، أو أوقفه ثم فعّله ليظهر الآن."
    override val todayChannelOff: String = "متوقف في إعدادات النظام."
    override val todayStreakTitle: String = "أظهر سلسلة الإقلاع في «اليوم»"
    override val todayStreakHint: String = "أطول سلسلة حالية، دون اسم العادة أبدًا."
    override fun todaySheetRow(on: Boolean): String = "$todaySettingTitle: ${if (on) "مفعّل" else "متوقف"}"
    override val todayOfferTitle: String = "هل تُظهر تقدّم اليوم على شاشة القفل؟"
    override val todayOfferBody: String =
        "إشعار صامت واحد بعاداتك الجيدة وما يلي. لا يذكر أبدًا اسم عادة تقلع عنها أو اسم مناسبة. " +
            "يمكنك سحبه ليختفي، أو إيقافه من الإعدادات."
    override val todayOfferConfirm: String = "أظهره"
    override val notificationAskTodayReason: String = "ليعرض NixUra تقدّم يومك على شاشة القفل. لا يُصدر صوتًا أبدًا."
    // ---- end of UX D ----

    /** A duration after "بعد": the dual changes case (ساعتان becomes ساعتين). */
    private fun afterPreposition(duration: String): String = com.adhamelgendy.nixura.domain.coach.arAfterPreposition(duration)

    /** The noun after a written number: 3–10 أكواب, 11–99 كوبًا, otherwise كوب. */
    private fun cupNoun(n: Int): String = when (ArabicPlurals.category(n.toLong())) {
        PluralCategory.FEW -> "أكواب"
        PluralCategory.MANY -> "كوبًا"
        else -> "كوب"
    }
}

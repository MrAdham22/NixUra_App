package com.adhamelgendy.nixura.domain.coach

import com.adhamelgendy.nixura.domain.locale.ArabicPlurals
import com.adhamelgendy.nixura.domain.locale.ContentLanguage
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

data class BadHabitSnapshot(
    val id: Long,
    val name: String,
    val elapsed: Duration,
    val longest: Duration,
    val relapseCount: Int,
)

data class GoodHabitSnapshot(
    val name: String,
    val todayCount: Int,
    val dailyTarget: Int,
    /** Days for a daily habit, weeks for a weekly one. */
    val streakDays: Int,
    /** Set for a weekly habit ("N days a week"): days done this week and this week's goal. */
    val weeklyTarget: Int? = null,
    val weekDone: Int = 0,
    val weekGoal: Int = 0,
) {
    /** Still worth doing today: below today's target, or for a weekly habit not done today and the week not met yet. */
    val pendingToday: Boolean
        get() = if (weeklyTarget != null) todayCount == 0 && weekDone < weekGoal else todayCount < dailyTarget

    val progressLine: String
        get() = if (weeklyTarget != null) "$weekDone of $weekGoal days this week" else "$todayCount of $dailyTarget today"
}

data class EventSnapshot(
    val title: String,
    val remaining: Duration,
)

/** Everything the offline coach knows right now. Built by the app from live data. */
data class CoachSnapshot(
    val userName: String = "",
    val badHabits: List<BadHabitSnapshot> = emptyList(),
    val goodHabits: List<GoodHabitSnapshot> = emptyList(),
    val nextEvent: EventSnapshot? = null,
    /** Quit habit the user picked in the coach screen to talk about (`null` = none picked). */
    val focusHabitId: Long? = null,
    /** The user's earlier chat messages (oldest first, without the current one), used for follow-ups like "and after a week?". */
    val recentUserTexts: List<String> = emptyList(),
)

data class HabitInsight(
    val category: HabitCategory,
    val achieved: List<BodyMilestone>,
    val next: BodyMilestone?,
    /** 0f..1f progress between the previous milestone and [next]. 1f when everything is reached. */
    val progressToNext: Float,
)

data class CoachNote(val title: String, val body: String)

/**
 * Offline rule-based coach. Pure Kotlin and deterministic for a given [seed], so it is fully testable.
 * A real AI backend can later replace [reply] behind the `CoachResponder` abstraction.
 */
class CoachEngine(
    private val knowledge: HabitKnowledgeBase = HabitKnowledgeBase(),
    /** The language replies and notes are written in. Matching what the user typed works in both languages. */
    private val language: ContentLanguage = ContentLanguage.English,
) {

    private val explainer = StopTimelineExplainer(knowledge, language)

    private val ar: Boolean get() = language.isArabic

    /** The English or the Arabic version of a sentence, by the current language. */
    private fun tr(en: String, arabic: String): String = if (ar) arabic else en

    private fun dur(d: Duration): String = durationText(d, ar)
    /** [dur] after a preposition (منذ, بعد, إلى): an Arabic dual turns genitive ("منذ يومين"). */
    private fun durAfter(d: Duration): String = if (ar) arAfterPreposition(dur(d)) else dur(d)
    private fun span(d: Duration): String = elapsedText(d, ar)

    fun categoryFor(habitName: String): HabitCategory = knowledge.match(habitName)

    /** A length in words in the coach's language ("3 days" / "3 أيام"). */
    fun durationWords(duration: Duration): String = dur(duration)

    /** "What happens if I stop for [elapsed]?" for any habit name (generic profile when the habit is not recognised). */
    fun explainStop(habitName: String, elapsed: Duration): StopTimelineAnswer = explainer.explain(habitName, elapsed)

    /**
     * The name to show in coach text. Sensitive habits are shown by their neutral category label, never by the
     * words the user typed.
     */
    fun displayName(habitName: String): String {
        val category = knowledge.match(habitName)
        return if (category.id in SENSITIVE_IDS) category.label else habitName
    }

    fun insightFor(habitName: String, elapsed: Duration): HabitInsight {
        val category = knowledge.match(habitName)
        val achieved = category.milestones.filter { it.after <= elapsed }
        val next = category.milestones.firstOrNull { it.after > elapsed }
        val previousBoundary = achieved.lastOrNull()?.after ?: Duration.ZERO
        val progress = if (next == null) {
            1f
        } else {
            val span = (next.after - previousBoundary).inWholeMilliseconds.coerceAtLeast(1)
            ((elapsed - previousBoundary).inWholeMilliseconds.toFloat() / span).coerceIn(0f, 1f)
        }
        return HabitInsight(category, achieved, next, progress)
    }

    // ---------- Motivation ----------

    fun motivation(habitName: String, elapsed: Duration, seed: Long): String {
        val variants = when {
            elapsed < ONE_HOUR -> listOf(
                tr("The first hour is the hardest part, and you are already in it. Stay with it.",
                    "الساعة الأولى هي الأصعب، وأنت فيها بالفعل. اصمد."),
                tr("You decided. That is the biggest step — now just get through the next few minutes.",
                    "لقد قرّرت، وهذه أكبر خطوة. الآن تجاوز الدقائق القليلة القادمة فقط."),
            )
            elapsed < 1.days -> listOf(
                tr("Every hour you stay clear is proof that you can do this. Keep stacking them.",
                    "كل ساعة تصمد فيها دليل على أنك تستطيع. واصل جمعها."),
                tr("Today is the foundation. Protect it one hour at a time.",
                    "اليوم هو الأساس. احمِه ساعة بعد ساعة."),
            )
            elapsed < 3.days -> listOf(
                tr("Cravings often peak around now. If it feels hard, that means your brain is rewiring.",
                    "كثيرًا ما تبلغ الرغبة ذروتها في هذا الوقت. إن شعرت بالصعوبة فهذا يعني أن دماغك يعيد ترتيب نفسه."),
                tr("Days 2–3 are the toughest stretch. You are crossing it — keep going.",
                    "اليومان الثاني والثالث أصعب مرحلة، وأنت تعبرها الآن. واصل."),
            )
            elapsed < 7.days -> listOf(
                tr("You are building real momentum. A week is within reach.",
                    "أنت تبني زخمًا حقيقيًا. الأسبوع صار قريبًا."),
                tr("Each day makes the old habit a little weaker and you a little stronger.",
                    "كل يوم يُضعف العادة القديمة قليلًا ويقوّيك قليلًا."),
            )
            elapsed < 14.days -> listOf(
                tr("Past the first week most people stop doubting that change is possible. You are one of them.",
                    "بعد الأسبوع الأول يتوقف معظم الناس عن الشك في قدرتهم على التغيير، وأنت منهم."),
                tr("You have shown consistency. Keep your triggers in sight and your plan ready.",
                    "أثبتّ أنك تستطيع الاستمرار. أبقِ محفّزاتك أمام عينيك وخطتك جاهزة."),
            )
            elapsed < 30.days -> listOf(
                tr("Two weeks of clarity. The habit loop is visibly weaker now.",
                    "أسبوعان من الصفاء. حلقة العادة أضعف بوضوح الآن."),
                tr("You are close to a full month. Protect the progress you have built.",
                    "أنت قريب من شهر كامل. احمِ التقدّم الذي بنيته."),
            )
            else -> listOf(
                tr("A month or more — this is no longer a streak, it is a new way of living.",
                    "شهر أو أكثر، هذه لم تعد سلسلة، بل طريقة جديدة للحياة."),
                tr("You have outlasted the hardest part. Stay curious about your triggers and keep going.",
                    "تجاوزت الجزء الأصعب. ابقَ منتبهًا لمحفّزاتك وواصل."),
            )
        }
        return variants[seed.mod(variants.size.toLong()).toInt()]
    }

    // ---------- Notifications ----------

    fun dailyCheckIn(snapshot: CoachSnapshot, seed: Long): CoachNote {
        val name = snapshot.userName.trim()
        val title = when {
            name.isEmpty() -> tr("Daily check-in", "رسالة اليوم")
            else -> tr("Daily check-in, $name", "رسالة اليوم يا $name")
        }
        val focus = checkInFocus(snapshot)
        val pendingGood = snapshot.goodHabits.firstOrNull { it.pendingToday }
        val body = when {
            // Hidden outside the app: the days and the encouragement, which is generic, without the name.
            focus != null && focus.name.isEmpty() -> tr(
                "${dur(focus.elapsed)} clear. ${motivation(focus.name, focus.elapsed, seed)}",
                "صامد منذ ${durAfter(focus.elapsed)}. ${motivation(focus.name, focus.elapsed, seed)}",
            )
            focus != null -> tr(
                "${displayName(focus.name)}: ${dur(focus.elapsed)} clear. ${motivation(focus.name, focus.elapsed, seed)}",
                "${displayName(focus.name)}: صامد منذ ${durAfter(focus.elapsed)}. ${motivation(focus.name, focus.elapsed, seed)}",
            )
            pendingGood != null -> tr(
                "Don't forget \"${pendingGood.name}\" today — ${progressLine(pendingGood)} so far.",
                "لا تنسَ «${pendingGood.name}» اليوم، أنجزت حتى الآن ${progressLine(pendingGood)}.",
            )
            else -> tr("Take a minute to notice how you are doing. Small steps count.", "خذ دقيقة لتلاحظ كيف حالك. الخطوات الصغيرة تُحسب.")
        }
        return CoachNote(title, body)
    }

    /** The quit habit the daily check-in is about: the newest streak, which needs it most. */
    fun checkInFocus(snapshot: CoachSnapshot): BadHabitSnapshot? = snapshot.badHabits.minByOrNull { it.elapsed }

    /** [habitName] `null` = hidden outside the app: no name, and no category text either, which would name it too. */
    fun milestoneNote(habitName: String?, reached: Duration): CoachNote {
        val milestone = habitName?.let { name -> knowledge.match(name).milestones.lastOrNull { it.after <= reached } }
        val title = if (habitName == null) {
            tr("${dur(reached)} clear", "صامد منذ ${durAfter(reached)}")
        } else {
            tr("${dur(reached)} clear: ${displayName(habitName)}", "صامد منذ ${durAfter(reached)}: ${displayName(habitName)}")
        }
        val body = milestone?.let { "${it.title}. ${it.detail}" }
            ?: tr("You reached ${dur(reached)}. Keep going — you are proving it to yourself.", "وصلت إلى ${durAfter(reached)}. واصل، فأنت تثبت ذلك لنفسك.")
        return CoachNote(title, body)
    }

    /** "3 of 5 today" / "2 of 4 days this week" in the current language. */
    private fun progressLine(h: GoodHabitSnapshot): String = when {
        !ar -> h.progressLine
        h.weeklyTarget != null -> "${h.weekDone} من ${h.weekGoal} أيام هذا الأسبوع"
        else -> "${h.todayCount} من ${h.dailyTarget} اليوم"
    }

    // ---------- Chat ----------

    fun greeting(snapshot: CoachSnapshot, seed: Long): String {
        val name = snapshot.userName.trim().substringBefore(' ') // first name keeps the greeting to one short line
        val hello = if (name.isEmpty()) tr("Hi there.", "أهلًا بك.") else tr("Hi $name.", "أهلًا يا $name.")
        val focus = snapshot.badHabits.maxByOrNull { it.elapsed }
        val status = when {
            focus != null -> tr(
                " You're **${dur(focus.elapsed)}** clear on **${displayName(focus.name)}**.",
                " أنت صامد منذ **${durAfter(focus.elapsed)}** على **${displayName(focus.name)}**.",
            )
            snapshot.goodHabits.isNotEmpty() -> tr(" Your good habits are waiting for today's check-in.", " عاداتك الجيدة تنتظر تسجيل اليوم.")
            else -> tr(" Add a habit you want to leave behind and I'll help you through it.", " أضف عادة تريد تركها خلفك، وسأساعدك على تجاوزها.")
        }
        return "$hello$status"
    }

    fun suggestions(snapshot: CoachSnapshot): List<String> {
        val list = mutableListOf<String>()
        if (snapshot.badHabits.isNotEmpty()) {
            list += tr("I have a craving", "عندي رغبة قوية")
            list += tr("What's happening in my body?", "ماذا يحدث في جسمي؟")
            list += tr("I slipped up", "تعثّرت")
        }
        list += tr("How am I doing?", "كيف تقدّمي؟")
        list += tr("Motivate me", "حفّزني")
        list += tr("Give me a reset plan", "أعطني خطة لبداية جديدة")
        return list
    }

    fun reply(userText: String, snapshot: CoachSnapshot, seed: Long): String {
        val text = userText.lowercase().trim()
        val tokens = CoachText.tokens(userText)
        val focused = snapshot.focusHabitId?.let { id -> snapshot.badHabits.firstOrNull { it.id == id } }
        val habit = findMentionedHabit(text, tokens, snapshot) ?: focused ?: snapshot.badHabits.minByOrNull { it.elapsed }
        val name = snapshot.userName.trim()
        val lead = when {
            name.isEmpty() -> ""
            ar -> "$name، "
            else -> "$name, "
        }
        val parsedDuration = DurationParser.find(userText)

        return when {
            isCrisis(userText) -> crisisReply()
            isWhatHappens(userText, tokens, parsedDuration, snapshot) -> whatHappensReply(lead, userText, parsedDuration, snapshot, focused, seed)
            hasAny(text, tokens, RELAPSE) -> relapseReply(lead, habit, seed)
            hasAny(text, tokens, CRAVING) -> cravingReply(lead, habit, seed)
            hasAny(text, tokens, BODY) -> bodyReply(lead, habit)
            hasAny(text, tokens, PROGRESS) -> progressReply(snapshot)
            hasAny(text, tokens, PLAN) -> planReply(lead, habit)
            hasAny(text, tokens, EMOTION) -> emotionReply(lead, habit)
            hasAny(text, tokens, MOTIVATION) -> motivationReply(lead, habit, seed)
            hasAny(text, tokens, GOOD) -> goodHabitsReply(snapshot)
            hasAny(text, tokens, EVENTS) -> eventReply(snapshot)
            hasWord(tokens, THANKS) -> thanksReply(name, seed)
            hasWord(tokens, GREETING) -> greeting(snapshot, seed)
            hasPhrase(tokens, HOW_ARE_YOU) -> tr(
                "I'm doing well, thank you for asking. More importantly, how are you doing today?",
                "أنا بخير، شكرًا لسؤالك. والأهم: كيف حالك أنت اليوم؟",
            )
            hasPhrase(tokens, WHO_ARE_YOU) -> capabilitiesReply()
            hasPhrase(tokens, GOODBYE) -> tr(
                "Take care${if (name.isEmpty()) "" else ", $name"}. I'm here whenever you need a reset or a boost.",
                "اعتنِ بنفسك${if (name.isEmpty()) "" else " يا $name"}. أنا هنا متى احتجت بداية جديدة أو دفعة حماس.",
            )
            focused != null -> habitOverviewReply(lead, focused)
            else -> tr(
                "I can help with cravings, your progress, what is happening in your body, what happens when you stop for a while, or a reset plan. " +
                    "Tell me what is on your mind, or pick one of the suggestions below.",
                "أستطيع مساعدتك في الرغبات القوية، وتقدّمك، وما يحدث في جسمك، وما يحدث حين تتوقف لفترة، أو في خطة لبداية جديدة. " +
                    "أخبرني بما يدور في ذهنك، أو اختر أحد الاقتراحات بالأسفل.",
            )
        }
    }

    // ---------- "What happens if I stop for ...?" ----------

    /**
     * True for questions like "what happens if I stop for 6 hours", "what will I feel after 3 days without it",
     * "how long until cravings stop", "ماذا يحدث لو بطلت يومين", and for short follow-ups such as "and after a week?".
     */
    private fun isWhatHappens(userText: String, tokens: List<String>, duration: ParsedDuration?, snapshot: CoachSnapshot): Boolean {
        if (looksLikeWhatHappens(tokens, duration, userText)) return true
        // Follow-up: the previous question was a "what happens" one and this is just a new time.
        if (duration != null && tokens.size <= FOLLOW_UP_MAX_WORDS) {
            val previous = snapshot.recentUserTexts.lastOrNull() ?: return false
            return looksLikeWhatHappens(CoachText.tokens(previous), DurationParser.find(previous), previous)
        }
        return false
    }

    private fun looksLikeWhatHappens(tokens: List<String>, duration: ParsedDuration?, rawText: String): Boolean {
        if (tokens.isEmpty()) return false
        val trigger = WHAT_HAPPENS_TRIGGERS.any { CoachText.hasPhrase(tokens, it) }
        val stopContext = STOP_CONTEXT.any { CoachText.hasWord(tokens, it) } || STOP_CONTEXT_PHRASES.any { CoachText.hasPhrase(tokens, it) }
        if (trigger && (duration != null || stopContext)) return true
        val timeWord = tokens.any { it == "after" || it == "without" || it == "بعد" || it == "بدون" }
        val questionLike = rawText.contains('?') || rawText.contains('؟') || QUESTION_WORDS.any { CoachText.hasWord(tokens, it) }
        return duration != null && (stopContext || timeWord) && questionLike
    }

    private class Resolved(val snap: BadHabitSnapshot?, val name: String)

    /** The habit a piece of text talks about: one of the user's habits, or just a recognised category. */
    private fun resolveMention(rawText: String, snapshot: CoachSnapshot): Resolved? {
        val tokens = CoachText.tokens(rawText)
        findMentionedHabit(rawText.lowercase(), tokens, snapshot)?.let { return Resolved(it, it.name) }
        val match = knowledge.matchDetailed(rawText) ?: return null
        return Resolved(null, match.category.label)
    }

    private fun whatHappensReply(
        lead: String,
        userText: String,
        duration: ParsedDuration?,
        snapshot: CoachSnapshot,
        focused: BadHabitSnapshot?,
        seed: Long,
    ): String {
        val remainder = if (duration != null) userText.removeRange(duration.start, duration.end) else userText
        val resolved: Resolved? = resolveMention(remainder, snapshot)
            ?: snapshot.recentUserTexts.asReversed().firstNotNullOfOrNull { resolveMention(it, snapshot) }
            ?: focused?.let { Resolved(it, it.name) }
            ?: snapshot.badHabits.singleOrNull()?.let { Resolved(it, it.name) }

        if (resolved == null && snapshot.badHabits.size > 1) {
            val names = snapshot.badHabits.map { displayName(it.name) }
            return if (ar) {
                val list = names.dropLast(1).joinToString("، ") + " أم " + names.last()
                "${lead}أي عادة تقصد: $list؟ اكتب اسمها مع المدة، مثل «ماذا يحدث لـ ${names.first()} بعد 6 ساعات؟»"
            } else {
                val list = names.dropLast(1).joinToString(", ") + " or " + names.last()
                "${lead}which habit do you mean: $list? Tell me its name together with the time, " +
                    "for example \"what happens to ${names.first()} after 6 hours?\""
            }
        }

        val habitName = resolved?.name ?: ""
        val snap = resolved?.snap
        val actual = snap?.elapsed ?: Duration.ZERO
        val opener = seed.mod(3L).toInt()

        if (duration == null) {
            val upcoming = explainer.upcoming(habitName, actual, 2)
            val answer = explainer.explain(habitName, actual)
            val where = if (snap != null) tr(" You are ${span(actual)} in.", " مضى عليك ${span(actual)}.") else ""
            val shown = if (snap != null) displayName(snap.name) else answer.habitLabel
            val sb = StringBuilder(tr("${lead}here is what is coming up for $shown.$where\n\n", "${lead}إليك ما ينتظرك مع $shown.$where\n\n"))
            if (upcoming.isEmpty()) {
                sb.append(
                    tr(
                        "The main milestones on this timeline are behind you. From here it is mostly about keeping your routines and knowing your triggers.\n\n",
                        "تجاوزت المحطات الرئيسية في هذا المسار. من الآن، الأمر يتعلق غالبًا بالحفاظ على روتينك ومعرفة محفّزاتك.\n\n",
                    ),
                )
            } else {
                upcoming.forEachIndexed { index, m ->
                    if (ar) {
                        val label = if (index == 0) "التالي" else "بعد ذلك"
                        sb.append("$label: ${m.title}، بعد نحو ${span(m.after - actual)}. ${m.detail}\n\n")
                    } else {
                        val label = if (index == 0) "Next" else "After that"
                        sb.append("$label: ${m.title}, in about ${span(m.after - actual)}. ${m.detail}\n\n")
                    }
                }
            }
            sb.append(
                tr(
                    "Tell me a time, for example \"what happens after 3 days?\", and I will show you that moment.\n\n",
                    "اذكر لي مدة، مثل «ماذا يحدث بعد 3 أيام؟»، وسأريك تلك اللحظة.\n\n",
                ),
            )
            appendSafetyAndDisclaimer(sb, answer)
            return sb.toString().trimEnd()
        }

        val answer = explainer.explain(habitName, duration.duration)
        val shown = if (snap != null) displayName(snap.name) else answer.habitLabel
        val time = span(duration.duration)
        val intro = when (opener) {
            0 -> tr("here is what typically happens around $time in with $shown.", "إليك ما يحدث عادةً بعد نحو $time مع $shown.")
            1 -> tr("around $time in with $shown, this is the usual picture.", "بعد نحو $time مع $shown، هذه هي الصورة المعتادة.")
            else -> tr("this is what many people notice about $time into $shown.", "هذا ما يلاحظه كثيرون بعد نحو $time مع $shown.")
        }
        val sb = StringBuilder("$lead$intro\n\n")
        sb.append("${answer.headline}\n${answer.whatIsHappening}\n\n")
        val before = answer.alreadyHappened.dropLast(1).takeLast(2)
        if (before.isNotEmpty()) {
            sb.append(tr("Earlier on the way: ${before.joinToString("; ") { it.title }}.\n\n", "وفي الطريق قبل ذلك: ${before.joinToString("؛ ") { it.title }}.\n\n"))
        }
        if (answer.next != null && answer.timeToNext != null) {
            sb.append(tr("Next: ${answer.next.title}, about ${span(answer.timeToNext)} later.\n\n", "التالي: ${answer.next.title}، بعد نحو ${span(answer.timeToNext)}.\n\n"))
        }
        if (answer.cravingTips.isNotEmpty()) {
            val tip = answer.cravingTips[seed.mod(answer.cravingTips.size.toLong()).toInt()]
            sb.append(tr("If urges show up: $tip\n\n", "إن جاءتك الرغبة: $tip\n\n"))
        }
        if (snap != null) sb.append(tr("You are actually ${span(snap.elapsed)} in right now.\n\n", "أنت الآن فعليًا عند ${span(snap.elapsed)}.\n\n"))
        appendSafetyAndDisclaimer(sb, answer, includeDisclaimer = false)
        return sb.toString().trimEnd()
    }

    private fun appendSafetyAndDisclaimer(sb: StringBuilder, answer: StopTimelineAnswer, includeDisclaimer: Boolean = true) {
        answer.safetyNote?.let { sb.append(tr("Important: $it\n\n", "مهم: $it\n\n")) }
        if (includeDisclaimer) {
            sb.append(tr("Timelines vary, and this is general information, not medical advice.", "تختلف المدد من شخص لآخر، وهذه معلومات عامة وليست نصيحة طبية."))
        }
    }

    private fun thanksReply(name: String, seed: Long): String {
        if (ar) {
            val who = if (name.isEmpty()) "يا صديقي" else "يا $name"
            return listOf(
                "العفو $who. أنا هنا متى احتجت بداية جديدة أو دفعة حماس.",
                "في أي وقت $who. كل تسجيل يُحسب.",
                "سعيد بمساعدتك $who. واصل، خطوة بخطوة.",
            )[seed.mod(3L).toInt()]
        }
        val who = name.ifEmpty { "friend" }
        return listOf(
            "You're welcome, $who. I'm here whenever you need a reset or a boost.",
            "Anytime, $who. Every check-in counts.",
            "Glad to help, $who. Keep going, one step at a time.",
        )[seed.mod(3L).toInt()]
    }

    private fun capabilitiesReply(): String = tr(
        "I'm your offline NixUra coach. I can help you through a craving, explain what happens in your body and mind when you stop for a few hours or days, " +
            "review your progress, build a reset plan, or just cheer you on. Replies are generated on your device.",
        "أنا مدرّبك في NixUra، وأعمل دون اتصال. أستطيع مساعدتك على تجاوز رغبة قوية، وشرح ما يحدث في جسمك وذهنك حين تتوقف لساعات أو أيام، " +
            "ومراجعة تقدّمك، ووضع خطة لبداية جديدة، أو تشجيعك فقط. تُكتب الردود على جهازك.",
    )

    private fun cravingReply(lead: String, habit: BadHabitSnapshot?, seed: Long): String {
        val category = habit?.let { knowledge.match(it.name) } ?: knowledge.localize(knowledge.generic)
        val tip = category.cravingTips[seed.mod(category.cravingTips.size.toLong()).toInt()]
        if (ar) {
            val where = habit?.let { " مضى عليك ${dur(it.elapsed)} مع ${displayName(it.name)}." } ?: ""
            val safety = category.safetyNote?.let { "\n\nمهم: $it" } ?: ""
            return "${lead}هذه الرغبة حقيقية، لكنها مؤقتة. معظم الرغبات تخفت خلال 10 دقائق.$where\n\n" +
                "جرّب هذا الآن:\n1. أجّل: اضبط مؤقتًا لعشر دقائق.\n2. تنفّس: 5 أنفاس بطيئة (شهيق 4، زفير 6).\n" +
                "3. اشرب كوب ماء.\n4. تحرّك: قم وانتقل إلى غرفة أخرى.\n\nنصيحة إضافية: $tip$safety\n\n" +
                "أخبرني حين ينتهي المؤقت وكيف تشعر."
        }
        val where = habit?.let { " You are ${dur(it.elapsed)} into ${displayName(it.name)}." } ?: ""
        val safety = category.safetyNote?.let { "\n\nImportant: $it" } ?: ""
        return "${lead}that urge is real, and it is temporary. Most cravings fade in 10 minutes.$where\n\n" +
            "Try this now:\n1. Delay — set a 10-minute timer.\n2. Breathe — 5 slow breaths (in 4, out 6).\n" +
            "3. Drink a glass of water.\n4. Move — stand up and change rooms.\n\nExtra tip: $tip$safety\n\n" +
            "Tell me when the timer ends and how it feels."
    }

    private fun relapseReply(lead: String, habit: BadHabitSnapshot?, seed: Long): String {
        if (ar) {
            val record = habit?.let { " أطول سلسلة لك، ${dur(it.longest)}، ما زالت تُحسب، فهي جزء من قصتك." } ?: ""
            val closing = listOf(
                "ماذا كان يحدث قبلها مباشرة؟ تسمية المحفّز هي طريقتنا للاستعداد له في المرة القادمة.",
                "ماذا تحتاج الآن: خطة لبداية جديدة، أم لحظة تلتقط فيها أنفاسك؟",
            )[seed.mod(2L).toInt()]
            return "${lead}التعثّر ليس النهاية، ولا يمحو تقدّمك.$record\n\n" +
                "كن لطيفًا مع نفسك، فاللوم يقوّي العادة، والفضول يقوّيك أنت. $closing\n\n" +
                "حين تكون مستعدًا، افتح العادة والمس «سجّل تعثّرًا» لتعيد العدّاد بصدق."
        }
        val record = habit?.let { " Your longest streak of ${dur(it.longest)} still counts — it is part of your story." } ?: ""
        val closing = listOf(
            "What was happening just before? Naming the trigger is how we plan for it next time.",
            "What do you need right now: a reset plan, or just a moment to breathe?",
        )[seed.mod(2L).toInt()]
        return "${lead}a slip is not the end, and it does not erase your progress.$record\n\n" +
            "Be kind to yourself — shame makes the habit stronger, curiosity makes you stronger. $closing\n\n" +
            "When you are ready, open the habit and tap \"Record relapse\" to restart the counter honestly."
    }

    private fun bodyReply(lead: String, habit: BadHabitSnapshot?): String {
        if (habit == null) {
            return tr(
                "${lead}add a habit you want to quit and I will show how your body and mind recover over time.",
                "${lead}أضف عادة تريد الإقلاع عنها، وسأريك كيف يتعافى جسمك وذهنك مع الوقت.",
            )
        }
        val insight = insightFor(habit.name, habit.elapsed)
        val latest = insight.achieved.lastOrNull()
        val next = insight.next
        val sb = StringBuilder(
            tr(
                "${lead}here is what is happening with ${displayName(habit.name)} (${insight.category.label}), ${dur(habit.elapsed)} in:\n\n",
                "${lead}إليك ما يحدث مع ${displayName(habit.name)} (${insight.category.label})، بعد ${durAfter(habit.elapsed)}:\n\n",
            ),
        )
        if (latest != null) {
            sb.append(tr("Now: ${latest.title}. ${latest.detail}\n\n", "الآن: ${latest.title}. ${latest.detail}\n\n"))
        } else {
            sb.append(tr("Your recovery has just started — the first changes begin soon.\n\n", "بدأ تعافيك للتو، وأول التغييرات قريبة.\n\n"))
        }
        if (next != null) {
            sb.append(tr("Next: ${next.title} in about ${dur(next.after - habit.elapsed)}.\n\n", "التالي: ${next.title} بعد نحو ${durAfter(next.after - habit.elapsed)}.\n\n"))
        }
        insight.category.safetyNote?.let { sb.append(tr("Important: $it\n\n", "مهم: $it\n\n")) }
        sb.append(tr("This is general information, not medical advice.", "هذه معلومات عامة، وليست نصيحة طبية."))
        return sb.toString()
    }

    /** Shown when the user picked a habit and sent something that matches no other topic. */
    private fun habitOverviewReply(lead: String, habit: BadHabitSnapshot): String {
        val insight = insightFor(habit.name, habit.elapsed)
        val shown = displayName(habit.name)
        val sb = StringBuilder()
        if (ar) {
            sb.append("${lead}لنتحدث عن $shown (${insight.category.label}). ")
            sb.append("مضى عليك ${dur(habit.elapsed)}، وأطول سلسلة لك ${dur(habit.longest)}")
            sb.append(if (habit.relapseCount == 0) "، ولم تسجّل أي تعثّر.\n\n" else "، مع ${habit.relapseCount} تعثّر مسجّل.\n\n")
            insight.achieved.lastOrNull()?.let { sb.append("أين جسمك الآن: ${it.title}. ${it.detail}\n\n") }
            insight.category.safetyNote?.let { sb.append("مهم: $it\n\n") }
            sb.append("أستطيع مساعدتك في رغبة قوية، أو شرح ما يحدث في جسمك، أو وضع خطة لبداية جديدة مع $shown. بماذا أساعدك؟")
        } else {
            sb.append("${lead}let's talk about $shown (${insight.category.label}). ")
            sb.append("You are ${dur(habit.elapsed)} in; your longest streak is ${dur(habit.longest)}")
            sb.append(if (habit.relapseCount == 0) " and you have no relapses recorded.\n\n" else " with ${habit.relapseCount} relapse(s) recorded.\n\n")
            insight.achieved.lastOrNull()?.let { sb.append("Where your body is now: ${it.title}. ${it.detail}\n\n") }
            insight.category.safetyNote?.let { sb.append("Important: $it\n\n") }
            sb.append("I can help with a craving, explain what is happening in your body, or build a reset plan for $shown. What do you need?")
        }
        return sb.toString()
    }

    private fun progressReply(snapshot: CoachSnapshot): String {
        if (snapshot.badHabits.isEmpty() && snapshot.goodHabits.isEmpty()) {
            return tr("Nothing to review yet. Add a habit and I will track your progress with you.", "لا شيء نراجعه بعد. أضف عادة وسأتابع تقدّمك معك.")
        }
        val sb = StringBuilder(tr("Here is where you stand:\n\n", "هذا موقعك الآن:\n\n"))
        snapshot.badHabits.forEach {
            sb.append(
                tr(
                    "• ${displayName(it.name)}: ${dur(it.elapsed)} clear (best ${dur(it.longest)})\n",
                    "• ${displayName(it.name)}: صامد منذ ${durAfter(it.elapsed)} (الأفضل ${dur(it.longest)})\n",
                ),
            )
        }
        snapshot.goodHabits.forEach {
            if (ar) {
                val streak = if (it.weeklyTarget != null) arWeeks(it.streakDays.toLong()) else arDays(it.streakDays.toLong())
                sb.append("• ${it.name}: ${progressLine(it)}، سلسلة $streak\n")
            } else {
                val unit = if (it.weeklyTarget != null) plural(it.streakDays, "week", "weeks") else plural(it.streakDays, "day", "days")
                sb.append("• ${it.name}: ${it.progressLine}, streak ${it.streakDays} $unit\n")
            }
        }
        val best = snapshot.badHabits.maxByOrNull { it.elapsed }
        if (best != null) {
            sb.append(tr("\nYour strongest streak is ${displayName(best.name)}. Keep it protected.", "\nأقوى سلسلة لديك هي ${displayName(best.name)}. احمِها."))
        }
        return sb.toString().trimEnd()
    }

    private fun planReply(lead: String, habit: BadHabitSnapshot?): String {
        val category = habit?.let { knowledge.match(it.name) } ?: knowledge.localize(knowledge.generic)
        if (ar) {
            val target = habit?.let { displayName(it.name) } ?: "عادتك"
            return "${lead}إليك خطة بسيطة لبداية جديدة مع $target:\n\n" +
                "1. اعرف المحفّز: متى وأين تبدأ الرغبة عادةً؟\n" +
                "2. اختر بديلًا: ${category.cravingTips.first()}\n" +
                "3. صعّب الأمر: أزل ما يذكّرك بها وضع عوائق حول $target.\n" +
                "4. أخبر شخصًا تثق به، أو سجّل هنا كل يوم.\n" +
                "5. احتفل بكل محطة مهما كانت صغيرة.\n\n" +
                category.whyItMatters
        }
        val target = habit?.let { displayName(it.name) } ?: "your habit"
        return "${lead}here is a simple reset plan for $target:\n\n" +
            "1. Spot the trigger — when and where does the urge usually start?\n" +
            "2. Pick a replacement — ${category.cravingTips.first()}\n" +
            "3. Make it harder — remove cues and add friction around $target.\n" +
            "4. Tell someone you trust, or check in here daily.\n" +
            "5. Celebrate every milestone, however small.\n\n" +
            category.whyItMatters
    }

    private fun emotionReply(lead: String, habit: BadHabitSnapshot?): String {
        if (ar) {
            val target = habit?.let { " مع ${displayName(it.name)}" } ?: ""
            return "${lead}المشاعر القوية من أكثر المحفّزات شيوعًا$target، والشعور بها لا يعني أن عليك أن تستجيب لها.\n\n" +
                "جرّب «تنفّس الصندوق» لدقيقتين: شهيق 4، احبس 4، زفير 4، احبس 4. ثم سمِّ شعورك بصوت عالٍ («أشعر بالتوتر») واسأل نفسك عمّا تحتاجه فعلًا: راحة، أم طعام، أم صحبة، أم استراحة.\n\n" +
                "إن كانت هذه المشاعر ثقيلة أو استمرت أسابيع، فالحديث مع شخص تثق به أو مع مختص قد يُحدث فرقًا حقيقيًا."
        }
        val target = habit?.let { " around ${displayName(it.name)}" } ?: ""
        return "${lead}strong feelings are one of the most common triggers$target — and feeling them does not mean you have to act on them.\n\n" +
            "Try box breathing for 2 minutes: in 4, hold 4, out 4, hold 4. Then name the feeling out loud (\"I feel stressed\") and ask what you actually need: rest, food, company, or a break.\n\n" +
            "If these feelings are heavy or last for weeks, talking to someone you trust or a professional can make a real difference."
    }

    private fun motivationReply(lead: String, habit: BadHabitSnapshot?, seed: Long): String {
        return if (habit != null) {
            tr(
                "$lead${motivation(habit.name, habit.elapsed, seed)}\n\nYou are ${dur(habit.elapsed)} into ${displayName(habit.name)}. That is real, measurable progress.",
                "$lead${motivation(habit.name, habit.elapsed, seed)}\n\nمضى عليك ${dur(habit.elapsed)} مع ${displayName(habit.name)}. هذا تقدّم حقيقي يمكن قياسه.",
            )
        } else {
            tr(
                "${lead}every habit begins with a decision. Add one you want to leave behind and I will cheer you on every step.",
                "${lead}كل عادة تبدأ بقرار. أضف عادة تريد تركها خلفك، وسأشجّعك في كل خطوة.",
            )
        }
    }

    private fun goodHabitsReply(snapshot: CoachSnapshot): String {
        if (snapshot.goodHabits.isEmpty()) {
            return tr(
                "Good habits crowd out bad ones. Add one small thing — a glass of water, a short walk — and check in daily.",
                "العادات الجيدة تزاحم السيئة. أضف شيئًا صغيرًا، كوب ماء أو مشيًا قصيرًا، وسجّله كل يوم.",
            )
        }
        val pending = snapshot.goodHabits.filter { it.pendingToday }
        return if (pending.isEmpty()) {
            tr(
                "You have completed all your good habits today. That discipline is what makes quitting stick.",
                "أنجزت كل عاداتك الجيدة اليوم. هذا الانضباط هو ما يجعل الإقلاع يدوم.",
            )
        } else {
            tr("Still to do today:\n", "باقٍ لليوم:\n") + pending.joinToString("\n") { "• ${it.name} (${progressLine(it)})" } +
                tr("\n\nSmall actions repeated daily become who you are.", "\n\nالخطوات الصغيرة التي تتكرّر كل يوم تصبح جزءًا منك.")
        }
    }

    private fun eventReply(snapshot: CoachSnapshot): String {
        val event = snapshot.nextEvent ?: return tr(
            "You have no upcoming events. Add one to the Events tab and I will help you stay on track until it arrives.",
            "ليست لديك مناسبات قادمة. أضف واحدة من تبويب المناسبات، وسأساعدك على البقاء على المسار حتى موعدها.",
        )
        return tr(
            "Your next event is \"${event.title}\" in ${dur(event.remaining)}. Staying consistent now means you arrive feeling proud of yourself.",
            "مناسبتك القادمة «${event.title}» بعد ${durAfter(event.remaining)}. استمرارك الآن يعني أن تصل إليها فخورًا بنفسك.",
        )
    }

    private fun crisisReply(): String = tr(
        "I'm really sorry you are feeling this way, and I'm glad you told me. You deserve support right now.\n\n" +
            "Please reach out to someone you trust, or contact your local emergency number or a crisis line in your country as soon as you can. " +
            "If you are in immediate danger, call emergency services now.\n\n" +
            "I am an app with simple automated replies, so I can't give you the care a person can — but you do not have to go through this alone.",
        "يؤسفني حقًا أنك تشعر بهذا، ويسعدني أنك أخبرتني. أنت تستحق الدعم الآن.\n\n" +
            "تواصل من فضلك مع شخص تثق به، أو اتصل برقم الطوارئ المحلي أو بخط دعم الأزمات في بلدك في أقرب وقت. " +
            "وإن كنت في خطر مباشر، فاتصل بخدمات الطوارئ الآن.\n\n" +
            "أنا تطبيق بردود آلية بسيطة، ولا أستطيع أن أقدّم لك الرعاية التي يقدّمها إنسان، لكنك لست مضطرًا لمواجهة هذا وحدك.",
    )

    // ---------- Helpers ----------

    private fun findMentionedHabit(text: String, tokens: List<String>, snapshot: CoachSnapshot): BadHabitSnapshot? {
        val normalized = CoachText.normalize(text)
        val byName = snapshot.badHabits.firstOrNull { habit ->
            val n = CoachText.normalize(habit.name)
            n.isNotEmpty() && (
                normalized.contains(n) ||
                    CoachText.tokens(n).any { key ->
                        key.length >= 4 && tokens.any { w ->
                            w.startsWith(key) || (key.startsWith(w) && w.length >= 4) || CoachText.wordMatches(w, key)
                        }
                    }
                )
        }
        if (byName != null) return byName
        // Not named, but a word that belongs to the same category ("cigarette" for a habit called "Smoking").
        val mentioned = knowledge.matchDetailed(text)?.category ?: return null
        return snapshot.badHabits.firstOrNull { knowledge.match(it.name).id == mentioned.id }
    }

    /** True when [userText] suggests self-harm. Callers (including any future online AI) must handle this locally. */
    fun isCrisis(userText: String): Boolean {
        val text = userText.lowercase()
        if (CRISIS.any { text.contains(it) }) return true
        val tokens = CoachText.tokens(userText)
        return CRISIS_PHRASES.any { CoachText.hasPhrase(tokens, it) }
    }

    /** A stem ("crav") matches a word that starts with it; a multi-word phrase matches consecutive words (typos allowed). */
    private fun hasAny(text: String, tokens: List<String>, stems: List<String>): Boolean = stems.any { stem ->
        val parts = CoachText.tokens(stem)
        when {
            parts.isEmpty() -> false
            parts.size > 1 -> CoachText.hasPhrase(tokens, parts)
            else -> tokens.any { CoachText.stemMatches(it, parts[0]) }
        }
    }

    private fun hasPhrase(tokens: List<String>, phrases: List<List<String>>): Boolean = phrases.any { CoachText.hasPhrase(tokens, it) }

    private fun hasWord(tokens: List<String>, candidates: List<String>): Boolean =
        candidates.any { CoachText.hasWord(tokens, CoachText.normalize(it)) }

    companion object {
        private val ONE_HOUR = Duration.parse("1h")
        private const val FOLLOW_UP_MAX_WORDS = 7

        /** Habit categories that are shown by their neutral label in coach text, never by the typed words. */
        private val SENSITIVE_IDS = setOf("explicitcontent")

        private fun phrases(vararg raw: String): List<List<String>> = raw.map { CoachText.phrase(it) }

        private val WHAT_HAPPENS_TRIGGERS = phrases(
            "what happens", "what will happen", "what would happen", "what is going to happen", "whats happening", "what is happening",
            "what do i feel", "what will i feel", "what would i feel", "how will i feel", "how would i feel", "what to expect",
            "what changes", "how long until", "how long before", "how long till", "how long will it take", "when will it get easier",
            "when does it get easier", "when do cravings", "when will cravings", "when does the craving", "when will the cravings",
            "ماذا يحدث", "ماذا سيحدث", "ماذا يحصل", "ايه اللي بيحصل", "ايه اللي هيحصل", "ايه اللي يحصل", "هيحصل ايه", "بيحصل ايه",
            "ايه بيحصل", "ايه هيحصل", "ايش يصير", "شو يصير", "شو بيصير", "هحس بايه", "ماذا اشعر", "امتى هيروح", "متى تتوقف",
        )
        private val STOP_CONTEXT = listOf(
            "stop", "stopped", "stopping", "quit", "quitting", "quitted", "without", "abstain", "abstaining", "clear",
            "cravings", "craving", "easier", "fade",
            "بطلت", "ابطل", "هبطل", "بطل", "اقلع", "اقلعت", "توقفت", "توقف", "بدون", "امتنع", "تركت", "اترك", "لو",
        )
        private val STOP_CONTEXT_PHRASES = phrases("go away", "cut out", "cut down", "من غير", "give up")
        private val QUESTION_WORDS = listOf("what", "how", "when", "will", "would", "ماذا", "ايه", "امتى", "هحس")

        private val CRISIS_PHRASES = phrases(
            "suicide", "suicidal", "kill myself", "killing myself", "end my life", "want to die", "wanna die", "hurt myself",
            "self harm", "dont want to live", "no reason to live", "better off dead", "wish i was dead", "end it all",
            "انتحار", "عايز اموت", "عاوز اموت", "نفسي اموت", "اقتل نفسي", "هقتل نفسي", "اموت نفسي", "مش عايز اعيش",
        )
        private val HOW_ARE_YOU = phrases("how are you", "how are you doing", "how r u", "كيف حالك", "عامل ايه", "ازيك", "اخبارك ايه")
        private val WHO_ARE_YOU = phrases("who are you", "what are you", "what can you do", "what do you do", "انت مين", "بتعمل ايه")
        private val GOODBYE = phrases("bye", "goodbye", "see you", "good night", "مع السلامه", "تصبح على خير")

        private val CRISIS = listOf(
            "suicid", "kill myself", "end my life", "want to die", "hurt myself", "self harm", "self-harm", "انتحار", "اموت نفسي", "أموت نفسي",
        )
        private val RELAPSE = listOf(
            "relaps", "slipped", "slip up", "slip-up", "failed", "fell off", "messed up", "gave in", "did it again", "broke my streak", "انتكاس", "فشلت", "رجعت",
            "تعثر", "ضعفت",
        )
        private val CRAVING = listOf(
            "crav", "urge", "tempt", "can't resist", "cant resist", "about to", "want to smoke", "want to drink", "i want a", "رغبة", "نفسي", "هضعف",
        )
        private val BODY = listOf(
            "body", "health", "benefit", "what happens", "improv", "timeline", "recover", "جسم", "صحة", "فوائد",
        )
        private val PROGRESS = listOf(
            "progress", "how am i doing", "how's it going", "status", "streak", "stats", "تقدم", "حالتي",
        )
        private val PLAN = listOf(
            "plan", "reset", "routine", "strateg", "tips", "advice", "help me", "what should i do", "خطة", "نصيحة",
            "بداية جديدة",
        )
        private val EMOTION = listOf(
            "stress", "anxi", "bored", "lonely", "sad", "angry", "upset", "overwhelm", "توتر", "ملل", "حزين", "زعلان",
        )
        private val MOTIVATION = listOf(
            "motivat", "encourag", "inspir", "give up", "struggl", "discourag", "tired", "hard", "تحفيز", "شجعني", "صعب",
            "حفزني", "حمس",
        )
        private val GOOD = listOf(
            "good habit", "water", "walk", "workout", "exercis", "check-in", "discipline",
            "عادات جيدة", "عادة جيدة", "ماء", "مياه", "مشي", "تمرين", "انضباط",
        )
        private val EVENTS = listOf("event", "countdown", "upcoming", "مناسب", "موعد")
        private val THANKS = listOf("thanks", "thank", "thx", "شكرا", "شكراً")
        private val GREETING = listOf("hi", "hello", "hey", "hola", "مرحبا", "اهلا", "أهلا", "السلام")
    }
}

/** "3 days", "5 hours", "12 minutes" — the largest meaningful unit, used in messages. [arabic]: "3 أيام", "ساعتان". */
fun durationText(duration: Duration, arabic: Boolean = false): String {
    val total = duration.coerceAtLeast(Duration.ZERO)
    val days = total.inWholeDays
    val hours = total.inWholeHours
    val minutes = total.inWholeMinutes
    if (arabic) {
        return when {
            days >= 1 -> arDays(days)
            hours >= 1 -> arHours(hours)
            else -> arMinutes(minutes.coerceAtLeast(0))
        }
    }
    return when {
        days >= 1 -> "$days ${plural(days.toInt(), "day", "days")}"
        hours >= 1 -> "$hours ${plural(hours.toInt(), "hour", "hours")}"
        else -> "${minutes.coerceAtLeast(0)} ${plural(minutes.toInt(), "minute", "minutes")}"
    }
}

private fun plural(count: Int, one: String, many: String) = if (count == 1) one else many

internal fun arDays(n: Long) = ArabicPlurals.format(n, "%d يوم", "يوم واحد", "يومان", "%d أيام", "%d يومًا", "%d يوم")
internal fun arHours(n: Long) = ArabicPlurals.format(n, "%d ساعة", "ساعة واحدة", "ساعتان", "%d ساعات", "%d ساعة", "%d ساعة")
internal fun arMinutes(n: Long) = ArabicPlurals.format(n, "%d دقيقة", "دقيقة واحدة", "دقيقتان", "%d دقائق", "%d دقيقة", "%d دقيقة")
internal fun arWeeks(n: Long) = ArabicPlurals.format(n, "%d أسبوع", "أسبوع واحد", "أسبوعان", "%d أسابيع", "%d أسبوعًا", "%d أسبوع")

/** A duration after a preposition (بعد, منذ, إلى): the dual is genitive there, so "ساعتان" becomes "ساعتين". */
internal fun arAfterPreposition(duration: String): String = duration
    .replace("ساعتان", "ساعتين").replace("يومان", "يومين").replace("دقيقتان", "دقيقتين").replace("أسبوعان", "أسبوعين")

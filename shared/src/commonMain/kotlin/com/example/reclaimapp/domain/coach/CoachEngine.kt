package com.example.reclaimapp.domain.coach

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
    val streakDays: Int,
)

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
class CoachEngine(private val knowledge: HabitKnowledgeBase = HabitKnowledgeBase()) {

    private val explainer = StopTimelineExplainer(knowledge)

    fun categoryFor(habitName: String): HabitCategory = knowledge.match(habitName)

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
                "The first hour is the hardest part, and you are already in it. Stay with it.",
                "You decided. That is the biggest step — now just get through the next few minutes.",
            )
            elapsed < 1.days -> listOf(
                "Every hour you stay clear is proof that you can do this. Keep stacking them.",
                "Today is the foundation. Protect it one hour at a time.",
            )
            elapsed < 3.days -> listOf(
                "Cravings often peak around now. If it feels hard, that means your brain is rewiring.",
                "Days 2–3 are the toughest stretch. You are crossing it — keep going.",
            )
            elapsed < 7.days -> listOf(
                "You are building real momentum. A week is within reach.",
                "Each day makes the old habit a little weaker and you a little stronger.",
            )
            elapsed < 14.days -> listOf(
                "Past the first week most people stop doubting that change is possible. You are one of them.",
                "You have shown consistency. Keep your triggers in sight and your plan ready.",
            )
            elapsed < 30.days -> listOf(
                "Two weeks of clarity. The habit loop is visibly weaker now.",
                "You are close to a full month. Protect the progress you have built.",
            )
            else -> listOf(
                "A month or more — this is no longer a streak, it is a new way of living.",
                "You have outlasted the hardest part. Stay curious about your triggers and keep going.",
            )
        }
        return variants[seed.mod(variants.size.toLong()).toInt()]
    }

    // ---------- Notifications ----------

    fun dailyCheckIn(snapshot: CoachSnapshot, seed: Long): CoachNote {
        val name = snapshot.userName.trim()
        val title = if (name.isEmpty()) "Daily check-in" else "Daily check-in, $name"
        val focus = snapshot.badHabits.minByOrNull { it.elapsed }
        val pendingGood = snapshot.goodHabits.firstOrNull { it.todayCount < it.dailyTarget }
        val body = when {
            focus != null -> "${displayName(focus.name)}: ${durationText(focus.elapsed)} clear. ${motivation(focus.name, focus.elapsed, seed)}"
            pendingGood != null -> "Don't forget \"${pendingGood.name}\" today — ${pendingGood.todayCount} of ${pendingGood.dailyTarget} so far."
            else -> "Take a minute to notice how you are doing. Small steps count."
        }
        return CoachNote(title, body)
    }

    fun milestoneNote(habitName: String, reached: Duration): CoachNote {
        val category = knowledge.match(habitName)
        val milestone = category.milestones.lastOrNull { it.after <= reached }
        val title = "${durationText(reached)} clear: ${displayName(habitName)}"
        val body = milestone?.let { "${it.title}. ${it.detail}" }
            ?: "You reached ${durationText(reached)}. Keep going — you are proving it to yourself."
        return CoachNote(title, body)
    }

    // ---------- Chat ----------

    fun greeting(snapshot: CoachSnapshot, seed: Long): String {
        val name = snapshot.userName.trim()
        val hello = if (name.isEmpty()) "Hi there." else "Hi $name."
        val focus = snapshot.badHabits.maxByOrNull { it.elapsed }
        val status = when {
            focus != null -> " You are ${durationText(focus.elapsed)} clear on ${displayName(focus.name)}. ${motivation(focus.name, focus.elapsed, seed)}"
            snapshot.goodHabits.isNotEmpty() -> " Your good habits are waiting for today's check-in."
            else -> " Add a habit you want to leave behind and I will help you through it."
        }
        return "$hello I'm your NixUra coach.$status What would help most right now?"
    }

    fun suggestions(snapshot: CoachSnapshot): List<String> {
        val list = mutableListOf<String>()
        if (snapshot.badHabits.isNotEmpty()) {
            list += "I have a craving"
            list += "What's happening in my body?"
            list += "I slipped up"
        }
        list += "How am I doing?"
        list += "Motivate me"
        list += "Give me a reset plan"
        return list
    }

    fun reply(userText: String, snapshot: CoachSnapshot, seed: Long): String {
        val text = userText.lowercase().trim()
        val tokens = CoachText.tokens(userText)
        val focused = snapshot.focusHabitId?.let { id -> snapshot.badHabits.firstOrNull { it.id == id } }
        val habit = findMentionedHabit(text, tokens, snapshot) ?: focused ?: snapshot.badHabits.minByOrNull { it.elapsed }
        val name = snapshot.userName.trim()
        val lead = if (name.isEmpty()) "" else "$name, "
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
            hasPhrase(tokens, HOW_ARE_YOU) -> "I'm doing well, thank you for asking. More importantly, how are you doing today?"
            hasPhrase(tokens, WHO_ARE_YOU) -> capabilitiesReply()
            hasPhrase(tokens, GOODBYE) -> "Take care${if (name.isEmpty()) "" else ", $name"}. I'm here whenever you need a reset or a boost."
            focused != null -> habitOverviewReply(lead, focused)
            else -> "I can help with cravings, your progress, what is happening in your body, what happens when you stop for a while, or a reset plan. " +
                "Tell me what is on your mind, or pick one of the suggestions below."
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
            val list = names.dropLast(1).joinToString(", ") + " or " + names.last()
            return "${lead}which habit do you mean: $list? Tell me its name together with the time, " +
                "for example \"what happens to ${names.first()} after 6 hours?\""
        }

        val habitName = resolved?.name ?: ""
        val snap = resolved?.snap
        val actual = snap?.elapsed ?: Duration.ZERO
        val opener = seed.mod(3L).toInt()

        if (duration == null) {
            val upcoming = explainer.upcoming(habitName, actual, 2)
            val answer = explainer.explain(habitName, actual)
            val where = if (snap != null) " You are ${elapsedText(actual)} in." else ""
            val shown = if (snap != null) displayName(snap.name) else answer.habitLabel
            val sb = StringBuilder("${lead}here is what is coming up for $shown.$where\n\n")
            if (upcoming.isEmpty()) {
                sb.append("The main milestones on this timeline are behind you. From here it is mostly about keeping your routines and knowing your triggers.\n\n")
            } else {
                upcoming.forEachIndexed { index, m ->
                    val label = if (index == 0) "Next" else "After that"
                    sb.append("$label: ${m.title}, in about ${elapsedText(m.after - actual)}. ${m.detail}\n\n")
                }
            }
            sb.append("Tell me a time, for example \"what happens after 3 days?\", and I will show you that moment.\n\n")
            appendSafetyAndDisclaimer(sb, answer)
            return sb.toString().trimEnd()
        }

        val answer = explainer.explain(habitName, duration.duration)
        val shown = if (snap != null) displayName(snap.name) else answer.habitLabel
        val intro = when (opener) {
            0 -> "here is what typically happens around ${elapsedText(duration.duration)} in with $shown."
            1 -> "around ${elapsedText(duration.duration)} in with $shown, this is the usual picture."
            else -> "this is what many people notice about ${elapsedText(duration.duration)} into $shown."
        }
        val sb = StringBuilder("$lead$intro\n\n")
        sb.append("${answer.headline}\n${answer.whatIsHappening}\n\n")
        val before = answer.alreadyHappened.dropLast(1).takeLast(2)
        if (before.isNotEmpty()) sb.append("Earlier on the way: ${before.joinToString("; ") { it.title }}.\n\n")
        if (answer.next != null && answer.timeToNext != null) {
            sb.append("Next: ${answer.next.title}, about ${elapsedText(answer.timeToNext)} later.\n\n")
        }
        if (answer.cravingTips.isNotEmpty()) {
            sb.append("If urges show up: ${answer.cravingTips[seed.mod(answer.cravingTips.size.toLong()).toInt()]}\n\n")
        }
        if (snap != null) sb.append("You are actually ${elapsedText(snap.elapsed)} in right now.\n\n")
        appendSafetyAndDisclaimer(sb, answer, includeDisclaimer = false)
        return sb.toString().trimEnd()
    }

    private fun appendSafetyAndDisclaimer(sb: StringBuilder, answer: StopTimelineAnswer, includeDisclaimer: Boolean = true) {
        answer.safetyNote?.let { sb.append("Important: $it\n\n") }
        if (includeDisclaimer) sb.append("Timelines vary, and this is general information, not medical advice.")
    }

    private fun thanksReply(name: String, seed: Long): String {
        val who = name.ifEmpty { "friend" }
        return listOf(
            "You're welcome, $who. I'm here whenever you need a reset or a boost.",
            "Anytime, $who. Every check-in counts.",
            "Glad to help, $who. Keep going, one step at a time.",
        )[seed.mod(3L).toInt()]
    }

    private fun capabilitiesReply(): String =
        "I'm your offline NixUra coach. I can help you through a craving, explain what happens in your body and mind when you stop for a few hours or days, " +
            "review your progress, build a reset plan, or just cheer you on. Replies are generated on your device."

    private fun cravingReply(lead: String, habit: BadHabitSnapshot?, seed: Long): String {
        val category = habit?.let { knowledge.match(it.name) } ?: knowledge.generic
        val tip = category.cravingTips[seed.mod(category.cravingTips.size.toLong()).toInt()]
        val where = habit?.let { " You are ${durationText(it.elapsed)} into ${displayName(it.name)}." } ?: ""
        val safety = category.safetyNote?.let { "\n\nImportant: $it" } ?: ""
        return "${lead}that urge is real, and it is temporary. Most cravings fade in 10 minutes.$where\n\n" +
            "Try this now:\n1. Delay — set a 10-minute timer.\n2. Breathe — 5 slow breaths (in 4, out 6).\n" +
            "3. Drink a glass of water.\n4. Move — stand up and change rooms.\n\nExtra tip: $tip$safety\n\n" +
            "Tell me when the timer ends and how it feels."
    }

    private fun relapseReply(lead: String, habit: BadHabitSnapshot?, seed: Long): String {
        val record = habit?.let { " Your longest streak of ${durationText(it.longest)} still counts — it is part of your story." } ?: ""
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
            return "${lead}add a habit you want to quit and I will show how your body and mind recover over time."
        }
        val insight = insightFor(habit.name, habit.elapsed)
        val latest = insight.achieved.lastOrNull()
        val next = insight.next
        val sb = StringBuilder("${lead}here is what is happening with ${displayName(habit.name)} (${insight.category.label}), ${durationText(habit.elapsed)} in:\n\n")
        if (latest != null) {
            sb.append("Now: ${latest.title}. ${latest.detail}\n\n")
        } else {
            sb.append("Your recovery has just started — the first changes begin soon.\n\n")
        }
        if (next != null) {
            sb.append("Next: ${next.title} in about ${durationText(next.after - habit.elapsed)}.\n\n")
        }
        insight.category.safetyNote?.let { sb.append("Important: $it\n\n") }
        sb.append("This is general information, not medical advice.")
        return sb.toString()
    }

    /** Shown when the user picked a habit and sent something that matches no other topic. */
    private fun habitOverviewReply(lead: String, habit: BadHabitSnapshot): String {
        val insight = insightFor(habit.name, habit.elapsed)
        val sb = StringBuilder("${lead}let's talk about ${displayName(habit.name)} (${insight.category.label}). ")
        sb.append("You are ${durationText(habit.elapsed)} in; your longest streak is ${durationText(habit.longest)}")
        sb.append(if (habit.relapseCount == 0) " and you have no relapses recorded.\n\n" else " with ${habit.relapseCount} relapse(s) recorded.\n\n")
        insight.achieved.lastOrNull()?.let { sb.append("Where your body is now: ${it.title}. ${it.detail}\n\n") }
        insight.category.safetyNote?.let { sb.append("Important: $it\n\n") }
        sb.append("I can help with a craving, explain what is happening in your body, or build a reset plan for ${displayName(habit.name)}. What do you need?")
        return sb.toString()
    }

    private fun progressReply(snapshot: CoachSnapshot): String {
        if (snapshot.badHabits.isEmpty() && snapshot.goodHabits.isEmpty()) {
            return "Nothing to review yet. Add a habit and I will track your progress with you."
        }
        val sb = StringBuilder("Here is where you stand:\n\n")
        snapshot.badHabits.forEach {
            sb.append("• ${displayName(it.name)}: ${durationText(it.elapsed)} clear (best ${durationText(it.longest)})\n")
        }
        snapshot.goodHabits.forEach {
            sb.append("• ${it.name}: ${it.todayCount}/${it.dailyTarget} today, streak ${it.streakDays} ${plural(it.streakDays, "day", "days")}\n")
        }
        val best = snapshot.badHabits.maxByOrNull { it.elapsed }
        if (best != null) sb.append("\nYour strongest streak is ${displayName(best.name)}. Keep it protected.")
        return sb.toString().trimEnd()
    }

    private fun planReply(lead: String, habit: BadHabitSnapshot?): String {
        val category = habit?.let { knowledge.match(it.name) } ?: knowledge.generic
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
        val target = habit?.let { " around ${displayName(it.name)}" } ?: ""
        return "${lead}strong feelings are one of the most common triggers$target — and feeling them does not mean you have to act on them.\n\n" +
            "Try box breathing for 2 minutes: in 4, hold 4, out 4, hold 4. Then name the feeling out loud (\"I feel stressed\") and ask what you actually need: rest, food, company, or a break.\n\n" +
            "If these feelings are heavy or last for weeks, talking to someone you trust or a professional can make a real difference."
    }

    private fun motivationReply(lead: String, habit: BadHabitSnapshot?, seed: Long): String {
        return if (habit != null) {
            "$lead${motivation(habit.name, habit.elapsed, seed)}\n\nYou are ${durationText(habit.elapsed)} into ${displayName(habit.name)}. That is real, measurable progress."
        } else {
            "${lead}every habit begins with a decision. Add one you want to leave behind and I will cheer you on every step."
        }
    }

    private fun goodHabitsReply(snapshot: CoachSnapshot): String {
        if (snapshot.goodHabits.isEmpty()) {
            return "Good habits crowd out bad ones. Add one small thing — a glass of water, a short walk — and check in daily."
        }
        val pending = snapshot.goodHabits.filter { it.todayCount < it.dailyTarget }
        return if (pending.isEmpty()) {
            "You have completed all your good habits today. That discipline is what makes quitting stick."
        } else {
            "Still to do today:\n" + pending.joinToString("\n") { "• ${it.name} (${it.todayCount}/${it.dailyTarget})" } +
                "\n\nSmall actions repeated daily become who you are."
        }
    }

    private fun eventReply(snapshot: CoachSnapshot): String {
        val event = snapshot.nextEvent ?: return "You have no upcoming events. Add one to the Events tab and I will help you stay on track until it arrives."
        return "Your next event is \"${event.title}\" in ${durationText(event.remaining)}. Staying consistent now means you arrive feeling proud of yourself."
    }

    private fun crisisReply(): String =
        "I'm really sorry you are feeling this way, and I'm glad you told me. You deserve support right now.\n\n" +
            "Please reach out to someone you trust, or contact your local emergency number or a crisis line in your country as soon as you can. " +
            "If you are in immediate danger, call emergency services now.\n\n" +
            "I am an app with simple automated replies, so I can't give you the care a person can — but you do not have to go through this alone."

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
        )
        private val EMOTION = listOf(
            "stress", "anxi", "bored", "lonely", "sad", "angry", "upset", "overwhelm", "توتر", "ملل", "حزين", "زعلان",
        )
        private val MOTIVATION = listOf(
            "motivat", "encourag", "inspir", "give up", "struggl", "discourag", "tired", "hard", "تحفيز", "شجعني", "صعب",
        )
        private val GOOD = listOf(
            "good habit", "water", "walk", "workout", "exercis", "check-in", "discipline",
        )
        private val EVENTS = listOf("event", "countdown", "upcoming")
        private val THANKS = listOf("thanks", "thank", "thx", "شكرا", "شكراً")
        private val GREETING = listOf("hi", "hello", "hey", "hola", "مرحبا", "اهلا", "أهلا", "السلام")
    }
}

/** "3 days", "5 hours", "12 minutes" — the largest meaningful unit, used in messages. */
fun durationText(duration: Duration): String {
    val total = duration.coerceAtLeast(Duration.ZERO)
    val days = total.inWholeDays
    val hours = total.inWholeHours
    val minutes = total.inWholeMinutes
    return when {
        days >= 1 -> "$days ${plural(days.toInt(), "day", "days")}"
        hours >= 1 -> "$hours ${plural(hours.toInt(), "hour", "hours")}"
        else -> "${minutes.coerceAtLeast(0)} ${plural(minutes.toInt(), "minute", "minutes")}"
    }
}

private fun plural(count: Int, one: String, many: String) = if (count == 1) one else many

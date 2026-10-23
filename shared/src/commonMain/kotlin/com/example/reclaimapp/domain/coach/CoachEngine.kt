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

    fun categoryFor(habitName: String): HabitCategory = knowledge.match(habitName)

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
            focus != null -> "${focus.name}: ${durationText(focus.elapsed)} clear. ${motivation(focus.name, focus.elapsed, seed)}"
            pendingGood != null -> "Don't forget \"${pendingGood.name}\" today — ${pendingGood.todayCount} of ${pendingGood.dailyTarget} so far."
            else -> "Take a minute to notice how you are doing. Small steps count."
        }
        return CoachNote(title, body)
    }

    fun milestoneNote(habitName: String, reached: Duration): CoachNote {
        val category = knowledge.match(habitName)
        val milestone = category.milestones.lastOrNull { it.after <= reached }
        val title = "${durationText(reached)} clear: $habitName"
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
            focus != null -> " You are ${durationText(focus.elapsed)} clear on ${focus.name}. ${motivation(focus.name, focus.elapsed, seed)}"
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
        val words = tokenize(text)
        val habit = findMentionedHabit(text, words, snapshot) ?: snapshot.badHabits.minByOrNull { it.elapsed }
        val name = snapshot.userName.trim()
        val lead = if (name.isEmpty()) "" else "$name, "

        return when {
            isCrisis(text) -> crisisReply()
            hasStem(text, words, RELAPSE) -> relapseReply(lead, habit, seed)
            hasStem(text, words, CRAVING) -> cravingReply(lead, habit, seed)
            hasStem(text, words, BODY) -> bodyReply(lead, habit)
            hasStem(text, words, PROGRESS) -> progressReply(snapshot)
            hasStem(text, words, PLAN) -> planReply(lead, habit)
            hasStem(text, words, EMOTION) -> emotionReply(lead, habit)
            hasStem(text, words, MOTIVATION) -> motivationReply(lead, habit, seed)
            hasStem(text, words, GOOD) -> goodHabitsReply(snapshot)
            hasStem(text, words, EVENTS) -> eventReply(snapshot)
            hasWord(words, THANKS) -> "You're welcome, ${name.ifEmpty { "friend" }}. I'm here whenever you need a reset or a boost."
            hasWord(words, GREETING) -> greeting(snapshot, seed)
            else -> "I can help with cravings, your progress, what is happening in your body, or a reset plan. " +
                "Tell me what is on your mind, or pick one of the suggestions below."
        }
    }

    private fun cravingReply(lead: String, habit: BadHabitSnapshot?, seed: Long): String {
        val category = habit?.let { knowledge.match(it.name) } ?: knowledge.generic
        val tip = category.cravingTips[seed.mod(category.cravingTips.size.toLong()).toInt()]
        val where = habit?.let { " You are ${durationText(it.elapsed)} into ${it.name}." } ?: ""
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
        val sb = StringBuilder("${lead}here is what is happening with ${habit.name} (${insight.category.label}), ${durationText(habit.elapsed)} in:\n\n")
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

    private fun progressReply(snapshot: CoachSnapshot): String {
        if (snapshot.badHabits.isEmpty() && snapshot.goodHabits.isEmpty()) {
            return "Nothing to review yet. Add a habit and I will track your progress with you."
        }
        val sb = StringBuilder("Here is where you stand:\n\n")
        snapshot.badHabits.forEach {
            sb.append("• ${it.name}: ${durationText(it.elapsed)} clear (best ${durationText(it.longest)})\n")
        }
        snapshot.goodHabits.forEach {
            sb.append("• ${it.name}: ${it.todayCount}/${it.dailyTarget} today, streak ${it.streakDays} ${plural(it.streakDays, "day", "days")}\n")
        }
        val best = snapshot.badHabits.maxByOrNull { it.elapsed }
        if (best != null) sb.append("\nYour strongest streak is ${best.name}. Keep it protected.")
        return sb.toString().trimEnd()
    }

    private fun planReply(lead: String, habit: BadHabitSnapshot?): String {
        val category = habit?.let { knowledge.match(it.name) } ?: knowledge.generic
        val target = habit?.name ?: "your habit"
        return "${lead}here is a simple reset plan for $target:\n\n" +
            "1. Spot the trigger — when and where does the urge usually start?\n" +
            "2. Pick a replacement — ${category.cravingTips.first()}\n" +
            "3. Make it harder — remove cues and add friction around $target.\n" +
            "4. Tell someone you trust, or check in here daily.\n" +
            "5. Celebrate every milestone, however small.\n\n" +
            category.whyItMatters
    }

    private fun emotionReply(lead: String, habit: BadHabitSnapshot?): String {
        val target = habit?.let { " around ${it.name}" } ?: ""
        return "${lead}strong feelings are one of the most common triggers$target — and feeling them does not mean you have to act on them.\n\n" +
            "Try box breathing for 2 minutes: in 4, hold 4, out 4, hold 4. Then name the feeling out loud (\"I feel stressed\") and ask what you actually need: rest, food, company, or a break.\n\n" +
            "If these feelings are heavy or last for weeks, talking to someone you trust or a professional can make a real difference."
    }

    private fun motivationReply(lead: String, habit: BadHabitSnapshot?, seed: Long): String {
        return if (habit != null) {
            "$lead${motivation(habit.name, habit.elapsed, seed)}\n\nYou are ${durationText(habit.elapsed)} into ${habit.name}. That is real, measurable progress."
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

    private fun findMentionedHabit(text: String, words: Set<String>, snapshot: CoachSnapshot): BadHabitSnapshot? =
        snapshot.badHabits.firstOrNull { habit ->
            val n = habit.name.lowercase()
            text.contains(n) || tokenize(n).any { it.length >= 4 && words.any { w -> w.startsWith(it) || it.startsWith(w) && w.length >= 4 } }
        }

    /** True when [userText] suggests self-harm. Callers (including any future online AI) must handle this locally. */
    fun isCrisis(userText: String): Boolean {
        val text = userText.lowercase()
        return CRISIS.any { text.contains(it) }
    }

    private fun tokenize(text: String): Set<String> {
        val result = mutableSetOf<String>()
        val current = StringBuilder()
        for (ch in text) {
            if (ch.isLetterOrDigit() || ch == '\'') {
                current.append(ch)
            } else if (current.isNotEmpty()) {
                result += current.toString()
                current.clear()
            }
        }
        if (current.isNotEmpty()) result += current.toString()
        return result
    }

    private fun hasStem(text: String, words: Set<String>, stems: List<String>): Boolean = stems.any { stem ->
        if (stem.contains(' ')) text.contains(stem) else words.any { it.startsWith(stem) }
    }

    private fun hasWord(words: Set<String>, candidates: List<String>): Boolean = candidates.any { it in words }

    companion object {
        private val ONE_HOUR = Duration.parse("1h")

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

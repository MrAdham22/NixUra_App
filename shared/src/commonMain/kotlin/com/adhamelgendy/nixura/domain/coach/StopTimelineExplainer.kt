package com.adhamelgendy.nixura.domain.coach

import com.adhamelgendy.nixura.domain.locale.ContentLanguage
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** What typically happens after [elapsed] of stopping (or interrupting) a habit, built from the habit's knowledge. */
data class StopTimelineAnswer(
    val habitLabel: String,
    val elapsed: Duration,
    /** Short human summary for that moment. */
    val headline: String,
    /** Detail for the closest earlier milestone, or an "early phase" text before the first one. */
    val whatIsHappening: String,
    val alreadyHappened: List<BodyMilestone>,
    val next: BodyMilestone?,
    val timeToNext: Duration?,
    val cravingTips: List<String>,
    val safetyNote: String?,
    /** True when the habit was not recognised and the general timeline is used. */
    val isGenericProfile: Boolean,
)

/**
 * Answers "what happens if I stop for N hours / days / weeks?" for any habit name, using whichever catalog the
 * [HabitKnowledgeBase] holds (including its `generic` profile). Pure and deterministic. General wellbeing
 * information only, never medical advice; wording stays neutral so it also fits habits that are patterns to
 * interrupt (sitting, procrastination) rather than things to abstain from.
 */
class StopTimelineExplainer(
    private val knowledge: HabitKnowledgeBase,
    private val language: ContentLanguage = ContentLanguage.English,
) {
    private val ar: Boolean get() = language.isArabic

    fun explain(habitName: String, elapsed: Duration): StopTimelineAnswer {
        val category = knowledge.match(habitName)
        val generic = category.id == knowledge.generic.id
        val label = if (generic) habitName.trim().ifEmpty { if (ar) "هذه العادة" else "this habit" } else category.label
        return explain(category, label, generic, elapsed)
    }

    /** The next [count] milestones still ahead after [elapsed] (empty when the main milestones are behind). */
    fun upcoming(habitName: String, elapsed: Duration, count: Int = 2): List<BodyMilestone> =
        knowledge.match(habitName).milestones.sortedBy { it.after }.filter { it.after > elapsed }.take(count)

    internal fun explain(category: HabitCategory, label: String, generic: Boolean, elapsed: Duration): StopTimelineAnswer {
        val time = elapsed.coerceAtLeast(Duration.ZERO)
        val milestones = category.milestones.sortedBy { it.after }
        val happened = milestones.filter { it.after <= time }
        val next = milestones.firstOrNull { it.after > time }
        val timeToNext = next?.let { it.after - time }
        val latest = happened.lastOrNull()
        val first = milestones.firstOrNull()
        val position = elapsedText(time, ar)
        val headline: String
        val body = StringBuilder()
        if (ar) {
            headline = when {
                milestones.isEmpty() -> "بعد $position: لا يوجد مسار مفصّل لـ $label بعد"
                time < ONE_MINUTE -> "في بداية الطريق مع $label"
                latest == null -> "بعد نحو $position: المرحلة الأولى"
                next == null -> "بعد نحو $position: تجاوزت المحطات الرئيسية"
                else -> "بعد نحو $position: ${latest.title}"
            }
            when {
                milestones.isEmpty() -> body.append("لا يوجد مسار مفصّل لهذه العادة بعد، لذا تنطبق النصيحة العامة: تعامل مع كل رغبة على حدة.")
                latest == null -> {
                    body.append("هذه هي المرحلة الأولى. لم يبدأ شيء من المسار المعتاد بعد، والمهم الآن أن تتجاوز البداية.")
                    if (first != null) body.append(" أول ما تنتظره هو «${first.title}»، عادةً بعد نحو ${elapsedText(first.after, true)}.")
                }
                next == null -> {
                    body.append("تجاوزت آخر محطة في هذا المسار («${latest.title}»). ${latest.detail}")
                    body.append(" من هنا، يعتمد التقدّم غالبًا على الحفاظ على روتينك ومعرفة محفّزاتك.")
                }
                else -> body.append("${latest.title}. ${latest.detail}")
            }
            body.append(" تختلف المدد من شخص لآخر، وهذه معلومات عامة وليست نصيحة طبية.")
            if (generic) {
                body.append(" هذا مسار عام لأنني لم أتعرّف على العادة. جرّب اسمًا أدق (مثل «التدخين» أو «وسائل التواصل») لتفاصيل تناسبها.")
            }
        } else {
            headline = when {
                milestones.isEmpty() -> "$position in: no detailed timeline for $label yet"
                time < ONE_MINUTE -> "At the very start of $label"
                latest == null -> "About $position in: the early phase"
                next == null -> "About $position in: the main milestones are behind you"
                else -> "About $position in: ${latest.title}"
            }
            when {
                milestones.isEmpty() -> body.append("There is no detailed timeline for this habit yet, so the general advice applies: take it one urge at a time.")
                latest == null -> {
                    body.append("This is the early phase. Nothing on the usual timeline has kicked in yet; the main task now is getting through the first stretch.")
                    if (first != null) body.append(" The first thing to look for is \"${first.title}\", typically around ${elapsedText(first.after)}.")
                }
                next == null -> {
                    body.append("You are past the last milestone on this timeline (\"${latest.title}\"). ${latest.detail}")
                    body.append(" From here, progress is mostly about keeping your routines and knowing your triggers.")
                }
                else -> body.append("${latest.title}. ${latest.detail}")
            }
            body.append(" Timelines vary from person to person; this is general information, not medical advice.")
            if (generic) {
                body.append(" This is a general timeline because I did not recognise the habit. Try a more specific name (for example \"smoking\" or \"social media\") for details that fit it.")
            }
        }

        return StopTimelineAnswer(
            habitLabel = label,
            elapsed = time,
            headline = headline,
            whatIsHappening = body.toString(),
            alreadyHappened = happened,
            next = next,
            timeToNext = timeToNext,
            cravingTips = category.cravingTips,
            safetyNote = category.safetyNote,
            isGenericProfile = generic,
        )
    }

    private companion object {
        val ONE_MINUTE: Duration = 1.minutes
    }
}

/** "35 minutes", "6 hours", "1 day 12 hours", "9 days": a readable length for messages (more precise than [durationText]). */
fun elapsedText(duration: Duration, arabic: Boolean = false): String {
    val total = duration.coerceAtLeast(Duration.ZERO)
    val days = total.inWholeDays
    val hoursLeft = (total - days.days).inWholeHours
    return when {
        days in 1..6 && hoursLeft > 0 && arabic -> "${durationText(days.days, true)} و${durationText(hoursLeft.hours, true)}"
        days in 1..6 && hoursLeft > 0 -> "${durationText(days.days)} ${durationText(hoursLeft.hours)}"
        else -> durationText(total, arabic)
    }
}

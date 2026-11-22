package com.adhamelgendy.nixura.domain.notification

import com.adhamelgendy.nixura.domain.recognition.HabitTopic
import com.adhamelgendy.nixura.domain.theme.HabitTheme

/**
 * Which small icon a notification has (owner decision, D-lock-screen 4.2). Shared so iOS can mirror it.
 *
 * A good habit shows the glyph on its card: the topic worked out from its name, because the stored theme has only
 * 7 values. Water and prayer keep their own icons. An unrecognised habit and a bundle of several show the NixUra
 * mark: no notification shows the leaf (owner decision 2026-11-22, evening). The card keeps its leaf.
 */
object NotificationGlyph {

    /**
     * The topic whose card glyph to draw, or `null` for the type's own icon (drop, arch, NixUra mark, calendar, chain...).
     * Never a topic for another kind: a category icon would reveal which habit is being quit.
     */
    fun topicToDraw(kind: NotificationKind, theme: HabitTheme?, topic: HabitTopic?): HabitTopic? = when {
        kind != NotificationKind.GOOD_HABIT -> null
        theme == HabitTheme.WATER || theme == HabitTheme.PRAYER -> null
        topic == HabitTopic.OTHER_BUILD -> null
        else -> topic
    }
}

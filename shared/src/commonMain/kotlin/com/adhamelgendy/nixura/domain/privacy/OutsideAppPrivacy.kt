package com.adhamelgendy.nixura.domain.privacy

import com.adhamelgendy.nixura.domain.model.Habit
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.money.HabitCost
import com.adhamelgendy.nixura.domain.recognition.HabitTopic

/**
 * "Hide name outside the app" (owner decision 3, D-lock-screen 4.1): what may leave the app about a quit habit.
 *
 * Applied at the two exits, the notification planner and the widget mapper, so renderers, receivers, stored plans
 * and armed alarms never get the real name. Three more things give a habit away and are neutral too: the category's
 * milestone text, the category's icon and what it used to cost ("Marlboro Red"). Inside the app nothing changes.
 */
object OutsideAppPrivacy {
    /** The name to show outside the app, or `null` when the user hid it. */
    fun nameOutside(habit: Habit): String? = if (habit.hideNameOutside) null else habit.name

    /** The glyph's topic outside the app: the category, or the neutral shield when the name is hidden. */
    fun topicOutside(habit: Habit, topicOf: (String) -> HabitTopic): HabitTopic =
        nameOutside(habit)?.let(topicOf) ?: HabitTopic.OTHER_QUIT

    /** The money tracker outside the app: the same sums, without the words for what it was. */
    fun costOutside(habit: Habit): HabitCost? = if (habit.hideNameOutside) habit.cost?.copy(item = "") else habit.cost

    /** The recent-apps preview is a picture of a screen with names on it, so it is hidden while any name is. */
    fun anyNameHidden(habits: List<HabitTracking>): Boolean = habits.any { it.habit.hideNameOutside }
}

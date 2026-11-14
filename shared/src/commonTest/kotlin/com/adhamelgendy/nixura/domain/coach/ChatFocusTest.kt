package com.adhamelgendy.nixura.domain.coach

import com.adhamelgendy.nixura.domain.model.CoachMessage
import com.adhamelgendy.nixura.domain.model.CoachRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration

class ChatFocusTest {
    private fun habit(id: Long, name: String) = BadHabitSnapshot(id, name, Duration.ZERO, Duration.ZERO, 0)
    private fun msg(at: Long, role: CoachRole, text: String) = CoachMessage(at, role, text, at)
    private val habits = listOf(habit(1, "smoking"), habit(2, "Junk Food"), habit(3, "food"))

    @Test
    fun theNewestUserMessageThatNamesAHabitWins() {
        val chat = listOf(
            msg(1, CoachRole.USER, "I'm having a craving for smoking"),
            msg(2, CoachRole.COACH, "Remember your 3 days clean from smoking"),
            msg(3, CoachRole.USER, "I'm having a craving for Junk Food right now."),
        )
        assertEquals(2L, ChatFocus.inferred(chat, habits))
    }

    @Test
    fun coachMessagesDoNotCount() {
        val chat = listOf(msg(1, CoachRole.COACH, "Hi. You're 3 days clear on smoking."))
        assertNull(ChatFocus.inferred(chat, habits))
    }

    @Test
    fun theLongerNameWinsOverAShorterOneInsideIt() {
        val chat = listOf(msg(1, CoachRole.USER, "junk food again"))
        assertEquals(2L, ChatFocus.inferred(chat, habits))
    }
}

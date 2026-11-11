package com.adhamelgendy.nixura.domain.usecase

import com.adhamelgendy.nixura.domain.coach.ai.AiContext
import kotlin.test.Test
import kotlin.test.assertEquals

class OfflineVisualPickerTest {
    private val context = AiContext(
        "2026-11-11", "Wednesday", "14:05",
        quit = listOf(AiContext.Quit(41, "Smoking", 52, 300, 2)),
        build = listOf(AiContext.Build(7, "Drink water", 3, 5, 4, null, null)),
        events = emptyList(),
    )

    @Test fun picksTheNamedHabitOrTheOverview() {
        assertEquals(listOf("quit:41"), OfflineVisualPicker.pick("How am I doing with smoking?", context, null))
        assertEquals(listOf("good:7"), OfflineVisualPicker.pick("my progress on drink water", context, null))
        assertEquals(listOf("overview"), OfflineVisualPicker.pick("How am I doing?", context, null))
        assertEquals(listOf("overview", "quit:41"), OfflineVisualPicker.pick("show my progress", context, 41))
    }

    @Test fun showsNothingForOtherMessagesOrWithoutHabits() {
        assertEquals(emptyList(), OfflineVisualPicker.pick("I really want to smoke right now", context, 41))
        assertEquals(emptyList(), OfflineVisualPicker.pick("How am I doing?", context.copy(quit = emptyList(), build = emptyList()), null))
    }
}

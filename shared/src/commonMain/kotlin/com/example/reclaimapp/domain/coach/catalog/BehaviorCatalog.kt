package com.example.reclaimapp.domain.coach.catalog

import com.example.reclaimapp.domain.coach.HabitCategory

/**
 * Knowledge catalog B: behavioural, screen, mental and body-reflex habits.
 *
 * Merged into [com.example.reclaimapp.domain.coach.HabitKnowledgeBase] by id after the legacy list, so
 * `screens`, `nails`, `sleep`, `procrastination` and `spending` here replace the legacy entries.
 * The content is split into several files in this folder only to keep each one readable.
 *
 * General wellbeing information only; never medical advice. Timelines for behavioural habits are typical
 * ranges based on general habit research and clinical guidance, and vary a lot between people.
 */
object BehaviorCatalog {
    val categories: List<HabitCategory> =
        BehaviorScreens.categories +
            BehaviorSleepMind.categories +
            BehaviorBodyReflex.categories +
            BehaviorSocialMoney.categories
}

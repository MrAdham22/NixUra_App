package com.example.reclaimapp.domain.coach.catalog

import com.example.reclaimapp.domain.coach.HabitCategory

/**
 * Knowledge catalog A: substances, food and drink habits.
 *
 * Merged into [com.example.reclaimapp.domain.coach.HabitKnowledgeBase] by id. A category declared here with
 * the same id as a legacy category (nicotine, caffeine, sugar, alcohol, junkfood) overrides the legacy one.
 * Entries are split across `Substance*.kt` files in this folder purely to keep each file readable.
 */
object SubstanceCatalog {
    val categories: List<HabitCategory> =
        nicotineCategories + drinkCategories + foodCategories + drugCategories
}

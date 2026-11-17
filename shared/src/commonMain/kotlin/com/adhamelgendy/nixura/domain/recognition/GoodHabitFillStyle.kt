package com.adhamelgendy.nixura.domain.recognition

/**
 * How a good-habit card shows today's progress. [WATER] habits fill with rising water; everything else uses the plain
 * green fill; [PRAYER] habits get the gold Islamic-art card. It follows from the habit's stored
 * [com.adhamelgendy.nixura.domain.theme.HabitTheme] (`theme.fillStyle`), never from the name.
 */
enum class GoodHabitFillStyle { WATER, PRAYER, PLAIN }

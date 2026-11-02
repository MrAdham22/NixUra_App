package com.example.reclaimapp.presentation.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AshFieldTest {
    private val solid = 0xFFF39F5A.toInt()

    private fun block(width: Int, height: Int, color: Int = solid) = IntArray(width * height) { color }

    @Test fun transparentPixelsMakeNoCells() {
        assertEquals(0, AshField.fromPixels(IntArray(100), 10, 10).count)
    }

    @Test fun solidAreaIsCoveredByCellsOfItsColour() {
        val field = AshField.fromPixels(block(10, 10), 10, 10)
        assertEquals(2, field.cellPx)
        assertEquals(25, field.count)
        assertTrue(field.argb.all { it == solid })
    }

    @Test fun cellCountStaysNearTheCap() {
        val field = AshField.fromPixels(block(400, 200), 400, 200, maxCells = 1000)
        assertTrue(field.count <= 1100, "got ${field.count}")
        assertTrue(field.cellPx > 2)
    }

    @Test fun nothingMovesAtTheStartAndEverythingIsGoneAtTheEnd() {
        val field = AshField.fromPixels(block(60, 20), 60, 20)
        for (i in 0 until field.count) {
            assertEquals(0f, field.cellProgress(i, 0f))
            assertEquals(1f, field.cellProgress(i, 1f))
        }
    }

    @Test fun theLeftGoesBeforeTheRightOnAverage() {
        val field = AshField.fromPixels(block(200, 20), 200, 20)
        val left = (0 until field.count).filter { field.xs[it] < 50f }
        val right = (0 until field.count).filter { field.xs[it] >= 150f }
        val average = { cells: List<Int> -> cells.map { field.cellProgress(it, 0.4f) }.average() }
        assertTrue(average(left) > average(right))
    }

    @Test fun sameSeedGivesSameField() {
        val a = AshField.fromPixels(block(40, 20), 40, 20)
        val b = AshField.fromPixels(block(40, 20), 40, 20)
        assertEquals(a.drifts.toList(), b.drifts.toList())
    }

    @Test fun mixHitsBothEndsAndSetsAlpha() {
        val red = 0xFFFF0000.toInt()
        val blue = 0xFF0000FF.toInt()
        assertEquals(red, AshField.mixArgb(red, blue, 0f, 1f))
        assertEquals(blue, AshField.mixArgb(red, blue, 1f, 1f))
        assertEquals(0, AshField.mixArgb(red, blue, 0.5f, 0f) ushr 24)
    }
}

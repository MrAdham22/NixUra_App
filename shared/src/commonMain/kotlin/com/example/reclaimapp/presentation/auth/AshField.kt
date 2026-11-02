package com.example.reclaimapp.presentation.auth

import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The cells a rendered word breaks into when it turns to ash. Pure maths, so it can be tested without a screen.
 *
 * The word's pixels are grouped into small square cells. Each cell keeps its colour and gets its own start delay
 * (mostly left to right, with noise, so the word goes in patches), drift and rise. Arrays instead of objects because
 * every cell is read on every frame.
 */
internal class AshField private constructor(
    val count: Int,
    val cellPx: Int,
    val widthPx: Int,
    val heightPx: Int,
    val xs: FloatArray,
    val ys: FloatArray,
    val argb: IntArray,
    private val delays: FloatArray,
    val drifts: FloatArray,
    val rises: FloatArray,
    val phases: FloatArray,
) {
    /** How far cell [i] has gone at overall [progress]: 0 = still in the word, 1 = gone. Every cell is gone at progress 1. */
    fun cellProgress(i: Int, progress: Float): Float =
        ((progress - delays[i]) / CELL_LIFE).coerceIn(0f, 1f)

    companion object {
        private const val MAX_DELAY = 0.62f
        private const val CELL_LIFE = 1f - MAX_DELAY
        private const val MIN_ALPHA = 40

        /** [pixels] is ARGB, row by row. Cells are made larger when needed so there are never more than about [maxCells]. */
        fun fromPixels(pixels: IntArray, width: Int, height: Int, maxCells: Int = 2600, seed: Int = 7): AshField {
            val opaque = pixels.count { (it ushr 24) >= MIN_ALPHA }
            val cell = maxOf(2, ceil(sqrt(opaque.toFloat() / maxCells)).toInt())
            val random = Random(seed)
            val xs = ArrayList<Float>()
            val ys = ArrayList<Float>()
            val colors = ArrayList<Int>()
            var y = 0
            while (y < height) {
                var x = 0
                while (x < width) {
                    val color = strongestPixel(pixels, width, height, x, y, cell)
                    if ((color ushr 24) >= MIN_ALPHA) {
                        xs += x.toFloat()
                        ys += y.toFloat()
                        colors += color
                    }
                    x += cell
                }
                y += cell
            }
            val n = xs.size
            val span = maxOf(1, width).toFloat()
            return AshField(
                count = n,
                cellPx = cell,
                widthPx = width,
                heightPx = height,
                xs = xs.toFloatArray(),
                ys = ys.toFloatArray(),
                argb = colors.toIntArray(),
                delays = FloatArray(n) { (xs[it] / span * 0.44f + random.nextFloat() * 0.18f).coerceAtMost(MAX_DELAY) },
                drifts = FloatArray(n) { 0.35f + random.nextFloat() * 0.65f },
                rises = FloatArray(n) { 0.25f + random.nextFloat() * 0.75f },
                phases = FloatArray(n) { random.nextFloat() * 6.283f },
            )
        }

        private fun strongestPixel(pixels: IntArray, width: Int, height: Int, x0: Int, y0: Int, cell: Int): Int {
            var best = 0
            for (y in y0 until minOf(y0 + cell, height)) {
                for (x in x0 until minOf(x0 + cell, width)) {
                    val p = pixels[y * width + x]
                    if ((p ushr 24) > (best ushr 24)) best = p
                }
            }
            return best
        }

        /** Blends two opaque-channel colours and sets the alpha; plain integer maths because it runs per cell, per frame. */
        fun mixArgb(from: Int, to: Int, amount: Float, alpha: Float): Int {
            val t = amount.coerceIn(0f, 1f)
            val r = channel(from shr 16, to shr 16, t)
            val g = channel(from shr 8, to shr 8, t)
            val b = channel(from, to, t)
            val a = (alpha.coerceIn(0f, 1f) * 255f).toInt()
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        private fun channel(from: Int, to: Int, t: Float): Int {
            val a = from and 0xFF
            val b = to and 0xFF
            return (a + (b - a) * t).toInt()
        }
    }
}

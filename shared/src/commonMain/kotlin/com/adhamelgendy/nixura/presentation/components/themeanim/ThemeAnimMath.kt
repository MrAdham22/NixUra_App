package com.adhamelgendy.nixura.presentation.components.themeanim

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/**
 * Pure math behind the theme backgrounds: no Compose, so it is unit-tested in commonTest. Every particle is a
 * function of its index and the time, so nothing is allocated per frame and nothing is stored between frames.
 */
internal object ThemeAnimMath {

    private const val TWO_PI = (2 * PI).toFloat()

    /** Fractional part in 0..1, also for negative numbers. */
    fun fract(x: Float): Float = x - floor(x)

    /** A stable pseudo-random 0..1 for [i] and [salt] (golden-ratio sequence), the same on every frame. */
    fun seeded(i: Int, salt: Int = 0): Float = fract((i + 1) * 0.6180339f + salt * 0.3819660f + salt * i * 0.1234567f)

    /**
     * Height of the water surface above its rest line at [x], made of two layered sine waves moving in opposite
     * directions. [width] is the page width; [amplitude] in pixels. [tilt] (-1..1, phone roll) slopes the surface.
     */
    fun waveOffset(x: Float, width: Float, time: Float, amplitude: Float, tilt: Float = 0f): Float {
        if (width <= 0f) return 0f
        val u = x / width
        val a = sin(u * TWO_PI * 1.3f + time * 1.6f) * amplitude
        val b = sin(u * TWO_PI * 2.7f - time * 2.3f + 1.1f) * amplitude * 0.45f
        val slope = (u - 0.5f) * tilt * amplitude * 4f
        return a + b + slope
    }

    /**
     * Y of the water's rest line for a fill [level] (0 = empty, 1 = full) on a page from [top] to [bottom]. Full
     * still leaves a few pixels of [headroom] so the wavy surface stays visible at the very top.
     */
    fun surfaceY(level: Float, top: Float, bottom: Float, headroom: Float): Float {
        val l = level.coerceIn(0f, 1f)
        val fullY = top + headroom
        return bottom - (bottom - fullY) * l
    }

    /** A rising bubble: where it is (0..1 of the water column, 0 = bottom) and its sideways wobble (-1..1). */
    data class Bubble(val rise: Float, val x: Float, val radius: Float)

    /**
     * Bubble [i] at [time]: rises at its own speed and wobbles side to side. [x] is 0..1 of the width, [radius]
     * 0..1 of the maximum size. Loops forever, so the count on screen never grows.
     */
    fun bubble(i: Int, time: Float): Bubble {
        val speed = 0.06f + seeded(i, 1) * 0.08f
        val rise = fract(seeded(i, 2) + time * speed)
        val wobble = sin(time * (1.5f + seeded(i, 3) * 1.5f) + i) * 0.015f
        val x = (0.05f + seeded(i, 4) * 0.9f + wobble).coerceIn(0f, 1f)
        val radius = 0.35f + seeded(i, 5) * 0.65f
        return Bubble(rise, x, radius)
    }

    /**
     * A splash droplet thrown from where the stream hits the water: offset from the impact point in units of the
     * splash size (x sideways, y up), and how visible it is (fades as it falls back).
     */
    data class Droplet(val dx: Float, val dy: Float, val alpha: Float)

    fun droplet(i: Int, time: Float): Droplet {
        val p = fract(time * 1.7f + seeded(i, 6))
        val side = if (i % 2 == 0) 1f else -1f
        val vx = side * (0.35f + seeded(i, 7) * 0.65f)
        val vy = 1.2f + seeded(i, 8) * 0.8f
        // A small parabola: up and out, then back down.
        val dx = vx * p
        val dy = vy * p - 2.2f * p * p
        return Droplet(dx, dy, (1f - p).coerceIn(0f, 1f))
    }

    /**
     * One print of a walk, in pixels: its centre, whether it is a left foot, and the way its toes point in degrees
     * (0 = up, 180 = down): the walking direction plus a slight outward turn of each foot.
     */
    data class Footprint(val x: Float, val y: Float, val left: Boolean, val angle: Float)

    /**
     * A point on the walk's S curve, 0..1 of the page: from the top end corner beside the title, down behind the form,
     * then across the open bottom of the page to the bottom start corner. [mirrored] walks the same curve reflected
     * left to right, for a second lane that crosses the first.
     */
    fun walkPoint(s: Float, mirrored: Boolean = false): Pair<Float, Float> {
        val t = s.coerceIn(0f, 1f)
        val u = 1f - t
        val a = u * u * u
        val b = 3f * u * u * t
        val c = 3f * u * t * t
        val d = t * t * t
        val x = a * 0.85f + b * 0.30f + c * 0.85f + d * 0.10f
        val y = a * -0.04f + b * 0.20f + c * 0.95f + d * 1.04f
        return (if (mirrored) 1f - x else x) to y
    }

    /**
     * The prints of one walk along [walkPoint] on a page of [width] × [height] px starting at [top]: one every
     * [stride] px along the path, left and right feet [gait] px either side of it, toes along the walking direction
     * and turned out by [splay] degrees. Left-to-right layout; the caller mirrors it for RTL.
     */
    fun footprintTrail(
        width: Float,
        height: Float,
        top: Float,
        stride: Float,
        gait: Float,
        mirrored: Boolean = false,
        splay: Float = FOOT_SPLAY_DEG,
        samples: Int = 240,
    ): List<Footprint> {
        if (width <= 0f || height <= 0f || stride <= 0f) return emptyList()
        val xs = FloatArray(samples + 1)
        val ys = FloatArray(samples + 1)
        for (i in 0..samples) {
            val (px, py) = walkPoint(i.toFloat() / samples, mirrored)
            xs[i] = px * width
            ys[i] = top + py * height
        }
        val prints = ArrayList<Footprint>()
        var travelled = 0f
        var next = stride / 2f
        var step = 0
        for (i in 1..samples) {
            val dx = xs[i] - xs[i - 1]
            val dy = ys[i] - ys[i - 1]
            val len = kotlin.math.sqrt(dx * dx + dy * dy)
            if (len <= 0f) continue
            while (travelled + len >= next) {
                val f = (next - travelled) / len
                val cx = xs[i - 1] + dx * f
                val cy = ys[i - 1] + dy * f
                // Unit heading; the walker's left-hand side is (hy, -hx) on screen (y grows downwards).
                val hx = dx / len
                val hy = dy / len
                val left = step % 2 == 0
                val side = if (left) 1f else -1f
                val heading = kotlin.math.atan2(hx, -hy) * 180f / PI.toFloat()
                // Feet turn slightly outwards: the left foot anticlockwise, the right foot clockwise.
                val angle = heading + if (left) -splay else splay
                prints += Footprint(cx + hy * side * gait, cy - hx * side * gait, left, angle)
                step++
                next += stride
            }
            travelled += len
        }
        return prints
    }

    const val FOOT_SPLAY_DEG = 9f

    /**
     * How visible a print is [age] seconds after it was stepped: a quick press in, a short hold, then a slow fade.
     * 0 before it appears and after it faded.
     */
    fun printAlpha(age: Float): Float {
        if (age < 0f) return 0f
        val pressIn = (age / PRINT_IN_S).coerceIn(0f, 1f)
        val fade = 1f - ((age - PRINT_HOLD_S) / PRINT_FADE_S).coerceIn(0f, 1f)
        return pressIn * fade
    }

    const val PRINT_IN_S = 0.18f
    const val PRINT_HOLD_S = 2.6f
    const val PRINT_FADE_S = 2.6f

    /** An energy particle rising from the bottom: 0..1 position and its life (1 = just born, 0 = gone). */
    data class Spark(val x: Float, val y: Float, val life: Float)

    fun spark(i: Int, time: Float): Spark {
        val speed = 0.18f + seeded(i, 9) * 0.22f
        val p = fract(seeded(i, 10) + time * speed)
        val x = (seeded(i, 11) + sin(time * 2.2f + i * 1.7f) * 0.03f).coerceIn(0f, 1f)
        return Spark(x, 1f - p * 0.85f, 1f - p)
    }

    /** A page drifting down: 0..1 position, its flip (-1..1, the page turning) and its tilt in degrees. */
    data class Page(val x: Float, val y: Float, val flip: Float, val rotation: Float)

    fun page(i: Int, time: Float): Page {
        val speed = 0.035f + seeded(i, 12) * 0.03f
        val p = fract(seeded(i, 13) + time * speed)
        val x = fract(seeded(i, 14) + sin(time * 0.4f + i) * 0.04f + p * 0.12f)
        val flip = kotlin.math.cos(time * (1.2f + seeded(i, 15)) + i * 2f)
        val rotation = sin(time * 0.7f + i) * 25f
        return Page(x, -0.08f + p * 1.16f, flip, rotation)
    }

    /** A star's twinkle, 0.25..1. */
    fun twinkle(i: Int, time: Float): Float = 0.25f + 0.75f * (0.5f + 0.5f * sin(time * (0.8f + seeded(i, 16) * 1.6f) + i * 2.3f))

    /**
     * Where the fish is on its single swim across the page, 0..1 of the width (start edge to end edge), or `null`
     * before it starts or after it left. [sinceAlive] is seconds since the water came alive.
     */
    fun fishProgress(sinceAlive: Float, delay: Float = FISH_DELAY_S, duration: Float = FISH_SWIM_S): Float? {
        val p = (sinceAlive - delay) / duration
        return if (p < 0f || p > 1f) null else -0.1f + p * 1.2f
    }

    const val FISH_DELAY_S = 1.8f
    const val FISH_SWIM_S = 7f

    /** A ripple's radius (0..1 of its maximum) and its fading opacity, [age] seconds after the tap. `null` = done. */
    fun ripple(age: Float, life: Float = RIPPLE_LIFE_S): Pair<Float, Float>? {
        if (age < 0f || age > life) return null
        val p = age / life
        val eased = 1f - (1f - p) * (1f - p)
        return eased to (1f - p)
    }

    const val RIPPLE_LIFE_S = 1.4f
}

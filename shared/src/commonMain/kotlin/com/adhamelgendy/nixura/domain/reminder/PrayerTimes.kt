package com.adhamelgendy.nixura.domain.reminder

import com.adhamelgendy.nixura.domain.time.CivilCalendar
import com.adhamelgendy.nixura.domain.time.DayCalculator
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/** A place on Earth in degrees (north and east positive). Kept on the device only, never synced or sent anywhere. */
data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0) { "Invalid coordinates" }
    }
}

/** The five daily prayers, in order. [index] is the prayer card's star (1 = Fajr … 5 = Isha). */
enum class Prayer(val index: Int) {
    FAJR(1), DHUHR(2), ASR(3), MAGHRIB(4), ISHA(5);

    companion object {
        fun ofIndex(index: Int): Prayer? = entries.firstOrNull { it.index == index }
    }
}

/** When each prayer starts on one day, as UTC epoch millis. A prayer is missing when it cannot be computed there. */
data class PrayerDay(val starts: Map<Prayer, Long>) {
    operator fun get(prayer: Prayer): Long? = starts[prayer]
}

/**
 * The calculation convention. Only the Egyptian General Authority of Survey is offered (owner decision 2026-11-21:
 * the default, changeable later if asked): Fajr at 19.5° below the horizon, Isha at 17.5°, Asr when a shadow equals
 * the object's length plus its noon shadow (standard / Shafi'i).
 */
enum class PrayerMethod(val fajrAngle: Double, val ishaAngle: Double, val asrShadowFactor: Double) {
    EGYPTIAN(19.5, 17.5, 1.0),
}

/**
 * Offline prayer times from coordinates, with the standard astronomical formulas (sun position after the U.S. Naval
 * Observatory's low-precision algorithm, as used by the widely published PrayTimes method). Pure, no dependencies,
 * no network. Accurate to about a minute against published timetables (see PrayerTimeCalculatorTest).
 *
 * Above ~48° latitude Fajr / Isha may not exist in summer (the sun never goes that far down); the "angle-based"
 * adjustment then limits the night portion they may take, as the common timetables do.
 */
object PrayerTimeCalculator {

    /** Prayer start times on the local calendar day [dayIndex] (days since 1970-01-01) at [place]. */
    fun forDay(dayIndex: Long, place: GeoPoint, method: PrayerMethod = PrayerMethod.EGYPTIAN): PrayerDay {
        val date = CivilCalendar.civilFromDayIndex(dayIndex)
        val jDate = julianDay(date.year, date.month, date.day) - place.longitude / (15.0 * 24.0)
        val lat = place.latitude

        // First guesses (hours), then one refinement pass with the sun's position at those moments.
        var fajr = 5.0
        var sunrise = 6.0
        var dhuhr = 12.0
        var asr = 13.0
        var sunset = 18.0
        var isha = 18.0
        repeat(2) {
            val fajrNew = sunAngleTime(jDate, lat, method.fajrAngle, fajr / 24, ccw = true)
            val sunriseNew = sunAngleTime(jDate, lat, RISE_SET_ANGLE, sunrise / 24, ccw = true)
            val dhuhrNew = midDay(jDate, dhuhr / 24)
            val asrNew = asrTime(jDate, lat, method.asrShadowFactor, asr / 24)
            val sunsetNew = sunAngleTime(jDate, lat, RISE_SET_ANGLE, sunset / 24, ccw = false)
            val ishaNew = sunAngleTime(jDate, lat, method.ishaAngle, isha / 24, ccw = false)
            fajr = fajrNew; sunrise = sunriseNew; dhuhr = dhuhrNew; asr = asrNew; sunset = sunsetNew; isha = ishaNew
        }

        // High latitudes: when the sun does not reach the angle (NaN) or the time runs past the night's limit, use the
        // angle-based portion of the night (angle / 60 of the night), like the published timetables.
        if (!sunrise.isNaN() && !sunset.isNaN()) {
            val night = timeDiff(sunset, sunrise)
            val fajrLimit = night * method.fajrAngle / 60.0
            if (fajr.isNaN() || timeDiff(fajr, sunrise) > fajrLimit) fajr = sunrise - fajrLimit
            val ishaLimit = night * method.ishaAngle / 60.0
            if (isha.isNaN() || timeDiff(sunset, isha) > ishaLimit) isha = sunset + ishaLimit
        }

        // Local solar hours → UTC hours, rounded to the minute like printed timetables.
        val utcMidnight = dayIndex * DayCalculator.MILLIS_PER_DAY
        fun toUtcMillis(solarHours: Double): Long? {
            if (solarHours.isNaN()) return null
            val utcHours = solarHours - place.longitude / 15.0
            return utcMidnight + (utcHours * 60.0).roundToLong() * 60_000L
        }
        return PrayerDay(
            buildMap {
                toUtcMillis(fajr)?.let { put(Prayer.FAJR, it) }
                toUtcMillis(dhuhr)?.let { put(Prayer.DHUHR, it) }
                toUtcMillis(asr)?.let { put(Prayer.ASR, it) }
                toUtcMillis(sunset)?.let { put(Prayer.MAGHRIB, it) } // Maghrib = sunset in this method
                toUtcMillis(isha)?.let { put(Prayer.ISHA, it) }
            },
        )
    }

    private const val RISE_SET_ANGLE = 0.833 // refraction + the sun's radius

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /** Declination (degrees) and equation of time (hours) at julian day [jd]. */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dSin(g) + 0.020 * dSin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(dAtan2(dCos(e) * dSin(l), dCos(l)) / 15.0)
        val eqt = q / 15.0 - ra
        val decl = dAsin(dSin(e) * dSin(l))
        return decl to eqt
    }

    private fun midDay(jDate: Double, dayFraction: Double): Double {
        val eqt = sunPosition(jDate + dayFraction).second
        return fixHour(12.0 - eqt)
    }

    /** The hour the sun is [angle] degrees below the horizon, before noon ([ccw]) or after it. NaN when it never is. */
    private fun sunAngleTime(jDate: Double, lat: Double, angle: Double, dayFraction: Double, ccw: Boolean): Double {
        val decl = sunPosition(jDate + dayFraction).first
        val noon = midDay(jDate, dayFraction)
        val cosT = (-dSin(angle) - dSin(decl) * dSin(lat)) / (dCos(decl) * dCos(lat))
        if (cosT < -1.0 || cosT > 1.0) return Double.NaN
        val t = dAcos(cosT) / 15.0
        return noon + if (ccw) -t else t
    }

    private fun asrTime(jDate: Double, lat: Double, factor: Double, dayFraction: Double): Double {
        val decl = sunPosition(jDate + dayFraction).first
        val angle = -dAcot(factor + dTan(abs(lat - decl)))
        return sunAngleTime(jDate, lat, angle, dayFraction, ccw = false)
    }

    private fun timeDiff(from: Double, to: Double): Double = fixHour(to - from)

    private fun fixAngle(a: Double): Double = (a % 360.0 + 360.0) % 360.0
    private fun fixHour(h: Double): Double = (h % 24.0 + 24.0) % 24.0
    private fun rad(d: Double) = d * kotlin.math.PI / 180.0
    private fun deg(r: Double) = r * 180.0 / kotlin.math.PI
    private fun dSin(d: Double) = sin(rad(d))
    private fun dCos(d: Double) = cos(rad(d))
    private fun dTan(d: Double) = tan(rad(d))
    private fun dAsin(x: Double) = deg(asin(x))
    private fun dAcos(x: Double) = deg(acos(x))
    private fun dAtan2(y: Double, x: Double) = deg(atan2(y, x))
    private fun dAcot(x: Double) = deg(atan(1.0 / x))
}

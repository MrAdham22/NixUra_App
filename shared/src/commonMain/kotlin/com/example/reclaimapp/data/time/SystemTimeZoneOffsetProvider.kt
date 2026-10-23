package com.example.reclaimapp.data.time

import com.example.reclaimapp.domain.time.TimeZoneOffsetProvider

/** Device time zone UTC offset (including daylight saving) at [epochMillis], in milliseconds. */
expect fun systemUtcOffsetMillis(epochMillis: Long): Long

class SystemTimeZoneOffsetProvider : TimeZoneOffsetProvider {
    override fun offsetMillisAt(epochMillis: Long): Long = systemUtcOffsetMillis(epochMillis)
}

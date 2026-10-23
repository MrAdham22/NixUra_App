package com.example.reclaimapp.data.time

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone
import platform.Foundation.secondsFromGMTForDate

actual fun systemUtcOffsetMillis(epochMillis: Long): Long {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    return NSTimeZone.localTimeZone.secondsFromGMTForDate(date) * 1000L
}

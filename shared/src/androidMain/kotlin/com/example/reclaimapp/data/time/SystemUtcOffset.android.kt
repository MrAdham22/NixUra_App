package com.example.reclaimapp.data.time

import java.util.TimeZone

actual fun systemUtcOffsetMillis(epochMillis: Long): Long = TimeZone.getDefault().getOffset(epochMillis).toLong()

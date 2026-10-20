package com.example.reclaimapp.domain.clock

interface Clock {
    fun nowEpochMillis(): Long
}

class SystemClock : Clock {
    override fun nowEpochMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
}

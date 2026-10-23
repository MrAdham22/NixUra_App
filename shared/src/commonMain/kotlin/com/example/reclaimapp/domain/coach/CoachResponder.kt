package com.example.reclaimapp.domain.coach

import com.example.reclaimapp.domain.clock.Clock

/**
 * Produces the coach's reply. The app ships [RuleBasedCoachResponder] (offline, free, private).
 * To upgrade to a real AI later, add another implementation (see docs/AI_COACH_UPGRADE.md) and
 * bind it in `AppModules.kt` — nothing else in the app changes.
 */
interface CoachResponder {
    suspend fun respond(userText: String, snapshot: CoachSnapshot): String
    suspend fun greeting(snapshot: CoachSnapshot): String
}

class RuleBasedCoachResponder(
    private val engine: CoachEngine,
    private val clock: Clock,
) : CoachResponder {
    override suspend fun respond(userText: String, snapshot: CoachSnapshot): String =
        engine.reply(userText, snapshot, seed())

    override suspend fun greeting(snapshot: CoachSnapshot): String = engine.greeting(snapshot, seed())

    private fun seed(): Long = clock.nowEpochMillis() / 60_000L
}

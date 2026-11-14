package com.adhamelgendy.nixura.domain.coach.ai

import com.adhamelgendy.nixura.domain.model.EventCategory

/** Something the AI understood the user wants to add. Nothing is created until the user confirms it. */
sealed interface CoachAction {
    data class Quit(val name: String) : CoachAction

    /** [daysPerWeek] null = every day, [perDay] times a day; 1..6 = a weekly habit. */
    data class Build(val name: String, val perDay: Int, val daysPerWeek: Int?) : CoachAction

    /** A local date and, when [hour] is set, a local time. The AI leaves [category] at Event; the user can change it. */
    data class Event(
        val title: String,
        val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int?,
        val minute: Int?,
        val category: EventCategory = EventCategory.EVENT,
    ) : CoachAction
}

/** What is left of today's free AI uses for this account. */
data class AiQuota(val chatLeft: Int, val voiceLeft: Int, val chatLimit: Int, val voiceLimit: Int)

/** The user's data as the AI sees it. It never holds a name, an email or an account id. */
data class AiContext(
    val today: String,
    val weekday: String,
    val time: String,
    val quit: List<Quit>,
    val build: List<Build>,
    val events: List<Event>,
) {
    data class Quit(val habitId: Long, val name: String, val streakHours: Long, val bestHours: Long, val relapses: Int)
    data class Build(val habitId: Long, val name: String, val today: Int, val target: Int, val streak: Int, val weekDone: Int?, val weekGoal: Int?)
    data class Event(val title: String, val date: String, val time: String?)
}

enum class AiTurnRole { USER, COACH }

data class AiTurn(val role: AiTurnRole, val text: String)

data class AiChatReply(
    val text: String,
    /** Progress cards to draw: "quit:<habitId>", "good:<habitId>" or "overview". */
    val visuals: List<String>,
    val actions: List<CoachAction>,
    val quota: AiQuota,
)

data class AiVoiceReply(val transcript: String, val actions: List<CoachAction>, val quota: AiQuota)

sealed interface AiResult<out T> {
    data class Ok<T>(val value: T) : AiResult<T>

    /** Today's free uses are gone. */
    data object LimitReached : AiResult<Nothing>

    /** No AI service could answer right now (all out of quota, or not set up). */
    data object Unavailable : AiResult<Nothing>
    data object Offline : AiResult<Nothing>
    data object SignedOut : AiResult<Nothing>
}

/** A recorded voice note, ready to send. */
class VoiceClip(val base64: String, val mime: String)

/** NixUra's AI server (the Cloudflare Worker). Keys for the AI services live there, never in the app. */
interface AiCoachGateway {
    suspend fun chat(turns: List<AiTurn>, context: AiContext, focusHabitId: Long?): AiResult<AiChatReply>
    suspend fun voice(clip: VoiceClip, context: AiContext): AiResult<AiVoiceReply>
    suspend fun quota(): AiResult<AiQuota>
}

/** Whether this user gets the real AI or the offline coach. */
enum class AiMode {
    /** Not signed in: offline coach, AI needs an account. */
    Guest,

    /** Signed in, not asked yet: show the consent question first. */
    NeedsConsent,
    On,

    /** Signed in but said no: offline coach. Can be changed in Settings. */
    Declined,
}

/** Microphone recording, per platform. */
interface VoiceRecorder {
    val isSupported: Boolean
    fun hasPermission(): Boolean

    /** Shows the system's microphone question. The answer is read with [hasPermission] afterwards. */
    fun requestPermission()

    /** Starts recording; false when the microphone could not be opened. */
    fun start(): Boolean

    /** Loudness right now, 0..1, for the animation. */
    fun level(): Float

    /** Stops and returns the clip; null when nothing usable was recorded. */
    fun stop(): VoiceClip?
    fun cancel()
}

object UnsupportedVoiceRecorder : VoiceRecorder {
    override val isSupported = false
    override fun hasPermission() = false
    override fun requestPermission() = Unit
    override fun start() = false
    override fun level() = 0f
    override fun stop(): VoiceClip? = null
    override fun cancel() = Unit
}

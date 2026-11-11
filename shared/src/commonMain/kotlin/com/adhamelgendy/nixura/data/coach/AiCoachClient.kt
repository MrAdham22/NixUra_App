package com.adhamelgendy.nixura.data.coach

import com.adhamelgendy.nixura.data.auth.AuthorizedApi
import com.adhamelgendy.nixura.data.auth.TransportResponse
import com.adhamelgendy.nixura.domain.coach.ai.AiChatReply
import com.adhamelgendy.nixura.domain.coach.ai.AiCoachGateway
import com.adhamelgendy.nixura.domain.coach.ai.AiContext
import com.adhamelgendy.nixura.domain.coach.ai.AiQuota
import com.adhamelgendy.nixura.domain.coach.ai.AiResult
import com.adhamelgendy.nixura.domain.coach.ai.AiTurn
import com.adhamelgendy.nixura.domain.coach.ai.AiTurnRole
import com.adhamelgendy.nixura.domain.coach.ai.AiVoiceReply
import com.adhamelgendy.nixura.domain.coach.ai.CoachAction
import com.adhamelgendy.nixura.domain.coach.ai.VoiceClip
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class AiQuitDto(val name: String, val streakHours: Long, val bestHours: Long, val relapses: Int)

@Serializable
internal data class AiBuildDto(val name: String, val today: Int, val target: Int, val streak: Int, val weekDone: Int? = null, val weekGoal: Int? = null)

@Serializable
internal data class AiEventDto(val title: String, val date: String, val time: String? = null)

@Serializable
internal data class AiContextDto(
    val today: String,
    val weekday: String,
    val time: String,
    val quit: List<AiQuitDto>,
    val build: List<AiBuildDto>,
    val events: List<AiEventDto>,
)

@Serializable
internal data class AiMessageDto(val role: String, val text: String)

@Serializable
internal data class AiChatRequest(val messages: List<AiMessageDto>, val context: AiContextDto, val focus: String? = null)

@Serializable
internal data class AiVoiceRequest(val audio: String, val mime: String, val context: AiContextDto)

@Serializable
internal data class AiActionDto(
    val type: String = "",
    val name: String = "",
    val title: String = "",
    val perDay: Int = 1,
    val daysPerWeek: Int? = null,
    val date: String = "",
    val time: String? = null,
)

@Serializable
internal data class AiRemainingDto(val chat: Int = 0, val voice: Int = 0, val chatLimit: Int = 0, val voiceLimit: Int = 0)

@Serializable
internal data class AiChatResponse(
    val reply: String = "",
    val show: List<String> = emptyList(),
    val actions: List<AiActionDto> = emptyList(),
    val remaining: AiRemainingDto = AiRemainingDto(),
)

@Serializable
internal data class AiVoiceResponse(val transcript: String = "", val actions: List<AiActionDto> = emptyList(), val remaining: AiRemainingDto = AiRemainingDto())

@Serializable
internal data class AiQuotaResponse(val remaining: AiRemainingDto = AiRemainingDto(), val available: Boolean = false)

@Serializable
internal data class AiErrorBody(val error: AiErrorDto = AiErrorDto())

@Serializable
internal data class AiErrorDto(val code: String = "")

/** Talks to the AI routes of NixUra's Worker with the user's Firebase sign-in. */
internal class AiCoachClient(private val api: AuthorizedApi) : AiCoachGateway {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    override suspend fun chat(turns: List<AiTurn>, context: AiContext, focusHabitId: Long?): AiResult<AiChatReply> {
        val focus = context.quit.indexOfFirst { it.habitId == focusHabitId }.takeIf { it >= 0 }?.let { "q${it + 1}" }
        val body = AiChatRequest(
            messages = turns.map { AiMessageDto(if (it.role == AiTurnRole.USER) "user" else "assistant", it.text) },
            context = context.toDto(),
            focus = focus,
        )
        return read(api.call("POST", "/v1/coach/chat", json.encodeToString(AiChatRequest.serializer(), body))) { text ->
            val r = json.decodeFromString(AiChatResponse.serializer(), text)
            require(r.reply.isNotBlank())
            AiChatReply(r.reply, r.show.mapNotNull { visualKey(it, context) }, r.actions.mapNotNull(::toAction), r.remaining.toQuota())
        }
    }

    override suspend fun voice(clip: VoiceClip, context: AiContext): AiResult<AiVoiceReply> {
        val body = AiVoiceRequest(clip.base64, clip.mime, context.toDto())
        return read(api.call("POST", "/v1/coach/voice", json.encodeToString(AiVoiceRequest.serializer(), body))) { text ->
            val r = json.decodeFromString(AiVoiceResponse.serializer(), text)
            AiVoiceReply(r.transcript, r.actions.mapNotNull(::toAction), r.remaining.toQuota())
        }
    }

    override suspend fun quota(): AiResult<AiQuota> = read(api.call("GET", "/v1/coach/quota", null)) { text ->
        val r = json.decodeFromString(AiQuotaResponse.serializer(), text)
        require(r.available)
        r.remaining.toQuota()
    }

    private fun <T> read(r: TransportResponse, parse: (String) -> T): AiResult<T> = when {
        r.isNetworkFailure -> AiResult.Offline
        r.status == 401 -> AiResult.SignedOut
        r.status == 429 && errorCode(r.body) == "daily_limit" -> AiResult.LimitReached
        r.status !in 200..299 -> AiResult.Unavailable
        else -> runCatching { AiResult.Ok(parse(r.body)) }.getOrElse { AiResult.Unavailable }
    }

    private fun errorCode(body: String): String =
        runCatching { json.decodeFromString(AiErrorBody.serializer(), body).error.code }.getOrDefault("")

    internal companion object {
        /** The server names habits by position ("q1", "b2"); the app turns that back into its own ids. */
        fun visualKey(ref: String, context: AiContext): String? {
            if (ref == "overview") return ref
            val index = ref.drop(1).toIntOrNull()?.minus(1) ?: return null
            return when (ref.firstOrNull()) {
                'q' -> context.quit.getOrNull(index)?.let { "quit:${it.habitId}" }
                'b' -> context.build.getOrNull(index)?.let { "good:${it.habitId}" }
                else -> null
            }
        }

        fun toAction(a: AiActionDto): CoachAction? = when (a.type) {
            "quit" -> a.name.trim().takeIf { it.isNotEmpty() }?.let { CoachAction.Quit(it.take(60)) }
            "build" -> a.name.trim().takeIf { it.isNotEmpty() }?.let {
                val week = a.daysPerWeek?.takeIf { d -> d in 1..6 }
                CoachAction.Build(it.take(80), if (week != null) 1 else a.perDay.coerceIn(1, 99), week)
            }
            "event" -> {
                val d = a.date.split('-').mapNotNull { it.toIntOrNull() }
                val t = a.time?.split(':')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 && it[0] in 0..23 && it[1] in 0..59 }
                val title = a.title.trim()
                if (title.isEmpty() || d.size != 3 || d[1] !in 1..12 || d[2] !in 1..31) null
                else CoachAction.Event(title.take(80), d[0], d[1], d[2], t?.get(0), t?.get(1))
            }
            else -> null
        }

        private fun AiRemainingDto.toQuota() = AiQuota(chat, voice, chatLimit, voiceLimit)

        private fun AiContext.toDto() = AiContextDto(
            today, weekday, time,
            quit.map { AiQuitDto(it.name, it.streakHours, it.bestHours, it.relapses) },
            build.map { AiBuildDto(it.name, it.today, it.target, it.streak, it.weekDone, it.weekGoal) },
            events.map { AiEventDto(it.title, it.date, it.time) },
        )
    }
}

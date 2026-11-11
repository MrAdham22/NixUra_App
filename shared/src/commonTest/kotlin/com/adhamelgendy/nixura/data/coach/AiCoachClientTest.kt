package com.adhamelgendy.nixura.data.coach

import com.adhamelgendy.nixura.data.auth.ApiTransport
import com.adhamelgendy.nixura.data.auth.AuthorizedApi
import com.adhamelgendy.nixura.data.auth.FakeFirebase
import com.adhamelgendy.nixura.data.auth.FirebaseAuthApi
import com.adhamelgendy.nixura.data.auth.FirebaseConfig
import com.adhamelgendy.nixura.data.auth.FirebaseSession
import com.adhamelgendy.nixura.data.auth.FixedClock
import com.adhamelgendy.nixura.data.auth.InMemorySecureStore
import com.adhamelgendy.nixura.data.auth.SessionManagerImpl
import com.adhamelgendy.nixura.data.auth.TransportResponse
import com.adhamelgendy.nixura.data.auth.storedSession
import com.adhamelgendy.nixura.domain.coach.ai.AiContext
import com.adhamelgendy.nixura.domain.coach.ai.AiResult
import com.adhamelgendy.nixura.domain.coach.ai.AiTurn
import com.adhamelgendy.nixura.domain.coach.ai.AiTurnRole
import com.adhamelgendy.nixura.domain.coach.ai.CoachAction
import com.adhamelgendy.nixura.domain.coach.ai.VoiceClip
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiCoachClientTest {
    private val clock = FixedClock()
    private val context = AiContext(
        today = "2026-11-11", weekday = "Wednesday", time = "14:05",
        quit = listOf(AiContext.Quit(41, "Smoking", 52, 300, 2), AiContext.Quit(42, "Sugar", 5, 5, 0)),
        build = listOf(AiContext.Build(7, "Water", 3, 5, 4, null, null), AiContext.Build(9, "Gym", 0, 1, 2, 1, 3)),
        events = listOf(AiContext.Event("Exam", "2026-11-14", "09:00")),
    )

    private class Recorder(private val answer: (String) -> TransportResponse) : ApiTransport {
        val bodies = mutableListOf<String>()
        val paths = mutableListOf<String>()
        override suspend fun execute(method: String, path: String, body: String?, bearer: String?): TransportResponse {
            paths += "$method $path"
            bodies += body.orEmpty()
            return answer(path)
        }
    }

    private suspend fun client(transport: ApiTransport): AiCoachClient {
        val firebase = FakeFirebase { _, _ -> com.adhamelgendy.nixura.data.auth.fbError("INVALID_REFRESH_TOKEN") }
        val sessions = SessionManagerImpl(InMemorySecureStore()).also { it.signIn(storedSession(expiresAt = clock.now + 3_600_000)) }
        return AiCoachClient(AuthorizedApi(transport, FirebaseSession(FirebaseAuthApi(FirebaseConfig("k"), firebase), sessions, clock)))
    }

    @Test fun chatSendsHabitDataButNothingPersonalAndMapsRefsBackToIds() = runTest {
        val t = Recorder {
            TransportResponse(
                200,
                """{"reply":"52 hours clean.","show":["q2","b1","overview","q9","x"],"actions":[{"type":"build","name":"Read","perDay":2}],"provider":"groq","remaining":{"chat":7,"voice":2,"chatLimit":10,"voiceLimit":2}}""",
            )
        }
        val turns = listOf(AiTurn(AiTurnRole.USER, "hi"), AiTurn(AiTurnRole.COACH, "hello"), AiTurn(AiTurnRole.USER, "how am I doing?"))
        val reply = assertIs<AiResult.Ok<*>>(client(t).chat(turns, context, focusHabitId = 42)).value as com.adhamelgendy.nixura.domain.coach.ai.AiChatReply
        assertEquals("52 hours clean.", reply.text)
        assertEquals(listOf("quit:42", "good:7", "overview"), reply.visuals)
        assertEquals(listOf<CoachAction>(CoachAction.Build("Read", 2, null)), reply.actions)
        assertEquals(7, reply.quota.chatLeft)
        assertEquals(listOf("POST /v1/coach/chat"), t.paths)
        val sent = t.bodies.single()
        assertTrue(""""focus":"q2"""" in sent)
        assertTrue(""""role":"assistant"""" in sent && """"name":"Smoking"""" in sent && """"weekGoal":3""" in sent)
        // The stored session belongs to "Ann" (ann@example.com, user id u1): none of it may leave the phone.
        assertFalse("Ann" in sent || "ann@example.com" in sent || "\"u1\"" in sent || "habitId" in sent)
    }

    @Test fun serverAnswersBecomeClearResults() = runTest {
        suspend fun chat(response: TransportResponse) = client(Recorder { response }).chat(listOf(AiTurn(AiTurnRole.USER, "hi")), context, null)
        assertEquals(AiResult.Offline, chat(TransportResponse(0)))
        assertEquals(AiResult.LimitReached, chat(TransportResponse(429, """{"error":{"code":"daily_limit","message":"x"}}""")))
        assertEquals(AiResult.Unavailable, chat(TransportResponse(429, """{"error":{"code":"rate_limited","message":"x"}}""")))
        assertEquals(AiResult.Unavailable, chat(TransportResponse(503, """{"error":{"code":"ai_unavailable"}}""")))
        assertEquals(AiResult.Unavailable, chat(TransportResponse(200, "not json")))
        assertEquals(AiResult.Unavailable, chat(TransportResponse(200, """{"reply":"  "}""")))
        assertEquals(AiResult.SignedOut, chat(TransportResponse(401)))
    }

    @Test fun voiceNoteBecomesThingsToAdd() = runTest {
        val t = Recorder {
            TransportResponse(
                200,
                """{"transcript":"stop smoking, water 5 a day, meeting Monday 7pm","actions":[
                    {"type":"quit","name":"Smoking"},{"type":"build","name":"Drink water","perDay":5,"daysPerWeek":null},
                    {"type":"event","title":"Meeting","date":"2026-11-16","time":"19:00"}],"remaining":{"chat":10,"voice":1,"chatLimit":10,"voiceLimit":2}}""",
            )
        }
        val reply = (assertIs<AiResult.Ok<*>>(client(t).voice(VoiceClip("QUJD", "audio/mp4"), context)).value as com.adhamelgendy.nixura.domain.coach.ai.AiVoiceReply)
        assertEquals(
            listOf(CoachAction.Quit("Smoking"), CoachAction.Build("Drink water", 5, null), CoachAction.Event("Meeting", 2026, 11, 16, 19, 0)),
            reply.actions,
        )
        assertEquals(1, reply.quota.voiceLeft)
        assertTrue(""""audio":"QUJD"""" in t.bodies.single())
    }

    @Test fun badActionsFromTheServerAreDropped() {
        fun action(type: String, name: String = "", title: String = "", perDay: Int = 1, week: Int? = null, date: String = "", time: String? = null) =
            AiCoachClient.toAction(AiActionDto(type, name, title, perDay, week, date, time))
        assertNull(action("quit", name = "  "))
        assertNull(action("delete_everything", name = "x"))
        assertNull(action("event", title = "Trip", date = "soon"))
        assertNull(action("event", title = "Trip", date = "2026-13-01"))
        assertEquals(CoachAction.Event("Trip", 2026, 12, 1, null, null), action("event", title = "Trip", date = "2026-12-01", time = "25:00"))
        assertEquals(CoachAction.Build("Gym", 1, 3), action("build", name = "Gym", perDay = 4, week = 3))
        assertEquals(CoachAction.Build("Run", 99, null), action("build", name = "Run", perDay = 500, week = 7))
        assertNull(AiCoachClient.visualKey("b3", context))
        assertNull(AiCoachClient.visualKey("q0", context))
    }
}

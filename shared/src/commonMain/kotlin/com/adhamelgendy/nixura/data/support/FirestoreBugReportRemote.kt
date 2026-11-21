package com.adhamelgendy.nixura.data.support

import com.adhamelgendy.nixura.data.auth.AuthorizedApi
import com.adhamelgendy.nixura.domain.support.BugRemoteResult
import com.adhamelgendy.nixura.domain.support.BugReportDraft
import com.adhamelgendy.nixura.domain.support.BugReportRemote
import com.adhamelgendy.nixura.domain.support.SupportEnvironment
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.random.Random

/**
 * Saves a bug report as `bugReports/{random id}` through Firestore's REST `commit` (shared code, no Firebase SDK).
 * `currentDocument.exists = false` makes it a create, the only write `firestore.rules` allows there; the server sets
 * `createdAt`. The owner reads reports in the Firebase console; the app can never read one back.
 */
internal class FirestoreBugReportRemote(
    private val api: AuthorizedApi,
    private val projectId: String,
    private val random: Random = Random.Default,
) : BugReportRemote {

    override suspend fun submit(draft: BugReportDraft, environment: SupportEnvironment, uid: String): BugRemoteResult {
        if (projectId.isBlank()) return BugRemoteResult.Failed
        val database = "projects/$projectId/databases/(default)/documents"
        val body = commitBody("$database/$COLLECTION/${newId()}", draft, environment, uid)
        val response = api.call("POST", "/$database:commit", Json.encodeToString(JsonElement.serializer(), body))
        return when {
            response.isNetworkFailure -> BugRemoteResult.Offline
            response.status == 200 -> BugRemoteResult.Saved
            // Quota used up / Firestore busy: the same as offline for the user (the email still goes).
            response.status == 429 || response.status == 503 || response.status == 504 -> BugRemoteResult.Offline
            else -> BugRemoteResult.Failed
        }
    }

    /** 20 letters and digits, like Firestore's own auto ids. */
    private fun newId(): String = buildString { repeat(20) { append(ID_CHARS[random.nextInt(ID_CHARS.length)]) } }

    internal companion object {
        const val COLLECTION = "bugReports"
        private const val ID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        fun commitBody(name: String, draft: BugReportDraft, env: SupportEnvironment, uid: String): JsonObject {
            val fields = linkedMapOf<String, JsonElement>(
                "uid" to str(uid),
                "category" to str(draft.category.wireName),
                "message" to str(draft.message),
                "steps" to str(draft.steps),
                "appVersion" to str(env.appVersion),
                "platform" to str(env.platform),
                "osVersion" to str(env.osVersion),
                "deviceModel" to str(env.deviceModel),
                "language" to str(env.language),
            )
            val write = JsonObject(
                mapOf(
                    "update" to JsonObject(mapOf("name" to JsonPrimitive(name), "fields" to JsonObject(fields))),
                    "currentDocument" to JsonObject(mapOf("exists" to JsonPrimitive(false))),
                    "updateTransforms" to JsonArray(
                        listOf(JsonObject(mapOf("fieldPath" to JsonPrimitive("createdAt"), "setToServerValue" to JsonPrimitive("REQUEST_TIME")))),
                    ),
                ),
            )
            return JsonObject(mapOf("writes" to JsonArray(listOf(write))))
        }

        private fun str(value: String) = JsonObject(mapOf("stringValue" to JsonPrimitive(value)))
    }
}

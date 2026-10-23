# Upgrading the AI Coach from offline rules to a real AI

Status today: the coach is **offline and rule-based** (`domain/coach/CoachEngine.kt` +
`HabitKnowledgeBase.kt`). It is free, private and works without internet. It is bound in
`di/AppModules.kt` as `CoachResponder -> RuleBasedCoachResponder`. A real AI only needs a second
implementation of that one interface — no screen, view model or database change.

## Which AI, and is there a free option?

| Option | Cost | Notes |
|---|---|---|
| **Google Gemini API** (key from Google AI Studio) | Free tier available | Easiest start. Free-tier rate limits are small and change often; Google's free tier terms may allow your prompts to be used to improve their products. Check the current limits/terms before launch. |
| **Gemini through Firebase AI Logic** | Free Spark tier available, paid beyond it | Best fit for a published mobile app: no API key inside the app, and App Check can restrict calls to your genuine app. |
| Other providers with free tiers (Groq, OpenRouter free models, …) | Free tiers vary | Fine for prototypes; limits and model availability change. |

Recommendation: start with **Gemini**, reached **through Firebase AI Logic or a tiny proxy server**,
never with a key compiled into the app.

Verify the current free-tier numbers yourself:
- API keys / limits: https://aistudio.google.com/apikey and https://ai.google.dev/gemini-api/docs/rate-limits
- Firebase AI Logic: https://firebase.google.com/docs/ai-logic

## Getting a Gemini API key (for a proxy or for testing)

1. Sign in at https://aistudio.google.com/apikey with a Google account.
2. Click **Create API key** (create or pick a Google Cloud project when asked).
3. Copy the key. Treat it like a password — do not commit it to git.

## Why not put the key in the app?

Anything inside an APK/IPA can be extracted. Someone could copy your key and burn your quota or
bill. So the key must live on a server (or be managed by Firebase with App Check).

## Recommended architecture

```
App (AiCoachResponder) --HTTPS--> your proxy (holds the key, rate-limits) --> Gemini API
                                   └─ or Firebase AI Logic + App Check (no proxy needed)
```

Free proxy options: a Cloudflare Worker, a Firebase Cloud Function, or a small Vercel/Render service.
The proxy should: store the key as a secret, accept only `{ messages, snapshot }`, enforce a per-device
daily limit, and return `{ reply }`.

## Steps in this codebase

1. **Add an HTTP client** (e.g. Ktor client: `ktor-client-core`, `ktor-client-okhttp` for Android,
   `ktor-client-darwin` for iOS, `kotlinx-serialization-json`). Add versions in `gradle/libs.versions.toml`.
2. **Create** `data/coach/AiCoachResponder.kt` implementing `CoachResponder`:

```kotlin
class AiCoachResponder(
    private val api: CoachApi,                  // thin Ktor wrapper around your proxy
    private val fallback: CoachResponder,       // RuleBasedCoachResponder
    private val engine: CoachEngine,
) : CoachResponder {

    override suspend fun respond(userText: String, snapshot: CoachSnapshot): String {
        // 1. Safety first — keep crisis handling LOCAL, never depend on the network for it.
        engine.reply(userText, snapshot, seed = 0).takeIf { engine.isCrisis(userText) }?.let { return it }
        // 2. Ask the AI, fall back to the offline coach on any failure.
        return runCatching { api.chat(userText, snapshot.toPromptContext()) }
            .getOrElse { fallback.respond(userText, snapshot) }
    }

    override suspend fun greeting(snapshot: CoachSnapshot) = fallback.greeting(snapshot)
}
```
   (Make `isCrisis` in `CoachEngine` public — it is currently private.)
3. **Write the system prompt** on the proxy: supportive habit coach; never give medical diagnoses or
   dosing; for alcohol/drug withdrawal advise seeing a doctor; for self-harm talk, respond with care and
   point to local emergency services; keep answers under ~120 words; use the snapshot (habit names,
   streak lengths, good-habit progress, next event).
4. **Bind it** in `di/AppModules.kt`:
   `single<CoachResponder> { AiCoachResponder(get(), RuleBasedCoachResponder(get(), get()), get()) }`
5. **Add a consent switch** in Settings ("Use online AI — your messages and habit names are sent to a
   server") and only bind/use `AiCoachResponder` when it is on. Update the privacy policy and the
   Play Store *Data safety* form accordingly. Today the app collects and uploads nothing.
6. **Android manifest**: add `<uses-permission android:name="android.permission.INTERNET" />`.
7. **Test**: offline mode (must fall back cleanly), slow network (typing indicator already exists),
   rate-limit errors, and crisis messages.

## Localisation note

The offline knowledge base (`HabitKnowledgeBase`, `CoachEngine`) is English with a few Arabic keywords
for habit matching. A real multilingual AI makes Arabic replies possible without translating that
content by hand.

package com.example.reclaimapp.domain.recognition

import com.example.reclaimapp.domain.coach.HabitKnowledgeBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Table-driven recognition tests. They run against the built-in catalog only (no Substance/Behavior catalogs),
 * so the expected ids stay stable when those catalogs grow.
 */
class HabitRecognizerTest {

    private val knowledge = HabitKnowledgeBase(null, emptyList(), emptyList())
    private val recognizer = HabitRecognizer(knowledge)

    /** input -> expected category id (null = must not match any quit category). */
    private val categoryCases: List<Pair<String, String?>> = listOf(
        "smoking" to "nicotine",
        "Smoking" to "nicotine",
        "SMOKING" to "nicotine",
        "smokng" to "nicotine",
        "smoikng" to "nicotine",
        "smokin" to "nicotine",
        "somking" to "nicotine",
        "smookiiing" to "nicotine",
        "smkng" to "nicotine",
        "smoke" to "nicotine",
        "smoker" to "nicotine",
        "i smoke too much" to "nicotine",
        "cigarettes" to "nicotine",
        "cigarets" to "nicotine",
        "cigrt" to "nicotine",
        "cigarette" to "nicotine",
        "cigar" to "nicotine",
        "vaping" to "nicotine",
        "vapng" to "nicotine",
        "vape" to "nicotine",
        "juul" to "nicotine",
        "nicotine" to "nicotine",
        "nicotin" to "nicotine",
        "tobacco" to "nicotine",
        "tobaco" to "nicotine",
        "shisha" to "nicotine",
        "shesha" to "nicotine",
        "hookah" to "nicotine",
        "hooka" to "nicotine",
        "quit smoking" to "nicotine",
        "stop smoking cigarettes" to "nicotine",
        "I want to stop smoking cigarettes" to "nicotine",
        "cigarettes smoking" to "nicotine",
        "no smoking" to "nicotine",
        "I want to stop smokng" to "nicotine",
        "trying to quit smoking" to "nicotine",
        "cut down on smoking" to "nicotine",
        "Smoking!!!" to "nicotine",
        "  smoking  " to "nicotine",
        "smoking 20 a day" to "nicotine",
        "smoking 😤" to "nicotine",
        "تدخين" to "nicotine",
        "التدخين" to "nicotine",
        "سجائر" to "nicotine",
        "السجائر" to "nicotine",
        "سجاير" to "nicotine",
        "السجاير" to "nicotine",
        "سيجارة" to "nicotine",
        "سيجاره" to "nicotine",
        "شيشة" to "nicotine",
        "شيشه" to "nicotine",
        "فيب" to "nicotine",
        "بطلت تدخين" to "nicotine",
        "عايز ابطل سجاير" to "nicotine",
        "اقلع عن التدخين" to "nicotine",
        "سجايير" to "nicotine",
        "تدخيين" to "nicotine",
        "smoking سجاير" to "nicotine",
        "شيشة shisha" to "nicotine",
        "التَّدخين" to "nicotine",
        "coffee" to "caffeine",
        "cofee" to "caffeine",
        "coffe" to "caffeine",
        "cofffee" to "caffeine",
        "coooffee" to "caffeine",
        "coffee addiction" to "caffeine",
        "caffeine" to "caffeine",
        "cafeine" to "caffeine",
        "energy drink" to "caffeine",
        "energy drinks" to "caffeine",
        "drink energy" to "caffeine",
        "red bull" to "caffeine",
        "espresso" to "caffeine",
        "expresso" to "caffeine",
        "drink too much coffee" to "caffeine",
        "less coffee" to "caffeine",
        "قهوة" to "caffeine",
        "القهوة" to "caffeine",
        "كافيين" to "caffeine",
        "نسكافيه" to "caffeine",
        "بطلت القهوة" to "caffeine",
        "sugar" to "sugar",
        "sugr" to "sugar",
        "suger" to "sugar",
        "sugary" to "sugar",
        "sweets" to "sugar",
        "sweet" to "sugar",
        "soda" to "sugar",
        "sodas" to "sugar",
        "candy" to "sugar",
        "chocolate" to "sugar",
        "choclate" to "sugar",
        "chocolat" to "sugar",
        "dessert" to "sugar",
        "desserts" to "sugar",
        "cola" to "sugar",
        "soft drink" to "sugar",
        "soft drinks" to "sugar",
        "Sugary drinks" to "sugar",
        "less sugar" to "sugar",
        "quit sweets" to "sugar",
        "drinking soda" to "sugar",
        "سكر" to "sugar",
        "السكر" to "sugar",
        "حلويات" to "sugar",
        "الحلويات" to "sugar",
        "شوكولاتة" to "sugar",
        "شوكولاته" to "sugar",
        "مشروبات غازية" to "sugar",
        "مشروبات غازيه" to "sugar",
        "alcohol" to "alcohol",
        "alchol" to "alcohol",
        "acohol" to "alcohol",
        "alcohal" to "alcohol",
        "beer" to "alcohol",
        "wine" to "alcohol",
        "vodka" to "alcohol",
        "whisky" to "alcohol",
        "whiskey" to "alcohol",
        "liquor" to "alcohol",
        "booze" to "alcohol",
        "drinking" to "alcohol",
        "drinking alcohol" to "alcohol",
        "stop drinking alcohol" to "alcohol",
        "كحول" to "alcohol",
        "الكحول" to "alcohol",
        "خمر" to "alcohol",
        "الخمر" to "alcohol",
        "scrolling" to "screens",
        "scroll" to "screens",
        "scroling" to "screens",
        "social media" to "screens",
        "media social" to "screens",
        "social midia" to "screens",
        "tiktok" to "screens",
        "instagram" to "screens",
        "instagrm" to "screens",
        "facebook" to "screens",
        "facebok" to "screens",
        "youtube" to "screens",
        "youtub" to "screens",
        "netflix" to "screens",
        "snapchat" to "screens",
        "reels" to "screens",
        "gaming" to "screens",
        "games" to "screens",
        "gamng" to "screens",
        "phone" to "screens",
        "phone addiction" to "screens",
        "screen time" to "screens",
        "doomscrolling" to "screens",
        "Late night scrolling" to "screens",
        "تصفح" to "screens",
        "تيك توك" to "screens",
        "فيسبوك" to "screens",
        "انستجرام" to "screens",
        "إنستجرام" to "screens",
        "موبايل" to "screens",
        "الموبايل" to "screens",
        "ألعاب" to "screens",
        "العاب" to "screens",
        "junk food" to "junkfood",
        "junk" to "junkfood",
        "fast food" to "junkfood",
        "burger" to "junkfood",
        "burgers" to "junkfood",
        "fries" to "junkfood",
        "french fries" to "junkfood",
        "chips" to "junkfood",
        "pizza" to "junkfood",
        "snacking" to "junkfood",
        "snacks" to "junkfood",
        "overeating" to "junkfood",
        "midnight snack" to "junkfood",
        "takeaway" to "junkfood",
        "fried food" to "junkfood",
        "وجبات سريعة" to "junkfood",
        "وجبة سريعة" to "junkfood",
        "تسالي" to "junkfood",
        "أكل" to "junkfood",
        "nail biting" to "nails",
        "nails" to "nails",
        "nail" to "nails",
        "bite nails" to "nails",
        "biting nails" to "nails",
        "nial biting" to "nails",
        "skin picking" to "nails",
        "hair pulling" to "nails",
        "اظافر" to "nails",
        "أظافر" to "nails",
        "قرض الاظافر" to "nails",
        "sleep late" to "sleep",
        "sleeping late" to "sleep",
        "staying up late" to "sleep",
        "stay up" to "sleep",
        "oversleep" to "sleep",
        "oversleeping" to "sleep",
        "all-nighter" to "sleep",
        "wake up late" to "sleep",
        "late night" to "sleep",
        "سهر" to "sleep",
        "السهر" to "sleep",
        "نوم" to "sleep",
        "procrastination" to "procrastination",
        "procrastinating" to "procrastination",
        "procrastnation" to "procrastination",
        "procrastinate" to "procrastination",
        "putting things off" to "procrastination",
        "laziness" to "procrastination",
        "lazy" to "procrastination",
        "تسويف" to "procrastination",
        "كسل" to "procrastination",
        "gambling" to "spending",
        "gamblng" to "spending",
        "gamble" to "spending",
        "betting" to "spending",
        "bettng" to "spending",
        "casino" to "spending",
        "lottery" to "spending",
        "online shopping" to "spending",
        "shopping" to "spending",
        "impulse buy" to "spending",
        "spending" to "spending",
        "spending money" to "spending",
        "قمار" to "spending",
        "القمار" to "spending",
        "مراهنات" to "spending",
        "تسوق" to "spending",
        "التسوق" to "spending",
        "drink water" to null,
        "drinking water" to null,
        "sleep early" to null,
        "sweet potato" to null,
        "sweet potatoes" to null,
        "game plan" to null,
        "phone call" to null,
        "reading books" to null,
        "read a book" to null,
        "running" to null,
        "walking" to null,
        "go to the gym" to null,
        "study" to null,
        "family" to null,
        "work" to null,
        "project" to null,
        "music" to null,
        "friend" to null,
        "good morning" to null,
        "ready" to null,
        "mailing" to null,
        "same" to null,
        "gate" to null,
        "bitten" to null,
        "nailed it" to null,
        "pray" to null,
        "play" to null,
        "sleepy" to null,
        "sweetheart" to null,
        "Skipping breakfast" to null,
        "  " to null,
        "" to null,
        "Be better between meals" to null,
        "xyzzy" to null,
        "123" to null,
        "😀😀" to null,
        "اشرب ماء" to null,
        "شرب الماء" to null,
        "meditation" to null,
        "yoga" to null,
        "eat vegetables" to null,
        "take vitamins" to null,
        "wake up early" to null,
        "brush teeth" to null,
    )

    /** input -> expected polarity. */
    private val polarityCases: List<Pair<String, HabitPolarity>> = listOf(
        "smoking" to HabitPolarity.BAD,
        "quit smoking" to HabitPolarity.BAD,
        "no smoking" to HabitPolarity.BAD,
        "stop drinking alcohol" to HabitPolarity.BAD,
        "drink alcohol" to HabitPolarity.BAD,
        "drink soda" to HabitPolarity.BAD,
        "eat junk food" to HabitPolarity.BAD,
        "eat junk" to HabitPolarity.BAD,
        "sleep late" to HabitPolarity.BAD,
        "oversleep" to HabitPolarity.BAD,
        "wake up late" to HabitPolarity.BAD,
        "scrolling" to HabitPolarity.BAD,
        "reduce sugar" to HabitPolarity.BAD,
        "less coffee" to HabitPolarity.BAD,
        "cut down on sugar" to HabitPolarity.BAD,
        "avoid fast food" to HabitPolarity.BAD,
        "give up smoking" to HabitPolarity.BAD,
        "بطلت تدخين" to HabitPolarity.BAD,
        "عايز ابطل سجاير" to HabitPolarity.BAD,
        "اقلع عن التدخين" to HabitPolarity.BAD,
        "سهر" to HabitPolarity.BAD,
        "قمار" to HabitPolarity.BAD,
        "stop procrastinating" to HabitPolarity.BAD,
        "I want to quit vaping" to HabitPolarity.BAD,
        "drinking soda" to HabitPolarity.BAD,
        "smokng" to HabitPolarity.BAD,
        "cigrt" to HabitPolarity.BAD,
        "nail biting" to HabitPolarity.BAD,
        "gambling" to HabitPolarity.BAD,
        "اقلع عن القهوة" to HabitPolarity.BAD,
        "drink water" to HabitPolarity.GOOD,
        "drinking water" to HabitPolarity.GOOD,
        "drink more water" to HabitPolarity.GOOD,
        "sleep early" to HabitPolarity.GOOD,
        "wake up early" to HabitPolarity.GOOD,
        "eat vegetables" to HabitPolarity.GOOD,
        "eat healthy" to HabitPolarity.GOOD,
        "workout" to HabitPolarity.GOOD,
        "exercise daily" to HabitPolarity.GOOD,
        "go to the gym" to HabitPolarity.GOOD,
        "daily walk" to HabitPolarity.GOOD,
        "walk 10000 steps" to HabitPolarity.GOOD,
        "running" to HabitPolarity.GOOD,
        "read books" to HabitPolarity.GOOD,
        "reading" to HabitPolarity.GOOD,
        "meditate" to HabitPolarity.GOOD,
        "meditation" to HabitPolarity.GOOD,
        "pray" to HabitPolarity.GOOD,
        "prayers" to HabitPolarity.GOOD,
        "journal" to HabitPolarity.GOOD,
        "study" to HabitPolarity.GOOD,
        "take vitamins" to HabitPolarity.GOOD,
        "brush teeth" to HabitPolarity.GOOD,
        "floss" to HabitPolarity.GOOD,
        "call family" to HabitPolarity.GOOD,
        "gratitude" to HabitPolarity.GOOD,
        "stretch" to HabitPolarity.GOOD,
        "yoga" to HabitPolarity.GOOD,
        "اشرب ماء" to HabitPolarity.GOOD,
        "شرب الماء" to HabitPolarity.GOOD,
        "رياضة" to HabitPolarity.GOOD,
        "قراءة" to HabitPolarity.GOOD,
        "صلاة" to HabitPolarity.GOOD,
        "مشي" to HabitPolarity.GOOD,
        "تمارين" to HabitPolarity.GOOD,
        "النوم بدري" to HabitPolarity.GOOD,
        "اقرا كتاب" to HabitPolarity.GOOD,
        "start walking" to HabitPolarity.GOOD,
        "build a reading habit" to HabitPolarity.GOOD,
        "don't skip workout" to HabitPolarity.GOOD,
        "dont miss prayers" to HabitPolarity.GOOD,
        "never skip gym" to HabitPolarity.GOOD,
        "stop skipping breakfast" to HabitPolarity.GOOD,
        "drnk watr" to HabitPolarity.GOOD,
        "excersize" to HabitPolarity.GOOD,
        "meditaton" to HabitPolarity.GOOD,
        "wokrout" to HabitPolarity.GOOD,
        "sweet potato" to HabitPolarity.UNKNOWN,
        "game plan" to HabitPolarity.UNKNOWN,
        "skipping breakfast" to HabitPolarity.UNKNOWN,
        "skip workout" to HabitPolarity.UNKNOWN,
        "quit my job" to HabitPolarity.UNKNOWN,
        "xyzzy" to HabitPolarity.UNKNOWN,
        "" to HabitPolarity.UNKNOWN,
        "drink water instead of soda" to HabitPolarity.UNKNOWN,
        "mailing" to HabitPolarity.UNKNOWN,
        "ready" to HabitPolarity.UNKNOWN,
        "family" to HabitPolarity.UNKNOWN,
        "hello world" to HabitPolarity.UNKNOWN,
    )

    @Test
    fun categoryTable_typosWrappersOrderArabicAndFalsePositives() {
        val failures = categoryCases.filter { (input, expected) ->
            recognizer.recognize(input).category?.id != expected
        }.map { (input, expected) -> "'$input' expected $expected but got ${recognizer.recognize(input).category?.id}" }
        assertTrue(failures.isEmpty(), failures.joinToString("; "))
    }

    @Test
    fun polarityTable_badGoodAndUnknown() {
        val failures = polarityCases.filter { (input, expected) ->
            recognizer.recognize(input).polarity != expected
        }.map { (input, expected) -> "'$input' expected $expected but got ${recognizer.recognize(input).polarity}" }
        assertTrue(failures.isEmpty(), failures.joinToString("; "))
    }

    @Test
    fun tableSizesStayAboveTheRequiredMinimum() {
        assertTrue(categoryCases.size + polarityCases.size >= 150, "need at least 150 table-driven cases")
    }

    @Test
    fun ambiguousVerbsAreResolvedByTheirObject() {
        assertEquals(HabitPolarity.GOOD, recognizer.recognize("drink water").polarity)
        assertEquals(HabitPolarity.BAD, recognizer.recognize("drink alcohol").polarity)
        assertEquals(HabitPolarity.BAD, recognizer.recognize("drink soda").polarity)
        assertEquals(HabitPolarity.GOOD, recognizer.recognize("sleep early").polarity)
        assertEquals(HabitPolarity.BAD, recognizer.recognize("sleep late").polarity)
        assertEquals(HabitPolarity.GOOD, recognizer.recognize("eat vegetables").polarity)
        assertEquals(HabitPolarity.BAD, recognizer.recognize("eat junk").polarity)
        assertEquals(HabitPolarity.BAD, recognizer.recognize("no smoking").polarity)
        assertEquals(HabitPolarity.GOOD, recognizer.recognize("don't skip workout").polarity)
    }

    @Test
    fun conflictingEvidenceIsUnknownNotGuessed() {
        val r = recognizer.recognize("drink water instead of soda")
        assertEquals(HabitPolarity.UNKNOWN, r.polarity)
    }

    @Test
    fun unrelatedTextHasNoCategoryAndUnknownPolarity() {
        listOf("sweet potato", "game plan", "hello world", "xyzzy", "", "   ").forEach { text ->
            val r = recognizer.recognize(text)
            assertNull(r.category, text)
            assertEquals(HabitPolarity.UNKNOWN, r.polarity, text)
            assertEquals(0f, r.categoryConfidence, text)
        }
    }

    @Test
    fun exactMatchesAreMoreConfidentThanTypos() {
        val exact = recognizer.recognize("smoking")
        val typo = recognizer.recognize("smokng")
        assertEquals("nicotine", exact.category?.id)
        assertEquals("nicotine", typo.category?.id)
        assertTrue(exact.categoryConfidence > typo.categoryConfidence)
        assertTrue(typo.categoryConfidence >= 0.5f)
        assertTrue(exact.categoryConfidence <= 1f)
    }

    @Test
    fun normalizedTextDropsIntentWrappers() {
        assertEquals("smoking cigarettes", recognizer.recognize("I want to stop smoking cigarettes").normalizedText)
        assertEquals("walking", recognizer.recognize("Start walking daily!").normalizedText)
        assertEquals("quit", recognizer.recognize("quit").normalizedText)
    }

    @Test
    fun suggestionsFixTypedTypos() {
        val cases = listOf(
            Triple("smokng", "smoking", "smoking"),
            Triple("I want to stop smokng", "smoking", "I want to stop smoking"),
            Triple("cofee", "coffee", "coffee"),
            Triple("alchol", "alcohol", "alcohol"),
            Triple("instagrm", "instagram", "instagram"),
            Triple("procrastnation", "procrastination", "procrastination"),
            Triple("tobaco", "tobacco", "tobacco"),
            Triple("gamblng", "gambling", "gambling"),
            Triple("sugr", "sugar", "sugar"),
            Triple("youtub", "youtube", "youtube"),
            Triple("wokrout", "workout", "workout"),
            Triple("meditaton", "meditation", "meditation"),
        )
        for ((input, word, fixed) in cases) {
            val r = recognizer.recognize(input)
            assertEquals(word, r.suggestedCorrection, input)
            assertEquals(fixed, r.correctedText, input)
        }
    }

    @Test
    fun correctlySpelledTextGetsNoSuggestion() {
        listOf("smoking", "coffee", "scrolling", "water", "hello world", "quit smoking", "تدخين").forEach {
            assertNull(recognizer.recognize(it).suggestedCorrection, it)
            assertNull(recognizer.recognize(it).correctedText, it)
        }
    }

    @Test
    fun matchDetailedReportsKeywordAndTypedWord() {
        val m = assertNotNull(knowledge.matchDetailed("smokng"))
        assertEquals("nicotine", m.category.id)
        assertEquals("smokng", m.typedWord)
        assertTrue(!m.exact)
        assertNull(knowledge.matchDetailed("hello world"))
        assertNull(knowledge.matchDetailed("   "))
    }

    @Test
    fun knowledgeBaseMatchStillFallsBackToGeneric() {
        assertEquals("generic", knowledge.match("hello world").id)
        assertEquals("generic", knowledge.match("").id)
        assertEquals("nicotine", knowledge.match("smokng").id)
    }

    @Test
    fun handoffReturnsTheNameOnlyOnce() {
        val handoff = HabitNameHandoff()
        assertNull(handoff.take())
        handoff.put("drink water")
        assertEquals("drink water", handoff.take())
        assertNull(handoff.take())
    }

    @Test
    fun recognitionIsFastEnoughForEveryKeystroke() {
        val text = "i want to stop smokng cigaretes"
        recognizer.recognize(text) // warm up lazy indexes
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        repeat(1000) { recognizer.recognize(text) }
        val elapsed = start.elapsedNow()
        assertTrue(elapsed.inWholeMilliseconds < 1000, "1000 recognitions took $elapsed")
    }
}

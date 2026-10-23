package com.example.reclaimapp.domain.coach

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

data class BodyMilestone(
    val after: Duration,
    val title: String,
    val detail: String,
)

data class HabitCategory(
    val id: String,
    val label: String,
    val keywords: List<String>,
    val whyItMatters: String,
    val milestones: List<BodyMilestone>,
    val cravingTips: List<String>,
    /** Shown prominently when stopping could be risky or when professional help is advisable. */
    val safetyNote: String? = null,
)

/**
 * Offline, rule-based "understanding" of a habit. The user's habit name is matched against keywords
 * (English + common Arabic spellings) to pick a category with a recovery timeline and craving tips.
 * General wellbeing information only — never medical advice.
 */
class HabitKnowledgeBase {

    val categories: List<HabitCategory> = listOf(
        HabitCategory(
            id = "nicotine",
            label = "Nicotine",
            keywords = listOf("smok", "cigar", "vape", "vaping", "nicotine", "tobacco", "shisha", "hookah", "juul", "تدخين", "سجائر", "سيجارة", "سجاير", "شيشة", "فيب"),
            whyItMatters = "Nicotine ties cravings to routines such as coffee, stress and breaks. Each craving usually fades within a few minutes.",
            milestones = listOf(
                BodyMilestone(20.minutes, "Heart rate starts to settle", "Within about 20 minutes your pulse and blood pressure begin moving back toward normal."),
                BodyMilestone(12.hours, "Carbon monoxide clears", "Carbon monoxide in your blood drops, so your blood can carry more oxygen again."),
                BodyMilestone(24.hours, "Lungs begin to clean house", "Your lungs start clearing mucus and debris. Strong cravings now are a sign your body is adjusting, not failing."),
                BodyMilestone(72.hours, "Nicotine leaves your body", "Most nicotine is gone. Withdrawal tends to peak around now, then eases."),
                BodyMilestone(14.days, "Circulation improves", "Blood flow improves, and walking or climbing stairs often feels easier."),
                BodyMilestone(30.days, "Coughing and breathlessness ease", "Many people notice less coughing, easier breathing and more energy in the first months."),
                BodyMilestone(90.days, "Lung function can improve", "Lung function can rise noticeably within the first three months."),
                BodyMilestone(365.days, "Heart risk is roughly halved", "After a year smoke-free, the extra risk of coronary heart disease is about half that of a smoker."),
            ),
            cravingTips = listOf(
                "Delay 10 minutes. Cravings rise and fall like a wave — you only have to outlast one.",
                "Take 5 slow breaths: in for 4, out for 6.",
                "Drink a glass of cold water or chew sugar-free gum.",
                "Change your scene: stand up, step outside, wash your hands.",
            ),
        ),
        HabitCategory(
            id = "caffeine",
            label = "Caffeine",
            keywords = listOf("caffeine", "coffee", "energy drink", "espresso", "red bull", "كافيين", "قهوة", "نسكافيه"),
            whyItMatters = "Caffeine borrows energy from later in the day. Stepping back lets your natural energy rhythm return.",
            milestones = listOf(
                BodyMilestone(12.hours, "Withdrawal can begin", "Some people feel tired or get a mild headache 12–24 hours after the last cup. It passes."),
                BodyMilestone(48.hours, "Peak withdrawal", "Symptoms usually peak around days 2–3, then start to fade."),
                BodyMilestone(7.days, "Symptoms fade", "Headaches and fog usually clear after about a week."),
                BodyMilestone(14.days, "A new energy baseline", "Energy now comes from sleep, food and movement rather than caffeine."),
                BodyMilestone(30.days, "Sleep and calm improve", "Many people sleep more deeply and feel steadier without caffeine."),
            ),
            cravingTips = listOf(
                "Drink water first — mild dehydration feels like tiredness.",
                "Take a 5-minute walk in daylight.",
                "Try a warm herbal tea so your hands and routine still get their ritual.",
            ),
        ),
        HabitCategory(
            id = "sugar",
            label = "Sugar",
            keywords = listOf("sugar", "sweet", "soda", "soft drink", "candy", "dessert", "chocolate", "cola", "sugary", "سكر", "حلويات", "مشروبات غازية", "شوكولاتة"),
            whyItMatters = "Sugar spikes and crashes drive more cravings. A few days without it can break that loop.",
            milestones = listOf(
                BodyMilestone(24.hours, "Blood sugar swings smooth out", "Without sugar spikes, energy dips between meals start to flatten."),
                BodyMilestone(72.hours, "Cravings peak, then ease", "Day 3 is often the hardest. After that cravings usually soften."),
                BodyMilestone(7.days, "Taste starts to reset", "Your taste buds begin adapting to less sweetness."),
                BodyMilestone(14.days, "Natural foods taste sweeter", "Fruit and plain foods taste noticeably better."),
                BodyMilestone(30.days, "Steadier energy", "Many people report steadier energy, better focus and fewer afternoon crashes."),
            ),
            cravingTips = listOf(
                "Eat protein or fruit first — it steadies blood sugar.",
                "Brush your teeth; sweet cravings often drop right away.",
                "Wait 10 minutes and drink a glass of water before deciding.",
            ),
        ),
        HabitCategory(
            id = "alcohol",
            label = "Alcohol",
            keywords = listOf("alcohol", "drinking", "beer", "wine", "liquor", "booze", "vodka", "whisky", "whiskey", "كحول", "خمر", "شرب"),
            whyItMatters = "Alcohol disrupts sleep, mood and hydration. Cutting it out gives your body room to recover.",
            milestones = listOf(
                BodyMilestone(12.hours, "Alcohol has left your system", "For most people alcohol is fully processed within about 12 hours."),
                BodyMilestone(24.hours, "Hydration begins to recover", "Your body rebalances fluids and blood sugar after a day without alcohol."),
                BodyMilestone(72.hours, "Sleep starts to rebalance", "Alcohol fragments sleep. Without it, deeper sleep stages begin to return."),
                BodyMilestone(7.days, "Energy and focus lift", "Many people feel clearer in the mornings and less anxious after a week."),
                BodyMilestone(14.days, "Mood and digestion settle", "Your stomach lining calms and mood swings tend to even out."),
                BodyMilestone(30.days, "Liver fat can start to fall", "Studies suggest a month without alcohol can reduce liver fat and improve blood pressure in regular drinkers."),
                BodyMilestone(90.days, "Sleep, skin and mood keep improving", "Benefits keep building — many people report better skin, steadier mood and deeper sleep."),
            ),
            cravingTips = listOf(
                "Pour a tall glass of something cold and fizzy — the ritual matters as much as the drink.",
                "Tell someone you trust that you are having a hard moment.",
                "Leave the setting where you usually drink, even for 20 minutes.",
                "Eat something: hunger and thirst often feel like cravings.",
            ),
            safetyNote = "If you drink heavily or every day, do not stop suddenly on your own — withdrawal can be dangerous. Please speak to a doctor first.",
        ),
        HabitCategory(
            id = "screens",
            label = "Screen time",
            keywords = listOf("scroll", "social media", "tiktok", "instagram", "facebook", "twitter", "snapchat", "youtube", "reels", "shorts", "phone", "screen", "game", "gaming", "netflix", "doomscroll", "تصفح", "سوشيال", "تيك توك", "فيسبوك", "انستجرام", "إنستجرام", "موبايل", "ألعاب", "العاب"),
            whyItMatters = "Feeds and games are designed to be hard to put down. Each break from them rebuilds attention and sleep.",
            milestones = listOf(
                BodyMilestone(24.hours, "Less restless checking", "The urge to check your phone stops feeling automatic after the first day."),
                BodyMilestone(72.hours, "Focus starts to return", "Your attention stretches — reading and conversations hold you for longer."),
                BodyMilestone(7.days, "Sleep and mood improve", "Especially if you cut late-night scrolling, falling asleep gets easier."),
                BodyMilestone(14.days, "New defaults form", "Reaching for a book, a walk or a friend begins to replace the old reflex."),
                BodyMilestone(30.days, "The loop loosens", "The habit loop is much weaker. Triggers still appear, but they have less pull."),
            ),
            cravingTips = listOf(
                "Put the phone in another room for 15 minutes.",
                "Do something with your hands: stretch, tidy, doodle.",
                "Open a timer app first — decide how long, then decide again.",
                "Log out of the app that tempts you most.",
            ),
        ),
        HabitCategory(
            id = "junkfood",
            label = "Junk food & snacking",
            keywords = listOf("junk", "fast food", "fries", "burger", "chips", "fried", "snack", "overeat", "midnight", "pizza", "takeaway", "وجبات سريعة", "وجبة سريعة", "تسالي", "أكل"),
            whyItMatters = "Highly processed food is built to be craved. Resetting your routine resets what you crave.",
            milestones = listOf(
                BodyMilestone(24.hours, "Cravings start resetting", "Your first full day gives your appetite a chance to settle."),
                BodyMilestone(72.hours, "Energy dips ease", "Heavy meals often cause crashes; lighter eating steadies the day."),
                BodyMilestone(7.days, "Digestion feels lighter", "Many people feel less bloated and sleep better after a week."),
                BodyMilestone(14.days, "Taste preferences shift", "Whole foods start to taste more satisfying."),
                BodyMilestone(30.days, "Energy and mood stabilise", "Steadier energy and mood are common after a month."),
            ),
            cravingTips = listOf(
                "Drink water and wait 10 minutes — thirst and boredom disguise themselves as hunger.",
                "Keep a ready-to-eat healthy snack within reach.",
                "Brush your teeth after dinner to close the kitchen.",
            ),
        ),
        HabitCategory(
            id = "nails",
            label = "Nail biting & picking",
            keywords = listOf("nail", "bite", "biting", "picking", "skin picking", "hair pulling", "اظافر", "أظافر", "قرض"),
            whyItMatters = "Biting and picking are usually automatic responses to stress or boredom. Awareness is half the battle.",
            milestones = listOf(
                BodyMilestone(24.hours, "Awareness grows", "You start catching the urge before your hand reaches your mouth."),
                BodyMilestone(72.hours, "Skin begins to heal", "Small cuts and rough edges around your nails start to close."),
                BodyMilestone(7.days, "Nails get a head start", "Nails grow around 3 mm a month — a week of rest is visible progress."),
                BodyMilestone(14.days, "Your hands feel different", "Healthier edges make biting less tempting."),
                BodyMilestone(30.days, "Visible growth", "A month of rest usually shows clearly in your nails."),
            ),
            cravingTips = listOf(
                "Keep your hands busy: a fidget toy, stress ball or lotion.",
                "Trim and file your nails so there is nothing to pick at.",
                "Notice what you were feeling right before the urge.",
            ),
        ),
        HabitCategory(
            id = "sleep",
            label = "Late nights",
            keywords = listOf("stay up", "staying up", "sleep late", "oversleep", "all-nighter", "wake up late", "late night", "سهر", "نوم"),
            whyItMatters = "Your body clock loves routine. Even small shifts earlier add up to better mood and energy.",
            milestones = listOf(
                BodyMilestone(24.hours, "Your rhythm starts resetting", "One consistent night gives your body clock a clear signal."),
                BodyMilestone(72.hours, "Falling asleep gets easier", "Your body begins expecting sleep at the new time."),
                BodyMilestone(7.days, "Daytime energy improves", "Many people feel more alert and less irritable after a week."),
                BodyMilestone(14.days, "Mood steadies", "Regular sleep supports mood, memory and focus."),
                BodyMilestone(30.days, "A reliable routine", "Your sleep schedule now runs mostly on its own."),
            ),
            cravingTips = listOf(
                "Dim the lights and put screens away 30 minutes before bed.",
                "Write tomorrow's top 3 tasks so your mind can switch off.",
                "Keep your phone charging outside the bed.",
            ),
        ),
        HabitCategory(
            id = "procrastination",
            label = "Procrastination",
            keywords = listOf("procrastinat", "putting off", "delay", "lazy", "laziness", "تسويف", "كسل"),
            whyItMatters = "Procrastination is about managing feelings, not time. Starting small is the fastest way through it.",
            milestones = listOf(
                BodyMilestone(24.hours, "You started once", "One task begun today proves you can begin."),
                BodyMilestone(72.hours, "Momentum builds", "Finishing small tasks makes the next one easier."),
                BodyMilestone(7.days, "Less dread, more flow", "Starting stops feeling like a battle."),
                BodyMilestone(14.days, "Confidence grows", "You trust yourself to follow through."),
                BodyMilestone(30.days, "A new default", "Beginning tasks right away becomes your normal."),
            ),
            cravingTips = listOf(
                "Work for just 5 minutes — you may keep going.",
                "Write the very first physical step of the task.",
                "Put your phone out of reach before you start.",
            ),
        ),
        HabitCategory(
            id = "spending",
            label = "Spending & gambling",
            keywords = listOf("gambl", "betting", "casino", "spending", "shopping", "impulse buy", "lottery", "قمار", "مراهنات", "تسوق"),
            whyItMatters = "Urges to spend or bet feel urgent but pass. Each urge you ride out protects your money and peace of mind.",
            milestones = listOf(
                BodyMilestone(24.hours, "Urges peak and pass", "Each urge you outlast weakens the next one."),
                BodyMilestone(72.hours, "Clearer head", "Stress around money and regret begins to ease."),
                BodyMilestone(7.days, "Breathing room", "A week of saved money and time is real progress."),
                BodyMilestone(30.days, "New priorities", "A month in, you are rebuilding trust with yourself."),
            ),
            cravingTips = listOf(
                "Wait 24 hours before any purchase or bet.",
                "Remove saved cards and delete the app or site from your phone.",
                "Tell someone you trust about the urge right now.",
            ),
            safetyNote = "If gambling or spending feels out of control, consider talking to a counsellor or a local support service. You do not have to do this alone.",
        ),
    )

    val generic: HabitCategory = HabitCategory(
        id = "generic",
        label = "Habit",
        keywords = emptyList(),
        whyItMatters = "Every habit is a loop of cue, routine and reward. Each day you break the loop, it gets weaker.",
        milestones = listOf(
            BodyMilestone(24.hours, "Your first full day", "You proved you can do 24 hours. That is the hardest first step."),
            BodyMilestone(72.hours, "Cravings usually peak", "Days 2–3 are often the toughest. If you are here, you are doing it."),
            BodyMilestone(7.days, "A week of proof", "Seven days shows your brain a new pattern."),
            BodyMilestone(14.days, "The loop weakens", "Triggers still appear, but they have less pull than before."),
            BodyMilestone(30.days, "A new normal", "A month in, the new routine starts to feel natural."),
            BodyMilestone(66.days, "Habit formation", "Research suggests new habits take about two months on average — it varies widely, so be patient with yourself."),
        ),
        cravingTips = listOf(
            "Ride the urge: set a 10-minute timer and just notice it rise and fall.",
            "Change your environment — stand up, walk, drink water.",
            "Write down what triggered the urge so you can plan for it.",
            "Message someone supportive or open the AI Coach.",
        ),
    )

    /** Picks the best-matching category for a free-text habit name. */
    fun match(habitName: String): HabitCategory {
        val text = habitName.lowercase().trim()
        if (text.isEmpty()) return generic
        var best: HabitCategory? = null
        var bestScore = 0
        for (category in categories) {
            val score = category.keywords.count { keyword -> text.contains(keyword) }
            if (score > bestScore) {
                best = category
                bestScore = score
            }
        }
        return best ?: generic
    }
}

package com.example.reclaimapp.domain.coach.catalog

import com.example.reclaimapp.domain.coach.BodyMilestone
import com.example.reclaimapp.domain.coach.HabitCategory
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Sleep, procrastination, thinking-pattern and sitting habits (part of [BehaviorCatalog]).
 * General wellbeing information only. Timelines are typical ranges and vary a lot.
 */
internal object BehaviorSleepMind {

    val categories: List<HabitCategory> = listOf(

        /**
         * Overrides the legacy `sleep` id (legacy label "Late nights").
         * Sources: NHS "How to get to sleep"; CDC "About sleep" (adults 7+ hours); AASM/SRS consensus 2015.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "sleep",
            label = "Staying up late",
            keywords = listOf(
                "staying up late", "stay up late", "staying up", "stay up", "going to bed late", "sleeping late", "night owl",
                "all nighter", "all-nighter", "sleep late", "sleep schedule", "irregular sleep", "bad sleep routine",
                "سهر", "السهر", "اسهر", "بسهر", "سهر بالليل", "السهر لحد الصبح", "نظام نوم غلط",
                "sahar", "el sahar",
            ),
            whyItMatters = "Your body clock likes routine. Even a small shift to an earlier, steadier bedtime tends to improve mood, focus and energy.",
            milestones = listOf(
                BodyMilestone(2.hours, "Wind-down window", "Dimming lights and slowing down in the last hour or two typically helps your body prepare for sleep."),
                BodyMilestone(8.hours, "First earlier night", "One earlier night often brings a bit more deep sleep. You may still feel wired at your old bedtime."),
                BodyMilestone(24.hours, "Clock gets a signal", "Waking at a fixed time and getting daylight in the morning typically gives your body clock a clear cue."),
                BodyMilestone(48.hours, "Easier to drift off", "Falling asleep at the new time commonly gets a little easier after two or three nights."),
                BodyMilestone(72.hours, "Daytime dips shrink", "Many people notice fewer afternoon energy dips by the third day."),
                BodyMilestone(7.days, "More alert days", "Many people feel more alert and less irritable after a week of consistent sleep."),
                BodyMilestone(14.days, "Mood steadies", "Regular sleep supports mood, memory and focus. The new bedtime starts to feel natural."),
                BodyMilestone(30.days, "A routine that holds", "Your schedule typically runs mostly on its own, with occasional slips that are easy to recover from."),
            ),
            cravingTips = listOf(
                "Set a fixed wake-up time first. Bedtime follows.",
                "Dim the lights and put screens away 30 minutes before bed.",
                "Write tomorrow's top 3 tasks so your mind can switch off.",
                "Keep your phone charging away from the bed.",
            ),
            safetyNote = "If trouble sleeping lasts a long time, you feel unwell during the day, or someone notices loud snoring or pauses in your breathing, speak to a doctor.",
        ),

        /**
         * Sources: NHS sleep guidance; Sleep Foundation / AASM on oversleeping and consistency.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "oversleeping",
            label = "Oversleeping & snoozing",
            keywords = listOf(
                "oversleeping", "over sleeping", "oversleep", "sleeping too much", "snoozing", "snooze button", "hitting snooze",
                "waking up late", "wake up late", "sleeping in too long", "cannot wake up", "stay in bed too long",
                "نوم كتير", "النوم الزيادة", "اصحى متاخر", "الاستيقاظ المتاخر", "ازاي اصحى بدري", "تأجيل المنبه", "اسنوز",
            ),
            whyItMatters = "Long, irregular sleep and repeated snoozing can leave you groggy and shift your body clock. A fixed wake-up time often feels better than extra minutes.",
            milestones = listOf(
                BodyMilestone(1.hours, "Out of bed", "The first hour after waking is when the pull to stay in bed is strongest. Standing up and opening the curtains typically helps."),
                BodyMilestone(8.hours, "The first alarm", "Getting up at the first alarm is the key step. The urge to snooze usually peaks right then."),
                BodyMilestone(24.hours, "One clean morning", "One snooze-free morning shows you what it feels like. Grogginess typically fades within the first hour."),
                BodyMilestone(48.hours, "Morning light helps", "Daylight and water soon after waking tend to speed up alertness."),
                BodyMilestone(72.hours, "Earlier tiredness", "You may feel sleepy earlier in the evening, which typically helps an earlier bedtime."),
                BodyMilestone(7.days, "Less sleep inertia", "Many people feel less groggy in the first minutes after waking."),
                BodyMilestone(14.days, "A steady wake time", "A regular wake time tends to feel more natural."),
                BodyMilestone(30.days, "Mornings you own", "Most people feel more in control of the start of the day."),
            ),
            cravingTips = listOf(
                "Put the alarm across the room so you have to stand up.",
                "Open the curtains or turn on a bright light right away.",
                "Drink a glass of water before checking your phone.",
                "Go to bed at a time that gives you about 7 to 9 hours.",
            ),
            safetyNote = "If you are always exhausted or sleep very long hours and still feel unrefreshed, or you feel low for weeks, speak to a doctor.",
        ),

        /**
         * Overrides the legacy `procrastination` id.
         * Sources: Sirois & Pychyl 2013 (Social and Personality Psychology Compass) on procrastination as
         * short-term mood repair; Lally et al. 2009 (Eur J Social Psychology) on habit formation.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "procrastination",
            label = "Procrastination",
            keywords = listOf(
                "procrastination", "procrastinating", "procrastinate", "putting things off", "putting off tasks", "putting off work",
                "delaying tasks", "leaving things to the last minute", "last minute work", "avoiding my tasks", "cant start working",
                "تسويف", "التسويف", "اسوف", "بسوف", "تأجيل المهام", "ابدأ متاخر", "كسل في الشغل", "تأجيل الشغل",
            ),
            whyItMatters = "Procrastination is mostly about managing uncomfortable feelings, not time. Starting small is often the fastest way through it.",
            milestones = listOf(
                BodyMilestone(1.hours, "The first five minutes", "Starting a task for just five minutes typically makes the urge to avoid it shrink."),
                BodyMilestone(4.hours, "Something started", "Beginning one task today shows you can start. Relief often follows."),
                BodyMilestone(12.hours, "Less dread tonight", "Unfinished tasks weigh less when you have made a first step."),
                BodyMilestone(24.hours, "A day of starting", "Many people feel a small boost in confidence after finishing something they had been avoiding."),
                BodyMilestone(72.hours, "Momentum builds", "Finishing small tasks makes the next one easier."),
                BodyMilestone(7.days, "Less drama, more flow", "Starting stops feeling like a battle for many people."),
                BodyMilestone(14.days, "Trust grows", "You begin to trust yourself to follow through."),
                BodyMilestone(30.days, "A new default", "Starting right away becomes more normal. Research suggests habits take weeks to months, and it varies a lot."),
            ),
            cravingTips = listOf(
                "Work for just 5 minutes. You may keep going.",
                "Write down the very first physical step of the task.",
                "Put your phone out of reach before you start.",
                "Break the task into pieces that take under 20 minutes.",
            ),
        ),

        /**
         * Sources: NHS Every Mind Matters and NHS talking therapies on worry and rumination;
         * Nolen-Hoeksema et al. 2008 (Perspectives on Psychological Science) on rumination; APA.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "overthinking",
            label = "Overthinking & rumination",
            keywords = listOf(
                "overthinking", "over thinking", "ruminating", "rumination", "replaying conversations", "worrying all the time",
                "thinking too much", "what if thoughts", "going in circles in my head", "mental loops", "cant stop thinking",
                "افكر كتير", "التفكير الزايد", "التفكير الزائد", "فرط التفكير", "اجترار الافكار", "قلق طول الوقت", "دماغي مش بتهدا", "بفكر كتير",
            ),
            whyItMatters = "Going over the same worries rarely solves them and often leaves you more tired and anxious. Learning to notice loops and step out of them is a skill that improves with practice.",
            milestones = listOf(
                BodyMilestone(1.hours, "Noticing the loop", "In the first hour many people catch the start of a loop. Just noticing it is the first step."),
                BodyMilestone(3.hours, "A pause helps", "Taking a short break, such as a walk or a task, typically eases the pull of the thought."),
                BodyMilestone(8.hours, "A calmer evening", "A scheduled worry time earlier in the day can leave the evening quieter."),
                BodyMilestone(24.hours, "Thoughts are not facts", "Many people find that urgent worries soften after a day of naming them instead of following them."),
                BodyMilestone(72.hours, "Sleep gets easier", "Less late-night looping often means falling asleep faster."),
                BodyMilestone(7.days, "More present", "Many people feel more present and less drained by their own thoughts."),
                BodyMilestone(14.days, "Faster recovery", "You typically bounce back from a spiral sooner."),
                BodyMilestone(30.days, "A steadier mind", "Practice tends to make loops shorter and less frequent. Progress is rarely a straight line."),
            ),
            cravingTips = listOf(
                "Write the thought down, then decide one small action or let it go.",
                "Name it: say 'I am noticing a worry loop'.",
                "Move your body for 5 minutes. Walking helps.",
                "Set a 10-minute 'worry time' and postpone the rest.",
            ),
            safetyNote = "If your thoughts feel overwhelming, last for weeks, or come with hopeless thoughts or thoughts of harming yourself, please talk to a doctor or counsellor, or contact your local emergency number or crisis line right away.",
        ),

        /**
         * Sources: Neff 2003 on self-compassion; NHS Every Mind Matters on self-esteem; CBT-based advice.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "negativeselftalk",
            label = "Negative self-talk",
            keywords = listOf(
                "negative self talk", "negative self-talk", "putting myself down", "being hard on myself", "self criticism",
                "self-criticism", "inner critic", "calling myself stupid", "talking badly to myself", "self hate talk",
                "جلد الذات", "احتقار النفس", "انتقاد نفسي", "اقول لنفسي كلام وحش", "لوم نفسي", "كلام سلبي مع نفسي",
            ),
            whyItMatters = "Harsh inner talk usually makes motivation and confidence worse, not better. Speaking to yourself the way you would to a friend is a learnable skill.",
            milestones = listOf(
                BodyMilestone(1.hours, "Catching it", "Many people first just notice the harsh voice. That is progress."),
                BodyMilestone(4.hours, "A kinder sentence", "Trying one kinder rewrite of a harsh thought feels awkward at first and usually gets easier."),
                BodyMilestone(12.hours, "Less sting", "After a day of noticing, many people feel the thoughts sting a bit less."),
                BodyMilestone(24.hours, "A day of noticing", "You may catch the pattern more often than you expected. This is normal and useful."),
                BodyMilestone(72.hours, "New phrases stick", "Short phrases like 'I did my best today' tend to start feeling more natural."),
                BodyMilestone(7.days, "Mood lifts for many", "Many people feel a bit lighter and more willing to try things."),
                BodyMilestone(14.days, "Shorter spirals", "Harsh thoughts typically pass faster and have less hold."),
                BodyMilestone(30.days, "A kinder default", "A kinder inner voice becomes more automatic, though bad days still happen."),
            ),
            cravingTips = listOf(
                "Ask: would I say this to a friend?",
                "Rewrite the thought into something fair and specific.",
                "Write down one thing that went okay today.",
                "Talk to someone you trust about how you speak to yourself.",
            ),
            safetyNote = "If negative thoughts feel constant, overwhelming or hopeless, or thoughts of harming yourself come up, please speak to a doctor or counsellor, or contact your local emergency number or crisis line right away. You deserve support.",
        ),

        /**
         * Sources: Festinger 1954 social comparison theory; Verduyn et al. 2020 on social comparison
         * online; APA guidance on social media and wellbeing.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "comparison",
            label = "Comparing myself to others",
            keywords = listOf(
                "comparing myself to others", "comparing myself", "compare myself", "envy", "jealousy of others",
                "feeling behind everyone", "measuring myself against others", "fomo", "keeping up with others",
                "مقارنة نفسي بالناس", "اقارن نفسي", "الغيرة", "الحسد", "حاسس اني متاخر عن الناس", "بقارن نفسي",
            ),
            whyItMatters = "People compare their everyday life to other people's best moments, especially online. Less comparing usually brings more contentment and focus on your own path.",
            milestones = listOf(
                BodyMilestone(1.hours, "Spotting the urge", "Many people first notice the comparison thought. Naming it often reduces its pull."),
                BodyMilestone(4.hours, "Less feed, less measuring", "If you avoid triggers like feeds, the comparisons often drop quickly."),
                BodyMilestone(12.hours, "A calmer evening", "Many people feel less restless when they are not measuring themselves."),
                BodyMilestone(24.hours, "Your own yardstick", "Focusing on your own progress today often feels more satisfying."),
                BodyMilestone(72.hours, "Gratitude helps", "Listing a few things you have or did can shift attention away from what others have."),
                BodyMilestone(7.days, "Lighter mood", "Many people feel less pressure and a bit more content after a week."),
                BodyMilestone(14.days, "Celebrating others", "It often gets easier to be happy for others without feeling smaller."),
                BodyMilestone(30.days, "Steadier self-worth", "Many people feel more settled in their own pace."),
            ),
            cravingTips = listOf(
                "Mute or unfollow accounts that leave you feeling worse.",
                "Compare yourself with yourself from a month ago.",
                "Write down three things you did well this week.",
                "Remember: you see someone's highlight reel, not their whole life.",
            ),
        ),

        /**
         * Sources: WHO 2020 guidelines on physical activity and sedentary behaviour (break up long sitting);
         * Dunstan et al. 2012 (Diabetes Care) on breaking up prolonged sitting.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "sitting",
            label = "Sitting too long",
            keywords = listOf(
                "sitting too long", "sitting too much", "sitting all day", "sedentary lifestyle", "sedentary", "not moving enough",
                "desk job posture", "prolonged sitting", "couch potato", "lack of movement",
                "قعاد كتير", "القعاد طول اليوم", "الجلوس لفترات طويلة", "الجلوس الطويل", "قلة الحركة", "خمول", "قاعد كتير",
            ),
            whyItMatters = "Long unbroken sitting is linked with stiffness and with long-term health risks. Short movement breaks through the day help more than most people expect.",
            milestones = listOf(
                BodyMilestone(30.minutes, "The first break", "Standing up and moving for a couple of minutes after about half an hour of sitting is a good rhythm."),
                BodyMilestone(2.hours, "Looser body", "Regular breaks typically ease stiffness in the back, hips and neck."),
                BodyMilestone(8.hours, "A better afternoon", "Many people report steadier energy in the afternoon on days with movement breaks."),
                BodyMilestone(24.hours, "Blood sugar benefits", "Studies suggest that short walking breaks can lower blood sugar after meals. Effects vary by person."),
                BodyMilestone(72.hours, "Easier to move", "Movement breaks start to feel more natural."),
                BodyMilestone(7.days, "Less aches", "Many people notice fewer aches and less stiffness after a week."),
                BodyMilestone(14.days, "Energy and mood", "Regular movement can lift energy and mood."),
                BodyMilestone(30.days, "A moving routine", "Short walks and stretches become part of the day."),
            ),
            cravingTips = listOf(
                "Set a timer to stand up every 30 to 45 minutes.",
                "Take phone calls standing or walking.",
                "Walk for 5 minutes after meals.",
                "Use stairs instead of lifts when you can.",
            ),
        ),
    )
}

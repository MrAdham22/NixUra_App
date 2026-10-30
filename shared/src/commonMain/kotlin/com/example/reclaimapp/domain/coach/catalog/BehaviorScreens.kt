package com.example.reclaimapp.domain.coach.catalog

import com.example.reclaimapp.domain.coach.BodyMilestone
import com.example.reclaimapp.domain.coach.HabitCategory
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Screen and digital habits (part of [BehaviorCatalog]). General wellbeing information only.
 * Timelines for behavioural habits are experience-based ranges, not clinical measurements.
 */
internal object BehaviorScreens {

    val categories: List<HabitCategory> = listOf(

        /**
         * Broad fallback; overrides the legacy `screens` id.
         * Sources: NHS "Sleep and tiredness" advice on screens before bed; Chang et al. 2015 (PNAS) on
         * e-readers and sleep; WHO 2019 guidance on screen time for young children (context only).
         * // UNVERIFIED: written from general knowledge, needs review (not re-checked online this session)
         */
        HabitCategory(
            id = "screens",
            label = "Screen time",
            keywords = listOf(
                "screen time", "too much screen time", "screens", "screen addiction", "staring at screens",
                "on my phone all day", "online too much", "internet addiction", "digital detox",
                "late night scrolling", "scrolling too much", "scrolling", "scroll",
                "وقت الشاشة", "الشاشات", "ادمان الشاشة", "ادمان الانترنت", "سكرين تايم", "قدام الشاشة كتير",
            ),
            whyItMatters = "Apps and feeds are designed to be hard to put down. Time away from screens gives attention, sleep and real-world connection room to recover.",
            milestones = listOf(
                BodyMilestone(1.hours, "The first pull", "In the first hour many people feel a reflex to reach for the screen. The urge usually rises, peaks and fades within minutes."),
                BodyMilestone(4.hours, "Restlessness settles", "Boredom and restlessness are typical early on. Many people find they fill the gap with a small offline task."),
                BodyMilestone(12.hours, "Evening without the glow", "If you skipped evening screens, falling asleep may feel a little easier tonight."),
                BodyMilestone(24.hours, "Less automatic checking", "After a day, picking up the device often stops feeling automatic and starts feeling like a choice."),
                BodyMilestone(72.hours, "Focus stretches", "Many people notice they can stay with a book, a task or a conversation for longer."),
                BodyMilestone(7.days, "Sleep and mood", "Cutting late-night screens often helps sleep come sooner, and mood can feel steadier."),
                BodyMilestone(14.days, "New defaults form", "Reaching for a walk, a book or a friend begins to replace the old reflex."),
                BodyMilestone(30.days, "The loop loosens", "The cue and reward loop is typically much weaker. Triggers still appear but have less pull."),
            ),
            cravingTips = listOf(
                "Put the device in another room for 15 minutes.",
                "Do something with your hands: stretch, tidy, doodle.",
                "Decide a time limit first, then set a timer.",
                "Log out of the app that tempts you most.",
            ),
        ),

        /**
         * Sources: NIH / NIMH and APA general guidance on social media and wellbeing; Hunt et al. 2018
         * (J Social and Clinical Psychology) on limiting social media; US Surgeon General advisory 2023.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "socialmedia",
            label = "Social media",
            keywords = listOf(
                "social media", "social networks", "facebook", "instagram", "insta", "twitter", "snapchat",
                "linkedin scrolling", "checking likes", "social media addiction", "social media scrolling",
                "سوشيال ميديا", "السوشيال ميديا", "مواقع التواصل", "التواصل الاجتماعي", "فيسبوك", "انستجرام", "تويتر", "سناب شات", "ادمان السوشيال",
                "facebook kteer",
            ),
            whyItMatters = "Social media rewards you unpredictably, which makes it easy to check again and again. Stepping back often helps with mood, comparison and time.",
            milestones = listOf(
                BodyMilestone(1.hours, "The urge to check", "Many people feel a pull to check for likes or messages within the first hour. It typically fades if you wait a few minutes."),
                BodyMilestone(3.hours, "Less fear of missing out", "Worry about what you are missing is common early on and usually shrinks as the hours pass."),
                BodyMilestone(8.hours, "A calmer evening", "Without a feed, many people notice quieter thoughts and an easier wind-down."),
                BodyMilestone(24.hours, "A day offline", "A full day often brings more present moments and fewer comparisons. The reflex to open the app is still strong."),
                BodyMilestone(72.hours, "Time shows up", "Many people notice how much time the feed used to take and fill it with something they chose."),
                BodyMilestone(7.days, "Mood and focus", "Studies on limiting social media suggest some people feel less lonely and less anxious after limiting it for a few weeks. Results vary."),
                BodyMilestone(14.days, "Habit loosens", "Opening the app without thinking becomes less frequent."),
                BodyMilestone(30.days, "A new balance", "Many people settle into a use pattern they actually like, or decide they do not miss it."),
            ),
            cravingTips = listOf(
                "Move the app off your home screen or delete it for a week.",
                "Turn off all non-essential notifications.",
                "Message one real person instead of scrolling.",
                "Set a 10-minute timer before opening it.",
            ),
        ),

        /**
         * Sources: general guidance on news and mental health (APA, NHS Every Mind Matters on coping with
         * bad news); Price et al. 2022 (Health Communication) on problematic news consumption.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "doomscrolling",
            label = "Doomscrolling & news checking",
            keywords = listOf(
                "doomscrolling", "doom scrolling", "doomscroll", "reading bad news", "checking the news constantly",
                "news addiction", "compulsively checking news", "refreshing the news", "constant news checking",
                "news checking", "following the news all day", "breaking news obsession",
                "اخبار سيئة", "متابعة الاخبار", "تصفح الاخبار", "ادمان الاخبار", "قراءة الاخبار طول اليوم", "اتابع الاخبار",
            ),
            whyItMatters = "Constant bad news keeps your body on alert without giving you anything to do about it. Setting limits helps you stay informed without staying stressed.",
            milestones = listOf(
                BodyMilestone(1.hours, "The itch to refresh", "Many people feel an urge to check for updates in the first hour. It usually passes in a few minutes."),
                BodyMilestone(3.hours, "Body comes off alert", "Without a stream of alarming headlines, tension in the shoulders and jaw often eases."),
                BodyMilestone(8.hours, "Calmer thoughts", "Many people notice fewer worst-case thoughts when they stop scanning for news."),
                BodyMilestone(24.hours, "A day without refreshing", "You are still informed enough. Important news typically reaches you anyway."),
                BodyMilestone(72.hours, "Evenings feel lighter", "Sleep and mood often improve when the last thing you read is not a crisis."),
                BodyMilestone(7.days, "A healthier news routine", "Many people prefer checking once or twice a day at set times, from a source they trust."),
                BodyMilestone(14.days, "Less dread", "Background worry about the world typically feels more manageable."),
                BodyMilestone(30.days, "Informed, not flooded", "A bounded news habit tends to stick once the reflex is weaker."),
            ),
            cravingTips = listOf(
                "Pick one or two fixed times a day to check the news.",
                "Mute breaking-news alerts.",
                "Replace the scroll with one action you can take, however small.",
                "Close the app and take five slow breaths.",
            ),
        ),

        /**
         * Sources: NIH-funded reviews on short-video use and attention are still emerging; guidance is
         * based on general habit-loop and variable-reward literature (e.g. APA).
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "shortvideo",
            label = "Short videos (Reels, Shorts, TikTok)",
            keywords = listOf(
                "reels", "instagram reels", "youtube shorts", "tiktok", "tik tok", "short videos", "short form video",
                "short-form video", "endless videos", "watching reels", "tiktok addiction",
                "ريلز", "الريلز", "شورتس", "تيك توك", "تيكتوك", "فيديوهات قصيرة", "فيديوهات التيك توك", "ادمان التيك توك",
            ),
            whyItMatters = "Short videos give a new reward every few seconds, so one more is always easy. Research on effects is still emerging, but many people report time loss and shorter attention.",
            milestones = listOf(
                BodyMilestone(1.hours, "The next-video pull", "The urge to swipe to the next clip is typically strongest in the first hour. It fades if you do not feed it."),
                BodyMilestone(4.hours, "Boredom without the stream", "Boredom is typical early on. Many people find it passes faster than expected."),
                BodyMilestone(12.hours, "Quieter evening", "Without rapid-fire clips, winding down and falling asleep often feels easier."),
                BodyMilestone(24.hours, "A day of longer attention", "Longer content, such as a chapter or a full conversation, often feels more manageable."),
                BodyMilestone(72.hours, "Time comes back", "Many people notice hours they did not realise were being spent and use them for something else."),
                BodyMilestone(7.days, "Focus improves", "Many people report holding attention on one thing more easily after a week."),
                BodyMilestone(14.days, "Less phantom swiping", "The thumb-flick reflex typically weakens."),
                BodyMilestone(30.days, "A weaker loop", "A month in, short videos tend to be a choice rather than a default."),
            ),
            cravingTips = listOf(
                "Delete the app, or use a web browser instead of the app.",
                "Set a daily limit in your phone's screen-time settings.",
                "Keep a book or a podcast ready for gaps.",
                "Ask yourself what you were feeling before you opened it.",
            ),
        ),

        /**
         * Sources: WHO ICD-11 "gaming disorder" (2019, context for when to seek help); AAP guidance on
         * media use; NHS advice on screen time and sleep.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "gaming",
            label = "Gaming",
            keywords = listOf(
                "playing games", "video games", "gaming", "gamer", "gaming marathon", "mobile games", "gaming addiction",
                "playstation", "xbox", "pubg", "fortnite", "fifa", "call of duty", "roblox", "minecraft", "valorant", "free fire", "league of legends",
                "العاب فيديو", "لعب الالعاب", "العاب الموبايل", "بلايستيشن", "ببجي", "فري فاير", "ادمان الالعاب", "العب كتير", "فيفا",
                "pubji",
            ),
            whyItMatters = "Games are built around goals and rewards that are hard to leave mid-way. Taking breaks gives sleep, study, work and relationships more room.",
            milestones = listOf(
                BodyMilestone(1.hours, "The one-more-round pull", "It is common to feel a strong pull to start or finish one more match. The urge typically fades in minutes."),
                BodyMilestone(4.hours, "Restless hands", "Restlessness and boredom are typical early on. Many people find a short walk or task helps."),
                BodyMilestone(8.hours, "A night off", "Skipping a late session often means an earlier bedtime and easier sleep."),
                BodyMilestone(24.hours, "Time returns", "A day without gaming leaves hours free. Many people feel a mix of boredom and relief."),
                BodyMilestone(72.hours, "Irritability eases", "Some people feel irritable or low for the first days. This typically eases."),
                BodyMilestone(7.days, "Energy and focus", "Many people notice better sleep, more energy and easier focus on daily tasks."),
                BodyMilestone(14.days, "Other rewards return", "Hobbies, exercise and friends often feel rewarding again."),
                BodyMilestone(30.days, "Choice, not compulsion", "Many people can return to gaming on their own terms, or choose to keep it small."),
            ),
            cravingTips = listOf(
                "Uninstall or log out so starting takes effort.",
                "Set a stop time before you start and use an alarm.",
                "Plan something social or active for your usual gaming hours.",
                "Take a 10-minute walk before deciding.",
            ),
            safetyNote = "If gaming is causing serious problems with sleep, work, study or relationships and you cannot cut back, consider talking to a doctor or counsellor.",
        ),

        /**
         * Sources: Exelmans & Van den Bulck 2017 (binge viewing and sleep); NHS sleep advice.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "bingewatching",
            label = "Binge-watching",
            keywords = listOf(
                "binge watching", "binge-watching", "binge watch", "bingeing series", "netflix", "watching series all night",
                "one more episode", "tv marathon", "watching tv too much", "streaming too much", "youtube rabbit hole", "watching youtube all day",
                "مسلسلات", "متابعة المسلسلات", "نتفلكس", "مشاهدة المسلسلات", "حلقة كمان", "افلام طول الليل", "يوتيوب",
            ),
            whyItMatters = "Autoplay removes natural stopping points, so one episode becomes five. Choosing when to stop protects sleep and time.",
            milestones = listOf(
                BodyMilestone(1.hours, "The autoplay pull", "The urge to press next is typically strongest right when an episode ends."),
                BodyMilestone(4.hours, "Free evening", "An evening without a binge leaves time for other things, and the hours often feel longer."),
                BodyMilestone(12.hours, "Better bedtime", "Many people go to bed earlier and fall asleep more easily."),
                BodyMilestone(24.hours, "A day without cliffhangers", "The need to find out what happens next typically fades faster than expected."),
                BodyMilestone(72.hours, "More active days", "Many people use the time to move, cook, read or meet someone."),
                BodyMilestone(7.days, "Mornings feel better", "Better sleep from earlier nights often shows up as easier mornings."),
                BodyMilestone(14.days, "Watching on purpose", "Many people prefer to watch one episode at a set time instead of by default."),
                BodyMilestone(30.days, "A balanced routine", "Watching becomes one choice among many rather than the default evening plan."),
            ),
            cravingTips = listOf(
                "Turn off autoplay.",
                "Decide how many episodes before you start.",
                "Stand up when an episode ends and do one small task.",
                "Watch earlier in the evening, not right before bed.",
            ),
        ),

        /**
         * Sources: Ward et al. 2017 (J Association for Consumer Research) on smartphone presence and
         * attention; research on phubbing and relationship satisfaction (Roberts & David 2016).
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "phonechecking",
            label = "Checking my phone",
            keywords = listOf(
                "checking my phone", "checking phone constantly", "phone checking", "picking up my phone", "phubbing",
                "phone addiction", "nomophobia", "using phone while talking", "phone at dinner", "ignoring people for my phone",
                "checking notifications", "notification checking",
                "ادمان الموبايل", "الموبايل في ايدي", "مسك الموبايل", "اشيك على الموبايل", "الاشعارات", "استخدام الموبايل كتير",
            ),
            whyItMatters = "Checking your phone dozens of times a day splits your attention, even when you do not pick it up. Fewer checks make conversations and tasks feel fuller.",
            milestones = listOf(
                BodyMilestone(1.hours, "Phantom buzz", "Many people feel imaginary vibrations or an urge to check in the first hour. It typically passes."),
                BodyMilestone(2.hours, "Hands feel empty", "Not knowing what you might be missing is typically uncomfortable at first."),
                BodyMilestone(6.hours, "Attention returns", "Many people find they can finish a task or conversation without breaking off."),
                BodyMilestone(24.hours, "Fewer reach-ins", "A day with set check times often cuts the automatic reach for the phone."),
                BodyMilestone(72.hours, "People notice", "Friends and family often notice you are more present."),
                BodyMilestone(7.days, "Lower background tension", "Many people feel less on-call and more relaxed."),
                BodyMilestone(14.days, "A new rhythm", "Checking at planned times can start to feel normal."),
                BodyMilestone(30.days, "A phone you use, not one that uses you", "Triggers still appear but you typically notice them earlier."),
            ),
            cravingTips = listOf(
                "Leave the phone in another room during meals and conversations.",
                "Turn off non-essential notifications.",
                "Pick two or three set times to check messages.",
                "Keep your phone face down, out of reach.",
            ),
        ),

        /**
         * Sources: Chang et al. 2015 (PNAS) on light-emitting devices before bed and melatonin;
         * NHS and CDC sleep hygiene advice.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "latenightphone",
            label = "Phone in bed at night",
            keywords = listOf(
                "phone in bed", "phone before bed", "scrolling in bed", "scrolling at night", "using phone at night",
                "night scrolling", "phone after midnight", "bedtime scrolling", "late night phone use", "scrolling before sleep",
                "الموبايل قبل النوم", "الموبايل في السرير", "تصفح بالليل", "سهر على الموبايل", "الموبايل بالليل",
            ),
            whyItMatters = "Light, content and notifications late at night make it harder to fall asleep and stay asleep. A phone-free hour before bed is one of the simplest sleep upgrades.",
            milestones = listOf(
                BodyMilestone(1.hours, "Quiet hour", "The first phone-free hour before bed often feels odd. Many people notice their mind slowing."),
                BodyMilestone(8.hours, "First night", "Many people fall asleep a bit faster after one phone-free evening."),
                BodyMilestone(24.hours, "Morning check", "You may feel a little more rested. Some people also feel a pull to catch up on the phone in the morning."),
                BodyMilestone(72.hours, "Sleep settles", "Falling asleep typically gets easier as the new routine sets in."),
                BodyMilestone(7.days, "Energy lifts", "Many people feel more alert in the morning after a week of earlier, calmer nights."),
                BodyMilestone(14.days, "A bedtime routine", "A simple wind-down, such as reading or stretching, starts to replace scrolling."),
                BodyMilestone(30.days, "Steadier sleep", "Better sleep often shows up as steadier mood and focus. Individual results vary."),
            ),
            cravingTips = listOf(
                "Charge the phone outside the bedroom or across the room.",
                "Use a basic alarm clock instead of the phone.",
                "Set a nightly phone curfew and a reminder.",
                "Keep a book or paper notebook by the bed.",
            ),
        ),

        /**
         * Sources: general communication and wellbeing literature; no specific clinical claims made.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "texting",
            label = "Excessive texting & messaging",
            keywords = listOf(
                "texting too much", "excessive texting", "constant texting", "messaging all day", "whatsapp all day",
                "checking whatsapp", "replying instantly", "group chats", "chatting all day", "texting nonstop",
                "رسائل كتير", "واتساب طول اليوم", "الواتساب", "مسجات", "شات طول اليوم", "جروبات الواتساب",
            ),
            whyItMatters = "Constant messaging breaks focus and makes every moment feel on-call. Setting reply windows keeps you connected without being always available.",
            milestones = listOf(
                BodyMilestone(1.hours, "Waiting to reply", "It is typical to feel a pull to reply right away. Most messages can wait an hour."),
                BodyMilestone(3.hours, "Deeper work", "Many people find they get further on one task without chat interruptions."),
                BodyMilestone(8.hours, "Batch reply", "Replying to everything in one go is often faster and calmer than replying all day."),
                BodyMilestone(24.hours, "Others adapt", "Friends and colleagues typically adjust quickly to slower replies."),
                BodyMilestone(72.hours, "Less on-call", "Many people feel less pressure to respond immediately."),
                BodyMilestone(7.days, "Better conversations", "With fewer, fuller chats, many people feel more present."),
                BodyMilestone(14.days, "Clear boundaries", "Reply windows start to feel normal for you and the people around you."),
                BodyMilestone(30.days, "Connection without pressure", "You stay in touch without feeling tied to the screen."),
            ),
            cravingTips = listOf(
                "Mute group chats and check them at set times.",
                "Turn off read receipts and message previews.",
                "Reply to messages in two or three batches a day.",
                "Call a friend instead of sending ten texts.",
            ),
        ),
    )
}

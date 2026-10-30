package com.example.reclaimapp.domain.coach.catalog

import com.example.reclaimapp.domain.coach.BodyMilestone
import com.example.reclaimapp.domain.coach.HabitCategory
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Money, impulse, speech and emotional-reaction habits (part of [BehaviorCatalog]).
 * General wellbeing information only, never financial, legal or medical advice.
 */
internal object BehaviorSocialMoney {

    val categories: List<HabitCategory> = listOf(

        /**
         * Overrides the legacy `spending` id (now focused on impulse and online shopping; gambling has its own id).
         * Sources: general guidance on impulse buying and cooling-off periods (e.g. consumer-finance and
         * behavioural-economics literature, UK MoneyHelper); no specific clinical claims.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "spending",
            label = "Impulse & online shopping",
            keywords = listOf(
                "impulse shopping", "impulse buying", "impulse buy", "online shopping", "shopping addiction", "retail therapy",
                "buying things i dont need", "overspending", "compulsive shopping", "shopping spree", "spending too much money",
                "shein", "temu", "amazon orders",
                "تسوق", "التسوق", "ادمان التسوق", "شراء بدون تفكير", "شراء اندفاعي", "الشراء اونلاين", "شوبينج", "صرف فلوس كتير", "جوميا", "بصرف كتير",
            ),
            whyItMatters = "Urges to buy feel urgent but pass, and the rush usually fades after the purchase. Each urge you ride out protects your money and your peace of mind.",
            milestones = listOf(
                BodyMilestone(1.hours, "The urge fades", "A buying urge typically peaks and drops within an hour if you do not act on it."),
                BodyMilestone(4.hours, "Cart still there", "Leaving items in the cart for a few hours often shows you whether you still want them."),
                BodyMilestone(12.hours, "Overnight check", "Waiting until tomorrow often changes your mind about non-essential items."),
                BodyMilestone(24.hours, "A day of savings", "A 24-hour rule is a simple, popular way to avoid impulse purchases."),
                BodyMilestone(72.hours, "Clearer head", "Money-related stress and regret often begin to ease."),
                BodyMilestone(7.days, "Breathing room", "A week of saved money and fewer packages is real progress."),
                BodyMilestone(14.days, "New rewards", "You find other ways to treat yourself that do not cost money."),
                BodyMilestone(30.days, "New priorities", "A month in, many people feel more in control of money and rebuild trust with themselves."),
            ),
            cravingTips = listOf(
                "Wait 24 hours before any non-essential purchase.",
                "Remove saved cards and delete shopping apps from your phone.",
                "Unsubscribe from sale emails and mute shopping notifications.",
                "Make a list before you shop and stick to it.",
            ),
            safetyNote = "If shopping or spending feels out of control, or you are hiding it or going into debt, consider talking to a counsellor, a doctor or a free money-advice service. You do not have to do this alone.",
        ),

        /**
         * Sources: NHS "Gambling addiction" pages (signs and where to get help); WHO ICD-11 gambling disorder;
         * general guidance on self-exclusion and urge surfing.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "gambling",
            label = "Gambling & betting",
            keywords = listOf(
                "gambling", "gambling addiction", "betting", "sports betting", "online betting", "casino", "online casino",
                "slot machines", "poker online", "lottery tickets", "scratch cards", "bookmaker", "betting apps", "chasing losses",
                "قمار", "المقامرة", "مراهنات", "الرهان", "المراهنة", "العب قمار", "رهان", "كازينو", "بيتنج",
            ),
            whyItMatters = "Gambling is designed so that near-wins feel like progress. Breaking the cycle protects your money, your relationships and your peace of mind.",
            milestones = listOf(
                BodyMilestone(1.hours, "The urge peaks", "A betting urge typically rises and then fades. Delaying even an hour can help."),
                BodyMilestone(4.hours, "Fewer thoughts of the next bet", "It is common to keep thinking about a bet. These thoughts typically ease if you stay busy."),
                BodyMilestone(12.hours, "Tonight without it", "Many people find evenings or paydays are the hardest. Having a plan helps."),
                BodyMilestone(24.hours, "A day gambling-free", "Each day you do not gamble is money kept and a small win."),
                BodyMilestone(72.hours, "Restlessness and mood", "Irritability, restlessness or low mood can show up early. These typically ease."),
                BodyMilestone(7.days, "Clearer head", "A week in, many people sleep better and feel less preoccupied."),
                BodyMilestone(14.days, "Rebuilding trust", "Money feels easier to manage, and honest conversations with others often become possible."),
                BodyMilestone(30.days, "A new normal", "Urges still appear but are typically weaker. Support and blocking tools make a big difference."),
                BodyMilestone(90.days, "Breathing space", "Many people feel more financial and emotional stability after about three months."),
            ),
            cravingTips = listOf(
                "Delay by an hour and do something else. Urges rise, peak and pass.",
                "Delete betting apps, use blocking software and self-exclusion tools.",
                "Hand your cards or bank access to someone you trust for a while.",
                "Tell someone you trust about the urge right now.",
            ),
            safetyNote = "If gambling feels out of control, you are hiding it, borrowing to gamble, or have thoughts of harming yourself, please talk to a doctor, a counsellor or a local gambling-support service, or contact your local emergency number or crisis line. You do not have to do this alone.",
        ),

        /**
         * Sources: general guidance on language habits and self-regulation; no clinical claims. Swearing is
         * not harmful in itself; this entry is for people who want to cut down.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "swearing",
            label = "Swearing",
            keywords = listOf(
                "swearing", "cursing", "cussing", "bad language", "foul language", "dirty mouth", "watching my language",
                "swear less", "using bad words", "vulgar language",
                "الشتيمة", "الشتايم", "الالفاظ السيئة", "الالفاظ البذيئة", "كلام بذيء", "بشتم", "الشتم",
            ),
            whyItMatters = "Swearing is often a quick way to release stress or emphasis. If you want to cut down, awareness and a few replacement phrases usually go a long way.",
            milestones = listOf(
                BodyMilestone(1.hours, "Catching the words", "In the first hour many people notice how often it slips out automatically."),
                BodyMilestone(4.hours, "Replacement words", "Having a harmless go-to word makes the pause easier."),
                BodyMilestone(12.hours, "A day of noticing", "Many people see which situations trigger it most, such as traffic, stress or certain company."),
                BodyMilestone(24.hours, "One clean day", "Many people find it takes real effort at first. That is normal."),
                BodyMilestone(72.hours, "Slips get rarer", "Slips often become less frequent and easier to catch."),
                BodyMilestone(7.days, "Calmer in stress", "Pausing before reacting often becomes more natural."),
                BodyMilestone(14.days, "New default words", "The replacement phrases start to feel natural."),
                BodyMilestone(30.days, "A new habit of speech", "Many people say their speech feels calmer and more deliberate."),
            ),
            cravingTips = listOf(
                "Pick a harmless replacement word or phrase.",
                "Take one slow breath before reacting.",
                "Ask a friend to gently point out slips.",
                "Notice which situations trigger it and plan for them.",
            ),
        ),

        /**
         * Sources: general guidance on communication and workplace wellbeing; no clinical claims.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "gossiping",
            label = "Gossiping",
            keywords = listOf(
                "gossiping", "gossip", "talking behind peoples backs", "talking about people behind their back", "badmouthing others",
                "spreading rumours", "spreading rumors", "trash talking", "speaking ill of others",
                "النميمة", "الغيبة", "الكلام عن الناس", "كلام عن الناس من ورا ضهرهم", "اتكلم عن الناس", "قيل وقال", "الوشاية",
            ),
            whyItMatters = "Gossip can feel bonding in the moment but often damages trust, including your own. Steering conversations elsewhere builds stronger relationships.",
            milestones = listOf(
                BodyMilestone(1.hours, "Noticing the pull", "Many people first notice how often conversations drift to other people."),
                BodyMilestone(4.hours, "A kind redirect", "Changing the subject or saying something positive typically gets easier with a little practice."),
                BodyMilestone(12.hours, "Fewer regrets", "Many people feel lighter when they have not talked about someone behind their back."),
                BodyMilestone(24.hours, "A day of kinder talk", "Conversations may feel different when they are about ideas, plans or shared interests."),
                BodyMilestone(72.hours, "More trust", "People often start to confide more when they sense you do not gossip."),
                BodyMilestone(7.days, "Easier boundaries", "Declining to join in feels less awkward."),
                BodyMilestone(14.days, "New topics", "You find other things to talk about."),
                BodyMilestone(30.days, "A trusted reputation", "Many people notice stronger, more honest relationships over time."),
            ),
            cravingTips = listOf(
                "Ask yourself: would I say this if they were here?",
                "Change the subject to a plan, a question or a compliment.",
                "Say something positive about the person instead.",
                "Spend less time in situations where gossip is the main activity.",
            ),
        ),

        /**
         * Sources: APA "Controlling anger before it controls you"; NHS "Anger"; Mayo Clinic anger management
         * (take a timeout, breathe, exercise, seek help when out of control).
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "anger",
            label = "Anger outbursts",
            keywords = listOf(
                "anger outbursts", "anger issues", "losing my temper", "lose my temper", "shouting at people", "yelling at people",
                "explosive anger", "short temper", "rage", "angry outbursts", "snapping at people", "anger management",
                "نوبات غضب", "الغضب", "العصبية", "عصبية زيادة", "بتعصب بسرعة", "بزعق", "الزعيق", "اتعصب", "فقدان الاعصاب",
            ),
            whyItMatters = "Anger is a normal emotion, but outbursts can hurt relationships and leave you feeling worse. Short pauses help you respond rather than react.",
            milestones = listOf(
                BodyMilestone(20.minutes, "The surge settles", "The intense physical surge of anger typically fades within minutes to about twenty minutes if you step away."),
                BodyMilestone(2.hours, "After the heat", "Many people feel calmer and can think more clearly once the surge has passed."),
                BodyMilestone(8.hours, "A calmer day", "Slow breathing, movement and a good night's sleep typically make irritability easier to manage."),
                BodyMilestone(24.hours, "A day of pausing", "Even one pause before reacting is a win. Many people notice their triggers more."),
                BodyMilestone(72.hours, "Fewer flare-ups", "With practice, many people notice fewer or shorter outbursts."),
                BodyMilestone(7.days, "Better repairs", "Apologising or explaining calmly becomes more possible."),
                BodyMilestone(14.days, "More patience", "Triggers still appear, but you can often respond more slowly."),
                BodyMilestone(30.days, "A steadier temper", "Many people feel more in control and notice relationships improving."),
            ),
            cravingTips = listOf(
                "Step away for a few minutes before you respond.",
                "Breathe out slowly: in for 4, out for 6.",
                "Move: walk fast, stretch or shake out your hands.",
                "Say what you need once calmly, instead of shouting.",
            ),
            safetyNote = "If your anger leads you to hurt someone or yourself, or you fear you may lose control, please speak to a doctor or counsellor. If someone is in immediate danger, contact your local emergency number.",
        ),

        /**
         * Neutral and clinical on purpose; no explicit wording. Keywords are the terms people actually type.
         * Sources: general guidance on compulsive sexual behaviour from NHS and Mayo Clinic (see a doctor or
         * counsellor if it harms relationships, work or wellbeing); WHO ICD-11 "compulsive sexual behaviour disorder".
         * // UNVERIFIED: written from general knowledge, needs review. Timeline is general, experience-based.
         */
        HabitCategory(
            id = "explicitcontent",
            label = "Explicit content",
            keywords = listOf(
                "explicit content", "adult content", "adult websites", "adult sites", "porn", "pornography", "nsfw", "x rated content",
                "porn addiction", "pornography addiction",
                "اباحية", "مواقع اباحية", "المحتوى الاباحي", "محتوى للكبار", "ادمان الاباحية", "المواقع الاباحية",
            ),
            whyItMatters = "For some people this habit takes up time, affects mood, sleep, focus or relationships, or feels hard to control. Cutting back can help you regain a sense of control. You do not have to be ashamed to ask for help.",
            milestones = listOf(
                BodyMilestone(1.hours, "The urge rises", "An urge typically rises, peaks and fades. Changing your setting or activity for ten minutes often helps."),
                BodyMilestone(4.hours, "Restlessness", "Restlessness, boredom or low mood often shows up as a trigger. Many people find movement or company helps."),
                BodyMilestone(12.hours, "A calmer night", "Putting devices away before bed often makes the evening easier."),
                BodyMilestone(24.hours, "A day on your terms", "A full day in which you decided is a clear win. Urges can still be strong."),
                BodyMilestone(72.hours, "Ups and downs", "Mood swings and strong urges are common in the first days. They typically ease with time and support."),
                BodyMilestone(7.days, "More time and focus", "Many people notice more free time and better focus. Experiences vary."),
                BodyMilestone(14.days, "Healthier routines", "New routines, exercise and sleep often help the urge lose its pull."),
                BodyMilestone(30.days, "Clearer control", "Many people report a stronger sense of control and less shame."),
                BodyMilestone(90.days, "A steadier pattern", "Triggers become easier to spot and plan for. Support from a therapist can help a lot."),
            ),
            cravingTips = listOf(
                "Move to another room or go outside for 10 minutes.",
                "Keep devices out of the bedroom and bathroom.",
                "Use website blockers or filters to add friction.",
                "Talk to someone you trust or a counsellor if the urges feel overwhelming.",
            ),
            safetyNote = "If this habit harms your relationships, work or mood, or you cannot cut back despite wanting to, consider talking to a doctor or counsellor. It is common, treatable, and nothing to be ashamed of.",
        ),
    )
}

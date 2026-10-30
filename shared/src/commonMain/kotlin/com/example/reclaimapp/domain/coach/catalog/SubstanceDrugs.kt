package com.example.reclaimapp.domain.coach.catalog

import com.example.reclaimapp.domain.coach.BodyMilestone
import com.example.reclaimapp.domain.coach.HabitCategory
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Alcohol, cannabis and dependence-forming medicines or drugs.
 *
 * Safety rules for this file: general wellbeing information only, never medical advice. No dosing, no
 * tapering schedules, no ways to obtain or use. Timelines stay general and conservative, and every
 * category here except cannabis carries a safetyNote about not stopping suddenly after heavy or long use.
 */
internal val drugCategories: List<HabitCategory> = listOf(
    /**
     * Sources: general knowledge consistent with NHS "Alcohol misuse" and "Alcohol withdrawal" guidance and
     * NIAAA (withdrawal can begin within hours and can be serious after heavy use).
     * UNVERIFIED: not re-checked online, needs review.
     */
    HabitCategory(
        id = "alcohol",
        label = "Alcohol",
        keywords = listOf(
            "alcohol", "alcoholic", "drinking alcohol", "beer", "beers", "wine", "liquor", "booze", "vodka", "whisky", "whiskey",
            "binge drinking", "drinking too much", "heavy drinking", "getting drunk", "cocktails", "tequila",
            "كحول", "الكحول", "كحوليات", "خمر", "الخمر", "بيرة", "نبيذ", "ويسكي", "فودكا", "شرب الخمر", "شرب الكحول", "سكر وعربدة", "مشروبات كحولية",
            "alchol", "alcohal", "alkohol",
        ),
        whyItMatters = "Alcohol disrupts sleep, mood and hydration. Cutting it out gives your body room to recover.",
        milestones = listOf(
            BodyMilestone(2.hours, "Alcohol is being cleared", "Your body clears roughly one standard drink per hour, so some may still be in your system."),
            BodyMilestone(6.hours, "Sleep and hydration dip", "Alcohol fragments sleep and dehydrates. You may feel tired, thirsty or low today."),
            BodyMilestone(12.hours, "Alcohol has left your system", "For most people alcohol is fully processed within about 12 hours after moderate drinking. Heavy drinkers may start to feel unwell, see the note below."),
            BodyMilestone(24.hours, "Hydration begins to recover", "Your body rebalances fluids and blood sugar after a day without alcohol. Anxiety or poor sleep can show up if you drink regularly."),
            BodyMilestone(48.hours, "Withdrawal window for heavy drinkers", "After regular heavy use, symptoms like shakiness, sweating or anxiety can occur in the first days and can become serious. Seek medical advice."),
            BodyMilestone(72.hours, "Sleep starts to rebalance", "Alcohol fragments sleep. Without it, deeper sleep stages begin to return."),
            BodyMilestone(7.days, "Energy and focus lift", "Many people feel clearer in the mornings and less anxious after a week."),
            BodyMilestone(14.days, "Mood and digestion settle", "Your stomach lining calms and mood swings tend to even out."),
            BodyMilestone(30.days, "Liver fat can start to fall", "Studies suggest a month without alcohol can reduce liver fat and improve blood pressure in regular drinkers."),
            BodyMilestone(90.days, "Sleep, skin and mood keep improving", "Benefits keep building. Many people report better skin, steadier mood and deeper sleep."),
        ),
        cravingTips = listOf(
            "Pour a tall glass of something cold and fizzy, the ritual matters as much as the drink.",
            "Tell someone you trust that you are having a hard moment.",
            "Leave the setting where you usually drink, even for 20 minutes.",
            "Eat something: hunger and thirst often feel like cravings.",
        ),
        safetyNote = "If you drink heavily or every day, do not stop suddenly on your own. Alcohol withdrawal can be dangerous. Please speak to a doctor or a local support service first. If you have seizures, confusion or severe shaking, get emergency help.",
    ),
    /**
     * Sources: NIDA and Virginia Department of Behavioral Health cannabis fact sheet, plus Medical News Today
     * summary of NIDA data (checked online 2026-10-28): withdrawal usually starts within 24 h, peaks in the first
     * week (about days 2 to 6), mostly resolves in about 2 weeks, sleep can stay disturbed for several weeks.
     * UNVERIFIED: longer-term milestones (60 d, 90 d) written from general knowledge, needs review.
     */
    HabitCategory(
        id = "cannabis",
        label = "Cannabis",
        keywords = listOf(
            "cannabis", "marijuana", "marihuana", "weed", "pot smoking", "smoking weed", "smoking pot", "hash", "hashish", "smoking joints",
            "edibles", "thc", "bhang",
            "حشيش", "الحشيش", "بانجو", "البانجو", "ماريجوانا", "حشيشة", "مخدرات حشيش", "سيجارة حشيش", "تدخين الحشيش", "غراس", "جوينت",
            "cannabus", "marijuanna", "hashesh", "bango",
        ),
        whyItMatters = "Regular cannabis use can affect sleep, motivation, memory and mood, and it is common to use it to cope with stress. Stopping gives your sleep and focus room to settle.",
        milestones = listOf(
            BodyMilestone(4.hours, "Effects wear off", "The high typically fades within a few hours, but THC stays in the body much longer."),
            BodyMilestone(12.hours, "First restlessness", "People who use daily may feel restless, edgy or bored within the first day."),
            BodyMilestone(24.hours, "Withdrawal can begin", "Irritability, low appetite, trouble sleeping and cravings typically start within about 24 hours."),
            BodyMilestone(48.hours, "Symptoms building", "Irritability, anxiety, sweating or headaches are common as withdrawal builds."),
            BodyMilestone(72.hours, "Peak withdrawal", "Many people find days 2 to 4 hardest, with poor sleep, vivid dreams, low mood and strong cravings."),
            BodyMilestone(7.days, "Physical symptoms ease", "Appetite typically returns and irritability and anxiety start to settle, though sleep can still be rough."),
            BodyMilestone(14.days, "Most withdrawal has passed", "Most acute symptoms have typically eased by about two weeks. Cravings can still spike with triggers."),
            BodyMilestone(30.days, "Sleep and focus recover", "Many people notice clearer thinking, better memory and more regular sleep, though sleep can take several weeks to settle."),
            BodyMilestone(90.days, "New coping routines", "Stress, boredom and evenings no longer automatically mean cannabis. Motivation and mood often keep improving."),
        ),
        cravingTips = listOf(
            "Expect poor sleep and vivid dreams for a while, keep a regular bedtime and avoid caffeine late.",
            "Move: a walk or workout helps mood, sleep and cravings.",
            "Avoid people and places linked with using, at least early on.",
            "Eat regular meals even if your appetite is low.",
        ),
        safetyNote = "Stopping cannabis is not usually dangerous, but withdrawal can be uncomfortable. If you feel very low, anxious or hopeless, or you also use other substances, please talk to a doctor or a local support service.",
    ),
    /**
     * Sources: general knowledge consistent with NHS and NIDA pages on opioid and painkiller withdrawal.
     * UNVERIFIED: not re-checked online, needs review. No dosing, tapering or sourcing information is given.
     */
    HabitCategory(
        id = "painkillers",
        label = "Tramadol & opioid painkillers",
        keywords = listOf(
            "tramadol", "tramal", "opioid painkillers", "opioids", "opiates", "painkiller addiction", "painkillers", "codeine", "morphine",
            "oxycodone", "hydrocodone", "pain pills", "pain medication dependence", "opioid addiction", "opioid dependence",
            "ترامادول", "الترامادول", "ترامال", "مسكنات", "مسكنات الالم", "مسكنات الألم", "اعتماد على المسكنات", "كودايين", "مورفين", "افيونات", "أفيونات", "حبوب الترامادول",
            "tramadool", "tramdol",
        ),
        whyItMatters = "Opioid painkillers such as tramadol can create physical dependence even when taken as prescribed. Getting off them safely usually works best with professional support.",
        milestones = listOf(
            BodyMilestone(6.hours, "Effects wearing off", "Pain relief fades and some people start to feel uneasy or restless as the medicine leaves their body."),
            BodyMilestone(12.hours, "Early withdrawal may begin", "After regular use, anxiety, sweating, yawning, runny nose or aches can begin within about 12 to 24 hours."),
            BodyMilestone(24.hours, "Symptoms can build", "Restlessness, muscle aches, stomach upset and trouble sleeping are common after regular use."),
            BodyMilestone(48.hours, "Often the roughest stretch", "Symptoms often peak in the first few days. Tramadol can also cause unusual anxiety or tingling in some people."),
            BodyMilestone(72.hours, "Peak, then easing", "Many people notice the worst of the physical symptoms levelling off, though sleep and mood stay shaky."),
            BodyMilestone(7.days, "Physical symptoms improving", "Aches and stomach problems typically ease, but low mood, anxiety and cravings can continue."),
            BodyMilestone(14.days, "Energy slowly returning", "Sleep and appetite gradually improve for many people, though recovery varies widely."),
            BodyMilestone(30.days, "Steadier mood and sleep", "Many people feel more like themselves, though lingering low mood or cravings are common and worth discussing with a clinician."),
            BodyMilestone(90.days, "Building a new routine", "Pain, stress and sleep are handled in other ways. Ongoing professional support helps hold the progress."),
        ),
        cravingTips = listOf(
            "Tell your doctor or a trusted person about the urge today.",
            "Ask a doctor or pharmacist about non-opioid ways to manage pain.",
            "Delay the urge by 10 minutes and do something physical: walk, shower, stretch.",
            "Keep a list of who you can call, including a local support line.",
        ),
        safetyNote = "Do not stop suddenly after heavy or long-term use. Withdrawal from tramadol and other opioids can be unpleasant and, in some cases, risky, and tramadol can sometimes cause seizures. Please speak to a doctor or a local support service before changing anything. Never take more to cope with withdrawal.",
    ),
    /**
     * Sources: general knowledge consistent with NHS and NICE guidance on benzodiazepines and Z-drugs
     * (withdrawal after long use can be serious, needs gradual medical supervision).
     * UNVERIFIED: not re-checked online, needs review. No dosing or tapering schedule is given.
     */
    HabitCategory(
        id = "sleepingpills",
        label = "Sleeping pills & sedatives",
        keywords = listOf(
            "sleeping pills", "sleeping pill", "sleep pills", "sleep aid dependence", "sedatives", "benzodiazepines", "benzos", "xanax",
            "valium", "diazepam", "zolpidem", "ambien", "lorazepam", "tranquilizers", "tranquillisers", "anti anxiety pills",
            "حبوب النوم", "حبوب منومة", "منومات", "مهدئات", "حبوب مهدئة", "اقراص مهدئة", "زاناكس", "فاليوم", "ادوية النوم",
            "sleping pills", "benzos addiction",
        ),
        whyItMatters = "Sleeping pills and sedatives can help short-term, but the body adapts quickly and stopping can bring back sleeplessness and anxiety. A gradual, supervised plan is usually the safest way.",
        milestones = listOf(
            BodyMilestone(6.hours, "Effects wearing off", "The sedating effect fades. If you rely on the pill to fall asleep, you may feel anxious about tonight."),
            BodyMilestone(12.hours, "Rebound feelings", "Anxiety, restlessness or a racing mind can show up when a regular dose is missed."),
            BodyMilestone(24.hours, "First night without", "Sleep is often lighter and more broken at first. This rebound is common and typically eases with time."),
            BodyMilestone(48.hours, "Rebound insomnia", "After regular use, trouble sleeping, sweating, shakiness or irritability can occur and may be strong."),
            BodyMilestone(72.hours, "Symptoms can peak", "Anxiety and sleeplessness often feel worst in the first days, depending on the medicine and length of use."),
            BodyMilestone(7.days, "Sleep slowly resetting", "Many people begin to notice a few better nights, though sleep may still be uneven."),
            BodyMilestone(14.days, "Gradual improvement", "Sleep and anxiety often keep easing, but timelines vary a lot and may be longer after long-term use."),
            BodyMilestone(30.days, "Natural sleep strengthening", "Your own sleep drive recovers for many people. Sleep habits matter more than ever now."),
            BodyMilestone(90.days, "A steadier sleep routine", "Regular sleep and wake times, daylight and calming routines are doing the work the pill used to do."),
        ),
        cravingTips = listOf(
            "Keep the same bedtime and wake time every day, even after a bad night.",
            "Get daylight in the morning and keep caffeine for the first half of the day.",
            "Try a wind-down hour: dim lights, no screens, slow breathing.",
            "Ask a doctor about sleep therapy (CBT-I) as a long-term alternative.",
        ),
        safetyNote = "Do not stop suddenly after regular or long-term use. Sleeping pills and benzodiazepines can cause serious withdrawal, including seizures, and are usually reduced gradually under medical supervision. Please talk to a doctor or local support service first. Never combine sedatives with alcohol.",
    ),
    /**
     * Sources: general knowledge consistent with NIDA pages on stimulants (cocaine, amphetamine-type drugs).
     * UNVERIFIED: not re-checked online, needs review. No dosing, sourcing or use information is given.
     */
    HabitCategory(
        id = "stimulants",
        label = "Stimulants (cocaine, amphetamine)",
        keywords = listOf(
            "cocaine", "cocaine habit", "crack", "amphetamine", "amphetamines", "speed drug", "adderall misuse", "methamphetamine", "meth",
            "captagon", "stimulant misuse", "stimulants", "ecstasy", "mdma", "study drugs",
            "كوكايين", "الكوكايين", "كوكاين", "امفيتامين", "أمفيتامين", "كبتاجون", "الكبتاجون", "منشطات", "حبوب منشطة", "شبو", "الشبو", "ميثامفيتامين",
            "cocain", "amphetamin",
        ),
        whyItMatters = "Stimulant drugs boost energy and mood and then leave a crash, which can keep people using to feel normal. Recovery is possible, and support makes it much easier.",
        milestones = listOf(
            BodyMilestone(4.hours, "The high fades", "Energy and mood typically drop as the drug wears off, and a crash can follow."),
            BodyMilestone(12.hours, "Crash begins", "Exhaustion, low mood, irritability and strong cravings are common after regular use."),
            BodyMilestone(24.hours, "Heavy tiredness", "Many people sleep a lot, feel very hungry and notice a flat or low mood."),
            BodyMilestone(48.hours, "Mood dips", "Anxiety, low mood and cravings are typical in the first days and can be strong."),
            BodyMilestone(72.hours, "Peak crash", "The first few days are often the hardest emotionally. Support from others matters most now."),
            BodyMilestone(7.days, "Sleep and appetite normalise", "Sleep and appetite often start to even out, though mood can still swing."),
            BodyMilestone(14.days, "Energy slowly returns", "Many people feel more energy and focus, though cravings and low mood may come and go."),
            BodyMilestone(30.days, "Thinking clearer", "Memory, focus and mood typically keep improving over weeks, but recovery varies."),
            BodyMilestone(90.days, "Brain reward system recovering", "Pleasure from everyday things often returns more fully. Ongoing support helps protect this progress."),
        ),
        cravingTips = listOf(
            "Reach out to a trusted person or a local support service as soon as a craving builds.",
            "Avoid the people, places and times of day linked to using.",
            "Eat, drink water and sleep regularly, the crash is worse when you are depleted.",
            "Delay 10 minutes and move: walk, shower, exercise.",
        ),
        safetyNote = "Stopping is not usually physically dangerous, but the low mood after stopping can be heavy, and some people feel hopeless. If you have thoughts of harming yourself, seek help right away from a doctor, emergency services or a local crisis line. A doctor or local support service can help you plan safely, especially after heavy or long-term use.",
    ),
)

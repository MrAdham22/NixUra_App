package com.example.reclaimapp.domain.coach.catalog

import com.example.reclaimapp.domain.coach.BodyMilestone
import com.example.reclaimapp.domain.coach.HabitCategory
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Nicotine and tobacco habits (cigarettes, vaping, shisha, smokeless tobacco).
 * General wellbeing information only, not medical advice.
 */
internal val nicotineCategories: List<HabitCategory> = listOf(
    /**
     * Sources: NHS Wales "Quit smoking timeline" and NHS Lanarkshire timeline (checked online 2026-10-28);
     * CDC "Benefits of quitting" (general knowledge, not re-checked).
     * UNVERIFIED: later milestones (14 d, 90 d, 1 y) written from general knowledge, needs review.
     */
    HabitCategory(
        id = "nicotine",
        label = "Cigarettes & smoking",
        keywords = listOf(
            "smoking", "smoke", "smoker", "cigarette", "cigarettes", "cigs", "cigar", "cigars", "tobacco",
            "nicotine", "ciggies", "packs a day", "smoking cigarettes", "chain smoking", "marlboro",
            "تدخين", "التدخين", "سجائر", "سجاير", "سيجارة", "سيجاره", "سجارة", "دخان", "نيكوتين", "تبغ",
            "tadkheen", "sigara", "sagayer", "cigara",
        ),
        whyItMatters = "Nicotine ties cravings to routines such as coffee, stress and breaks. Each craving usually fades within a few minutes.",
        milestones = listOf(
            BodyMilestone(20.minutes, "Heart rate starts to settle", "Within about 20 minutes your pulse and blood pressure typically begin moving back toward normal."),
            BodyMilestone(1.hours, "First cravings", "Many people feel the first urge within an hour or two of the last cigarette. It usually passes in 5 to 10 minutes."),
            BodyMilestone(4.hours, "Nicotine level dropping", "Nicotine in your blood is falling fast. Restlessness, irritability or trouble focusing are common now."),
            BodyMilestone(8.hours, "Carbon monoxide falling", "Carbon monoxide and nicotine in your blood have dropped by about half, so your blood can carry more oxygen."),
            BodyMilestone(12.hours, "Oxygen levels recover", "Carbon monoxide has typically fallen close to normal. Cravings can come in waves around routines like meals and coffee."),
            BodyMilestone(24.hours, "Your first smoke-free day", "Urges and irritability are common today. They are a sign your body is adjusting, not that you are failing."),
            BodyMilestone(48.hours, "Taste and smell wake up", "Most nicotine has left your body, and many people notice food tasting and smelling better."),
            BodyMilestone(72.hours, "Withdrawal tends to peak", "Headaches, mood swings and strong cravings often peak around now. Breathing often feels easier too."),
            BodyMilestone(7.days, "Cravings come less often", "Urges usually arrive less often and fade faster, though routine triggers can still catch you out."),
            BodyMilestone(14.days, "Withdrawal mostly passes", "For many people the physical withdrawal has mostly eased and circulation is improving."),
            BodyMilestone(30.days, "Coughing and breathlessness ease", "Many people notice less coughing, easier breathing and more energy."),
            BodyMilestone(90.days, "Lung function can improve", "Lung function can rise noticeably within the first few months."),
            BodyMilestone(365.days, "Heart risk is roughly halved", "After a year smoke-free, the extra risk of coronary heart disease is typically about half that of a smoker."),
        ),
        cravingTips = listOf(
            "Delay 10 minutes. Cravings rise and fall like a wave, you only have to outlast one.",
            "Take 5 slow breaths: in for 4, out for 6.",
            "Drink a glass of cold water or chew sugar-free gum.",
            "Change your scene: stand up, step outside, wash your hands.",
            "Plan your trigger moments (coffee, after meals, stress) and decide in advance what you will do instead.",
        ),
        safetyNote = "Quitting nicotine is not dangerous, but a doctor, pharmacist or local quit-smoking service can offer free support and options that make it easier.",
    ),
    /**
     * Sources: general knowledge consistent with NHS and CDC e-cigarette pages.
     * UNVERIFIED: written from general knowledge, needs review (vaping evidence is still evolving).
     */
    HabitCategory(
        id = "vaping",
        label = "Vaping & e-cigarettes",
        keywords = listOf(
            "vaping", "vape", "vapes", "vaper", "e-cigarette", "e cigarette", "ecigarette", "e-cig", "ecig",
            "juul", "puff bar", "disposable vape", "nicotine pod", "pod vape", "vape pen", "elf bar",
            "فيب", "الفيب", "فيبينج", "سيجارة الكترونية", "سيجارة إلكترونية", "سجائر الكترونية", "فيب الكتروني", "بخار",
            "fayb", "vapp",
        ),
        whyItMatters = "Vapes deliver nicotine quickly and are easy to use all day, so the habit can become constant. Breaking the all-day pattern is usually the hardest part.",
        milestones = listOf(
            BodyMilestone(20.minutes, "Nicotine starts to fall", "Nicotine levels begin dropping soon after the last puff, so the first urge can come quickly for people who vape often."),
            BodyMilestone(1.hours, "First cravings", "Many people feel the first urge within an hour or two. It typically passes in a few minutes."),
            BodyMilestone(4.hours, "Restless and edgy", "Irritability, restlessness or trouble concentrating are common as nicotine levels drop."),
            BodyMilestone(12.hours, "Hand-to-mouth habit stands out", "Part of the pull is the routine itself: the gesture, the breaks, the flavour. Expect urges tied to those moments."),
            BodyMilestone(24.hours, "Your first vape-free day", "Cravings, irritability and a short temper are common today and usually ease over the coming days."),
            BodyMilestone(72.hours, "Withdrawal tends to peak", "Many people find days 2 to 4 the toughest, with headaches, low mood and strong urges."),
            BodyMilestone(7.days, "Urges space out", "Cravings usually come less often and pass faster, though triggers like stress or friends who vape can bring them back."),
            BodyMilestone(14.days, "Withdrawal mostly eases", "For many people the physical symptoms have mostly settled, and sleep and mood begin to steady."),
            BodyMilestone(30.days, "Breathing and energy improve", "Many people report easier breathing, less throat irritation and steadier energy."),
            BodyMilestone(90.days, "A new routine", "Breaks, meals and stress no longer automatically mean reaching for the vape."),
        ),
        cravingTips = listOf(
            "Delay 10 minutes and notice the urge rise and fall.",
            "Keep your hands and mouth busy: sugar-free gum, a straw, a stress ball.",
            "Get rid of spare pods and devices so a craving needs effort to act on.",
            "Tell a friend you are quitting so they do not offer.",
        ),
        safetyNote = "Quitting nicotine is not dangerous, but a doctor, pharmacist or local quit service can offer support that doubles as a safety net if cravings feel overwhelming.",
    ),
    /**
     * Sources: general knowledge consistent with WHO and CDC hookah pages (shisha delivers nicotine and smoke).
     * UNVERIFIED: written from general knowledge, needs review.
     */
    HabitCategory(
        id = "shisha",
        label = "Shisha & hookah",
        keywords = listOf(
            "shisha", "sheesha", "hookah", "hookah smoking", "waterpipe", "water pipe", "narghile", "argileh",
            "hubbly bubbly", "goza", "shisha session", "smoking shisha",
            "شيشة", "الشيشة", "شيشه", "شيشا", "معسل", "نرجيلة", "أرجيلة", "ارجيلة", "جوزة", "حجر شيشة", "تدخين الشيشة",
            "shesha", "arguila",
        ),
        whyItMatters = "A shisha session is often social and long, so the smoke exposure can be heavy even if it feels casual. Cutting it out means changing the social ritual as well as the nicotine.",
        milestones = listOf(
            BodyMilestone(1.hours, "Nicotine starts dropping", "Nicotine begins to leave your system. If shisha is a social habit the urge may be about the setting more than the nicotine."),
            BodyMilestone(4.hours, "Smoke exposure falling", "Carbon monoxide from the coals and smoke starts clearing from your blood."),
            BodyMilestone(12.hours, "Oxygen recovers", "Carbon monoxide typically falls toward normal, so your blood can carry oxygen better."),
            BodyMilestone(24.hours, "First shisha-free day", "Many people feel urges mostly around cafes, friends and evenings. Plan those moments."),
            BodyMilestone(48.hours, "Taste and smell improve", "Most nicotine has left your body and many people notice food tasting better."),
            BodyMilestone(72.hours, "Cravings can peak", "Irritability and strong urges often peak around now, then ease."),
            BodyMilestone(7.days, "Breathing feels easier", "Coughing and a heavy chest often start to improve."),
            BodyMilestone(14.days, "New social routine", "Meeting friends without a pipe starts to feel normal."),
            BodyMilestone(30.days, "Lungs recovering", "Many people notice less coughing and better stamina on stairs or walks."),
            BodyMilestone(90.days, "The habit loop weakens", "Cafes and evenings no longer automatically mean shisha."),
        ),
        cravingTips = listOf(
            "Suggest a different plan with friends: tea, a walk, a game, a meal.",
            "Order a drink you enjoy so your hands and mouth have something to do.",
            "Decide ahead what you will say if someone offers a mouthpiece.",
            "Delay 10 minutes before deciding.",
        ),
        safetyNote = "Quitting nicotine is not dangerous, but a doctor or local quit-smoking service can offer support if cravings feel hard to manage.",
    ),
    /**
     * Sources: general knowledge consistent with NHS and NCI pages on smokeless tobacco.
     * UNVERIFIED: written from general knowledge, needs review.
     */
    HabitCategory(
        id = "smokeless",
        label = "Chewing tobacco & snus",
        keywords = listOf(
            "chewing tobacco", "chew tobacco", "chewing tobbaco", "dip tobacco", "snus", "snuff", "nicotine pouch",
            "nicotine pouches", "zyn", "smokeless tobacco", "spit tobacco", "paan with tobacco", "betel quid", "gutka", "gutkha",
            "تبغ المضغ", "مضغ التبغ", "تبغ للمضغ", "سعوط", "شمة", "نشوق", "تمباك", "زين",
        ),
        whyItMatters = "Smokeless tobacco and pouches hold nicotine in the mouth for long periods, so it can keep your nicotine level high all day. Stopping gives your mouth and gums time to recover.",
        milestones = listOf(
            BodyMilestone(1.hours, "First urges", "Many people feel the first urge within an hour or two of the last pouch or chew."),
            BodyMilestone(4.hours, "Nicotine falling", "Irritability, restlessness and poor focus are common as nicotine drops."),
            BodyMilestone(12.hours, "Mouth habit stands out", "Part of the pull is the routine of tucking something in your lip or cheek. Expect urges at set times of day."),
            BodyMilestone(24.hours, "First nicotine-free day", "Cravings and a short temper are common. They usually ease over the next few days."),
            BodyMilestone(48.hours, "Withdrawal building", "Headaches, low mood and strong cravings are typical around days 2 to 4."),
            BodyMilestone(72.hours, "Peak withdrawal", "Most nicotine has left your body and withdrawal often peaks, then starts to ease."),
            BodyMilestone(7.days, "Urges space out", "Cravings come less often. Your mouth and gums may start to feel less irritated."),
            BodyMilestone(14.days, "Gums start to heal", "Soreness or irritation in the mouth often begins to settle, though healing time varies."),
            BodyMilestone(30.days, "New routines", "Work breaks, driving and meals no longer automatically trigger the urge for many people."),
            BodyMilestone(90.days, "Habit loop weakens", "Triggers still appear, but with much less pull."),
        ),
        cravingTips = listOf(
            "Keep something else for your mouth: sugar-free gum, sunflower seeds, a toothpick.",
            "Delay 10 minutes and notice the urge fade.",
            "Remove stashes from your car, bag and workplace.",
            "Rinse with water when the urge hits.",
        ),
        safetyNote = "Quitting nicotine is not dangerous, but a doctor, dentist or local quit service can offer support. See a dentist if you notice white patches or sores in your mouth that do not heal.",
    ),
)

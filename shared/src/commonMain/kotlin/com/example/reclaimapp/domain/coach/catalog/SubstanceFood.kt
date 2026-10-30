package com.example.reclaimapp.domain.coach.catalog

import com.example.reclaimapp.domain.coach.BodyMilestone
import com.example.reclaimapp.domain.coach.HabitCategory
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Food habits (sweets, fast food, late-night eating, overeating, salt, fried food).
 * General wellbeing information only, not medical advice. Not weight-loss or diet advice.
 */
internal val foodCategories: List<HabitCategory> = listOf(
    /**
     * Sources: general knowledge consistent with WHO free-sugars guideline and NHS "Sugar: the facts".
     * UNVERIFIED: written from general knowledge, needs review.
     */
    HabitCategory(
        id = "sugar",
        label = "Sweets & sugar",
        keywords = listOf(
            "sugar", "sweets", "candy", "candies", "dessert", "desserts", "chocolate", "chocolates", "cookies", "cakes",
            "sugar addiction", "sweet tooth", "sugar cravings", "eating sweets", "ice cream", "biscuits", "pastries", "baklava",
            "سكر", "السكر", "حلويات", "الحلويات", "حلو", "شوكولاتة", "شوكولاته", "شيكولاته", "بسكويت", "كيك", "ايس كريم", "كنافة", "بسبوسة", "سكريات",
            "helw", "halawiyat", "sweeets",
        ),
        whyItMatters = "Sugar spikes and crashes drive more cravings. A few days without it can break that loop.",
        milestones = listOf(
            BodyMilestone(2.hours, "Sugar high wears off", "Blood sugar from your last sweet treat has been absorbed. A dip and a new craving are common."),
            BodyMilestone(6.hours, "First strong cravings", "Many people feel the urge hardest in the afternoon or evening or after meals. It typically fades in 15 to 20 minutes."),
            BodyMilestone(12.hours, "Mood and energy wobble", "Some people feel a little tired or irritable without their usual sugar hit."),
            BodyMilestone(24.hours, "Blood sugar swings smooth out", "Without sugar spikes, energy dips between meals start to flatten."),
            BodyMilestone(48.hours, "Cravings can be loud", "Headaches, tiredness and strong cravings are possible but usually mild."),
            BodyMilestone(72.hours, "Cravings peak, then ease", "Day 3 is often the hardest. After that, cravings usually soften."),
            BodyMilestone(7.days, "Taste starts to reset", "Your taste buds begin adapting to less sweetness."),
            BodyMilestone(14.days, "Natural foods taste sweeter", "Fruit and plain foods taste noticeably better."),
            BodyMilestone(30.days, "Steadier energy", "Many people report steadier energy, better focus and fewer afternoon crashes."),
            BodyMilestone(90.days, "Sweets are a treat again", "Sugar stops being a daily need and becomes an occasional choice."),
        ),
        cravingTips = listOf(
            "Eat protein or fruit first, it steadies blood sugar.",
            "Brush your teeth, sweet cravings often drop right away.",
            "Wait 10 minutes and drink a glass of water before deciding.",
            "Keep sweets out of sight or out of the house.",
        ),
    ),
    /**
     * Sources: general knowledge consistent with NHS and WHO guidance on ultra-processed food.
     * UNVERIFIED: written from general knowledge, needs review.
     */
    HabitCategory(
        id = "junkfood",
        label = "Fast food & junk food",
        keywords = listOf(
            "junk food", "junk", "fast food", "fastfood", "takeaway", "takeout", "burger", "burgers", "pizza", "fries", "chips",
            "mcdonalds", "kfc", "snacking", "snacks", "processed food", "ultra processed", "ordering food", "delivery food",
            "وجبات سريعة", "وجبة سريعة", "اكل سريع", "أكل سريع", "اكل جاهز", "تسالي", "برجر", "بيتزا", "ماكدونالدز", "كنتاكي", "بطاطس مقلية", "اكل الشارع", "دليفري", "اكل بره",
            "junkfood", "fastfod",
        ),
        whyItMatters = "Highly processed food is built to be craved. Resetting your routine resets what you crave.",
        milestones = listOf(
            BodyMilestone(2.hours, "Heavy meal passes", "After a rich meal many people feel a crash or sluggishness a couple of hours later. Lighter food avoids that."),
            BodyMilestone(6.hours, "Hunger reset", "Real hunger starts to show as a steady signal rather than an urge for something salty or sweet."),
            BodyMilestone(12.hours, "Evening cravings", "Cravings tied to habit, such as screens or tiredness, often show up now. They typically pass."),
            BodyMilestone(24.hours, "Cravings start resetting", "Your first full day gives your appetite a chance to settle."),
            BodyMilestone(72.hours, "Energy dips ease", "Heavy meals often cause crashes, lighter eating steadies the day."),
            BodyMilestone(7.days, "Digestion feels lighter", "Many people feel less bloated and sleep better after a week."),
            BodyMilestone(14.days, "Taste preferences shift", "Whole foods start to taste more satisfying."),
            BodyMilestone(30.days, "Energy and mood stabilise", "Steadier energy and mood are common after a month."),
            BodyMilestone(90.days, "A new food routine", "Planned meals and a stocked kitchen now do most of the work for you."),
        ),
        cravingTips = listOf(
            "Drink water and wait 10 minutes, thirst and boredom disguise themselves as hunger.",
            "Keep a ready-to-eat healthy snack within reach.",
            "Brush your teeth after dinner to close the kitchen.",
            "Delete the delivery app or move it off your home screen.",
        ),
    ),
    /**
     * Sources: general knowledge consistent with NHS and Mayo Clinic notes on eating late and sleep.
     * UNVERIFIED: written from general knowledge, needs review.
     */
    HabitCategory(
        id = "latenighteating",
        label = "Late-night eating",
        keywords = listOf(
            "late night eating", "late-night eating", "eating at night", "midnight snacking", "midnight snack", "night snacking",
            "eating before bed", "night eating", "snacking at night", "eating late", "late night snacks", "midnight munchies",
            "اكل بالليل", "أكل بالليل", "الاكل بالليل", "اكل متأخر", "تسالي بالليل", "اكل بعد نص الليل", "اكل قبل النوم", "الاكل في وقت متاخر", "سناكس بالليل",
            "midnite snack", "late nite eating",
        ),
        whyItMatters = "Eating late can make it harder to fall asleep and can mean extra calories you are not really hungry for. It is often driven by boredom, tiredness or screens rather than hunger.",
        milestones = listOf(
            BodyMilestone(1.hours, "Urge to snack", "The evening urge is often about habit or boredom. It typically peaks and fades within 15 to 20 minutes."),
            BodyMilestone(4.hours, "Bedtime without a snack", "Many people fall asleep fine. If hunger is real, a small planned snack earlier is better than grazing late."),
            BodyMilestone(8.hours, "Morning check-in", "Waking up, many people notice less heaviness or reflux and better morning appetite."),
            BodyMilestone(24.hours, "One night closed", "A kitchen that closes at a set time starts to feel like a rule rather than a battle."),
            BodyMilestone(72.hours, "Cravings ease", "The evening urge tends to weaken as your body gets used to the new pattern."),
            BodyMilestone(7.days, "Sleep and mornings improve", "Many people sleep more soundly and feel less sluggish in the morning."),
            BodyMilestone(14.days, "Daytime eating improves", "With late snacks gone, many people eat more regular meals during the day."),
            BodyMilestone(30.days, "New evening routine", "Tea, a shower or a book now fills the slot where snacking used to be."),
        ),
        cravingTips = listOf(
            "Brush your teeth right after dinner to signal that the kitchen is closed.",
            "Make a warm unsweetened drink and take it away from the kitchen.",
            "Eat a proper dinner with protein so you are not truly hungry later.",
            "Notice whether you are bored, tired or on a screen when the urge hits.",
        ),
    ),
    /**
     * Sources: general knowledge consistent with NHS "Binge eating" and "Eating habits" pages.
     * UNVERIFIED: written from general knowledge, needs review.
     */
    HabitCategory(
        id = "overeating",
        label = "Overeating",
        keywords = listOf(
            "overeating", "over eating", "overeat", "eating too much", "eating too fast", "stuffing myself", "binge eating", "binging on food",
            "emotional eating", "stress eating", "eating when bored", "second helpings", "big portions", "eating until full", "comfort eating",
            "الاكل الزائد", "أكل زيادة", "اكل كتير", "الإفراط في الأكل", "الافراط في الاكل", "الاكل العاطفي", "اكل من الزهق", "اكل بسرعة", "الشراهة", "شراهة في الاكل",
            "overeatin", "overeeting",
        ),
        whyItMatters = "Eating more than you need is often about emotions, habit or speed rather than hunger. Slowing down and noticing fullness can make a big difference.",
        milestones = listOf(
            BodyMilestone(1.hours, "Fullness signals", "Your body's fullness signal takes about 20 minutes to register. Eating slower often means eating less."),
            BodyMilestone(4.hours, "Real hunger returns", "Hunger tends to return on its own schedule, which helps you tell it from emotional urges."),
            BodyMilestone(12.hours, "A lighter evening", "Many people sleep better and feel less heavy after a more moderate dinner."),
            BodyMilestone(24.hours, "One day of listening", "A single day of eating when hungry and stopping when comfortable is real practice."),
            BodyMilestone(72.hours, "Urges become easier to spot", "You start to notice whether an urge is hunger, boredom, stress or habit."),
            BodyMilestone(7.days, "Digestion calms", "Many people feel less bloated and sluggish after a week of smaller portions."),
            BodyMilestone(14.days, "Portion sense returns", "Comfortable fullness starts to feel natural, and stopping gets easier."),
            BodyMilestone(30.days, "A steadier relationship with food", "Many people report more energy and less guilt around meals."),
        ),
        cravingTips = listOf(
            "Pause halfway through a meal and ask how hungry you still are.",
            "Eat slowly, put the fork down between bites.",
            "Serve one plate and put the rest away before you sit down.",
            "When stress eating, take 5 slow breaths or a short walk first.",
        ),
        safetyNote = "If you feel out of control around food, binge often or feel intense guilt or shame afterward, please talk to a doctor or a counsellor. Support works and you do not have to manage this alone.",
    ),
    /**
     * Sources: general knowledge consistent with WHO salt-reduction fact sheet and NHS "Salt: the facts"
     * (taste adapts within weeks).
     * UNVERIFIED: written from general knowledge, needs review.
     */
    HabitCategory(
        id = "salt",
        label = "Too much salt",
        keywords = listOf(
            "salt", "too much salt", "excess salt", "salty food", "salty foods", "salty snacks", "adding salt", "salt intake",
            "sodium", "high sodium", "salt shaker", "extra salt", "instant noodles",
            "ملح", "الملح", "ملح زيادة", "اكل مالح", "أكل مالح", "مالح", "الصوديوم", "كتر الملح", "اندومي", "سناكس مالحة",
            "salty fod", "to much salt",
        ),
        whyItMatters = "A lot of salt over time can raise blood pressure. Your taste for salt adapts quickly, so cutting back is easier than it sounds.",
        milestones = listOf(
            BodyMilestone(6.hours, "Food tastes bland", "Meals may taste flatter at first. That is your taste buds, not the food."),
            BodyMilestone(24.hours, "Less thirst and puffiness", "After a lower-salt day some people feel less thirsty or puffy, as the body holds a little less water."),
            BodyMilestone(72.hours, "Taste begins to adapt", "Your taste for salt typically starts adjusting within days."),
            BodyMilestone(7.days, "Flavours become clearer", "Herbs, lemon and spices stand out more as salt stops masking them."),
            BodyMilestone(14.days, "Salty food tastes too salty", "Many people find their old favourites now taste noticeably saltier."),
            BodyMilestone(30.days, "New taste baseline", "Taste preference has typically shifted, and lower-salt food feels normal."),
            BodyMilestone(90.days, "Lasting benefit", "Over months, eating less salt can help lower blood pressure in many people, especially if it was high."),
        ),
        cravingTips = listOf(
            "Take the salt shaker off the table and taste before you add.",
            "Season with lemon, garlic, herbs, cumin or pepper instead.",
            "Check labels on bread, sauces and instant foods, they hide a lot of salt.",
            "Cut back a little at a time, taste adapts in a couple of weeks.",
        ),
        safetyNote = "If you have high blood pressure, kidney or heart problems, or take diuretics, ask your doctor before changing your salt intake.",
    ),
    /**
     * Sources: general knowledge consistent with NHS and AHA guidance on fried and fatty foods.
     * UNVERIFIED: written from general knowledge, needs review.
     */
    HabitCategory(
        id = "friedfood",
        label = "Fried food",
        keywords = listOf(
            "fried food", "fried foods", "deep fried", "deep-fried", "fried chicken", "french fries", "greasy food", "oily food",
            "fried snacks", "frying", "fried stuff", "fish and chips", "tempura",
            "اكل مقلي", "أكل مقلي", "المقليات", "مقليات", "مقلي", "بطاطس محمرة", "فراخ مقلية", "اكل دهني", "اكل مشبع بالزيت", "طعمية", "فلافل", "سمبوسة",
            "fried fud", "firied food",
        ),
        whyItMatters = "Fried food is heavy in fat and calories and can leave you sluggish. Swapping the cooking method often changes how you feel after meals.",
        milestones = listOf(
            BodyMilestone(2.hours, "Lighter after meals", "Without a greasy meal you may feel less heavy and sleepy a couple of hours later."),
            BodyMilestone(6.hours, "First craving", "The urge is often for crunch and salt. It typically fades in 15 to 20 minutes."),
            BodyMilestone(24.hours, "Digestion calms", "Many people feel less bloated or acidic after a day without heavy fried food."),
            BodyMilestone(72.hours, "Cravings ease", "The pull of greasy food tends to loosen as your taste adjusts."),
            BodyMilestone(7.days, "Lighter feeling", "Many people report more energy after meals and less heartburn."),
            BodyMilestone(14.days, "Taste shifts", "Grilled, baked and roasted food starts to taste more satisfying."),
            BodyMilestone(30.days, "Steadier energy", "Meals stop causing as many energy crashes for many people."),
            BodyMilestone(90.days, "New cooking habits", "Baking, grilling or air-frying is now your default."),
        ),
        cravingTips = listOf(
            "Try baking or air-frying for the crunch with less oil.",
            "Keep fried food for a planned treat, not a daily default.",
            "Have a crunchy swap ready: nuts, roasted chickpeas, raw veg.",
            "Eat a balanced meal with protein and fibre before you go out.",
        ),
    ),
)

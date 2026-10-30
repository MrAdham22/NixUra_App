package com.example.reclaimapp.domain.coach.catalog

import com.example.reclaimapp.domain.coach.BodyMilestone
import com.example.reclaimapp.domain.coach.HabitCategory
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Caffeinated and sugary drink habits (coffee, energy drinks, heavy tea, soda, sugar in drinks).
 * General wellbeing information only, not medical advice.
 */
internal val drinkCategories: List<HabitCategory> = listOf(
    /**
     * Sources: general knowledge consistent with Mayo Clinic and Johns Hopkins caffeine-withdrawal summaries
     * (onset about 12 to 24 h, peak about 20 to 51 h, lasts about 2 to 9 days).
     * UNVERIFIED: not re-checked online, needs review.
     */
    HabitCategory(
        id = "caffeine",
        label = "Coffee & caffeine",
        keywords = listOf(
            "caffeine", "coffee", "coffe", "cofee", "espresso", "latte", "cappuccino", "americano", "nescafe",
            "instant coffee", "turkish coffee", "cups of coffee", "coffee addiction", "morning coffee", "cold brew",
            "قهوة", "القهوة", "قهوه", "كافيين", "نسكافيه", "نسكافية", "اسبريسو", "قهوة تركي", "قهوة فرنساوي",
            "ahwa", "ahwah", "kahwa",
        ),
        whyItMatters = "Caffeine borrows energy from later in the day. Stepping back lets your natural energy rhythm return.",
        milestones = listOf(
            BodyMilestone(2.hours, "Caffeine still active", "Caffeine from your last cup is still in your system. The half-life is about 5 hours, so you may feel fine for now."),
            BodyMilestone(6.hours, "Level halving", "About half the caffeine has typically cleared. Some people already feel a mild dip in energy or focus."),
            BodyMilestone(12.hours, "Withdrawal can begin", "Many people feel tired, foggy or get a mild headache 12 to 24 hours after the last cup. It passes."),
            BodyMilestone(24.hours, "Headache and fatigue window", "Headaches, sleepiness and a low mood are common today, especially for people who drink several cups a day."),
            BodyMilestone(48.hours, "Peak withdrawal", "Symptoms typically peak around days 2 to 3, then start to fade."),
            BodyMilestone(72.hours, "Easing begins", "Most people notice headaches and fog starting to lift, though tiredness can linger."),
            BodyMilestone(7.days, "Symptoms fade", "For many people the worst is over after about a week, though withdrawal can last up to 9 days."),
            BodyMilestone(14.days, "A new energy baseline", "Energy now comes more from sleep, food and movement than from caffeine."),
            BodyMilestone(30.days, "Sleep and calm improve", "Many people sleep more deeply and feel steadier without caffeine."),
        ),
        cravingTips = listOf(
            "Drink water first, mild dehydration feels like tiredness.",
            "Take a 5-minute walk in daylight.",
            "Try a warm herbal tea so your hands and routine still get their ritual.",
            "Cut down gradually if stopping at once gives you strong headaches.",
        ),
        safetyNote = "Stopping caffeine is not dangerous for most people. If headaches are severe, or you are pregnant or have a heart condition, ask a doctor or pharmacist.",
    ),
    /**
     * Sources: general knowledge consistent with Mayo Clinic, FDA and NHS notes on caffeine and sugar in energy drinks.
     * UNVERIFIED: written from general knowledge, needs review.
     */
    HabitCategory(
        id = "energydrinks",
        label = "Energy drinks",
        keywords = listOf(
            "energy drink", "energy drinks", "red bull", "redbull", "monster energy", "monster drink", "rockstar energy",
            "energy can", "pre workout drink", "power horse", "v energy", "cans of energy",
            "مشروبات الطاقة", "مشروب طاقة", "مشروب الطاقة", "ريد بول", "ريدبول", "مونستر", "مشروبات طاقه", "انرجي درينك",
            "energy drinkk", "enrgy drink",
        ),
        whyItMatters = "Energy drinks combine a lot of caffeine and often a lot of sugar, which can leave you wired and then crashing. Cutting back usually helps sleep and steadiness.",
        milestones = listOf(
            BodyMilestone(2.hours, "Still wired", "Caffeine from your last can is still active, so you may not feel any change yet."),
            BodyMilestone(6.hours, "Crash window", "Without another can, tiredness or a dip in focus is common as caffeine and sugar fade."),
            BodyMilestone(12.hours, "Withdrawal can start", "Many people feel foggy or get a headache 12 to 24 hours after the last can."),
            BodyMilestone(24.hours, "Headache and tiredness", "Fatigue, headache and low mood are common today, especially for daily drinkers."),
            BodyMilestone(48.hours, "Peak withdrawal", "Symptoms typically peak around days 2 to 3, then start to ease."),
            BodyMilestone(72.hours, "Fog begins to lift", "Many people notice their head clearing and the crashes becoming less intense."),
            BodyMilestone(7.days, "Steadier energy", "Without sugar spikes and caffeine peaks, energy typically feels flatter but more even. Sleep often starts to improve."),
            BodyMilestone(14.days, "Heart and sleep settle", "Many people notice fewer jitters and palpitations, and falling asleep gets easier."),
            BodyMilestone(30.days, "A new baseline", "Energy comes more from sleep, food and movement. Cravings are much weaker."),
        ),
        cravingTips = listOf(
            "Swap in cold sparkling water with lemon for the fizz and the ritual.",
            "Eat something with protein, tiredness is often hunger.",
            "Go for a 10-minute walk outside in daylight.",
            "Do not keep cans at home or at your desk.",
        ),
        safetyNote = "Stopping is not dangerous for most people. If you have heart palpitations, chest pain or fainting, see a doctor promptly.",
    ),
    /**
     * Sources: general knowledge consistent with NHS notes on caffeine in tea.
     * UNVERIFIED: written from general knowledge, needs review.
     */
    HabitCategory(
        id = "tea",
        label = "Heavy tea drinking",
        keywords = listOf(
            "tea", "too much tea", "black tea", "strong tea", "cups of tea", "tea addiction", "chai", "drinking tea",
            "tea all day", "tea with sugar", "green tea habit",
            "شاي", "الشاي", "شاى", "شاي كتير", "شاي تقيل", "اكواب شاي", "شاي بالنعناع", "شاي اسود", "كوباية شاي",
            "shai", "shay", "chay",
        ),
        whyItMatters = "Tea has less caffeine than coffee, but many cups a day, strong brews or tea late in the day can still affect sleep and appetite. It can also come with a lot of added sugar.",
        milestones = listOf(
            BodyMilestone(2.hours, "Routine gap", "The first urge is often about the break and the warm cup rather than caffeine."),
            BodyMilestone(6.hours, "Caffeine level falling", "About half the caffeine from your last cup has typically cleared."),
            BodyMilestone(12.hours, "Mild withdrawal possible", "If you drink a lot of strong tea you may feel a little tired or have a mild headache."),
            BodyMilestone(24.hours, "First day with less tea", "Many people feel the habit more than the chemistry. Plan replacement drinks for your usual tea times."),
            BodyMilestone(48.hours, "Withdrawal at its worst", "Mild headaches or tiredness, if they appear, typically peak around days 2 to 3."),
            BodyMilestone(7.days, "Symptoms fade", "Headaches and fog usually pass. Sleep and appetite often start to feel steadier."),
            BodyMilestone(14.days, "Appetite and sleep settle", "Many people notice better appetite for meals and easier sleep."),
            BodyMilestone(30.days, "New drink habits", "Water, herbal tea or a smaller cup now fills the old slot in your routine."),
        ),
        cravingTips = listOf(
            "Switch to herbal tea, mint or warm water with lemon in the same cup.",
            "Make one smaller cup instead of refilling the pot.",
            "Have tea only before midday if it affects your sleep.",
            "Drink a glass of water first and wait 10 minutes.",
        ),
    ),
    /**
     * Sources: general knowledge consistent with WHO sugar guideline (2015) and CDC sugary-drink pages.
     * UNVERIFIED: written from general knowledge, needs review.
     */
    HabitCategory(
        id = "softdrinks",
        label = "Soda & sugary drinks",
        keywords = listOf(
            "soda", "sodas", "soft drink", "soft drinks", "fizzy drink", "fizzy drinks", "cola", "coca cola", "pepsi",
            "sprite", "fanta", "sugary drink", "sugary drinks", "sweet drinks", "bottled juice", "sugary juice",
            "مشروبات غازية", "مشروب غازي", "غازيات", "كولا", "كوكاكولا", "بيبسي", "سفن اب", "ميرندا", "عصاير معلبة", "مياه غازية",
            "pepsi addiction", "sodaa",
        ),
        whyItMatters = "Sugary drinks deliver a lot of sugar fast without filling you up, so they are an easy way to take in extra calories. Swapping them out is one of the simplest changes with a big payoff.",
        milestones = listOf(
            BodyMilestone(2.hours, "Sugar spike fades", "Blood sugar from your last drink has been absorbed. An energy dip and a new craving are common."),
            BodyMilestone(6.hours, "First real cravings", "Many people feel the urge for something sweet or fizzy, often tied to meals or breaks."),
            BodyMilestone(12.hours, "Habit moment stands out", "The urge is often tied to a routine such as lunch, driving or screen time rather than real thirst."),
            BodyMilestone(24.hours, "Energy swings smooth out", "Without the spike and crash from sugary drinks, energy between meals starts to flatten."),
            BodyMilestone(72.hours, "Cravings peak, then ease", "Day 3 is often the hardest for people who drank soda daily. Caffeine from cola may add a mild headache."),
            BodyMilestone(7.days, "Taste starts to reset", "Plain water and unsweetened drinks start to taste better, and sweet drinks may taste too sweet."),
            BodyMilestone(14.days, "Less bloating", "Many people feel less bloated and notice fewer cravings after meals."),
            BodyMilestone(30.days, "Steadier energy and teeth", "Many people report steadier energy, and your teeth get a break from regular sugar and acid."),
            BodyMilestone(90.days, "A new default drink", "Water or sparkling water is now your normal. Soda becomes an occasional choice, not a reflex."),
        ),
        cravingTips = listOf(
            "Swap to sparkling water with lemon, lime or a splash of juice for the fizz.",
            "Drink a glass of cold water first and wait 10 minutes.",
            "Keep soda out of the house so it needs effort to get.",
            "Pair meals with a set drink, like water or unsweetened tea.",
        ),
    ),
    /**
     * Sources: general knowledge consistent with WHO sugar guideline and NHS "Sugar: the facts".
     * UNVERIFIED: written from general knowledge, needs review.
     */
    HabitCategory(
        id = "sugarindrinks",
        label = "Sugar in tea & coffee",
        keywords = listOf(
            "sugar in tea", "sugar in my tea", "sugar in coffee", "sugar in my coffee", "sweetened coffee", "sweetened tea",
            "sweet tea", "sweet coffee", "spoons of sugar", "teaspoons of sugar", "added sugar in drinks", "sugar in drinks",
            "sugar in my drinks", "sweet latte", "flavoured coffee", "syrup in coffee",
            "سكر في الشاي", "سكر في القهوة", "سكر في المشروبات", "ملاعق سكر", "شاي بسكر", "قهوة بسكر", "شاي بالسكر", "شاي سكر زيادة",
            "sokkar fel shai", "sukar fi shai",
        ),
        whyItMatters = "Several spoons of sugar a day in hot drinks add up quickly and keep your taste for sweetness high. Reducing it gradually is easy to do and often barely noticeable after a couple of weeks.",
        milestones = listOf(
            BodyMilestone(1.hours, "First unsweetened cup", "Drinks may taste flat or bitter at first. That is your taste buds adjusting."),
            BodyMilestone(4.hours, "Small energy dip", "Some people feel a short dip or sweet craving without the sugar boost, which usually passes."),
            BodyMilestone(24.hours, "Cravings come in waves", "The urge is mostly habit. Many people find it fades in 10 to 15 minutes."),
            BodyMilestone(72.hours, "Taste adapting", "Your taste buds begin adjusting and drinks start to taste less bland."),
            BodyMilestone(7.days, "Sweetness resets", "Many people find their old sweetened drink now tastes too sweet."),
            BodyMilestone(14.days, "Flavour comes through", "You start to taste the tea or coffee itself rather than just sweetness."),
            BodyMilestone(30.days, "Fewer sugar swings", "Cutting several spoons a day removes a regular source of added sugar, and many people notice steadier energy."),
            BodyMilestone(90.days, "New normal", "Unsweetened or lightly sweetened drinks are now what feels normal."),
        ),
        cravingTips = listOf(
            "Reduce by half a spoon every few days instead of stopping at once.",
            "Add cinnamon, cardamom, mint or a slice of lemon for flavour.",
            "Use a smaller spoon or a smaller cup.",
            "Pair the drink with a piece of fruit if you want something sweet.",
        ),
    ),
)

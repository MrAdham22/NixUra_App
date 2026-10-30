package com.example.reclaimapp.domain.recognition

/**
 * Bundled lexicon of habits people usually want to BUILD (English + Arabic), plus a few neutral phrases
 * that must never be read as a quit-habit ("sweet potato", "game plan").
 */
internal object GoodHabitLexicon {

    class GoodEntry(val id: String, val keywords: List<String>)

    val entries: List<GoodEntry> = listOf(
        GoodEntry("water", listOf("water", "hydrate", "hydration", "drink water", "drinking water", "glass of water", "ماء", "مياه", "شرب الماء", "شرب ماء", "شرب المياه")),
        GoodEntry("workout", listOf("workout", "workouts", "work out", "exercise", "exercises", "exercising", "excercise", "excersize", "excersise", "gym", "training", "train", "fitness", "cardio", "رياضه", "تمرين", "تمارين", "جيم", "نادي رياضي", "تمرن")),
        GoodEntry("walk", listOf("walk", "walking", "steps", "10k steps", "stroll", "مشي", "المشي", "امشي", "تمشيه")),
        GoodEntry("run", listOf("run", "running", "jog", "jogging", "جري", "الجري", "ركض", "اجري")),
        GoodEntry("stretch", listOf("stretch", "stretching", "yoga", "pilates", "mobility", "تمدد", "اطالات", "يوجا", "تمارين اطاله")),
        GoodEntry("read", listOf("read", "reading", "book", "books", "قراءه", "اقرا", "قرايه", "كتاب", "كتب", "القراءه")),
        GoodEntry("meditate", listOf("meditate", "meditation", "mindfulness", "breathing exercise", "deep breathing", "تامل", "تأمل", "تنفس", "التنفس")),
        GoodEntry("pray", listOf("pray", "prayer", "prayers", "salah", "salat", "quran", "dhikr", "zikr", "athkar", "azkar", "adhkar", "صلاه", "الصلاه", "صلاة", "قران", "القران", "قرآن", "ورد", "اذكار", "الاذكار", "ذكر", "استغفار")),
        GoodEntry("journal", listOf("journal", "journaling", "diary", "write diary", "gratitude journal", "يوميات", "مذكرات", "كتابه يوميات")),
        GoodEntry("study", listOf("study", "studying", "homework", "revision", "revise", "learn", "learning", "course", "مذاكره", "دراسه", "ادرس", "تعلم", "اذاكر", "المذاكره")),
        GoodEntry("vitamins", listOf("vitamin", "vitamins", "supplement", "supplements", "take medicine", "take medication", "فيتامينات", "فيتامين", "مكملات")),
        GoodEntry("sleep_early", listOf("sleep early", "early sleep", "early bed", "go to bed early", "bed early", "sleep on time", "sleep enough", "sleep well", "sleep more", "sleep hours", "good sleep", "نوم بدري", "النوم بدري", "نوم مبكر", "النوم مبكرا", "ننام بدري")),
        GoodEntry("wake_early", listOf("wake up early", "wake early", "get up early", "rise early", "early rising", "early morning", "morning routine", "الاستيقاظ مبكرا", "صحيان بدري", "اصحي بدري", "الصحيان بدري", "استيقظ مبكرا")),
        GoodEntry("healthy_food", listOf("healthy meal", "healthy meals", "healthy eating", "healthy food", "eat healthy", "eat healthier", "balanced diet", "clean eating", "breakfast", "meal prep", "اكل صحي", "الاكل الصحي", "وجبه صحيه", "غذاء صحي", "فطار")),
        GoodEntry("vegetables", listOf("vegetable", "vegetables", "veggies", "veggie", "salad", "salads", "fruit", "fruits", "خضار", "خضروات", "فواكه", "فاكهه", "سلطه")),
        GoodEntry("teeth", listOf("floss", "flossing", "brush teeth", "brushing teeth", "brush my teeth", "toothbrush", "dental", "خيط الاسنان", "غسل الاسنان", "تفريش الاسنان", "فرشه الاسنان", "اسنان")),
        GoodEntry("call_family", listOf("call family", "call mom", "call mum", "call dad", "call parents", "call grandma", "call friend", "call friends", "family time", "visit family", "اتصل بالاهل", "اتصل بامي", "اتصل بابي", "صله الرحم", "زياره الاهل", "مكالمه الاهل")),
        GoodEntry("gratitude", listOf("gratitude", "grateful", "thankful", "thanks journal", "امتنان", "الامتنان", "حمد", "شكر")),
        GoodEntry("cold_shower", listOf("cold shower", "cold showers", "shower", "دش بارد", "استحمام")),
        GoodEntry("skincare", listOf("skincare", "skin care", "sunscreen", "moisturize", "العنايه بالبشره", "كريم الشمس", "ترطيب")),
        GoodEntry("cook", listOf("cook", "cooking", "cook at home", "home cooking", "طبخ", "الطبخ", "اطبخ")),
        GoodEntry("save", listOf("save money", "saving money", "savings", "budget", "budgeting", "invest", "investing", "ادخار", "توفير", "ميزانيه", "استثمار")),
        GoodEntry("charity", listOf("volunteer", "volunteering", "charity", "donate", "donation", "sadaqah", "sadaqa", "تطوع", "صدقه", "الصدقه", "تبرع")),
        GoodEntry("practice", listOf("practice guitar", "practice piano", "piano", "guitar", "instrument", "learn language", "language practice", "vocabulary", "جيتار", "بيانو", "تعلم لغه", "مفردات")),
        GoodEntry("swim", listOf("swim", "swimming", "سباحه", "السباحه", "اسبح")),
        GoodEntry("cycle", listOf("cycle", "cycling", "bike", "biking", "bicycle", "دراجه", "العجله", "ركوب الدراجه")),
        GoodEntry("pushups", listOf("pushup", "pushups", "push up", "push ups", "situp", "situps", "sit ups", "squat", "squats", "plank", "planks", "تمارين الضغط", "بلانك", "سكوات")),
        GoodEntry("weights", listOf("weights", "weightlifting", "lifting", "strength training", "dumbbell", "dumbbells", "اثقال", "رفع الاثقال")),
        GoodEntry("posture", listOf("posture", "sit straight", "stand more", "قوام", "وقفه الظهر")),
        GoodEntry("sunlight", listOf("sunlight", "sunshine", "outdoors", "outside time", "fresh air", "nature walk", "شمس", "الشمس", "هواء نقي")),
        GoodEntry("tidy", listOf("tidy", "tidying", "clean room", "clean house", "cleaning", "make bed", "make my bed", "declutter", "decluttering", "تنظيف", "ترتيب", "ترتيب السرير", "ترتيب الغرفه")),
        GoodEntry("plan", listOf("plan day", "plan my day", "planning", "planner", "todo list", "to do list", "daily plan", "تخطيط", "خطه اليوم", "قائمه المهام")),
        GoodEntry("creative", listOf("draw", "drawing", "paint", "painting", "sketch", "creative writing", "write", "writing", "hobby", "رسم", "الرسم", "كتابه", "هوايه")),
        GoodEntry("podcast", listOf("podcast", "podcasts", "audiobook", "audiobooks", "listen to book", "بودكاست", "كتاب مسموع")),
        GoodEntry("walk_dog", listOf("walk dog", "walk the dog", "walk my dog", "pet care", "مشي الكلب", "تمشيه الكلب")),
        GoodEntry("breathe", listOf("breathe", "breath work", "breathwork", "relax", "relaxation", "استرخاء")),
        GoodEntry("stairs", listOf("take stairs", "use stairs", "stairs", "سلالم", "الدرج")),
        GoodEntry("floss_hands", listOf("wash hands", "handwashing", "غسل اليدين")),
        GoodEntry("music", listOf("music practice", "sing", "singing", "موسيقي", "غناء")),
        GoodEntry("smile", listOf("compliment", "kindness", "be kind", "random act of kindness", "لطف", "مجامله")),
        GoodEntry("learn_skill", listOf("learn coding", "coding", "programming", "learn to code", "برمجه", "تعلم البرمجه")),
        GoodEntry("sports", listOf("football", "soccer", "basketball", "tennis", "padel", "volleyball", "martial arts", "boxing", "كره القدم", "كره السله", "تنس", "ملاكمه")),
        GoodEntry("fasting", listOf("intermittent fasting", "صيام متقطع")),
        GoodEntry("water_fruit", listOf("herbal tea", "green tea", "شاي اخضر", "اعشاب")),
        GoodEntry("quran_memorize", listOf("memorize quran", "quran memorization", "tajweed", "حفظ القران", "تجويد")),
        GoodEntry("tahajjud", listOf("tahajjud", "witr", "duha", "تهجد", "وتر", "ضحي")),
        GoodEntry("family", listOf("play with kids", "kids time", "date night", "اللعب مع الاطفال", "وقت مع الاولاد")),
        GoodEntry("sleep_routine", listOf("bedtime routine", "wind down", "روتين النوم")),
        GoodEntry("digital", listOf("screen free hour", "offline hour", "digital detox hour", "ساعه بدون موبايل")),
        GoodEntry("morning", listOf("make coffee mindfully", "morning walk", "morning stretch", "مشي الصباح", "رياضه الصباح")),
        GoodEntry("posture2", listOf("neck stretch", "back stretch", "تمارين الرقبه", "تمارين الظهر")),
        GoodEntry("hydrate_lemon", listOf("lemon water", "warm water", "ماء دافئ", "ماء بليمون")),
        GoodEntry("protein", listOf("protein", "high protein", "بروتين")),
        GoodEntry("fiber", listOf("fiber", "fibre", "oats", "شوفان", "الياف")),
        GoodEntry("nuts", listOf("nuts", "almonds", "مكسرات", "لوز")),
        GoodEntry("eye", listOf("eye exercises", "eye rest", "twenty twenty twenty", "راحه العين")),
        GoodEntry("learn", listOf("flashcards", "anki", "duolingo", "بطاقات")),
        GoodEntry("tidy_desk", listOf("clean desk", "organize desk", "مكتب مرتب")),
        GoodEntry("budget_track", listOf("track expenses", "track spending", "expense tracking", "تتبع المصاريف")),
    )

    /** Token sequences that look like a quit-habit keyword but are not ("sweet potato", "game plan"). */
    val neutralPhrases: List<String> = listOf(
        "sweet potato", "sweet potatoes", "sweet corn", "sweet dreams", "sweet home", "sweet spot",
        "game plan", "game changer", "game day", "phone call", "phone calls", "phone number",
        "nailed", "nail polish", "bite size", "bite sized", "screen shot", "screenshot",
    )

    val index: KeywordIndex by lazy { KeywordIndex(entries.map { it.keywords }) }

    private val neutral: List<List<String>> by lazy {
        neutralPhrases.map { HabitTextNormalizer.normalize(it).split(' ') }
    }

    /** Marks every token of [tokens] that belongs to a neutral phrase. */
    fun maskNeutral(tokens: List<String>, mask: BooleanArray) {
        for (p in neutral) {
            var i = 0
            while (i + p.size <= tokens.size) {
                var ok = true
                for (k in p.indices) if (tokens[i + k] != p[k]) { ok = false; break }
                if (ok) for (k in p.indices) mask[i + k] = true
                i++
            }
        }
    }

    /** Single good-habit words (5+ letters) that feed the spelling-suggestion dictionary. */
    fun dictionaryWords(): List<String> =
        entries.flatMap { it.keywords }
            .map { HabitTextNormalizer.normalize(it) }
            .filter { it.length >= 5 && !it.contains(' ') && it[0].code < 0x0600 }
            .distinct()
}

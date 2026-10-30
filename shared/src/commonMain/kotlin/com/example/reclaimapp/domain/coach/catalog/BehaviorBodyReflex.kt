package com.example.reclaimapp.domain.coach.catalog

import com.example.reclaimapp.domain.coach.BodyMilestone
import com.example.reclaimapp.domain.coach.HabitCategory
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Body-focused repetitive habits and reflexes (part of [BehaviorCatalog]).
 * General wellbeing information only, never a diagnosis. Awareness-first and non-shaming.
 */
internal object BehaviorBodyReflex {

    val categories: List<HabitCategory> = listOf(

        /**
         * Overrides the legacy `nails` id (nail biting only; skin picking and hair pulling now have their own ids).
         * Sources: American Academy of Dermatology on nail growth and nail biting; NHS and NIDirect
         * pages on body-focused repetitive behaviours (CBT, habit reversal). Fingernails grow roughly 3 mm per month.
         * // UNVERIFIED (partly): nail growth rate and the NHS pointers were checked; the timeline text is general knowledge.
         */
        HabitCategory(
            id = "nails",
            label = "Nail biting",
            keywords = listOf(
                "nail biting", "biting my nails", "bite my nails", "nail biter", "chewing nails", "chewing my nails",
                "biting nails", "onychophagia", "nail chewing", "picking at my nails",
                "قرض الاظافر", "قضم الاظافر", "اظافر", "قرض الضوافر", "ضوافر", "اقرض ضوافري", "اقضم اظافري",
                "adafer", "adhafer",
            ),
            whyItMatters = "Nail biting is usually an automatic response to stress, boredom or concentration. Awareness of the trigger is half the battle.",
            milestones = listOf(
                BodyMilestone(1.hours, "Catching the reach", "In the first hours many people start noticing the hand moving toward the mouth. That moment of noticing is the skill you are building."),
                BodyMilestone(4.hours, "Urges come in waves", "Urges typically rise with stress or boredom and fade within minutes."),
                BodyMilestone(12.hours, "Rough edges", "Rough edges are often the trigger. A quick file or trim can help."),
                BodyMilestone(24.hours, "A full day of awareness", "Many people feel the habit is more conscious now and less automatic."),
                BodyMilestone(72.hours, "Skin starts to heal", "Small cuts and sore skin around the nails typically start to close."),
                BodyMilestone(7.days, "A head start", "Fingernails grow around 3 mm a month, so a week of rest is a small but real start."),
                BodyMilestone(14.days, "Nails look different", "Healthier edges and a little length often make biting less tempting."),
                BodyMilestone(30.days, "Visible growth", "A month of rest usually shows clearly in your nails. Full regrowth of a nail takes several months."),
            ),
            cravingTips = listOf(
                "Keep your hands busy: a fidget toy, stress ball or lotion.",
                "Trim and file your nails so there is nothing to bite.",
                "Notice what you were feeling right before the urge.",
                "Try a bitter nail polish or a manicure you want to protect.",
            ),
            safetyNote = "If you bite until nails or skin bleed or get infected, or you cannot stop even though you want to, a doctor or therapist can help. Habit-reversal therapy works well for many people.",
        ),

        /**
         * Sources: NHS information on skin picking disorder (see a GP if you cannot stop, cause serious damage
         * or feel distress); NIDirect and NHS Wales on body-focused repetitive behaviours;
         * habit reversal training / CBT as first-line approach (Mayo Clinic, TLC Foundation).
         * Checked online: NHS pages on hair pulling and skin picking (when to see a GP, CBT, diary and triggers).
         */
        HabitCategory(
            id = "skinpicking",
            label = "Skin picking",
            keywords = listOf(
                "skin picking", "picking my skin", "picking at my skin", "picking at spots", "picking pimples", "picking spots",
                "picking scabs", "dermatillomania", "excoriation", "squeezing pimples", "picking at scars",
                "نتف الجلد", "قشر الجلد", "قشط الجلد", "عصر الحبوب", "حك الجلد", "اقشر جلدي", "بقشر في جلدي",
            ),
            whyItMatters = "Skin picking is often an automatic response to stress, boredom or a wish to smooth the skin. It is not a failure of willpower, and awareness and support can help.",
            milestones = listOf(
                BodyMilestone(1.hours, "Noticing the urge", "In the first hour many people notice the start of an urge to pick. Noticing it is the first step."),
                BodyMilestone(4.hours, "Urges rise and fall", "Urges typically peak and then ease within minutes if you redirect your hands."),
                BodyMilestone(8.hours, "Skin gets a rest", "Skin that is left alone for a few hours can start to calm down."),
                BodyMilestone(24.hours, "A day to heal", "Fresh marks begin to settle when they are not touched. Redness may linger."),
                BodyMilestone(72.hours, "Scabs and spots settle", "Small wounds typically start to close over the first days. Everyone heals at a different pace."),
                BodyMilestone(7.days, "Visible calm", "Many people notice less redness and fewer new marks after a week."),
                BodyMilestone(14.days, "More control", "Triggers still appear, but you can often step in earlier."),
                BodyMilestone(30.days, "Skin recovers", "Skin typically looks calmer. Scars can take months to fade."),
            ),
            cravingTips = listOf(
                "Keep your hands busy: a fidget toy, putty or lotion.",
                "Cover trouble spots with a patch or plaster.",
                "Keep nails short and avoid picking in front of mirrors.",
                "Write down when and where the urge appears so you can see the pattern.",
            ),
            safetyNote = "If you cannot stop picking, cause cuts, bleeding or infection, or feel distress or shame, please see a GP or doctor. Treatments such as cognitive behavioural therapy can help. This is not a personal failing.",
        ),

        /**
         * Sources: NHS and NIDirect pages on trichotillomania (see a GP; CBT; keep a diary of pulling and triggers;
         * replace the action, for example with a stress ball; many people feel relief afterwards).
         * Checked online: NHS pages on trichotillomania.
         */
        HabitCategory(
            id = "hairpulling",
            label = "Hair pulling",
            keywords = listOf(
                "hair pulling", "pulling my hair", "pulling out my hair", "trichotillomania", "trich", "pulling eyelashes",
                "pulling eyebrows", "twisting my hair", "plucking hair compulsively", "hair twirling",
                "نتف الشعر", "شد الشعر", "نتف رموش", "نتف الحواجب", "بنتف شعري", "بشد في شعري",
            ),
            whyItMatters = "Hair pulling is often an automatic response to tension, boredom or concentration, and many people feel relief afterwards. It is common, nothing to be ashamed of, and treatable.",
            milestones = listOf(
                BodyMilestone(1.hours, "Spotting the build-up", "In the first hour many people notice the tension that comes before a pull. Noticing is the first step."),
                BodyMilestone(4.hours, "Redirected urges", "Urges typically ease within minutes if you redirect your hands to something else."),
                BodyMilestone(8.hours, "A calmer scalp", "Skin that is not being pulled can start to feel less sore."),
                BodyMilestone(24.hours, "A day of awareness", "Many people notice their triggers more, such as reading, driving or being on the phone."),
                BodyMilestone(72.hours, "Fewer pulls", "With strategies and cover, many people notice fewer episodes."),
                BodyMilestone(7.days, "Skin recovers", "Redness and soreness typically ease in the first weeks."),
                BodyMilestone(14.days, "New growth begins", "Hair typically begins to regrow, though new hair is short and slow at first."),
                BodyMilestone(30.days, "Visible change", "Many people can see short new growth after a month. Hair grows about 1 cm per month, so progress is gradual."),
            ),
            cravingTips = listOf(
                "Keep your hands busy: squeeze a stress ball or use a fidget toy.",
                "Wear a hat, scarf or gloves in your trigger situations.",
                "Keep a diary of when and where you pull to find the patterns.",
                "Try a barrier such as short hair or a bandana at high-risk times.",
            ),
            safetyNote = "If you are pulling out hair and cannot stop, have bald patches, or feel distressed, please see a GP or doctor. Cognitive behavioural therapy and habit-reversal training help many people. If you swallow hair, speak to a doctor.",
        ),

        /**
         * Sources: general guidance on body-focused repetitive behaviours (NHS, TLC Foundation);
         * no specific clinical figures claimed.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "lipbiting",
            label = "Lip and cheek biting",
            keywords = listOf(
                "lip biting", "biting my lips", "biting my lip", "chewing my lips", "cheek biting", "biting my cheek",
                "chewing the inside of my cheek", "lip picking", "peeling my lips", "lip chewing",
                "عض الشفايف", "عض الشفة", "قرض الشفايف", "عض الخد", "بعض شفايفي", "اقشر شفايفي",
            ),
            whyItMatters = "Biting your lips or cheeks is usually an unconscious habit linked to stress or concentration. It can cause sore spots that make it more tempting to keep going.",
            milestones = listOf(
                BodyMilestone(1.hours, "Catching it", "In the first hour many people start to notice when they bite. That is the skill you are building."),
                BodyMilestone(4.hours, "Urge comes and goes", "Urges typically rise and fade within a few minutes."),
                BodyMilestone(12.hours, "Less irritation", "Sore spots start to settle when they are left alone."),
                BodyMilestone(24.hours, "Gentle healing", "A full day often shows less swelling and fewer sore spots."),
                BodyMilestone(72.hours, "Smoother skin", "Small cuts and rough skin typically start to heal."),
                BodyMilestone(7.days, "Calmer mouth", "Many people notice fewer sore spots after a week."),
                BodyMilestone(14.days, "Less automatic", "Biting becomes more conscious, which makes it easier to catch."),
                BodyMilestone(30.days, "A new pattern", "Many people have a calmer mouth and a different default response to stress."),
            ),
            cravingTips = listOf(
                "Use lip balm regularly so there is no dry skin to bite or peel.",
                "Chew sugar-free gum or sip water when the urge comes.",
                "Relax your jaw: lips together, teeth apart.",
                "Notice the stress or boredom that comes just before the urge.",
            ),
            safetyNote = "If you bite until the skin bleeds, get ulcers that do not heal, or cannot stop even though you want to, speak to a doctor or dentist.",
        ),

        /**
         * Sources: Unger 1998 (Arthritis & Rheumatism) and DeWeber et al. 2011 (J Am Board Fam Med) found no link
         * between habitual knuckle cracking and hand osteoarthritis; Harvard Health summary.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "knucklecracking",
            label = "Knuckle cracking",
            keywords = listOf(
                "knuckle cracking", "cracking my knuckles", "crack my knuckles", "cracking knuckles", "popping my knuckles",
                "cracking my fingers", "cracking joints", "neck cracking", "cracking my neck",
                "طقطقة الاصابع", "فرقعة الاصابع", "فرقعة المفاصل", "اطقطق صوابعي", "اطقطق اصابعي", "اطرقع صوابعي",
            ),
            whyItMatters = "Knuckle cracking is usually harmless. Studies have not found that it causes arthritis, but it can become a distracting reflex, and some people notice swelling or a weaker grip.",
            milestones = listOf(
                BodyMilestone(1.hours, "Noticing the reflex", "In the first hour many people notice how often their fingers start to move."),
                BodyMilestone(4.hours, "Urges fade", "The urge to crack typically fades if you wait a few minutes."),
                BodyMilestone(8.hours, "Hands at rest", "Hands that are not being cracked can feel less tense and strained."),
                BodyMilestone(24.hours, "A day of awareness", "Many people find it is mostly an automatic habit during stress or boredom."),
                BodyMilestone(72.hours, "Less often", "With a replacement action, the urge typically becomes less frequent."),
                BodyMilestone(7.days, "Calmer hands", "Many people notice fewer urges after a week."),
                BodyMilestone(14.days, "A new fidget", "Another gentle action, such as stretching, starts to take over."),
                BodyMilestone(30.days, "Hand habit fades", "The reflex typically weakens, though it can return in stressful moments."),
            ),
            cravingTips = listOf(
                "Squeeze a stress ball or press your palms together instead.",
                "Stretch your hands and wrists gently.",
                "Hold something, such as a pen or a coin, during idle moments.",
                "Notice if stress or boredom comes just before the urge.",
            ),
            safetyNote = "If cracking is painful, or your joints are swollen, stiff or locked, speak to a doctor.",
        ),

        /**
         * Sources: NHS "Teeth grinding (bruxism)" (can be linked to stress; avoid caffeine and alcohol; relax the jaw;
         * a dentist can provide a mouthguard); Mayo Clinic bruxism. Awareness only, not treatment.
         * // UNVERIFIED: written from general knowledge, needs review
         */
        HabitCategory(
            id = "teethgrinding",
            label = "Teeth grinding & jaw clenching",
            keywords = listOf(
                "teeth grinding", "grinding my teeth", "clenching my jaw", "jaw clenching", "clenching my teeth", "bruxism",
                "grinding teeth at night", "tight jaw", "jaw tension",
                "صرير الاسنان", "الضغط على الاسنان", "شد الفك", "اصر على اسناني", "بصر على سناني", "الفك مشدود",
            ),
            whyItMatters = "Grinding and clenching often happen without you noticing, and stress can make them worse. Awareness and relaxation can reduce strain on your jaw and teeth.",
            milestones = listOf(
                BodyMilestone(1.hours, "Jaw check", "Checking your jaw every hour often reveals clenching. Lips together, teeth apart is the relaxed position."),
                BodyMilestone(4.hours, "Less daytime tension", "Regular jaw checks typically help you release tension during the day."),
                BodyMilestone(8.hours, "A calmer evening", "A relaxing wind-down may reduce clenching at night."),
                BodyMilestone(24.hours, "More awareness", "Many people find they clench most when focused or stressed."),
                BodyMilestone(72.hours, "Looser jaw", "Many people notice their jaw feels less tight or sore."),
                BodyMilestone(7.days, "Fewer morning aches", "Some people notice fewer jaw or head aches in the morning. Results vary."),
                BodyMilestone(14.days, "Habit loosens", "You typically catch clenching sooner and relax your jaw."),
                BodyMilestone(30.days, "A relaxed default", "A relaxed jaw tends to feel more normal. Night-time grinding can need dental advice."),
            ),
            cravingTips = listOf(
                "Remind yourself: lips together, teeth apart.",
                "Place a note or reminder on your screen to check your jaw.",
                "Warm compresses and gentle jaw massage can ease tension.",
                "Cut back on caffeine and alcohol in the evening.",
            ),
            safetyNote = "If you wake with jaw pain, headaches, or worn or cracked teeth, please see a dentist or doctor. A mouthguard or other treatment may help.",
        ),
    )
}

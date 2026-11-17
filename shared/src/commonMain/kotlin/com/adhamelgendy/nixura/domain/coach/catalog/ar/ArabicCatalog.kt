package com.adhamelgendy.nixura.domain.coach.catalog.ar

import com.adhamelgendy.nixura.domain.coach.BodyMilestone
import com.adhamelgendy.nixura.domain.coach.HabitCategory

/**
 * Arabic text of one catalog category. Milestones are in the same order as the English category (times come from
 * the English entry, so they can never drift apart).
 */
internal class CategoryText(
    val label: String,
    val whyItMatters: String,
    /** Title to detail, one pair per English milestone, same order. */
    val milestones: List<Pair<String, String>>,
    val cravingTips: List<String>,
    val safetyNote: String? = null,
)

/**
 * The Arabic version of the habit catalog (the `catalog` package and `HabitKnowledgeBase`). Matching is untouched: the English
 * entry keeps its id, keywords (English and Arabic spellings) and milestone times; only the shown text is swapped.
 * A category without Arabic text, or whose milestone count no longer matches its English entry, stays in English
 * except its label (see [labels]), so the timeline is never half translated.
 */
internal object ArabicCatalog {

    fun localize(category: HabitCategory): HabitCategory {
        val text = texts[category.id]
        if (text == null || text.milestones.size != category.milestones.size) {
            val label = labels[category.id] ?: return category
            return category.copy(label = label)
        }
        return category.copy(
            label = text.label,
            whyItMatters = text.whyItMatters,
            milestones = category.milestones.mapIndexed { i, m -> BodyMilestone(m.after, text.milestones[i].first, text.milestones[i].second) },
            cravingTips = text.cravingTips,
            safetyNote = category.safetyNote?.let { text.safetyNote ?: it },
        )
    }

    /** Ids with a full Arabic text. */
    val translatedIds: Set<String> get() = texts.keys

    internal val texts: Map<String, CategoryText> by lazy { ArabicSubstanceTexts.all + ArabicBehaviorTexts.all + ArabicCoreTexts.all }

    /** Arabic labels for every category, also those whose details are still English. */
    internal val labels: Map<String, String> by lazy { ArabicLabels.all }
}

package com.elendheim.overwatchchallenges.data

/**
 * A rule inside a pack. The tag is free text: "Warmup" and "Chaos" behave
 * like the built-in intensities, anything else becomes a custom tag that
 * only rolls in Mixed. An optional hero locks the rule to that hero.
 */
data class CustomRule(val text: String, val tag: String, val hero: String? = null)

/**
 * A named, toggleable bundle of custom rules. Packs can run alongside the
 * standard pool, replace it entirely, or sit disabled until game night.
 *
 * The code is the pack's permanent identity: generated once, never reused,
 * kept through exports and imports, and enough to summon the pack back from
 * the archive by typing it.
 */
data class RulePack(
    val name: String,
    val enabled: Boolean,
    val rules: List<CustomRule>,
    val code: String = "",
)

fun RulePack.toChallenges(): List<Challenge> = rules.map { rule ->
    val tag = rule.tag.trim().ifEmpty { Intensity.CHAOS.label }
    val hero = rule.hero?.trim()?.takeUnless { it.isEmpty() }
    when (tag.lowercase()) {
        "warmup" -> Challenge(rule.text, Category.HOUSE, Intensity.WARMUP, packName = name, heroName = hero)
        "chaos" -> Challenge(rule.text, Category.HOUSE, Intensity.CHAOS, packName = name, heroName = hero)
        else -> Challenge(rule.text, Category.HOUSE, Intensity.CHAOS, customTag = tag, packName = name, heroName = hero)
    }
}

package com.elendheim.overwatchchallenges.data

/**
 * A rule inside a pack. The tag is free text: "Warmup" and "Chaos" behave
 * like the built-in intensities, anything else becomes a custom tag that
 * only rolls in Mixed.
 */
data class CustomRule(val text: String, val tag: String)

/**
 * A named, toggleable bundle of custom rules. Packs can run alongside the
 * standard pool, replace it entirely, or sit disabled until game night.
 */
data class RulePack(
    val name: String,
    val enabled: Boolean,
    val rules: List<CustomRule>,
)

fun RulePack.toChallenges(): List<Challenge> = rules.map { rule ->
    val tag = rule.tag.trim().ifEmpty { Intensity.CHAOS.label }
    when (tag.lowercase()) {
        "warmup" -> Challenge(rule.text, Category.HOUSE, Intensity.WARMUP, packName = name)
        "chaos" -> Challenge(rule.text, Category.HOUSE, Intensity.CHAOS, packName = name)
        else -> Challenge(rule.text, Category.HOUSE, Intensity.CHAOS, customTag = tag, packName = name)
    }
}

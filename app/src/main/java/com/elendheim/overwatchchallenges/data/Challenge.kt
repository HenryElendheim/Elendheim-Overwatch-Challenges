package com.elendheim.overwatchchallenges.data

/**
 * WARMUP challenges are the ones you can bring into a comp warmup without
 * throwing. CHAOS is everything you should keep to QP and customs.
 */
enum class Intensity(val label: String) {
    WARMUP("Warmup"),
    CHAOS("Chaos"),
}

/**
 * Which slice of the pool the roller draws from. MIXED lets the chaos flow,
 * and it's the only mode that includes custom-tagged pack rules.
 */
enum class PoolMode(val label: String) {
    WARMUP("Warmup"),
    MIXED("Mixed"),
    CHAOS("Chaos"),
}

enum class Category(val label: String) {
    AIM("Aim"),
    MOVEMENT("Movement"),
    ABILITIES("Abilities"),
    HERO_POOL("Hero pool"),
    COMMS("Comms"),
    MEME("Meme"),
    SUPPORT("Support"),
    TANK("Tank"),
    HOUSE("House rule"),
}

/**
 * @param roles which roles this challenge makes sense for; null means anyone.
 * @param overridesHero true when the constraint itself decides what you play,
 *   so the rolled hero is just a suggestion.
 * @param customTag free-text tag from a rule pack. Tagged rules only roll in
 *   Mixed, since Warmup and Chaos filter to their exact tag.
 * @param packName which rule pack this came from, shown instead of the category.
 */
data class Challenge(
    val text: String,
    val category: Category,
    val intensity: Intensity,
    val roles: Set<Role>? = null,
    val overridesHero: Boolean = false,
    val customTag: String? = null,
    val packName: String? = null,
)

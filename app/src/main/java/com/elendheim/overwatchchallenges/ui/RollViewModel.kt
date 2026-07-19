package com.elendheim.overwatchchallenges.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.elendheim.overwatchchallenges.data.Challenge
import com.elendheim.overwatchchallenges.data.CustomRule
import com.elendheim.overwatchchallenges.data.Intensity
import com.elendheim.overwatchchallenges.data.PoolMode
import com.elendheim.overwatchchallenges.data.Role
import com.elendheim.overwatchchallenges.data.RulePack
import com.elendheim.overwatchchallenges.data.toChallenges
import com.elendheim.overwatchchallenges.engine.RollEngine
import com.elendheim.overwatchchallenges.engine.RollResult
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject

enum class TextSize(val label: String, val scale: Float) {
    SMALL("Small", 0.85f),
    NORMAL("Normal", 1f),
    LARGE("Large", 1.15f),
    LARGER("Larger", 1.3f),
    LARGEST("Largest", 1.45f),
}

/** How tightly the reel packs hero names during the spin. */
enum class ReelDensity(val label: String, val itemHeight: Int) {
    COMPACT("Compact", 72),
    NORMAL("Normal", 96),
    ROOMY("Roomy", 128),
}

enum class ReelColor(val label: String) {
    ROLE("Role colors"),
    EMBER("Orange"),
    WHITE("White"),
}

enum class ArrowSize(val label: String, val sizeDp: Int) {
    SMALL("Small", 18),
    MEDIUM("Medium", 26),
    LARGE("Large", 36),
}

data class RollUiState(
    val roleFilter: Role? = null,
    val poolMode: PoolMode = PoolMode.MIXED,
    val stakes: Boolean = false,
    val squadSeed: String? = null,
    val result: RollResult? = null,
    val rollId: Int = 0,
    val mutations: Int = 0,
    val deaths: Int = 0,
    val disabledHeroes: Set<String> = emptySet(),
    val mysteryEnabled: Boolean = true,
    val noChallengeMode: Boolean = false,
    val standardEnabled: Boolean = true,
    val rulePacks: List<RulePack> = emptyList(),
    val showPoolCounts: Boolean = true,
    val textSize: TextSize = TextSize.NORMAL,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val spinSeconds: Float = 2.4f,
    val reelDensity: ReelDensity = ReelDensity.NORMAL,
    val reelColor: ReelColor = ReelColor.ROLE,
    val overshootEnabled: Boolean = true,
    val suspenseEnabled: Boolean = true,
    val landingArrow: Boolean = true,
    val arrowSize: ArrowSize = ArrowSize.MEDIUM,
    val landingUnderline: Boolean = false,
    val arrowColor: Int? = null,
    val roleColors: Map<Role, Int> = emptyMap(),
    val hapticsEnabled: Boolean = true,
)

class RollViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("elendheim", Context.MODE_PRIVATE)

    var state by mutableStateOf(
        RollUiState(
            disabledHeroes = prefs.getStringSet(KEY_DISABLED_HEROES, null).orEmpty().toSet(),
            mysteryEnabled = prefs.getBoolean(KEY_MYSTERY, true),
            noChallengeMode = prefs.getBoolean(KEY_NO_CHALLENGE, false),
            standardEnabled = prefs.getBoolean(KEY_STANDARD, true),
            rulePacks = loadPacks(prefs),
            showPoolCounts = prefs.getBoolean(KEY_SHOW_COUNTS, true),
            textSize = TextSize.entries.firstOrNull { it.name == prefs.getString(KEY_TEXT_SIZE, null) }
                ?: TextSize.NORMAL,
            highContrast = prefs.getBoolean(KEY_HIGH_CONTRAST, false),
            reduceMotion = prefs.getBoolean(KEY_REDUCE_MOTION, false),
            spinSeconds = prefs.getFloat(KEY_SPIN_SECONDS, 2.4f),
            reelDensity = ReelDensity.entries.firstOrNull { it.name == prefs.getString(KEY_REEL_DENSITY, null) }
                ?: ReelDensity.NORMAL,
            reelColor = ReelColor.entries.firstOrNull { it.name == prefs.getString(KEY_REEL_COLOR, null) }
                ?: ReelColor.ROLE,
            overshootEnabled = prefs.getBoolean(KEY_OVERSHOOT, true),
            suspenseEnabled = prefs.getBoolean(KEY_SUSPENSE, true),
            landingArrow = prefs.getBoolean(KEY_LANDING_ARROW, true),
            arrowSize = ArrowSize.entries.firstOrNull { it.name == prefs.getString(KEY_ARROW_SIZE, null) }
                ?: ArrowSize.MEDIUM,
            landingUnderline = prefs.getBoolean(KEY_LANDING_UNDERLINE, false),
            arrowColor = if (prefs.contains(KEY_ARROW_COLOR)) prefs.getInt(KEY_ARROW_COLOR, 0) else null,
            roleColors = Role.entries.mapNotNull { role ->
                val key = KEY_ROLE_COLOR_PREFIX + role.name
                if (prefs.contains(key)) role to prefs.getInt(key, 0) else null
            }.toMap(),
            hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
        )
    )
        private set

    private var engine = RollEngine()

    init {
        // packs from before codes existed get one on first launch
        if (state.rulePacks.any { it.code.isBlank() }) {
            val taken = allKnownCodes().toMutableSet()
            savePacks(
                state.rulePacks.map { pack ->
                    if (pack.code.isNotBlank()) pack
                    else pack.copy(code = newCode(taken).also { taken += it })
                }
            )
        }
    }

    private fun packExtras(): List<Challenge> =
        state.rulePacks.filter { it.enabled }.flatMap { it.toChallenges() }

    fun setRoleFilter(role: Role?) {
        state = state.copy(roleFilter = role)
    }

    fun setPoolMode(mode: PoolMode) {
        state = state.copy(poolMode = mode)
    }

    /** Only shows or hides the punishment. The rolled one sticks until the next roll. */
    fun toggleStakes() {
        state = state.copy(stakes = !state.stakes)
    }

    /** Blank or null drops back to solo rolling. */
    fun setSquadSeed(seed: String?) {
        val clean = seed?.trim()?.takeUnless { it.isEmpty() }
        engine = if (clean != null) RollEngine(RollEngine.seedFrom(clean)) else RollEngine()
        state = state.copy(squadSeed = clean, result = null, mutations = 0)
    }

    /** Tap a hero in settings to ban them from the roller, tap again to allow them. */
    fun toggleHero(name: String) {
        val disabled = state.disabledHeroes.toMutableSet()
        if (!disabled.add(name)) disabled.remove(name)
        saveDisabled(disabled)
    }

    /** The all-or-nothing switches in settings: whole pool or a whole role at once. */
    fun setGroupEnabled(names: List<String>, enabled: Boolean) {
        val disabled = state.disabledHeroes.toMutableSet()
        if (enabled) disabled.removeAll(names.toSet()) else disabled.addAll(names)
        saveDisabled(disabled)
    }

    private fun saveDisabled(disabled: Set<String>) {
        prefs.edit().putStringSet(KEY_DISABLED_HEROES, disabled).apply()
        state = state.copy(disabledHeroes = disabled)
    }

    fun toggleMystery() {
        val enabled = !state.mysteryEnabled
        prefs.edit().putBoolean(KEY_MYSTERY, enabled).apply()
        state = state.copy(mysteryEnabled = enabled)
    }

    fun toggleNoChallengeMode() {
        val enabled = !state.noChallengeMode
        prefs.edit().putBoolean(KEY_NO_CHALLENGE, enabled).apply()
        state = state.copy(noChallengeMode = enabled)
    }

    /** The built-in pool. Off means the roller runs purely on your packs. */
    fun toggleStandard() {
        val enabled = !state.standardEnabled
        prefs.edit().putBoolean(KEY_STANDARD, enabled).apply()
        state = state.copy(standardEnabled = enabled)
    }

    fun toggleShowPoolCounts() {
        val show = !state.showPoolCounts
        prefs.edit().putBoolean(KEY_SHOW_COUNTS, show).apply()
        state = state.copy(showPoolCounts = show)
    }

    fun setTextSize(size: TextSize) {
        prefs.edit().putString(KEY_TEXT_SIZE, size.name).apply()
        state = state.copy(textSize = size)
    }

    fun toggleHighContrast() {
        val on = !state.highContrast
        prefs.edit().putBoolean(KEY_HIGH_CONTRAST, on).apply()
        state = state.copy(highContrast = on)
    }

    fun toggleReduceMotion() {
        val on = !state.reduceMotion
        prefs.edit().putBoolean(KEY_REDUCE_MOTION, on).apply()
        state = state.copy(reduceMotion = on)
    }

    fun setSpinSeconds(seconds: Float) {
        val clamped = seconds.coerceIn(1f, 5f)
        prefs.edit().putFloat(KEY_SPIN_SECONDS, clamped).apply()
        state = state.copy(spinSeconds = clamped)
    }

    fun setReelDensity(density: ReelDensity) {
        prefs.edit().putString(KEY_REEL_DENSITY, density.name).apply()
        state = state.copy(reelDensity = density)
    }

    fun setReelColor(color: ReelColor) {
        prefs.edit().putString(KEY_REEL_COLOR, color.name).apply()
        state = state.copy(reelColor = color)
    }

    fun toggleOvershoot() {
        val on = !state.overshootEnabled
        prefs.edit().putBoolean(KEY_OVERSHOOT, on).apply()
        state = state.copy(overshootEnabled = on)
    }

    fun toggleSuspense() {
        val on = !state.suspenseEnabled
        prefs.edit().putBoolean(KEY_SUSPENSE, on).apply()
        state = state.copy(suspenseEnabled = on)
    }

    fun toggleLandingArrow() {
        val on = !state.landingArrow
        prefs.edit().putBoolean(KEY_LANDING_ARROW, on).apply()
        state = state.copy(landingArrow = on)
    }

    fun setArrowSize(size: ArrowSize) {
        prefs.edit().putString(KEY_ARROW_SIZE, size.name).apply()
        state = state.copy(arrowSize = size)
    }

    fun toggleLandingUnderline() {
        val on = !state.landingUnderline
        prefs.edit().putBoolean(KEY_LANDING_UNDERLINE, on).apply()
        state = state.copy(landingUnderline = on)
    }

    /** Null goes back to the default white arrow. */
    fun setArrowColor(argb: Int?) {
        prefs.edit().apply {
            if (argb == null) remove(KEY_ARROW_COLOR) else putInt(KEY_ARROW_COLOR, argb)
        }.apply()
        state = state.copy(arrowColor = argb)
    }

    /** Null restores the role's original color. */
    fun setRoleColor(role: Role, argb: Int?) {
        val key = KEY_ROLE_COLOR_PREFIX + role.name
        prefs.edit().apply {
            if (argb == null) remove(key) else putInt(key, argb)
        }.apply()
        val colors = state.roleColors.toMutableMap()
        if (argb == null) colors.remove(role) else colors[role] = argb
        state = state.copy(roleColors = colors)
    }

    fun toggleHaptics() {
        val on = !state.hapticsEnabled
        prefs.edit().putBoolean(KEY_HAPTICS, on).apply()
        state = state.copy(hapticsEnabled = on)
    }

    // rule packs

    fun createPack(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        if (state.rulePacks.any { it.name.equals(clean, ignoreCase = true) }) return
        savePacks(
            state.rulePacks +
                RulePack(clean, enabled = true, rules = emptyList(), code = generatePackCode())
        )
    }

    fun deletePack(name: String) {
        savePacks(state.rulePacks.filterNot { it.name == name })
    }

    fun togglePack(name: String) {
        savePacks(state.rulePacks.map { if (it.name == name) it.copy(enabled = !it.enabled) else it })
    }

    fun addRule(packName: String, text: String, tag: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return
        val cleanTag = tag.trim().ifEmpty { Intensity.CHAOS.label }
        savePacks(
            state.rulePacks.map { pack ->
                if (pack.name != packName) return@map pack
                if (pack.rules.any { it.text.equals(cleanText, ignoreCase = true) }) return@map pack
                pack.copy(rules = pack.rules + CustomRule(cleanText, cleanTag))
            }
        )
    }

    fun updateRule(packName: String, oldRule: CustomRule, newText: String, newTag: String) {
        val text = newText.trim()
        if (text.isEmpty()) return
        val tag = newTag.trim().ifEmpty { Intensity.CHAOS.label }
        savePacks(
            state.rulePacks.map { pack ->
                if (pack.name != packName) pack
                else pack.copy(rules = pack.rules.map { if (it == oldRule) CustomRule(text, tag) else it })
            }
        )
    }

    fun removeRules(packName: String, rules: Collection<CustomRule>) {
        val doomed = rules.toSet()
        savePacks(
            state.rulePacks.map { pack ->
                if (pack.name == packName) pack.copy(rules = pack.rules.filterNot { it in doomed }) else pack
            }
        )
    }

    private fun packJson(pack: RulePack): JSONObject {
        val rules = JSONArray()
        pack.rules.forEach { rules.put(JSONObject().put("text", it.text).put("tag", it.tag)) }
        return JSONObject().put("name", pack.name).put("code", pack.code).put("rules", rules)
    }

    /** A pack as a shareable code: paste it on a friend's phone, done. */
    fun exportPack(name: String): String? {
        val pack = state.rulePacks.firstOrNull { it.name == name } ?: return null
        return PACK_CODE_PREFIX + Base64.encodeToString(
            packJson(pack).toString().toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP,
        )
    }

    /** The same pack as a pretty JSON file. Imports straight back in. */
    fun exportPackJson(name: String): String? {
        val pack = state.rulePacks.firstOrNull { it.name == name } ?: return null
        return packJson(pack).toString(2)
    }

    /** Human-readable markdown with the import code at the bottom. */
    fun exportPackMarkdown(name: String): String? {
        val pack = state.rulePacks.firstOrNull { it.name == name } ?: return null
        return buildString {
            appendLine("# ${pack.name}")
            appendLine()
            appendLine("Code: ${pack.code}")
            appendLine()
            appendLine("An Elendheim Overwatch Challenges rule pack, ${pack.rules.size} rules.")
            appendLine()
            pack.rules.forEach { rule ->
                appendLine("- ${rule.text} (${rule.tag})")
            }
            appendLine()
            appendLine("Import code (paste it under Challenges, Import a pack, or import this file directly):")
            appendLine()
            appendLine(exportPack(name))
        }
    }

    /**
     * Takes a short pack code (looked up from this phone's archive), a share
     * code, a raw JSON export, or a markdown file containing the code.
     */
    fun importPack(code: String): Boolean {
        val trimmed = code.trim()
        if (trimmed.matches(SHORT_CODE)) return importPackByCode(trimmed)
        val decoded = when {
            trimmed.startsWith(PACK_CODE_PREFIX) -> decodeCode(trimmed) ?: return false
            trimmed.startsWith("{") -> trimmed
            else -> trimmed.lines()
                .map { it.trim() }
                .firstOrNull { it.startsWith(PACK_CODE_PREFIX) }
                ?.let { decodeCode(it) }
                ?: return false
        }
        return runCatching {
            val json = JSONObject(decoded)
            val rulesJson = json.getJSONArray("rules")
            val rules = (0 until rulesJson.length()).map { i ->
                val rule = rulesJson.getJSONObject(i)
                CustomRule(rule.getString("text"), rule.optString("tag", Intensity.CHAOS.label))
            }
            val incomingCode = json.optString("code", "").trim().uppercase()
            val packCode = if (incomingCode.matches(SHORT_CODE)) incomingCode else generatePackCode()
            // same code = same pack: a re-import refreshes it instead of duplicating
            val existing = state.rulePacks.firstOrNull { it.code == packCode }
            if (existing != null) {
                savePacks(
                    state.rulePacks.map {
                        if (it.code == packCode) it.copy(rules = rules, enabled = true) else it
                    }
                )
                return@runCatching true
            }
            var name = json.getString("name")
            var counter = 2
            while (state.rulePacks.any { it.name.equals(name, ignoreCase = true) }) {
                name = "${json.getString("name")} ($counter)"
                counter++
            }
            savePacks(state.rulePacks + RulePack(name, enabled = true, rules = rules, code = packCode))
            true
        }.getOrDefault(false)
    }

    /** The archive remembers every pack this phone has ever had; a code brings it back. */
    private fun importPackByCode(input: String): Boolean {
        val code = input.trim().uppercase()
        if (state.rulePacks.any { it.code == code }) return true
        val archived = loadArchive().firstOrNull { it.code == code } ?: return false
        var name = archived.name
        var counter = 2
        while (state.rulePacks.any { it.name.equals(name, ignoreCase = true) }) {
            name = "${archived.name} ($counter)"
            counter++
        }
        savePacks(state.rulePacks + archived.copy(name = name, enabled = true))
        return true
    }

    private fun decodeCode(code: String): String? = runCatching {
        String(
            Base64.decode(code.removePrefix(PACK_CODE_PREFIX), Base64.DEFAULT),
            Charsets.UTF_8,
        )
    }.getOrNull()

    private fun savePacks(packs: List<RulePack>) {
        prefs.edit().putString(KEY_RULE_PACKS, serializePacks(packs)).apply()
        // the archive only grows: every pack ever seen stays findable by code,
        // even after it's deleted from the active list
        val archive = loadArchive().toMutableList()
        packs.filter { it.code.isNotBlank() }.forEach { pack ->
            val index = archive.indexOfFirst { it.code == pack.code }
            if (index >= 0) archive[index] = pack else archive.add(pack)
        }
        prefs.edit().putString(KEY_PACK_ARCHIVE, serializePacks(archive)).apply()
        state = state.copy(rulePacks = packs)
    }

    private fun loadArchive(): List<RulePack> =
        prefs.getString(KEY_PACK_ARCHIVE, null)
            ?.let { runCatching { parsePacks(it) }.getOrDefault(emptyList()) }
            .orEmpty()

    private fun allKnownCodes(): Set<String> =
        (state.rulePacks.map { it.code } + loadArchive().map { it.code })
            .filter { it.isNotBlank() }
            .toSet()

    private fun generatePackCode(): String = newCode(allKnownCodes())

    // the loop

    fun roll() {
        val hero = engine.rollHero(state.roleFilter, state.disabledHeroes)
        // the rare ??? wildcard: a constraint that decides your hero instead
        // of the roll. the chance check always runs so squad seeds stay in step.
        // wildcards live in the standard pool and make no sense hero-less
        val mystery = engine.rollMysteryChance() &&
            state.mysteryEnabled && !state.noChallengeMode && state.standardEnabled
        val challenge = (if (mystery) engine.rollMysteryChallenge(state.poolMode) else null)
            ?: engine.rollChallenge(
                hero.role,
                state.poolMode,
                extras = packExtras(),
                includeStandard = state.standardEnabled,
            )
        // always roll the punishment so the stakes toggle can show and hide the
        // same one, and so squad seeds stay in step whatever anyone toggles
        val punishment = engine.rollPunishment()
        val result = if (state.noChallengeMode) {
            RollResult(hero, emptyList(), punishment, pendingChallenge = challenge)
        } else {
            RollResult(hero, listOf(challenge), punishment)
        }
        state = state.copy(
            result = result,
            rollId = state.rollId + 1,
            mutations = 0,
            deaths = 0,
        )
    }

    /** Keep the hero, reroll the newest challenge on the stack. */
    fun mutate() {
        val current = state.result ?: return
        if (current.challenges.isEmpty()) return
        val fresh = engine.rollChallenge(
            current.hero.role,
            state.poolMode,
            exclude = current.challenges,
            extras = packExtras(),
            includeStandard = state.standardEnabled,
        )
        val challenges = current.challenges.dropLast(1) + fresh
        state = state.copy(
            result = current.copy(challenges = challenges),
            mutations = state.mutations + 1,
        )
    }

    /** Stack one more constraint on top of whatever is already rolling. */
    fun escalate() {
        val current = state.result ?: return
        // no-challenge mode: the first escalate deals the constraint the roll
        // was holding back, and it lands locked like any opener
        val pending = current.pendingChallenge
        if (pending != null) {
            state = state.copy(
                result = current.copy(
                    challenges = current.challenges + pending,
                    pendingChallenge = null,
                )
            )
            return
        }
        val extra = engine.rollChallenge(
            current.hero.role,
            state.poolMode,
            exclude = current.challenges,
            extras = packExtras(),
            includeStandard = state.standardEnabled,
        )
        state = state.copy(result = current.copy(challenges = current.challenges + extra))
    }

    /** Death mid-match means the stack grows. Tap it, respawn, suffer. */
    fun died() {
        if (state.result == null) return
        state = state.copy(deaths = state.deaths + 1)
        escalate()
    }

    /** Swipe-away for escalations. The first constraint is locked in. */
    fun removeChallenge(index: Int) {
        val current = state.result ?: return
        if (index <= 0 || index >= current.challenges.size) return
        val challenges = current.challenges.filterIndexed { i, _ -> i != index }
        state = state.copy(result = current.copy(challenges = challenges))
    }

    private companion object {
        const val PACK_CODE_PREFIX = "ELNDPACK1:"
        val SHORT_CODE = Regex("^[A-Za-z0-9]{4,8}$")
        // no lookalike characters, so codes survive being read out loud
        const val CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        const val KEY_PACK_ARCHIVE = "pack_archive"
        const val KEY_DISABLED_HEROES = "disabled_heroes"

        fun newCode(taken: Set<String>): String {
            while (true) {
                val code = buildString {
                    repeat(6) { append(CODE_CHARS[Random.nextInt(CODE_CHARS.length)]) }
                }
                if (code !in taken) return code
            }
        }

        fun serializePacks(packs: List<RulePack>): String {
            val json = JSONArray()
            packs.forEach { pack ->
                val rules = JSONArray()
                pack.rules.forEach { rule ->
                    rules.put(JSONObject().put("text", rule.text).put("tag", rule.tag))
                }
                json.put(
                    JSONObject()
                        .put("name", pack.name)
                        .put("code", pack.code)
                        .put("enabled", pack.enabled)
                        .put("rules", rules)
                )
            }
            return json.toString()
        }
        const val KEY_MYSTERY = "mystery_enabled"
        const val KEY_NO_CHALLENGE = "no_challenge_mode"
        const val KEY_CUSTOM_CHALLENGES = "custom_challenges"
        const val KEY_RULE_PACKS = "rule_packs"
        const val KEY_STANDARD = "standard_enabled"
        const val KEY_SHOW_COUNTS = "show_pool_counts"
        const val KEY_TEXT_SIZE = "text_size"
        const val KEY_HIGH_CONTRAST = "high_contrast"
        const val KEY_REDUCE_MOTION = "reduce_motion"
        const val KEY_SPIN_SECONDS = "spin_seconds"
        const val KEY_REEL_DENSITY = "reel_density"
        const val KEY_REEL_COLOR = "reel_color"
        const val KEY_OVERSHOOT = "overshoot_enabled"
        const val KEY_SUSPENSE = "suspense_enabled"
        const val KEY_LANDING_ARROW = "landing_arrow"
        const val KEY_ARROW_SIZE = "arrow_size"
        const val KEY_LANDING_UNDERLINE = "landing_underline"
        const val KEY_ARROW_COLOR = "arrow_color"
        const val KEY_ROLE_COLOR_PREFIX = "role_color_"
        const val KEY_HAPTICS = "haptics_enabled"

        fun loadPacks(prefs: SharedPreferences): List<RulePack> {
            val raw = prefs.getString(KEY_RULE_PACKS, null)
            if (raw != null) {
                return runCatching { parsePacks(raw) }.getOrDefault(emptyList())
            }
            // migrate the old flat house-rules list into a starter pack
            val legacy = prefs.getString(KEY_CUSTOM_CHALLENGES, null) ?: return emptyList()
            return runCatching {
                val json = JSONArray(legacy)
                val rules = (0 until json.length()).map { i ->
                    val entry = json.getJSONObject(i)
                    val tag = if (entry.optString("intensity") == Intensity.CHAOS.name) {
                        Intensity.CHAOS.label
                    } else {
                        Intensity.WARMUP.label
                    }
                    CustomRule(entry.getString("text"), tag)
                }
                if (rules.isEmpty()) emptyList()
                else listOf(RulePack("House rules", enabled = true, rules = rules))
            }.getOrDefault(emptyList())
        }

        fun parsePacks(raw: String): List<RulePack> {
            val json = JSONArray(raw)
            return (0 until json.length()).map { i ->
                val pack = json.getJSONObject(i)
                val rulesJson = pack.getJSONArray("rules")
                RulePack(
                    name = pack.getString("name"),
                    enabled = pack.optBoolean("enabled", true),
                    rules = (0 until rulesJson.length()).map { j ->
                        val rule = rulesJson.getJSONObject(j)
                        CustomRule(rule.getString("text"), rule.optString("tag", Intensity.CHAOS.label))
                    },
                    code = pack.optString("code", ""),
                )
            }
        }
    }
}

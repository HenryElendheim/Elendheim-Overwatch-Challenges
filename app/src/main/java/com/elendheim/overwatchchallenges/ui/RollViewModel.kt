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
import org.json.JSONArray
import org.json.JSONObject

enum class TextSize(val label: String, val scale: Float) {
    SMALL("Small", 0.85f),
    NORMAL("Normal", 1f),
    LARGE("Large", 1.15f),
    LARGER("Larger", 1.3f),
    LARGEST("Largest", 1.45f),
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
        )
    )
        private set

    private var engine = RollEngine()

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

    // rule packs

    fun createPack(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        if (state.rulePacks.any { it.name.equals(clean, ignoreCase = true) }) return
        savePacks(state.rulePacks + RulePack(clean, enabled = true, rules = emptyList()))
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

    /** A pack as a shareable code: paste it on a friend's phone, done. */
    fun exportPack(name: String): String? {
        val pack = state.rulePacks.firstOrNull { it.name == name } ?: return null
        val rules = JSONArray()
        pack.rules.forEach { rules.put(JSONObject().put("text", it.text).put("tag", it.tag)) }
        val json = JSONObject().put("name", pack.name).put("rules", rules)
        return PACK_CODE_PREFIX + Base64.encodeToString(
            json.toString().toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP,
        )
    }

    fun importPack(code: String): Boolean {
        val trimmed = code.trim()
        if (!trimmed.startsWith(PACK_CODE_PREFIX)) return false
        return runCatching {
            val decoded = String(
                Base64.decode(trimmed.removePrefix(PACK_CODE_PREFIX), Base64.DEFAULT),
                Charsets.UTF_8,
            )
            val json = JSONObject(decoded)
            val rulesJson = json.getJSONArray("rules")
            val rules = (0 until rulesJson.length()).map { i ->
                val rule = rulesJson.getJSONObject(i)
                CustomRule(rule.getString("text"), rule.optString("tag", Intensity.CHAOS.label))
            }
            var name = json.getString("name")
            var counter = 2
            while (state.rulePacks.any { it.name.equals(name, ignoreCase = true) }) {
                name = "${json.getString("name")} ($counter)"
                counter++
            }
            savePacks(state.rulePacks + RulePack(name, enabled = true, rules = rules))
            true
        }.getOrDefault(false)
    }

    private fun savePacks(packs: List<RulePack>) {
        val json = JSONArray()
        packs.forEach { pack ->
            val rules = JSONArray()
            pack.rules.forEach { rule ->
                rules.put(JSONObject().put("text", rule.text).put("tag", rule.tag))
            }
            json.put(
                JSONObject()
                    .put("name", pack.name)
                    .put("enabled", pack.enabled)
                    .put("rules", rules)
            )
        }
        prefs.edit().putString(KEY_RULE_PACKS, json.toString()).apply()
        state = state.copy(rulePacks = packs)
    }

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
        const val KEY_DISABLED_HEROES = "disabled_heroes"
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
                )
            }
        }
    }
}

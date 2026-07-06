package com.elendheim.overwatchchallenges.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.elendheim.overwatchchallenges.data.PoolMode
import com.elendheim.overwatchchallenges.data.Role
import com.elendheim.overwatchchallenges.engine.RollEngine
import com.elendheim.overwatchchallenges.engine.RollResult

data class RollUiState(
    val roleFilter: Role? = null,
    val poolMode: PoolMode = PoolMode.MIXED,
    val stakes: Boolean = false,
    val squadSeed: String? = null,
    val result: RollResult? = null,
    val rollId: Int = 0,
    val mutations: Int = 0,
    val disabledHeroes: Set<String> = emptySet(),
    val mysteryEnabled: Boolean = true,
)

class RollViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("elendheim", Context.MODE_PRIVATE)

    var state by mutableStateOf(
        RollUiState(
            disabledHeroes = prefs.getStringSet(KEY_DISABLED_HEROES, null).orEmpty().toSet(),
            mysteryEnabled = prefs.getBoolean(KEY_MYSTERY, true),
        )
    )
        private set

    private var engine = RollEngine()

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

    fun roll() {
        val hero = engine.rollHero(state.roleFilter, state.disabledHeroes)
        // the rare ??? wildcard: a constraint that decides your hero instead
        // of the roll. the chance check always runs so squad seeds stay in step
        val mystery = engine.rollMysteryChance() && state.mysteryEnabled
        val challenge = (if (mystery) engine.rollMysteryChallenge(state.poolMode) else null)
            ?: engine.rollChallenge(hero.role, state.poolMode)
        // always roll the punishment so the stakes toggle can show and hide the
        // same one, and so squad seeds stay in step whatever anyone toggles
        val punishment = engine.rollPunishment()
        state = state.copy(
            result = RollResult(hero, listOf(challenge), punishment),
            rollId = state.rollId + 1,
            mutations = 0,
        )
    }

    /** Keep the hero, reroll the newest challenge on the stack. */
    fun mutate() {
        val current = state.result ?: return
        val fresh = engine.rollChallenge(current.hero.role, state.poolMode, exclude = current.challenges)
        val challenges = current.challenges.dropLast(1) + fresh
        state = state.copy(
            result = current.copy(challenges = challenges),
            mutations = state.mutations + 1,
        )
    }

    /** Stack one more constraint on top of whatever is already rolling. */
    fun escalate() {
        val current = state.result ?: return
        val extra = engine.rollChallenge(current.hero.role, state.poolMode, exclude = current.challenges)
        state = state.copy(result = current.copy(challenges = current.challenges + extra))
    }

    /** Swipe-away for escalations. The first constraint is locked in. */
    fun removeChallenge(index: Int) {
        val current = state.result ?: return
        if (index <= 0 || index >= current.challenges.size) return
        val challenges = current.challenges.filterIndexed { i, _ -> i != index }
        state = state.copy(result = current.copy(challenges = challenges))
    }

    private companion object {
        const val KEY_DISABLED_HEROES = "disabled_heroes"
    }
}

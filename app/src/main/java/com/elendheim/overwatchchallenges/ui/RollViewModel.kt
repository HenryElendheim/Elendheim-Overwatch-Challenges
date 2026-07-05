package com.elendheim.overwatchchallenges.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
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
)

class RollViewModel : ViewModel() {

    var state by mutableStateOf(RollUiState())
        private set

    private var engine = RollEngine()

    fun setRoleFilter(role: Role?) {
        state = state.copy(roleFilter = role)
    }

    fun setPoolMode(mode: PoolMode) {
        state = state.copy(poolMode = mode)
    }

    fun toggleStakes() {
        state = state.copy(stakes = !state.stakes)
    }

    /** Blank or null drops back to solo rolling. */
    fun setSquadSeed(seed: String?) {
        val clean = seed?.trim()?.takeUnless { it.isEmpty() }
        engine = if (clean != null) RollEngine(RollEngine.seedFrom(clean)) else RollEngine()
        state = state.copy(squadSeed = clean, result = null)
    }

    fun roll() {
        val hero = engine.rollHero(state.roleFilter)
        val challenge = engine.rollChallenge(hero.role, state.poolMode)
        val punishment = if (state.stakes) engine.rollPunishment() else null
        state = state.copy(result = RollResult(hero, listOf(challenge), punishment))
    }

    /** Keep the hero, reroll the newest challenge on the stack. */
    fun mutate() {
        val current = state.result ?: return
        val fresh = engine.rollChallenge(current.hero.role, state.poolMode, exclude = current.challenges)
        val challenges = current.challenges.dropLast(1) + fresh
        state = state.copy(result = current.copy(challenges = challenges))
    }

    /** Stack one more constraint on top of whatever is already rolling. */
    fun escalate() {
        val current = state.result ?: return
        val extra = engine.rollChallenge(current.hero.role, state.poolMode, exclude = current.challenges)
        state = state.copy(result = current.copy(challenges = current.challenges + extra))
    }
}

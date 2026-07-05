package com.elendheim.overwatchchallenges.engine

import com.elendheim.overwatchchallenges.data.Challenge
import com.elendheim.overwatchchallenges.data.ChallengePool
import com.elendheim.overwatchchallenges.data.Hero
import com.elendheim.overwatchchallenges.data.PoolMode
import com.elendheim.overwatchchallenges.data.Punishments
import com.elendheim.overwatchchallenges.data.Role
import com.elendheim.overwatchchallenges.data.Roster
import kotlin.random.Random

/**
 * One roll: a hero, one or more stacked challenges, and (with stakes on)
 * a punishment for failing.
 */
data class RollResult(
    val hero: Hero,
    val challenges: List<Challenge>,
    val punishment: String? = null,
)

/**
 * All randomness goes through this so squad sync stays deterministic:
 * same seed word + same settings + same taps = same rolls for everyone.
 */
class RollEngine(seed: Long? = null) {

    private val random: Random = if (seed != null) Random(seed) else Random.Default

    fun rollHero(roleFilter: Role?): Hero {
        val pool = roleFilter?.let(Roster::byRole) ?: Roster.heroes
        return pool.random(random)
    }

    fun rollChallenge(
        role: Role,
        mode: PoolMode,
        exclude: List<Challenge> = emptyList(),
    ): Challenge {
        val pool = ChallengePool.matching(role, mode, exclude)
            .ifEmpty { ChallengePool.matching(role, mode) }
        return pool.random(random)
    }

    fun rollPunishment(): String = Punishments.all.random(random)

    companion object {
        /** Stable hash so every device turns the same squad word into the same seed. */
        fun seedFrom(phrase: String): Long {
            var hash = 1125899906842597L
            for (ch in phrase.trim().lowercase()) {
                hash = 31 * hash + ch.code
            }
            return hash
        }
    }
}

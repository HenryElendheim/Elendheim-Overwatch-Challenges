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
 * One roll: a hero, one or more stacked challenges, and a punishment for
 * failing. The punishment always gets rolled; the stakes toggle just decides
 * whether it's shown.
 *
 * In no-challenge mode the rolled constraint waits in [pendingChallenge]
 * instead of the stack; the first escalate deals it out as the locked opener.
 */
data class RollResult(
    val hero: Hero,
    val challenges: List<Challenge>,
    val punishment: String,
    val pendingChallenge: Challenge? = null,
)

/**
 * All randomness goes through this so squad sync stays deterministic:
 * same seed word + same settings + same taps = same rolls for everyone.
 */
class RollEngine(seed: Long? = null) {

    private val random: Random = if (seed != null) Random(seed) else Random.Default

    fun rollHero(roleFilter: Role?, disabled: Set<String> = emptySet()): Hero {
        val base = roleFilter?.let(Roster::byRole) ?: Roster.heroes
        // if someone bans an entire role's worth of heroes, roll from the full
        // list rather than rolling nothing
        val pool = base.filterNot { it.name in disabled }.ifEmpty { base }
        return pool.random(random)
    }

    fun rollChallenge(
        role: Role,
        mode: PoolMode,
        exclude: List<Challenge> = emptyList(),
        extras: List<Challenge> = emptyList(),
    ): Challenge {
        val pool = ChallengePool.matching(role, mode, exclude, extras)
            .ifEmpty { ChallengePool.matching(role, mode, extras = extras) }
        return pool.random(random)
    }

    fun rollPunishment(): String = Punishments.all.random(random)

    /** One roll in fifty comes up as a ??? wildcard. */
    fun rollMysteryChance(): Boolean = random.nextInt(50) == 0

    fun rollMysteryChallenge(mode: PoolMode): Challenge? {
        val pool = ChallengePool.mysteries(mode)
        return if (pool.isEmpty()) null else pool.random(random)
    }

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

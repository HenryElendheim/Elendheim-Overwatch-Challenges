package com.elendheim.overwatchchallenges.data

/**
 * The whole pool lives here. To expand the app, add entries to this list
 * (or a new Category) and everything downstream picks them up.
 */
object ChallengePool {

    val all: List<Challenge> = listOf(

        // Aim / mechanics - actually good warmup
        Challenge("Body shots only. Crits are banned.", Category.AIM, Intensity.WARMUP),
        Challenge("Crits only. An elim doesn't count unless it was a headshot.", Category.AIM, Intensity.WARMUP),
        Challenge("No abilities until your team wins the first fight.", Category.AIM, Intensity.WARMUP),
        Challenge("Reload after every single kill. No ammo-dumping.", Category.AIM, Intensity.WARMUP),
        Challenge("Never reload manually. Only when the mag runs dry.", Category.AIM, Intensity.WARMUP),
        Challenge("No healing from your supports. Survive on your own kit.", Category.AIM, Intensity.WARMUP),
        Challenge("Secondary fire only, wherever the hero allows it.", Category.AIM, Intensity.WARMUP),
        Challenge("No cooldowns until you've dealt 500 damage.", Category.AIM, Intensity.WARMUP),
        Challenge("Pick one enemy each fight and track only them. Ignore everyone else.", Category.AIM, Intensity.WARMUP),
        Challenge("Land 3 headshots before you're allowed to touch an ability.", Category.AIM, Intensity.WARMUP),

        // Movement / positioning
        Challenge("Never stop moving. Standing still is a self-imposed fail.", Category.MOVEMENT, Intensity.WARMUP),
        Challenge("High ground only. If you're on low ground, get up before you shoot.", Category.MOVEMENT, Intensity.WARMUP),
        Challenge("Always flank. The direct route to the fight is banned.", Category.MOVEMENT, Intensity.WARMUP),
        Challenge("No backpedaling, ever. Advance or strafe.", Category.MOVEMENT, Intensity.WARMUP),
        Challenge("Stay within 10 meters of a teammate the whole match. Buddy system.", Category.MOVEMENT, Intensity.WARMUP),
        Challenge("Never use the same route to the point twice.", Category.MOVEMENT, Intensity.WARMUP),
        Challenge("Melee finals only. Chip them down, punch to finish.", Category.MOVEMENT, Intensity.CHAOS),

        // Ability restrictions
        Challenge("No ultimate the entire game.", Category.ABILITIES, Intensity.WARMUP),
        Challenge("One-ability challenge. Pick a single ability, that's all you use.", Category.ABILITIES, Intensity.CHAOS),
        Challenge("Ults on cooldown. Use it the instant it's up, no saving.", Category.ABILITIES, Intensity.CHAOS),
        Challenge("No defensive cooldowns. Offense abilities only.", Category.ABILITIES, Intensity.WARMUP),
        Challenge("Movement abilities only for escaping, never for engaging.", Category.ABILITIES, Intensity.WARMUP),

        // Hero-pool chaos
        Challenge("Play the hero you have the fewest hours on.", Category.HERO_POOL, Intensity.CHAOS, overridesHero = true),
        Challenge("Play your most-hated hero and try to enjoy it.", Category.HERO_POOL, Intensity.CHAOS, overridesHero = true),
        Challenge("Swap hero after every death.", Category.HERO_POOL, Intensity.CHAOS),
        Challenge("Swap hero every time you get a kill.", Category.HERO_POOL, Intensity.CHAOS),
        Challenge("Mirror the enemy. Copy whatever their last-picked hero was.", Category.HERO_POOL, Intensity.CHAOS, overridesHero = true),
        Challenge("Play of the game mindset. Go for the highlight, every fight.", Category.HERO_POOL, Intensity.CHAOS),
        Challenge("Off-role roulette. Queue the role you play least and live with it.", Category.HERO_POOL, Intensity.CHAOS, overridesHero = true),

        // Comms / mental
        Challenge("Call every ult and cooldown out loud, even in solo queue.", Category.COMMS, Intensity.WARMUP),
        Challenge("Compliment an enemy in chat after they beat you. Tilt-proofing.", Category.COMMS, Intensity.WARMUP),
        Challenge("No looking at the kill feed. Play purely on what you see.", Category.COMMS, Intensity.WARMUP),
        Challenge("Positive callouts only. Zero complaining in chat, all match.", Category.COMMS, Intensity.WARMUP),
        Challenge("Shotcall the entire match, even if nobody listens.", Category.COMMS, Intensity.WARMUP),

        // Pure meme - customs and friends
        Challenge("No shooting. Melee and abilities only.", Category.MEME, Intensity.CHAOS),
        Challenge("Emote after every kill. Dangerous. Glorious.", Category.MEME, Intensity.CHAOS),
        Challenge("Crouch-walk everywhere. Spy mode.", Category.MEME, Intensity.CHAOS),
        Challenge("Say hello (voice line) before engaging anyone.", Category.MEME, Intensity.CHAOS),
        Challenge("Pick one teammate and bodyguard them. Their death is your loss.", Category.MEME, Intensity.CHAOS),
        Challenge("Everyone rolls a random hero, no swaps, play it out.", Category.MEME, Intensity.CHAOS),
        Challenge("You're locked to the last hero in your role's roster, alphabetically.", Category.MEME, Intensity.CHAOS, overridesHero = true),
        Challenge("Whisper strat. No callout louder than one word.", Category.MEME, Intensity.CHAOS),

        // Support-specific
        Challenge("Heal one teammate exclusively, the entire match.", Category.SUPPORT, Intensity.CHAOS, roles = setOf(Role.SUPPORT)),
        Challenge("Damage-boost and discord priority. You're a battle-medic, not a pocket.", Category.SUPPORT, Intensity.WARMUP, roles = setOf(Role.SUPPORT)),
        Challenge("Never heal anyone above 50%. Top-offs are banned, triage only.", Category.SUPPORT, Intensity.CHAOS, roles = setOf(Role.SUPPORT)),
        Challenge("Out-damage your healing this game.", Category.SUPPORT, Intensity.WARMUP, roles = setOf(Role.SUPPORT)),

        // Tank-specific
        Challenge("Never retreat first. You're the last one out of every fight.", Category.TANK, Intensity.WARMUP, roles = setOf(Role.TANK)),
        Challenge("Hold exactly one choke. Never overextend past it.", Category.TANK, Intensity.WARMUP, roles = setOf(Role.TANK)),
        Challenge("Create space with your body only. Abilities banned.", Category.TANK, Intensity.CHAOS, roles = setOf(Role.TANK)),
        Challenge("Peel for your supports all game. The backline is your only job.", Category.TANK, Intensity.WARMUP, roles = setOf(Role.TANK)),
    )

    /**
     * Everything a given hero role is allowed to draw in the given mode,
     * minus anything already on the stack. Hero-overriding constraints are
     * excluded here; they only show up through the rare ??? wildcard path.
     */
    fun matching(
        role: Role,
        mode: PoolMode,
        exclude: Collection<Challenge> = emptyList(),
        extras: List<Challenge> = emptyList(),
        includeStandard: Boolean = true,
    ): List<Challenge> {
        val base = if (includeStandard) all + extras else extras
        return base.filter { challenge ->
            val roleOk = challenge.roles == null || role in challenge.roles
            !challenge.overridesHero && roleOk && matchesMode(challenge, mode) && challenge !in exclude
        }
    }

    /** The wildcard pool: constraints that decide your hero for you. */
    fun mysteries(mode: PoolMode): List<Challenge> =
        all.filter { it.overridesHero && matchesMode(it, mode) }

    private fun matchesMode(challenge: Challenge, mode: PoolMode): Boolean = when (mode) {
        // custom-tagged pack rules only surface in the everything-goes pool
        PoolMode.MIXED -> true
        PoolMode.WARMUP -> challenge.customTag == null && challenge.intensity == Intensity.WARMUP
        PoolMode.CHAOS -> challenge.customTag == null && challenge.intensity == Intensity.CHAOS
    }
}

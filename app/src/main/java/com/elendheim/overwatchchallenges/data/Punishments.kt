package com.elendheim.overwatchchallenges.data

/**
 * Stakes mode pulls one of these alongside the roll. Fail the challenge and
 * the punishment applies to your next game.
 */
object Punishments {

    val all: List<String> = listOf(
        "Next roll comes from the chaos pool, whatever your toggle says.",
        "Escalate twice before your next game starts.",
        "Next game is off-role. Queue the role you play least.",
        "Crouch-walk out of spawn after every death next game.",
        "No ultimate at all next game.",
        "Emote after every death next game. Yes, every single one.",
        "Next game you type nothing in chat except compliments.",
        "Mutate is banned on your next roll. First roll is final.",
    )
}

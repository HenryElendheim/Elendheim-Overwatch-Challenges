package com.elendheim.overwatchchallenges.data

enum class Role(val label: String) {
    TANK("Tank"),
    DAMAGE("Damage"),
    SUPPORT("Support"),
}

data class Hero(val name: String, val role: Role)

object Roster {

    val heroes: List<Hero> = listOf(
        // Tank
        Hero("D.Va", Role.TANK),
        Hero("Domina", Role.TANK),
        Hero("Doomfist", Role.TANK),
        Hero("Hazard", Role.TANK),
        Hero("Junker Queen", Role.TANK),
        Hero("Mauga", Role.TANK),
        Hero("Orisa", Role.TANK),
        Hero("Ramattra", Role.TANK),
        Hero("Reinhardt", Role.TANK),
        Hero("Roadhog", Role.TANK),
        Hero("Sigma", Role.TANK),
        Hero("Winston", Role.TANK),
        Hero("Wrecking Ball", Role.TANK),
        Hero("Zarya", Role.TANK),

        // Damage
        Hero("Anran", Role.DAMAGE),
        Hero("Ashe", Role.DAMAGE),
        Hero("Bastion", Role.DAMAGE),
        Hero("Cassidy", Role.DAMAGE),
        Hero("Echo", Role.DAMAGE),
        Hero("Emre", Role.DAMAGE),
        Hero("Freja", Role.DAMAGE),
        Hero("Genji", Role.DAMAGE),
        Hero("Hanzo", Role.DAMAGE),
        Hero("Junkrat", Role.DAMAGE),
        Hero("Mei", Role.DAMAGE),
        Hero("Pharah", Role.DAMAGE),
        Hero("Reaper", Role.DAMAGE),
        Hero("Shion", Role.DAMAGE),
        Hero("Sierra", Role.DAMAGE),
        Hero("Sojourn", Role.DAMAGE),
        Hero("Soldier: 76", Role.DAMAGE),
        Hero("Sombra", Role.DAMAGE),
        Hero("Symmetra", Role.DAMAGE),
        Hero("Torbjörn", Role.DAMAGE),
        Hero("Tracer", Role.DAMAGE),
        Hero("Vendetta", Role.DAMAGE),
        Hero("Venture", Role.DAMAGE),
        Hero("Widowmaker", Role.DAMAGE),

        // Support
        Hero("Ana", Role.SUPPORT),
        Hero("Baptiste", Role.SUPPORT),
        Hero("Brigitte", Role.SUPPORT),
        Hero("Illari", Role.SUPPORT),
        Hero("Jetpack Cat", Role.SUPPORT),
        Hero("Juno", Role.SUPPORT),
        Hero("Kiriko", Role.SUPPORT),
        Hero("Lifeweaver", Role.SUPPORT),
        Hero("Lúcio", Role.SUPPORT),
        Hero("Mercy", Role.SUPPORT),
        Hero("Mizuki", Role.SUPPORT),
        Hero("Moira", Role.SUPPORT),
        Hero("Wuyang", Role.SUPPORT),
        Hero("Zenyatta", Role.SUPPORT),
    )

    fun byRole(role: Role): List<Hero> = heroes.filter { it.role == role }
}

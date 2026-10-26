package com.example.samuraisprint.model

import androidx.compose.ui.graphics.Color

data class Warrior(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val primaryColor: Color,
    val accentColor: Color,
    val speedMultiplier: Float = 1.0f,
    val jumpBonus: Float = 0.0f,
    val slashRangeBonus: Float = 0.0f,
    val coinMultiplier: Float = 1.0f,
    val extraHearts: Int = 0,
    val unlockCost: Int = 0,
    val isUnlocked: Boolean = false
)

data class Katana(
    val id: String,
    val name: String,
    val description: String,
    val bladeColor: Color,
    val trailColor: Color,
    val specialEffect: String,
    val unlockCost: Int = 0,
    val isUnlocked: Boolean = false
)

enum class UpgradeType(
    val title: String,
    val description: String,
    val iconName: String,
    val maxLevel: Int,
    val baseCost: Int
) {
    SLASH_REACH(
        title = "Blade Extension",
        description = "Increases katana slash strike hit box distance.",
        iconName = "content_cut",
        maxLevel = 5,
        baseCost = 150
    ),
    MAX_HEALTH(
        title = "Bushido Vitality",
        description = "Grants extra maximum hearts for runs.",
        iconName = "favorite",
        maxLevel = 5,
        baseCost = 250
    ),
    FOCUS_CHARGE(
        title = "Spirit Gathering",
        description = "Earn Dragon Sprint focus meter faster from slashes.",
        iconName = "bolt",
        maxLevel = 5,
        baseCost = 180
    ),
    COIN_MAGNET(
        title = "Fortune Attunement",
        description = "Automatically pull spirit coins from farther away.",
        iconName = "stars",
        maxLevel = 5,
        baseCost = 120
    ),
    COMBO_MASTERY(
        title = "Sword Dance Flow",
        description = "Extends combo timer window and increases score multiplier.",
        iconName = "speed",
        maxLevel = 5,
        baseCost = 200
    )
}

data class HonorTrial(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val rewardCoins: Int,
    val progress: Int = 0,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false
)

enum class StyleRank(val label: String, val multiplier: Float, val color: Color) {
    D("DISCIPLE", 1.0f, Color(0xFFAAAAAA)),
    C("CHUNIN", 1.5f, Color(0xFF4FC3F7)),
    B("BUSHIDO", 2.0f, Color(0xFF81C784)),
    A("ASSASSIN", 2.5f, Color(0xFFFFB74D)),
    S("SWORDMASTER", 3.5f, Color(0xFFFF5252)),
    SSS("SHINOBI GOD", 5.0f, Color(0xFFFFD700));

    companion object {
        fun fromCombo(combo: Int): StyleRank = when {
            combo >= 25 -> SSS
            combo >= 18 -> S
            combo >= 12 -> A
            combo >= 7 -> B
            combo >= 3 -> C
            else -> D
        }
    }
}

enum class ObstacleKind {
    BAMBOO_STAKE,
    ROLLING_BARREL,
    LOW_BEAM,
    TENGU_CROW,
    SHADOW_RONIN,
    FLYING_SHURIKEN,
    SPIRIT_MON,
    SPIRIT_ORB,
    JADE_HEART,
    ONI_BOSS
}

data class ActiveEntity(
    val id: Long,
    val kind: ObstacleKind,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var health: Int = 1,
    var isDeflected: Boolean = false,
    var isCollected: Boolean = false,
    var isSliced: Boolean = false,
    var sliceProgress: Float = 0f,
    var animTimer: Float = 0f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    val color: Color,
    var alpha: Float = 1.0f,
    var life: Float = 1.0f,
    val isPetal: Boolean = false,
    var rotation: Float = 0f,
    var rotSpeed: Float = 0f
)

data class FloatingCombatText(
    val id: Long,
    val text: String,
    var x: Float,
    var y: Float,
    val color: Color,
    var alpha: Float = 1.0f,
    var scale: Float = 1.2f
)

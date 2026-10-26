package com.example.samuraisprint.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.example.samuraisprint.model.HonorTrial
import com.example.samuraisprint.model.Katana
import com.example.samuraisprint.model.UpgradeType
import com.example.samuraisprint.model.Warrior
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("samurai_sprint_prefs", Context.MODE_PRIVATE)

    private val _coins = MutableStateFlow(prefs.getInt(KEY_COINS, 250))
    val coins: StateFlow<Int> = _coins.asStateFlow()

    private val _highScore = MutableStateFlow(prefs.getInt(KEY_HIGH_SCORE, 0))
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    private val _bestDistance = MutableStateFlow(prefs.getInt(KEY_BEST_DISTANCE, 0))
    val bestDistance: StateFlow<Int> = _bestDistance.asStateFlow()

    private val _totalSlices = MutableStateFlow(prefs.getInt(KEY_TOTAL_SLICES, 0))
    val totalSlices: StateFlow<Int> = _totalSlices.asStateFlow()

    private val _totalParries = MutableStateFlow(prefs.getInt(KEY_TOTAL_PARRIES, 0))
    val totalParries: StateFlow<Int> = _totalParries.asStateFlow()

    private val _selectedWarriorId = MutableStateFlow(prefs.getString(KEY_WARRIOR, "kage") ?: "kage")
    val selectedWarriorId: StateFlow<String> = _selectedWarriorId.asStateFlow()

    private val _selectedKatanaId = MutableStateFlow(prefs.getString(KEY_KATANA, "steel") ?: "steel")
    val selectedKatanaId: StateFlow<String> = _selectedKatanaId.asStateFlow()

    private val _unlockedWarriorIds = MutableStateFlow(loadUnlockedWarriors())
    val unlockedWarriorIds: StateFlow<Set<String>> = _unlockedWarriorIds.asStateFlow()

    private val _unlockedKatanaIds = MutableStateFlow(loadUnlockedKatanas())
    val unlockedKatanaIds: StateFlow<Set<String>> = _unlockedKatanaIds.asStateFlow()

    // Upgrades map: UpgradeType -> Level (1..5)
    private val _upgradeLevels = MutableStateFlow(loadUpgrades())
    val upgradeLevels: StateFlow<Map<UpgradeType, Int>> = _upgradeLevels.asStateFlow()

    val warriors = listOf(
        Warrior(
            id = "kage",
            name = "Kage",
            title = "Shadow Ronin",
            description = "A swift wanderer of the night. Master of balanced acrobatic sprints and deadly swift slashes.",
            primaryColor = Color(0xFF2B2D42),
            accentColor = Color(0xFFE52538),
            speedMultiplier = 1.0f,
            jumpBonus = 0f,
            slashRangeBonus = 0f,
            coinMultiplier = 1.0f,
            unlockCost = 0,
            isUnlocked = true
        ),
        Warrior(
            id = "tomoe",
            name = "Tomoe",
            title = "Sakura Kunoichi",
            description = "Elegance meets lethality. Possesses triple-jump acrobatics and harvests spirit orbs with grace.",
            primaryColor = Color(0xFFD81B60),
            accentColor = Color(0xFFFF80AB),
            speedMultiplier = 1.05f,
            jumpBonus = 0.15f,
            slashRangeBonus = 0.1f,
            coinMultiplier = 1.25f,
            unlockCost = 500,
            isUnlocked = isWarriorUnlocked("tomoe")
        ),
        Warrior(
            id = "musashi",
            name = "Musashi",
            title = "Twin Blade Master",
            description = "Invincible under the heavens. Massive katana reach and doubled combo multiplier gain.",
            primaryColor = Color(0xFFE65100),
            accentColor = Color(0xFFFFB74D),
            speedMultiplier = 1.1f,
            jumpBonus = -0.05f,
            slashRangeBonus = 0.35f,
            coinMultiplier = 1.15f,
            unlockCost = 1200,
            isUnlocked = isWarriorUnlocked("musashi")
        ),
        Warrior(
            id = "jin",
            name = "Jin",
            title = "Ghost of Tsushima",
            description = "Unyielding fortress of iron will. Starts runs with +2 bonus hearts and prolonged focus surge.",
            primaryColor = Color(0xFF004D40),
            accentColor = Color(0xFF00E5FF),
            speedMultiplier = 0.98f,
            jumpBonus = 0.05f,
            slashRangeBonus = 0.15f,
            coinMultiplier = 1.35f,
            extraHearts = 2,
            unlockCost = 2500,
            isUnlocked = isWarriorUnlocked("jin")
        )
    )

    val katanas = listOf(
        Katana(
            id = "steel",
            name = "Tamahagane Steel",
            description = "Forged by ancestral master swordsmiths. Reliable and balanced razor edge.",
            bladeColor = Color(0xFFECEFF1),
            trailColor = Color(0xFFFFFFFF),
            specialEffect = "Standard razor sharp strike",
            unlockCost = 0,
            isUnlocked = true
        ),
        Katana(
            id = "crimson_dragon",
            name = "Crimson Dragon",
            description = "Infused with dragon blood. Leaves fiery crimson slashes and cleaves through obstacles.",
            bladeColor = Color(0xFFFF1744),
            trailColor = Color(0xFFFF5252),
            specialEffect = "+25% Slash Hitbox & Flaming Trail",
            unlockCost = 400,
            isUnlocked = isKatanaUnlocked("crimson_dragon")
        ),
        Katana(
            id = "thunder_flash",
            name = "Raikiri (Thunder Slicer)",
            description = "Said to have sliced a bolt of lightning. Emits crackling electric sparks that draw nearby coins.",
            bladeColor = Color(0xFFFFD600),
            trailColor = Color(0xFF00E5FF),
            specialEffect = "Magnetizes spirit coins on strike",
            unlockCost = 900,
            isUnlocked = isKatanaUnlocked("thunder_flash")
        ),
        Katana(
            id = "sakura_void",
            name = "Sakura Blossom Void",
            description = "Forged under sacred blossoms. Generates whirlwind petal vortex and boosts focus bar rapidly.",
            bladeColor = Color(0xFFE040FB),
            trailColor = Color(0xFFFF4081),
            specialEffect = "+50% Focus meter generation",
            unlockCost = 1800,
            isUnlocked = isKatanaUnlocked("sakura_void")
        )
    )

    private fun loadUnlockedWarriors(): Set<String> {
        val set = mutableSetOf("kage")
        listOf("tomoe", "musashi", "jin").forEach { id ->
            if (prefs.getBoolean("unlocked_warrior_$id", false)) set.add(id)
        }
        return set
    }

    private fun loadUnlockedKatanas(): Set<String> {
        val set = mutableSetOf("steel")
        listOf("crimson_dragon", "thunder_flash", "sakura_void").forEach { id ->
            if (prefs.getBoolean("unlocked_katana_$id", false)) set.add(id)
        }
        return set
    }

    fun getSelectedWarrior(): Warrior {
        val id = _selectedWarriorId.value
        return warriors.find { it.id == id } ?: warriors[0]
    }

    fun getSelectedKatana(): Katana {
        val id = _selectedKatanaId.value
        return katanas.find { it.id == id } ?: katanas[0]
    }

    private fun isWarriorUnlocked(id: String): Boolean {
        if (id == "kage") return true
        return prefs.getBoolean("unlocked_warrior_$id", false)
    }

    private fun isKatanaUnlocked(id: String): Boolean {
        if (id == "steel") return true
        return prefs.getBoolean("unlocked_katana_$id", false)
    }

    fun unlockWarrior(warrior: Warrior): Boolean {
        val currentCoins = _coins.value
        if (currentCoins >= warrior.unlockCost && !_unlockedWarriorIds.value.contains(warrior.id)) {
            val updatedCoins = currentCoins - warrior.unlockCost
            _coins.value = updatedCoins
            _unlockedWarriorIds.value = _unlockedWarriorIds.value + warrior.id
            prefs.edit()
                .putInt(KEY_COINS, updatedCoins)
                .putBoolean("unlocked_warrior_${warrior.id}", true)
                .apply()
            return true
        }
        return false
    }

    fun unlockKatana(katana: Katana): Boolean {
        val currentCoins = _coins.value
        if (currentCoins >= katana.unlockCost && !_unlockedKatanaIds.value.contains(katana.id)) {
            val updatedCoins = currentCoins - katana.unlockCost
            _coins.value = updatedCoins
            _unlockedKatanaIds.value = _unlockedKatanaIds.value + katana.id
            prefs.edit()
                .putInt(KEY_COINS, updatedCoins)
                .putBoolean("unlocked_katana_${katana.id}", true)
                .apply()
            return true
        }
        return false
    }

    fun selectWarrior(id: String) {
        _selectedWarriorId.value = id
        prefs.edit().putString(KEY_WARRIOR, id).apply()
    }

    fun selectKatana(id: String) {
        _selectedKatanaId.value = id
        prefs.edit().putString(KEY_KATANA, id).apply()
    }

    private fun loadUpgrades(): Map<UpgradeType, Int> {
        val map = mutableMapOf<UpgradeType, Int>()
        UpgradeType.entries.forEach { type ->
            val lvl = prefs.getInt("upgrade_${type.name}", 1).coerceIn(1, type.maxLevel)
            map[type] = lvl
        }
        return map
    }

    fun upgradeSkill(type: UpgradeType): Boolean {
        val currentLevel = _upgradeLevels.value[type] ?: 1
        if (currentLevel >= type.maxLevel) return false
        val cost = getUpgradeCost(type, currentLevel)
        val currentCoins = _coins.value
        if (currentCoins >= cost) {
            val newCoins = currentCoins - cost
            val newLevel = currentLevel + 1
            _coins.value = newCoins
            val updatedMap = _upgradeLevels.value.toMutableMap()
            updatedMap[type] = newLevel
            _upgradeLevels.value = updatedMap

            prefs.edit()
                .putInt(KEY_COINS, newCoins)
                .putInt("upgrade_${type.name}", newLevel)
                .apply()
            return true
        }
        return false
    }

    fun getUpgradeCost(type: UpgradeType, level: Int): Int {
        return (type.baseCost * (1.0 + (level - 1) * 0.75)).toInt()
    }

    fun addCoins(amount: Int) {
        val updated = _coins.value + amount
        _coins.value = updated
        prefs.edit().putInt(KEY_COINS, updated).apply()
    }

    fun saveRunResults(distance: Int, score: Int, coinsCollected: Int, slices: Int, parries: Int) {
        val newCoins = _coins.value + coinsCollected
        _coins.value = newCoins

        var newHigh = _highScore.value
        if (score > newHigh) {
            newHigh = score
            _highScore.value = newHigh
        }

        var newBestDist = _bestDistance.value
        if (distance > newBestDist) {
            newBestDist = distance
            _bestDistance.value = newBestDist
        }

        val newSlices = _totalSlices.value + slices
        _totalSlices.value = newSlices

        val newParries = _totalParries.value + parries
        _totalParries.value = newParries

        prefs.edit()
            .putInt(KEY_COINS, newCoins)
            .putInt(KEY_HIGH_SCORE, newHigh)
            .putInt(KEY_BEST_DISTANCE, newBestDist)
            .putInt(KEY_TOTAL_SLICES, newSlices)
            .putInt(KEY_TOTAL_PARRIES, newParries)
            .apply()
    }

    fun getHonorTrials(): List<HonorTrial> {
        val slices = _totalSlices.value
        val parries = _totalParries.value
        val bestDist = _bestDistance.value
        val highSc = _highScore.value

        val trialTemplates = listOf(
            TrialDef("t_first_blood", "Way of the Blade", "Slice 15 enemies with your katana", 15, slices, 100),
            TrialDef("t_slayer_100", "Master Executioner", "Defeat 100 oncoming enemy ninja", 100, slices, 300),
            TrialDef("t_parry_master", "Iron Reflection", "Successfully parry or deflect 10 ninja projectiles", 10, parries, 250),
            TrialDef("t_distance_500", "Torii Wayfarer", "Sprint 500 meters into feudal lands", 500, bestDist, 150),
            TrialDef("t_distance_1500", "Netherrealm Infiltrator", "Survive past 1,500 meters in a single run", 1500, bestDist, 500),
            TrialDef("t_score_5000", "Bushido Legend", "Score over 5,000 points in one sprint", 5000, highSc, 400)
        )

        return trialTemplates.map { def ->
            val isClaimed = prefs.getBoolean("trial_claimed_${def.id}", false)
            val isDone = def.current >= def.target
            HonorTrial(
                id = def.id,
                title = def.title,
                description = def.description,
                target = def.target,
                rewardCoins = def.reward,
                progress = def.current.coerceAtMost(def.target),
                isCompleted = isDone,
                isClaimed = isClaimed
            )
        }
    }

    fun claimTrialReward(trialId: String, reward: Int): Boolean {
        if (!prefs.getBoolean("trial_claimed_$trialId", false)) {
            prefs.edit().putBoolean("trial_claimed_$trialId", true).apply()
            addCoins(reward)
            return true
        }
        return false
    }

    private data class TrialDef(
        val id: String,
        val title: String,
        val description: String,
        val target: Int,
        val current: Int,
        val reward: Int
    )

    companion object {
        private const val KEY_COINS = "coins"
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_BEST_DISTANCE = "best_distance"
        private const val KEY_TOTAL_SLICES = "total_slices"
        private const val KEY_TOTAL_PARRIES = "total_parries"
        private const val KEY_WARRIOR = "selected_warrior"
        private const val KEY_KATANA = "selected_katana"
    }
}

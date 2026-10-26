package com.example.samuraisprint.engine

import androidx.compose.ui.graphics.Color
import com.example.samuraisprint.audio.SoundManager
import com.example.samuraisprint.data.GameRepository
import com.example.samuraisprint.model.ActiveEntity
import com.example.samuraisprint.model.FloatingCombatText
import com.example.samuraisprint.model.Katana
import com.example.samuraisprint.model.ObstacleKind
import com.example.samuraisprint.model.Particle
import com.example.samuraisprint.model.StyleRank
import com.example.samuraisprint.model.UpgradeType
import com.example.samuraisprint.model.Warrior
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(
    private val repository: GameRepository
) {
    // Canvas virtual coordinates
    val worldWidth = 1000f
    val worldHeight = 600f
    val groundY = 460f

    // Game states
    val isRunning = MutableStateFlow(false)
    val isPaused = MutableStateFlow(false)
    val isGameOver = MutableStateFlow(false)

    // Player physical stats
    var playerX = 140f
    var playerY = groundY
    var playerVelY = 0f
    var isGrounded = true
    var jumpCount = 0
    var maxJumps = 2

    // Action states
    var isSlashing = false
    var slashTimer = 0f
    var slashDuration = 0.28f
    var slashCooldown = 0f

    var isSliding = false
    var slideTimer = 0f
    var slideDuration = 0.45f

    var isInvulnerable = false
    var invulnerableTimer = 0f

    // Dragon Sprint mode
    var isDragonSprint = false
    var dragonSprintTimer = 0f
    val dragonSprintMaxDuration = 6.0f
    val focusMeter = MutableStateFlow(0f) // 0..100

    // Stats
    val score = MutableStateFlow(0)
    val distance = MutableStateFlow(0f)
    val coinsCollected = MutableStateFlow(0)
    val currentHearts = MutableStateFlow(3)
    val maxHearts = MutableStateFlow(3)
    val comboCount = MutableStateFlow(0)
    var comboTimer = 0f
    val comboMaxTime = 2.8f
    val currentRank = MutableStateFlow(StyleRank.D)

    var runSlices = 0
    var runParries = 0

    // Speed & world progression
    var currentSpeed = 380f
    val baseSpeed = 380f

    // Entities & effects
    val entities = CopyOnWriteArrayList<ActiveEntity>()
    val particles = CopyOnWriteArrayList<Particle>()
    val floatingTexts = CopyOnWriteArrayList<FloatingCombatText>()

    // Parallax positions
    var bgFarX = 0f
    var bgMidX = 0f
    var bgNearX = 0f

    // Warrior & katana active configurations
    lateinit var warrior: Warrior
    lateinit var katana: Katana
    private var upgrades: Map<UpgradeType, Int> = emptyMap()

    private var spawnTimer = 0f
    private var nextSpawnTime = 1.8f
    private var entityIdCounter = 0L
    private var textIdCounter = 0L

    init {
        initSakuraPetals()
    }

    private fun initSakuraPetals() {
        particles.clear()
        for (i in 0 until 35) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * worldWidth,
                    y = Random.nextFloat() * worldHeight,
                    vx = -(Random.nextFloat() * 60f + 40f),
                    vy = Random.nextFloat() * 30f + 15f,
                    size = Random.nextFloat() * 7f + 5f,
                    color = if (Random.nextBoolean()) Color(0xFFFFB7B2) else Color(0xFFFF69B4),
                    alpha = Random.nextFloat() * 0.6f + 0.3f,
                    life = 100f,
                    isPetal = true,
                    rotation = Random.nextFloat() * 360f,
                    rotSpeed = Random.nextFloat() * 60f - 30f
                )
            )
        }
    }

    fun startRun() {
        warrior = repository.getSelectedWarrior()
        katana = repository.getSelectedKatana()
        upgrades = repository.upgradeLevels.value

        val vitalityLvl = upgrades[UpgradeType.MAX_HEALTH] ?: 1
        val computedMaxHearts = 3 + (vitalityLvl - 1) + warrior.extraHearts
        maxHearts.value = computedMaxHearts
        currentHearts.value = computedMaxHearts

        maxJumps = if (warrior.id == "tomoe") 3 else 2

        score.value = 0
        distance.value = 0f
        coinsCollected.value = 0
        comboCount.value = 0
        comboTimer = 0f
        currentRank.value = StyleRank.D
        focusMeter.value = 0f
        isDragonSprint = false
        dragonSprintTimer = 0f

        runSlices = 0
        runParries = 0

        playerX = 140f
        playerY = groundY
        playerVelY = 0f
        isGrounded = true
        jumpCount = 0

        isSlashing = false
        slashTimer = 0f
        slashCooldown = 0f

        isSliding = false
        slideTimer = 0f

        isInvulnerable = false
        invulnerableTimer = 0f

        currentSpeed = baseSpeed * warrior.speedMultiplier
        entities.clear()
        floatingTexts.clear()
        spawnTimer = 0f
        nextSpawnTime = 1.6f

        initSakuraPetals()

        isGameOver.value = false
        isPaused.value = false
        isRunning.value = true
    }

    fun jump() {
        if (!isRunning.value || isPaused.value || isGameOver.value) return
        if (isSliding) {
            isSliding = false
            slideTimer = 0f
        }

        if (jumpCount < maxJumps) {
            val bonus = warrior.jumpBonus
            val jumpImpulse = if (jumpCount == 0) -760f * (1f + bonus) else -680f * (1f + bonus)
            playerVelY = jumpImpulse
            isGrounded = false
            jumpCount++
            SoundManager.playSound(SoundManager.SoundType.JUMP)
            SoundManager.vibrate(25, 80)

            // Jump dust particles
            createDustParticles(playerX + 20f, playerY)
        }
    }

    fun slash() {
        if (!isRunning.value || isPaused.value || isGameOver.value) return
        if (slashCooldown > 0f) return

        isSlashing = true
        slashTimer = slashDuration
        slashCooldown = 0.16f
        SoundManager.playSound(SoundManager.SoundType.SLASH)
        SoundManager.vibrate(35, 140)

        // Slash arc spark particles
        createSlashTrailParticles()

        // Check combat hits immediately
        performCombatSlash()
    }

    fun slide() {
        if (!isRunning.value || isPaused.value || isGameOver.value) return
        if (!isGrounded) {
            // Dive downwards if in air!
            playerVelY = 600f
            return
        }
        if (!isSliding) {
            isSliding = true
            slideTimer = slideDuration
            SoundManager.vibrate(20, 60)
            createDustParticles(playerX, playerY)
        }
    }

    fun triggerDragonSprint() {
        if (!isRunning.value || isPaused.value || isGameOver.value) return
        if (focusMeter.value >= 100f && !isDragonSprint) {
            isDragonSprint = true
            dragonSprintTimer = dragonSprintMaxDuration
            focusMeter.value = 100f
            SoundManager.playSound(SoundManager.SoundType.DRAGON_SPRINT)
            SoundManager.vibrate(100, 255)
            spawnFloatingText("DRAGON SPRINT!", playerX + 50f, playerY - 80f, Color(0xFFFFD700))
        }
    }

    fun togglePause() {
        if (isRunning.value && !isGameOver.value) {
            isPaused.value = !isPaused.value
        }
    }

    fun update(deltaSeconds: Float) {
        if (!isRunning.value || isPaused.value || isGameOver.value) return

        val dt = deltaSeconds.coerceIn(0.005f, 0.05f)

        // Distance & speed progression
        val distAdd = currentSpeed * dt * 0.05f
        distance.value += distAdd

        // Gradually accelerate game speed
        val targetSpeed = (baseSpeed + (distance.value * 0.08f).coerceAtMost(250f)) *
                (if (isDragonSprint) 1.55f else 1.0f) * warrior.speedMultiplier
        currentSpeed = currentSpeed + (targetSpeed - currentSpeed) * 0.05f

        // Parallax background movement
        bgFarX = (bgFarX - currentSpeed * dt * 0.12f) % worldWidth
        bgMidX = (bgMidX - currentSpeed * dt * 0.45f) % worldWidth
        bgNearX = (bgNearX - currentSpeed * dt * 0.95f) % worldWidth

        // Gravity & Player Physics
        val gravity = 1850f
        if (!isGrounded) {
            playerVelY += gravity * dt
            playerY += playerVelY * dt

            if (playerY >= groundY) {
                playerY = groundY
                playerVelY = 0f
                isGrounded = true
                jumpCount = 0
                createDustParticles(playerX + 20f, groundY)
            }
        }

        // Action timers
        if (slashCooldown > 0f) {
            slashCooldown -= dt
        }

        if (isSlashing) {
            slashTimer -= dt
            if (slashTimer <= 0f) {
                isSlashing = false
            } else {
                performCombatSlash()
            }
        }

        if (isSliding) {
            slideTimer -= dt
            if (slideTimer <= 0f) {
                isSliding = false
            }
        }

        if (isInvulnerable) {
            invulnerableTimer -= dt
            if (invulnerableTimer <= 0f) {
                isInvulnerable = false
            }
        }

        // Dragon Sprint timer
        if (isDragonSprint) {
            dragonSprintTimer -= dt
            focusMeter.value = (dragonSprintTimer / dragonSprintMaxDuration) * 100f
            if (dragonSprintTimer <= 0f) {
                isDragonSprint = false
                focusMeter.value = 0f
            }
        }

        // Combo timer decay
        if (comboCount.value > 0) {
            comboTimer -= dt
            if (comboTimer <= 0f) {
                comboCount.value = 0
                currentRank.value = StyleRank.D
            }
        }

        // Spawn obstacles & collectibles
        spawnTimer += dt
        if (spawnTimer >= nextSpawnTime) {
            spawnTimer = 0f
            spawnEntityGroup()
            nextSpawnTime = (1.4f - (distance.value / 2500f).coerceAtMost(0.6f)) + Random.nextFloat() * 0.8f
        }

        // Update active entities
        updateEntities(dt)

        // Update floating texts
        updateFloatingTexts(dt)

        // Update particles
        updateParticles(dt)
    }

    private fun performCombatSlash() {
        val slashLvl = upgrades[UpgradeType.SLASH_REACH] ?: 1
        val extraReach = (slashLvl - 1) * 20f + (warrior.slashRangeBonus * 50f)
        val slashReach = 110f + extraReach
        val playerHeight = if (isSliding) 38f else 75f
        val playerTop = playerY - playerHeight

        val slashBoxLeft = playerX + 15f
        val slashBoxRight = playerX + slashReach + 35f
        val slashBoxTop = playerTop - 25f
        val slashBoxBottom = playerY + 10f

        for (entity in entities) {
            if (entity.isSliced || entity.isCollected) continue

            val entRight = entity.x + entity.width
            val entBottom = entity.y + entity.height

            // Check collision with slash box
            if (entity.x < slashBoxRight && entRight > slashBoxLeft &&
                entity.y < slashBoxBottom && entBottom > slashBoxTop
            ) {
                when (entity.kind) {
                    ObstacleKind.FLYING_SHURIKEN -> {
                        // PARRY / DEFLECT!
                        if (!entity.isDeflected) {
                            entity.isDeflected = true
                            entity.velocityX = 900f // Reflect forward!
                            runParries++
                            addCombo(points = 300, isParry = true)
                            SoundManager.playSound(SoundManager.SoundType.PARRY)
                            SoundManager.vibrate(60, 200)
                            createParryRing(entity.x, entity.y + entity.height / 2f)
                            spawnFloatingText("PARRY! +300", entity.x, entity.y - 30f, Color(0xFF00E5FF))
                            addFocus(18f)
                        }
                    }
                    ObstacleKind.SHADOW_RONIN,
                    ObstacleKind.BAMBOO_STAKE,
                    ObstacleKind.ROLLING_BARREL,
                    ObstacleKind.TENGU_CROW -> {
                        sliceEntity(entity)
                    }
                    ObstacleKind.ONI_BOSS -> {
                        entity.health--
                        SoundManager.playSound(SoundManager.SoundType.SLASH)
                        SoundManager.vibrate(50, 180)
                        createBloodSparks(entity.x + 30f, entity.y + entity.height / 2f)
                        if (entity.health <= 0) {
                            sliceEntity(entity)
                            spawnFloatingText("ONI SLAIN! +1500", entity.x, entity.y - 40f, Color(0xFFFFD700))
                            addCombo(points = 1500, isParry = false)
                            addFocus(40f)
                        } else {
                            spawnFloatingText("HIT!", entity.x, entity.y - 20f, Color(0xFFFF5252))
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun sliceEntity(entity: ActiveEntity) {
        entity.isSliced = true
        entity.sliceProgress = 0f
        runSlices++
        val basePoints = when (entity.kind) {
            ObstacleKind.SHADOW_RONIN -> 250
            ObstacleKind.TENGU_CROW -> 200
            ObstacleKind.ROLLING_BARREL -> 180
            ObstacleKind.BAMBOO_STAKE -> 120
            ObstacleKind.ONI_BOSS -> 1500
            else -> 100
        }
        addCombo(basePoints, isParry = false)
        addFocus(12f)
        SoundManager.playSound(SoundManager.SoundType.SLASH)
        SoundManager.vibrate(40, 160)
        createBloodSparks(entity.x + entity.width / 2f, entity.y + entity.height / 2f)
        spawnFloatingText("SLASH! +$basePoints", entity.x, entity.y - 30f, Color(0xFFFF5252))

        // Chance to spawn a spirit coin upon defeating enemy
        if (entity.kind == ObstacleKind.SHADOW_RONIN || entity.kind == ObstacleKind.TENGU_CROW) {
            entities.add(
                ActiveEntity(
                    id = ++entityIdCounter,
                    kind = ObstacleKind.SPIRIT_MON,
                    x = entity.x + 20f,
                    y = entity.y - 20f,
                    width = 28f,
                    height = 28f,
                    velocityX = 0f,
                    velocityY = -100f
                )
            )
        }
    }

    private fun addCombo(points: Int, isParry: Boolean) {
        val comboLvl = upgrades[UpgradeType.COMBO_MASTERY] ?: 1
        comboTimer = comboMaxTime + (comboLvl - 1) * 0.4f
        val newCombo = comboCount.value + 1
        comboCount.value = newCombo
        val rank = StyleRank.fromCombo(newCombo)
        currentRank.value = rank

        val mult = rank.multiplier * (if (warrior.id == "musashi") 1.5f else 1.0f)
        val finalScore = (points * mult).toInt()
        score.value += finalScore

        if (newCombo % 5 == 0) {
            spawnFloatingText("${rank.label} ${newCombo}x!", playerX + 40f, playerY - 90f, rank.color)
        }
    }

    private fun addFocus(amount: Float) {
        val focusLvl = upgrades[UpgradeType.FOCUS_CHARGE] ?: 1
        val bonus = (focusLvl - 1) * 0.2f + (if (katana.id == "sakura_void") 0.5f else 0f)
        val finalAmount = amount * (1f + bonus)
        focusMeter.value = (focusMeter.value + finalAmount).coerceAtMost(100f)
    }

    private fun updateEntities(dt: Float) {
        val magnetLvl = upgrades[UpgradeType.COIN_MAGNET] ?: 1
        val magnetDistance = 110f + (magnetLvl - 1) * 45f + (if (katana.id == "thunder_flash") 70f else 0f)

        val playerHeight = if (isSliding) 36f else 70f
        val playerBoxLeft = playerX
        val playerBoxRight = playerX + 40f
        val playerBoxTop = playerY - playerHeight
        val playerBoxBottom = playerY

        for (entity in entities) {
            // Move entity
            val vx = entity.velocityX - (if (entity.isDeflected) 0f else currentSpeed)
            entity.x += vx * dt
            entity.y += entity.velocityY * dt
            entity.animTimer += dt

            if (entity.isSliced) {
                entity.sliceProgress += dt * 3.5f
                if (entity.sliceProgress >= 1f) {
                    entities.remove(entity)
                    continue
                }
            }

            // Deflected shuriken strikes enemies ahead!
            if (entity.isDeflected && entity.kind == ObstacleKind.FLYING_SHURIKEN) {
                for (target in entities) {
                    if (target != entity && !target.isSliced && !target.isCollected &&
                        target.kind != ObstacleKind.SPIRIT_MON && target.kind != ObstacleKind.SPIRIT_ORB
                    ) {
                        if (entity.x + entity.width > target.x && entity.x < target.x + target.width) {
                            sliceEntity(target)
                            entities.remove(entity)
                            break
                        }
                    }
                }
            }

            // Magnet attraction for collectibles
            if (!entity.isCollected && (entity.kind == ObstacleKind.SPIRIT_MON || entity.kind == ObstacleKind.SPIRIT_ORB || entity.kind == ObstacleKind.JADE_HEART)) {
                val dx = (playerX + 20f) - (entity.x + entity.width / 2f)
                val dy = (playerY - 35f) - (entity.y + entity.height / 2f)
                val distSq = dx * dx + dy * dy
                if (distSq < magnetDistance * magnetDistance) {
                    entity.x += dx * 7.5f * dt
                    entity.y += dy * 7.5f * dt
                }
            }

            // Check collision with player
            if (!entity.isSliced && !entity.isCollected) {
                val entRight = entity.x + entity.width
                val entBottom = entity.y + entity.height

                val isColliding = entity.x < playerBoxRight && entRight > playerBoxLeft &&
                        entity.y < playerBoxBottom && entBottom > playerBoxTop

                if (isColliding) {
                    when (entity.kind) {
                        ObstacleKind.SPIRIT_MON -> {
                            entity.isCollected = true
                            val coinsAdd = (1 * warrior.coinMultiplier).toInt().coerceAtLeast(1)
                            coinsCollected.value += coinsAdd
                            score.value += 50
                            SoundManager.playSound(SoundManager.SoundType.COIN)
                            createCoinSparkles(entity.x, entity.y)
                            entities.remove(entity)
                        }
                        ObstacleKind.SPIRIT_ORB -> {
                            entity.isCollected = true
                            addFocus(25f)
                            score.value += 100
                            SoundManager.playSound(SoundManager.SoundType.COIN)
                            createCoinSparkles(entity.x, entity.y)
                            spawnFloatingText("+FOCUS", entity.x, entity.y - 20f, Color(0xFF00E5FF))
                            entities.remove(entity)
                        }
                        ObstacleKind.JADE_HEART -> {
                            entity.isCollected = true
                            if (currentHearts.value < maxHearts.value) {
                                currentHearts.value++
                                SoundManager.playSound(SoundManager.SoundType.COIN)
                                spawnFloatingText("+1 HEART", entity.x, entity.y - 20f, Color(0xFF00E676))
                            }
                            entities.remove(entity)
                        }
                        ObstacleKind.LOW_BEAM -> {
                            // If sliding under low beam, pass through safely!
                            if (isSliding) {
                                // Safe!
                            } else if (isDragonSprint) {
                                sliceEntity(entity)
                            } else {
                                takeDamage()
                            }
                        }
                        else -> {
                            // Danger hazard / enemy!
                            if (isDragonSprint) {
                                sliceEntity(entity)
                            } else if (isInvulnerable) {
                                // Passing through while invulnerable
                            } else {
                                takeDamage()
                            }
                        }
                    }
                }
            }

            // Remove off-screen entities
            if (entity.x < -180f || entity.x > worldWidth + 300f) {
                entities.remove(entity)
            }
        }
    }

    private fun takeDamage() {
        if (isInvulnerable || isDragonSprint) return

        currentHearts.value--
        SoundManager.playSound(SoundManager.SoundType.HURT)
        SoundManager.vibrate(90, 220)

        // Drop combo
        comboCount.value = 0
        currentRank.value = StyleRank.D

        if (currentHearts.value <= 0) {
            triggerGameOver()
        } else {
            isInvulnerable = true
            invulnerableTimer = if (warrior.id == "jin") 2.4f else 1.5f
            spawnFloatingText("OUCH!", playerX + 20f, playerY - 60f, Color(0xFFFF3333))
        }
    }

    private fun triggerGameOver() {
        isGameOver.value = true
        isRunning.value = false
        SoundManager.playSound(SoundManager.SoundType.GAME_OVER)
        SoundManager.vibrate(150, 255)

        // Save run stats to repository
        repository.saveRunResults(
            distance = distance.value.toInt(),
            score = score.value,
            coinsCollected = coinsCollected.value,
            slices = runSlices,
            parries = runParries
        )
    }

    private fun spawnEntityGroup() {
        val dist = distance.value
        val r = Random.nextFloat()

        // Spawning patterns based on distance milestones
        if (dist >= 800f && dist % 1000f in 0f..100f && !entities.any { it.kind == ObstacleKind.ONI_BOSS }) {
            // Spawn Boss!
            entities.add(
                ActiveEntity(
                    id = ++entityIdCounter,
                    kind = ObstacleKind.ONI_BOSS,
                    x = worldWidth + 80f,
                    y = groundY - 110f,
                    width = 85f,
                    height = 110f,
                    health = 3
                )
            )
            spawnFloatingText("DEMON ONI APPROACHES!", worldWidth / 2f, 150f, Color(0xFFFF1744))
            return
        }

        when {
            r < 0.25f -> {
                // Ground obstacle: Spike stakes or rolling barrel
                if (Random.nextBoolean()) {
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.BAMBOO_STAKE,
                            x = worldWidth + 50f,
                            y = groundY - 45f,
                            width = 42f,
                            height = 45f
                        )
                    )
                } else {
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.ROLLING_BARREL,
                            x = worldWidth + 50f,
                            y = groundY - 42f,
                            width = 45f,
                            height = 42f,
                            velocityX = -60f
                        )
                    )
                }
            }
            r < 0.45f -> {
                // Low ceiling beam requiring slide
                entities.add(
                    ActiveEntity(
                        id = ++entityIdCounter,
                        kind = ObstacleKind.LOW_BEAM,
                        x = worldWidth + 50f,
                        y = groundY - 80f,
                        width = 85f,
                        height = 42f
                    )
                )
            }
            r < 0.70f -> {
                // Shadow Ronin enemy with potential shuriken throw
                entities.add(
                    ActiveEntity(
                        id = ++entityIdCounter,
                        kind = ObstacleKind.SHADOW_RONIN,
                        x = worldWidth + 60f,
                        y = groundY - 72f,
                        width = 48f,
                        height = 72f,
                        velocityX = -50f
                    )
                )

                // Sometimes shoots a projectile
                if (dist > 300f && Random.nextFloat() < 0.65f) {
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.FLYING_SHURIKEN,
                            x = worldWidth + 120f,
                            y = groundY - 55f,
                            width = 28f,
                            height = 28f,
                            velocityX = -180f
                        )
                    )
                }
            }
            r < 0.85f -> {
                // Aerial Tengu Crow
                entities.add(
                    ActiveEntity(
                        id = ++entityIdCounter,
                        kind = ObstacleKind.TENGU_CROW,
                        x = worldWidth + 60f,
                        y = groundY - (Random.nextFloat() * 60f + 70f),
                        width = 45f,
                        height = 35f,
                        velocityX = -100f
                    )
                )
            }
            else -> {
                // Coin arc or Spirit Orbs
                val startX = worldWidth + 50f
                val count = Random.nextInt(3, 6)
                for (i in 0 until count) {
                    val arcY = groundY - 30f - sin((i.toFloat() / count) * Math.PI.toFloat()) * 80f
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.SPIRIT_MON,
                            x = startX + i * 36f,
                            y = arcY,
                            width = 26f,
                            height = 26f
                        )
                    )
                }

                // Occasionally bonus Spirit Orb or Jade Heart
                if (Random.nextFloat() < 0.35f) {
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.SPIRIT_ORB,
                            x = startX + count * 36f + 40f,
                            y = groundY - 60f,
                            width = 30f,
                            height = 30f
                        )
                    )
                } else if (currentHearts.value < maxHearts.value && Random.nextFloat() < 0.2f) {
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.JADE_HEART,
                            x = startX + count * 36f + 40f,
                            y = groundY - 70f,
                            width = 30f,
                            height = 30f
                        )
                    )
                }
            }
        }
    }

    private fun updateFloatingTexts(dt: Float) {
        for (item in floatingTexts) {
            item.y -= 50f * dt
            item.alpha -= dt * 1.2f
            if (item.alpha <= 0f) {
                floatingTexts.remove(item)
            }
        }
    }

    private fun spawnFloatingText(text: String, x: Float, y: Float, color: Color) {
        floatingTexts.add(
            FloatingCombatText(
                id = ++textIdCounter,
                text = text,
                x = x,
                y = y,
                color = color
            )
        )
    }

    private fun updateParticles(dt: Float) {
        for (p in particles) {
            p.x += p.vx * dt
            p.y += p.vy * dt

            if (p.isPetal) {
                p.rotation += p.rotSpeed * dt
                // Respawn petals when they fly off screen
                if (p.x < -20f) {
                    p.x = worldWidth + 20f
                    p.y = Random.nextFloat() * worldHeight
                }
                if (p.y > worldHeight + 20f) {
                    p.y = -20f
                    p.x = Random.nextFloat() * worldWidth
                }
            } else {
                p.alpha -= dt * 2.8f
                p.life -= dt
                if (p.alpha <= 0f || p.life <= 0f) {
                    particles.remove(p)
                }
            }
        }
    }

    private fun createDustParticles(x: Float, y: Float) {
        for (i in 0..4) {
            particles.add(
                Particle(
                    x = x,
                    y = y - 4f,
                    vx = -(Random.nextFloat() * 70f + 20f),
                    vy = -(Random.nextFloat() * 40f + 10f),
                    size = Random.nextFloat() * 5f + 3f,
                    color = Color(0xFFC7B198),
                    alpha = 0.8f,
                    life = 0.35f
                )
            )
        }
    }

    private fun createSlashTrailParticles() {
        val color = katana.trailColor
        for (i in 0..8) {
            val angle = (Random.nextFloat() * 60f - 30f) * (Math.PI / 180.0)
            val spd = Random.nextFloat() * 140f + 60f
            particles.add(
                Particle(
                    x = playerX + 45f,
                    y = playerY - 35f,
                    vx = (cos(angle) * spd).toFloat(),
                    vy = (sin(angle) * spd).toFloat(),
                    size = Random.nextFloat() * 6f + 3f,
                    color = color,
                    alpha = 0.9f,
                    life = 0.28f
                )
            )
        }
    }

    private fun createBloodSparks(x: Float, y: Float) {
        for (i in 0..12) {
            val angle = Random.nextFloat() * Math.PI * 2.0
            val speed = Random.nextFloat() * 200f + 80f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    size = Random.nextFloat() * 5f + 2f,
                    color = if (Random.nextBoolean()) Color(0xFFFF2233) else Color(0xFFFFD700),
                    alpha = 1.0f,
                    life = 0.4f
                )
            )
        }
    }

    private fun createParryRing(x: Float, y: Float) {
        for (i in 0..16) {
            val angle = (i / 16.0) * Math.PI * 2.0
            val speed = 180f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    size = 5f,
                    color = Color(0xFF00E5FF),
                    alpha = 1.0f,
                    life = 0.35f
                )
            )
        }
    }

    private fun createCoinSparkles(x: Float, y: Float) {
        for (i in 0..5) {
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = Random.nextFloat() * 80f - 40f,
                    vy = -(Random.nextFloat() * 70f + 30f),
                    size = 4f,
                    color = Color(0xFFFFD700),
                    alpha = 1.0f,
                    life = 0.4f
                )
            )
        }
    }
}

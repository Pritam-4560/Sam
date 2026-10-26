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
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(
    private val repository: GameRepository
) {
    // Portrait-optimized virtual world coordinates (9:16 aspect ratio)
    val worldWidth = 540f
    val worldHeight = 960f
    val groundY = 670f

    // Game states
    val isRunning = MutableStateFlow(false)
    val isPaused = MutableStateFlow(false)
    val isGameOver = MutableStateFlow(false)
    val frameTick = MutableStateFlow(0L)

    // Player physical stats
    var playerX = 85f
    var playerY = groundY
    var playerVelY = 0f
    var isGrounded = true
    var jumpCount = 0
    var maxJumps = 2
    private var fastFalling = false

    // Action states
    var isSlashing = false
    var slashTimer = 0f
    val slashDuration = 0.26f
    var slashCooldown = 0f
    private var currentSlashId = 0L

    var isSliding = false
    var slideTimer = 0f
    val slideDuration = 0.62f

    var isInvulnerable = false
    var invulnerableTimer = 0f

    // Screen shake feedback
    var screenShake = 0f

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
    val comboMaxTime = 3.2f
    val currentRank = MutableStateFlow(StyleRank.D)

    var runSlices = 0
    var runParries = 0
    private var nextBossMilestone = 450f

    // Speed & world progression
    var currentSpeed = 255f
    val baseSpeed = 255f

    // Entities & effects
    val entities = CopyOnWriteArrayList<ActiveEntity>()
    val particles = CopyOnWriteArrayList<Particle>()
    val floatingTexts = CopyOnWriteArrayList<FloatingCombatText>()

    // Parallax positions
    var bgFarX = 0f
    var bgMidX = 0f
    var bgNearX = 0f

    // Warrior & katana active configurations
    var warrior: Warrior = repository.getSelectedWarrior()
    var katana: Katana = repository.getSelectedKatana()
    private var upgrades: Map<UpgradeType, Int> = emptyMap()

    private var spawnTimer = 0f
    private var nextSpawnTime = 1.4f
    private var entityIdCounter = 0L
    private var textIdCounter = 0L

    init {
        initSakuraPetals()
    }

    private fun initSakuraPetals() {
        particles.clear()
        for (i in 0 until 32) {
            particles.add(
                Particle(
                    x = Random.nextFloat() * worldWidth,
                    y = Random.nextFloat() * groundY,
                    vx = -(Random.nextFloat() * 55f + 35f),
                    vy = Random.nextFloat() * 35f + 15f,
                    size = Random.nextFloat() * 6f + 4f,
                    color = if (Random.nextBoolean()) Color(0xFFFFB7B2) else Color(0xFFFF69B4),
                    alpha = Random.nextFloat() * 0.55f + 0.3f,
                    life = 100f,
                    isPetal = true,
                    rotation = Random.nextFloat() * 360f,
                    rotSpeed = Random.nextFloat() * 70f - 35f
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
        focusMeter.value = 25f // Start with a little focus energy
        isDragonSprint = false
        dragonSprintTimer = 0f
        screenShake = 0f

        runSlices = 0
        runParries = 0
        nextBossMilestone = 450f

        playerX = 85f
        playerY = groundY
        playerVelY = 0f
        isGrounded = true
        jumpCount = 0
        fastFalling = false

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
        nextSpawnTime = 1.0f

        initSakuraPetals()

        // Spawn an initial welcoming coin arc and enemy ahead so action begins right away
        spawnWelcomeWave()

        isGameOver.value = false
        isPaused.value = false
        isRunning.value = true
        frameTick.value++
    }

    private fun spawnWelcomeWave() {
        val startX = 320f
        for (i in 0 until 4) {
            entities.add(
                ActiveEntity(
                    id = ++entityIdCounter,
                    kind = ObstacleKind.SPIRIT_MON,
                    x = startX + i * 36f,
                    y = groundY - 40f - sin((i / 3f) * Math.PI.toFloat()) * 65f,
                    width = 28f,
                    height = 28f
                )
            )
        }
        entities.add(
            ActiveEntity(
                id = ++entityIdCounter,
                kind = ObstacleKind.SHADOW_RONIN,
                x = 560f,
                y = groundY - 82f,
                width = 50f,
                height = 82f,
                velocityX = -30f
            )
        )
    }

    fun jump() {
        if (!isRunning.value || isPaused.value || isGameOver.value) return
        if (isSliding) {
            isSliding = false
            slideTimer = 0f
        }
        fastFalling = false

        if (jumpCount < maxJumps) {
            val bonus = warrior.jumpBonus
            val jumpImpulse = if (jumpCount == 0) -770f * (1f + bonus) else -690f * (1f + bonus)
            playerVelY = jumpImpulse
            isGrounded = false
            jumpCount++
            SoundManager.playSound(SoundManager.SoundType.JUMP)
            SoundManager.vibrate(25, 80)

            if (jumpCount > 1) {
                spawnFloatingText("DOUBLE JUMP!", playerX + 20f, playerY - 60f, Color(0xFF00E5FF))
            }
            createDustParticles(playerX + 20f, playerY)
        }
    }

    fun slash() {
        if (!isRunning.value || isPaused.value || isGameOver.value) return
        if (slashCooldown > 0f) return

        if (isSliding) {
            isSliding = false
            slideTimer = 0f
        }

        currentSlashId++
        isSlashing = true
        slashTimer = slashDuration
        slashCooldown = 0.08f
        SoundManager.playSound(SoundManager.SoundType.SLASH)
        SoundManager.vibrate(35, 140)

        createSlashTrailParticles()
        performCombatSlash()
    }

    fun slide() {
        if (!isRunning.value || isPaused.value || isGameOver.value) return
        if (!isGrounded) {
            // Fast-fall slam from mid-air and queue slide on landing!
            playerVelY = 980f
            fastFalling = true
            isSliding = true
            slideTimer = slideDuration
            SoundManager.vibrate(25, 90)
            return
        }
        isSliding = true
        slideTimer = slideDuration
        SoundManager.vibrate(20, 60)
        createDustParticles(playerX + 10f, playerY)
    }

    fun triggerDragonSprint() {
        if (!isRunning.value || isPaused.value || isGameOver.value) return
        if (focusMeter.value >= 100f && !isDragonSprint) {
            isDragonSprint = true
            dragonSprintTimer = dragonSprintMaxDuration
            focusMeter.value = 100f
            screenShake = 14f
            SoundManager.playSound(SoundManager.SoundType.DRAGON_SPRINT)
            SoundManager.vibrate(100, 255)
            spawnFloatingText("DRAGON RUSH!", playerX + 30f, playerY - 110f, Color(0xFFFFD700))
        }
    }

    fun togglePause() {
        if (isRunning.value && !isGameOver.value) {
            isPaused.value = !isPaused.value
        }
    }

    fun update(deltaSeconds: Float) {
        if (!isRunning.value || isPaused.value || isGameOver.value) return

        val dt = deltaSeconds.coerceIn(0.004f, 0.045f)

        // Decay screen shake
        if (screenShake > 0f) {
            screenShake = (screenShake - dt * 42f).coerceAtLeast(0f)
        }

        // Distance & speed progression
        val distAdd = currentSpeed * dt * 0.06f
        distance.value += distAdd

        // Gradually accelerate game speed
        val targetSpeed = (baseSpeed + (distance.value * 0.09f).coerceAtMost(165f)) *
                (if (isDragonSprint) 1.55f else 1.0f) * warrior.speedMultiplier
        currentSpeed += (targetSpeed - currentSpeed) * 0.08f

        // Parallax background movement
        bgFarX = (bgFarX - currentSpeed * dt * 0.14f) % worldWidth
        bgMidX = (bgMidX - currentSpeed * dt * 0.42f) % worldWidth
        bgNearX = (bgNearX - currentSpeed * dt * 0.95f) % worldWidth

        // Gravity & Player Physics
        val gravity = 1950f
        if (!isGrounded) {
            playerVelY += gravity * dt
            playerY += playerVelY * dt

            if (playerY >= groundY) {
                playerY = groundY
                playerVelY = 0f
                isGrounded = true
                jumpCount = 0
                if (fastFalling) {
                    fastFalling = false
                    isSliding = true
                    slideTimer = slideDuration * 0.85f
                }
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

        if (isSliding && isGrounded) {
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
            focusMeter.value = ((dragonSprintTimer / dragonSprintMaxDuration) * 100f).coerceIn(0f, 100f)
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

        // Spawn obstacles & collectibles (pause normal spawns if an Oni Boss is dueling)
        val bossActive = entities.any { it.kind == ObstacleKind.ONI_BOSS && !it.isSliced }
        if (!bossActive) {
            spawnTimer += dt
            if (spawnTimer >= nextSpawnTime) {
                spawnTimer = 0f
                spawnEntityGroup()
                val speedFactor = (baseSpeed / currentSpeed).coerceIn(0.55f, 1.0f)
                nextSpawnTime = (1.35f * speedFactor) + Random.nextFloat() * 0.55f
            }
        }

        // Update active entities
        updateEntities(dt)

        // Update floating texts
        updateFloatingTexts(dt)

        // Update particles
        updateParticles(dt)

        // Advance frameTick to trigger Compose Canvas redraw at 60 FPS!
        frameTick.value++
    }

    private fun performCombatSlash() {
        val slashLvl = upgrades[UpgradeType.SLASH_REACH] ?: 1
        val extraReach = (slashLvl - 1) * 18f + (warrior.slashRangeBonus * 45f) +
                (if (katana.id == "crimson_dragon") 25f else 0f)
        val slashReach = 125f + extraReach
        val playerHeight = if (isSliding) 42f else 84f
        val playerTop = playerY - playerHeight

        val slashBoxLeft = playerX - 10f
        val slashBoxRight = playerX + slashReach
        val slashBoxTop = playerTop - 35f
        val slashBoxBottom = playerY + 15f

        for (entity in entities) {
            if (entity.isSliced || entity.isCollected) continue
            if (entity.lastHitSlashId == currentSlashId) continue

            val entRight = entity.x + entity.width
            val entBottom = entity.y + entity.height

            // Check collision with generous slash hitbox
            if (entity.x < slashBoxRight && entRight > slashBoxLeft &&
                entity.y < slashBoxBottom && entBottom > slashBoxTop
            ) {
                entity.lastHitSlashId = currentSlashId
                when (entity.kind) {
                    ObstacleKind.FLYING_SHURIKEN -> {
                        if (!entity.isDeflected) {
                            entity.isDeflected = true
                            entity.velocityX = 850f
                            runParries++
                            screenShake = 8f
                            addCombo(points = 300)
                            SoundManager.playSound(SoundManager.SoundType.PARRY)
                            SoundManager.vibrate(60, 200)
                            createParryRing(entity.x + entity.width / 2f, entity.y + entity.height / 2f)
                            spawnFloatingText("PARRY! +300", entity.x, entity.y - 25f, Color(0xFF00E5FF))
                            addFocus(20f)
                        }
                    }
                    ObstacleKind.SHADOW_RONIN,
                    ObstacleKind.BAMBOO_STAKE,
                    ObstacleKind.ROLLING_BARREL,
                    ObstacleKind.TENGU_CROW -> {
                        sliceEntity(entity)
                    }
                    ObstacleKind.ONI_BOSS -> {
                        damageBoss(entity, 1)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun damageBoss(boss: ActiveEntity, dmg: Int) {
        boss.health -= dmg
        screenShake = 10f
        SoundManager.playSound(SoundManager.SoundType.SLASH)
        SoundManager.vibrate(55, 200)
        createBloodSparks(boss.x + 30f, boss.y + boss.height / 2f)
        if (boss.health <= 0) {
            sliceEntity(boss)
            spawnFloatingText("ONI VANQUISHED! +1500", boss.x - 40f, boss.y - 40f, Color(0xFFFFD700))
            addCombo(points = 1500)
            addFocus(45f)
            // Shower of coins on boss defeat!
            for (i in 0 until 6) {
                entities.add(
                    ActiveEntity(
                        id = ++entityIdCounter,
                        kind = ObstacleKind.SPIRIT_MON,
                        x = boss.x + i * 24f,
                        y = groundY - 50f - (i % 2) * 35f,
                        width = 28f,
                        height = 28f
                    )
                )
            }
        } else {
            // Knock boss back slightly to duel position
            boss.x = (boss.x + 95f).coerceAtMost(worldWidth - 130f)
            boss.velocityX = 0f
            spawnFloatingText("BOSS HIT! (${boss.health} HP)", boss.x - 20f, boss.y - 25f, Color(0xFFFF5252))
        }
    }

    private fun sliceEntity(entity: ActiveEntity) {
        entity.isSliced = true
        entity.sliceProgress = 0f
        runSlices++
        screenShake = 6f
        val basePoints = when (entity.kind) {
            ObstacleKind.SHADOW_RONIN -> 250
            ObstacleKind.TENGU_CROW -> 200
            ObstacleKind.ROLLING_BARREL -> 180
            ObstacleKind.BAMBOO_STAKE -> 120
            ObstacleKind.ONI_BOSS -> 1500
            else -> 100
        }
        addCombo(basePoints)
        addFocus(14f)
        SoundManager.playSound(SoundManager.SoundType.SLASH)
        SoundManager.vibrate(40, 160)
        createBloodSparks(entity.x + entity.width / 2f, entity.y + entity.height / 2f)
        spawnFloatingText("SLASH! +$basePoints", entity.x, entity.y - 25f, Color(0xFFFF5252))

        if (entity.kind == ObstacleKind.SHADOW_RONIN || entity.kind == ObstacleKind.TENGU_CROW) {
            entities.add(
                ActiveEntity(
                    id = ++entityIdCounter,
                    kind = ObstacleKind.SPIRIT_MON,
                    x = entity.x + 15f,
                    y = (entity.y - 10f).coerceAtMost(groundY - 35f),
                    width = 28f,
                    height = 28f
                )
            )
        }
    }

    private fun addCombo(points: Int) {
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
            spawnFloatingText("${rank.label} ${newCombo}x!", playerX + 20f, playerY - 100f, rank.color)
        }
    }

    private fun addFocus(amount: Float) {
        if (isDragonSprint) return
        val focusLvl = upgrades[UpgradeType.FOCUS_CHARGE] ?: 1
        val bonus = (focusLvl - 1) * 0.2f + (if (katana.id == "sakura_void") 0.5f else 0f)
        val finalAmount = amount * (1f + bonus)
        focusMeter.value = (focusMeter.value + finalAmount).coerceAtMost(100f)
    }

    private fun updateEntities(dt: Float) {
        val magnetLvl = upgrades[UpgradeType.COIN_MAGNET] ?: 1
        val magnetDistance = 120f + (magnetLvl - 1) * 45f + (if (katana.id == "thunder_flash") 75f else 0f)

        val playerHeight = if (isSliding) 40f else 80f
        // Forgiving player hurtbox so near-misses feel fair
        val playerBoxLeft = playerX + 8f
        val playerBoxRight = playerX + 36f
        val playerBoxTop = playerY - playerHeight + 6f
        val playerBoxBottom = playerY - 4f

        for (entity in entities) {
            entity.animTimer += dt

            if (entity.isSliced) {
                entity.x -= currentSpeed * 0.4f * dt
                entity.sliceProgress += dt * 3.2f
                if (entity.sliceProgress >= 1f) {
                    entities.remove(entity)
                }
                continue
            }

            // Special AI for Oni Boss Duel!
            if (entity.kind == ObstacleKind.ONI_BOSS) {
                val duelX = worldWidth - 135f
                if (entity.x > duelX && entity.velocityX >= 0f) {
                    entity.x -= currentSpeed * 0.85f * dt
                } else {
                    entity.attackTimer += dt
                    // Move with charge if lunging, otherwise hold duel position
                    if (entity.velocityX < 0f) {
                        entity.x += entity.velocityX * dt
                        if (entity.x < -120f) {
                            // Boss loops back to duel position until defeated!
                            entity.x = worldWidth + 40f
                            entity.velocityX = 0f
                        }
                    } else {
                        entity.x = duelX + sin(entity.animTimer * 3f) * 12f
                    }

                    // Boss fires a shuriken or charges every 1.65s
                    if (entity.attackTimer >= 1.65f) {
                        entity.attackTimer = 0f
                        if (Random.nextFloat() < 0.65f) {
                            // Throw parryable shuriken!
                            entities.add(
                                ActiveEntity(
                                    id = ++entityIdCounter,
                                    kind = ObstacleKind.FLYING_SHURIKEN,
                                    x = entity.x - 20f,
                                    y = groundY - 58f,
                                    width = 32f,
                                    height = 32f,
                                    velocityX = -110f
                                )
                            )
                            spawnFloatingText("PARRY SHURIKEN!", entity.x - 80f, entity.y - 20f, Color(0xFF00E5FF))
                        } else {
                            // Boss charges forward so player can slash or jump over!
                            entity.velocityX = -(currentSpeed * 1.15f)
                            spawnFloatingText("ONI CHARGE!", entity.x - 70f, entity.y - 25f, Color(0xFFFF1744))
                        }
                    }
                }
            } else {
                // Normal movement
                val vx = if (entity.isDeflected) entity.velocityX else (entity.velocityX - currentSpeed)
                entity.x += vx * dt
                entity.y += entity.velocityY * dt
            }

            // Deflected shuriken strikes enemies or Oni Boss ahead!
            if (entity.isDeflected && entity.kind == ObstacleKind.FLYING_SHURIKEN) {
                for (target in entities) {
                    if (target != entity && !target.isSliced && !target.isCollected &&
                        target.kind != ObstacleKind.SPIRIT_MON &&
                        target.kind != ObstacleKind.SPIRIT_ORB &&
                        target.kind != ObstacleKind.JADE_HEART
                    ) {
                        if (entity.x + entity.width > target.x && entity.x < target.x + target.width) {
                            if (target.kind == ObstacleKind.ONI_BOSS) {
                                damageBoss(target, 1)
                            } else {
                                sliceEntity(target)
                            }
                            entities.remove(entity)
                            break
                        }
                    }
                }
            }

            // Magnet attraction for collectibles
            if (!entity.isCollected && (
                        entity.kind == ObstacleKind.SPIRIT_MON ||
                        entity.kind == ObstacleKind.SPIRIT_ORB ||
                        entity.kind == ObstacleKind.JADE_HEART
                    )
            ) {
                val dx = (playerX + 22f) - (entity.x + entity.width / 2f)
                val dy = (playerY - 40f) - (entity.y + entity.height / 2f)
                val distSq = dx * dx + dy * dy
                if (distSq < magnetDistance * magnetDistance) {
                    entity.x += dx * 8.5f * dt
                    entity.y += dy * 8.5f * dt
                }
            }

            // Check collision with player
            if (!entity.isSliced && !entity.isCollected) {
                val entRight = entity.x + entity.width
                val entBottom = entity.y + entity.height

                // Expand collectible pickup radius slightly for great feel
                val isCollectible = entity.kind == ObstacleKind.SPIRIT_MON ||
                        entity.kind == ObstacleKind.SPIRIT_ORB ||
                        entity.kind == ObstacleKind.JADE_HEART

                val pad = if (isCollectible) 14f else 0f
                val isColliding = (entity.x - pad) < playerBoxRight &&
                        (entRight + pad) > playerBoxLeft &&
                        (entity.y - pad) < playerBoxBottom &&
                        (entBottom + pad) > playerBoxTop

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
                            addFocus(28f)
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
                            } else {
                                score.value += 250
                                spawnFloatingText("+250 BONUS", entity.x, entity.y - 20f, Color(0xFF00E676))
                            }
                            entities.remove(entity)
                        }
                        ObstacleKind.LOW_BEAM -> {
                            if (isSliding) {
                                // Safe slide under beam!
                            } else if (isDragonSprint) {
                                sliceEntity(entity)
                            } else {
                                takeDamage()
                            }
                        }
                        ObstacleKind.ONI_BOSS -> {
                            if (isDragonSprint) {
                                damageBoss(entity, 3)
                            } else {
                                takeDamage()
                            }
                        }
                        else -> {
                            if (isDragonSprint) {
                                sliceEntity(entity)
                            } else {
                                takeDamage()
                            }
                        }
                    }
                }
            }

            // Remove off-screen entities
            if (entity.x < -160f || entity.x > worldWidth + 240f) {
                entities.remove(entity)
            }
        }
    }

    private fun takeDamage() {
        if (isInvulnerable || isDragonSprint) return

        currentHearts.value--
        screenShake = 14f
        SoundManager.playSound(SoundManager.SoundType.HURT)
        SoundManager.vibrate(90, 220)

        comboCount.value = 0
        currentRank.value = StyleRank.D

        if (currentHearts.value <= 0) {
            triggerGameOver()
        } else {
            isInvulnerable = true
            invulnerableTimer = if (warrior.id == "jin") 2.4f else 1.6f
            spawnFloatingText("HIT! -1 HEART", playerX + 10f, playerY - 90f, Color(0xFFFF3333))
        }
    }

    private fun triggerGameOver() {
        isGameOver.value = true
        isRunning.value = false
        SoundManager.playSound(SoundManager.SoundType.GAME_OVER)
        SoundManager.vibrate(150, 255)

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

        // Boss encounter every 500m milestone!
        if (dist >= nextBossMilestone && !entities.any { it.kind == ObstacleKind.ONI_BOSS }) {
            nextBossMilestone += 500f
            entities.add(
                ActiveEntity(
                    id = ++entityIdCounter,
                    kind = ObstacleKind.ONI_BOSS,
                    x = worldWidth + 70f,
                    y = groundY - 125f,
                    width = 92f,
                    height = 125f,
                    health = 3,
                    maxHealth = 3
                )
            )
            spawnFloatingText("ONI BOSS DUEL!", worldWidth * 0.35f, groundY - 220f, Color(0xFFFF1744))
            return
        }

        val spawnX = worldWidth + 55f
        when {
            r < 0.22f -> {
                // Ground obstacle: Bamboo Spike Stakes or Rolling Spiked Barrel
                if (Random.nextBoolean()) {
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.BAMBOO_STAKE,
                            x = spawnX,
                            y = groundY - 52f,
                            width = 48f,
                            height = 52f
                        )
                    )
                    // Coin floating above spike to reward jumping!
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.SPIRIT_MON,
                            x = spawnX + 10f,
                            y = groundY - 135f,
                            width = 28f,
                            height = 28f
                        )
                    )
                } else {
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.ROLLING_BARREL,
                            x = spawnX,
                            y = groundY - 50f,
                            width = 50f,
                            height = 50f,
                            velocityX = -55f
                        )
                    )
                }
            }
            r < 0.40f -> {
                // Low hanging Torii Fire Beam requiring Slide
                // Leaves a 48f tall gap at bottom (groundY - 48f to groundY) so sliding (height 40f) passes cleanly underneath!
                entities.add(
                    ActiveEntity(
                        id = ++entityIdCounter,
                        kind = ObstacleKind.LOW_BEAM,
                        x = spawnX,
                        y = groundY - 125f,
                        width = 96f,
                        height = 75f
                    )
                )
                // Coins along the ground under the beam to reward sliding!
                for (i in 0..2) {
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.SPIRIT_MON,
                            x = spawnX + 12f + i * 30f,
                            y = groundY - 30f,
                            width = 26f,
                            height = 26f
                        )
                    )
                }
            }
            r < 0.65f -> {
                // Shadow Ronin OR Shuriken (spaced apart so they don't overlap unfairly)
                if (dist > 120f && Random.nextFloat() < 0.45f) {
                    // Flying Shuriken to Parry or Jump!
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.FLYING_SHURIKEN,
                            x = spawnX,
                            y = groundY - 62f,
                            width = 32f,
                            height = 32f,
                            velocityX = -115f
                        )
                    )
                    // Spawn a Shadow Ronin well behind the shuriken so parrying the shuriken slices the Ronin!
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.SHADOW_RONIN,
                            x = spawnX + 210f,
                            y = groundY - 82f,
                            width = 50f,
                            height = 82f,
                            velocityX = -30f
                        )
                    )
                } else {
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.SHADOW_RONIN,
                            x = spawnX,
                            y = groundY - 82f,
                            width = 50f,
                            height = 82f,
                            velocityX = -35f
                        )
                    )
                }
            }
            r < 0.82f -> {
                // Aerial Tengu Crow (Jump + Slash or Slide under!)
                entities.add(
                    ActiveEntity(
                        id = ++entityIdCounter,
                        kind = ObstacleKind.TENGU_CROW,
                        x = spawnX,
                        y = groundY - (Random.nextFloat() * 55f + 75f),
                        width = 52f,
                        height = 40f,
                        velocityX = -75f
                    )
                )
            }
            else -> {
                // Coin arc + Spirit Orb / Jade Heart
                val count = Random.nextInt(4, 7)
                for (i in 0 until count) {
                    val arcY = groundY - 38f - sin((i.toFloat() / (count - 1)) * Math.PI.toFloat()) * 95f
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.SPIRIT_MON,
                            x = spawnX + i * 36f,
                            y = arcY,
                            width = 28f,
                            height = 28f
                        )
                    )
                }

                if (Random.nextFloat() < 0.45f) {
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.SPIRIT_ORB,
                            x = spawnX + count * 36f + 25f,
                            y = groundY - 85f,
                            width = 32f,
                            height = 32f
                        )
                    )
                } else if (currentHearts.value < maxHearts.value && Random.nextFloat() < 0.3f) {
                    entities.add(
                        ActiveEntity(
                            id = ++entityIdCounter,
                            kind = ObstacleKind.JADE_HEART,
                            x = spawnX + count * 36f + 25f,
                            y = groundY - 85f,
                            width = 32f,
                            height = 32f
                        )
                    )
                }
            }
        }
    }

    private fun updateFloatingTexts(dt: Float) {
        for (item in floatingTexts) {
            item.y -= 55f * dt
            item.alpha -= dt * 1.15f
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
                x = x.coerceIn(30f, worldWidth - 140f),
                y = y.coerceIn(80f, groundY - 40f),
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
                if (p.x < -20f) {
                    p.x = worldWidth + 20f
                    p.y = Random.nextFloat() * groundY
                }
                if (p.y > groundY) {
                    p.y = -20f
                    p.x = Random.nextFloat() * worldWidth
                }
            } else {
                p.alpha -= dt * 2.6f
                p.life -= dt
                if (p.alpha <= 0f || p.life <= 0f) {
                    particles.remove(p)
                }
            }
        }
    }

    private fun createDustParticles(x: Float, y: Float) {
        for (i in 0..5) {
            particles.add(
                Particle(
                    x = x,
                    y = y - 4f,
                    vx = -(Random.nextFloat() * 80f + 25f),
                    vy = -(Random.nextFloat() * 45f + 10f),
                    size = Random.nextFloat() * 6f + 3f,
                    color = Color(0xFFC7B198),
                    alpha = 0.85f,
                    life = 0.35f
                )
            )
        }
    }

    private fun createSlashTrailParticles() {
        val color = katana.trailColor
        for (i in 0..9) {
            val angle = (Random.nextFloat() * 70f - 35f) * (Math.PI / 180.0)
            val spd = Random.nextFloat() * 160f + 70f
            particles.add(
                Particle(
                    x = playerX + 45f,
                    y = playerY - 42f,
                    vx = (cos(angle) * spd).toFloat(),
                    vy = (sin(angle) * spd).toFloat(),
                    size = Random.nextFloat() * 6f + 3f,
                    color = color,
                    alpha = 0.95f,
                    life = 0.28f
                )
            )
        }
    }

    private fun createBloodSparks(x: Float, y: Float) {
        for (i in 0..14) {
            val angle = Random.nextFloat() * Math.PI * 2.0
            val speed = Random.nextFloat() * 220f + 80f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    size = Random.nextFloat() * 6f + 2.5f,
                    color = if (Random.nextBoolean()) Color(0xFFFF2233) else Color(0xFFFFD700),
                    alpha = 1.0f,
                    life = 0.42f
                )
            )
        }
    }

    private fun createParryRing(x: Float, y: Float) {
        for (i in 0..16) {
            val angle = (i / 16.0) * Math.PI * 2.0
            val speed = 190f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    size = 5.5f,
                    color = Color(0xFF00E5FF),
                    alpha = 1.0f,
                    life = 0.38f
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
                    vx = Random.nextFloat() * 90f - 45f,
                    vy = -(Random.nextFloat() * 80f + 30f),
                    size = 4.5f,
                    color = Color(0xFFFFD700),
                    alpha = 1.0f,
                    life = 0.4f
                )
            )
        }
    }
}

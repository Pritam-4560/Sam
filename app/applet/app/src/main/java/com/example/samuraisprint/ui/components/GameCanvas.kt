package com.example.samuraisprint.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import com.example.samuraisprint.engine.GameEngine
import com.example.samuraisprint.model.ActiveEntity
import com.example.samuraisprint.model.ObstacleKind
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameCanvas(
    engine: GameEngine,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val w = size.width
                        val h = size.height
                        // Tap controls: Left side = Slide, Right top = Jump, Right bottom = Slash
                        if (offset.x < w * 0.35f) {
                            engine.slide()
                        } else if (offset.y < h * 0.5f) {
                            engine.jump()
                        } else {
                            engine.slash()
                        }
                    }
                )
            }
    ) {
        val scaleX = size.width / engine.worldWidth
        val scaleY = size.height / engine.worldHeight
        val scale = minOf(scaleX, scaleY)
        val offsetX = (size.width - engine.worldWidth * scale) / 2f
        val offsetY = (size.height - engine.worldHeight * scale) / 2f

        // Draw parallax feudal Japanese background
        drawFeudalBackground(engine, scale, offsetX, offsetY)

        // Draw Ground Track
        drawGroundTrack(engine, scale, offsetX, offsetY)

        // Draw Entities (Obstacles, Enemies, Collectibles)
        for (entity in engine.entities) {
            drawGameEntity(entity, scale, offsetX, offsetY)
        }

        // Draw Player Samurai
        drawSamuraiPlayer(engine, scale, offsetX, offsetY)

        // Draw Particles (Sakura petals, sparks, dust)
        drawParticles(engine, scale, offsetX, offsetY)

        // Draw Floating Combat Text
        drawFloatingCombatTexts(engine, scale, offsetX, offsetY)
    }
}

private fun DrawScope.drawFeudalBackground(
    engine: GameEngine,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val dist = engine.distance.value
    // Biome colors shift over distance
    val (skyTop, skyBottom, moonColor) = when {
        dist > 1500f -> Triple(Color(0xFF220815), Color(0xFF6B0E23), Color(0xFFFF2A42)) // Blood Moon Realm
        dist > 1000f -> Triple(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFFE2E8F0)) // Stormy Night
        dist > 500f -> Triple(Color(0xFF31103F), Color(0xFF9C27B0), Color(0xFFFFD54F)) // Sunset Ridge
        else -> Triple(Color(0xFF0A0F1D), Color(0xFF1F1D36), Color(0xFFFFF3E0)) // Moonlit Sakura
    }

    // Sky gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(skyTop, skyBottom),
            startY = 0f,
            endY = size.height
        ),
        topLeft = Offset(0f, 0f),
        size = size
    )

    // Giant celestial moon
    val moonX = offsetX + (engine.worldWidth * 0.75f) * scale
    val moonY = offsetY + (engine.worldHeight * 0.22f) * scale
    val moonR = 68f * scale
    drawCircle(
        color = moonColor.copy(alpha = 0.2f),
        radius = moonR * 1.3f,
        center = Offset(moonX, moonY)
    )
    drawCircle(
        color = moonColor,
        radius = moonR,
        center = Offset(moonX, moonY)
    )

    // Far mountains (Layer 1 parallax)
    val farX = offsetX + (engine.bgFarX * scale)
    val mountainPath = Path().apply {
        moveTo(farX, offsetY + 360f * scale)
        lineTo(farX + 250f * scale, offsetY + 220f * scale)
        lineTo(farX + 500f * scale, offsetY + 360f * scale)
        lineTo(farX + 750f * scale, offsetY + 200f * scale)
        lineTo(farX + 1000f * scale, offsetY + 360f * scale)
        lineTo(farX + 1250f * scale, offsetY + 220f * scale)
        lineTo(farX + 1500f * scale, offsetY + 360f * scale)
        lineTo(farX + 1500f * scale, size.height)
        lineTo(farX, size.height)
        close()
    }
    drawPath(mountainPath, color = Color(0xFF181528).copy(alpha = 0.8f))

    // Midground Pagodas & Bamboo Silhouettes (Layer 2 parallax)
    val midX = offsetX + (engine.bgMidX * scale)
    for (i in 0..2) {
        val pagodaX = midX + (i * 450f) * scale
        if (pagodaX > -200f && pagodaX < size.width + 200f) {
            drawPagodaSilhouette(pagodaX, offsetY + 260f * scale, scale)
        }
    }
}

private fun DrawScope.drawPagodaSilhouette(x: Float, baseY: Float, scale: Float) {
    val darkColor = Color(0xFF12101F)
    // Tier 1 roof
    val roof1 = Path().apply {
        moveTo(x - 50f * scale, baseY + 60f * scale)
        lineTo(x + 50f * scale, baseY + 60f * scale)
        lineTo(x + 35f * scale, baseY + 45f * scale)
        lineTo(x - 35f * scale, baseY + 45f * scale)
        close()
    }
    drawPath(roof1, color = darkColor)
    // Tier 2 roof
    val roof2 = Path().apply {
        moveTo(x - 40f * scale, baseY + 40f * scale)
        lineTo(x + 40f * scale, baseY + 40f * scale)
        lineTo(x + 25f * scale, baseY + 25f * scale)
        lineTo(x - 25f * scale, baseY + 25f * scale)
        close()
    }
    drawPath(roof2, color = darkColor)
    // Spire
    drawRect(
        color = darkColor,
        topLeft = Offset(x - 3f * scale, baseY),
        size = Size(6f * scale, 30f * scale)
    )
}

private fun DrawScope.drawGroundTrack(
    engine: GameEngine,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val groundScreenY = offsetY + (engine.groundY * scale)
    val groundHeight = size.height - groundScreenY

    // Ground base gradient (stone / earth)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF282538), Color(0xFF14121F)),
            startY = groundScreenY,
            endY = size.height
        ),
        topLeft = Offset(0f, groundScreenY),
        size = Size(size.width, groundHeight)
    )

    // Golden boundary line along top of the ground
    drawLine(
        color = Color(0xFFFFB800).copy(alpha = 0.85f),
        start = Offset(0f, groundScreenY),
        end = Offset(size.width, groundScreenY),
        strokeWidth = 3f * scale
    )

    // Moving road markers / wooden planks (Layer 3 parallax)
    val nearX = offsetX + (engine.bgNearX * scale)
    for (i in -2..18) {
        val plankX = nearX + (i * 90f) * scale
        if (plankX >= 0f && plankX <= size.width) {
            drawLine(
                color = Color(0xFF3E3952),
                start = Offset(plankX, groundScreenY),
                end = Offset(plankX + 25f * scale, groundScreenY + 22f * scale),
                strokeWidth = 2.5f * scale
            )
        }
    }
}

private fun DrawScope.drawSamuraiPlayer(
    engine: GameEngine,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    if (engine.isInvulnerable && (System.currentTimeMillis() / 80) % 2 == 0L) {
        // Flicker during invulnerability
        return
    }

    val px = offsetX + (engine.playerX * scale)
    val py = offsetY + (engine.playerY * scale)
    val warrior = engine.warrior
    val katana = engine.katana

    // Dragon sprint aura glow
    if (engine.isDragonSprint) {
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = 0.35f),
            radius = 55f * scale,
            center = Offset(px + 20f * scale, py - 35f * scale)
        )
        drawCircle(
            color = Color(0xFFFF3D00).copy(alpha = 0.25f),
            radius = 45f * scale,
            center = Offset(px + 20f * scale, py - 35f * scale)
        )
    }

    if (engine.isSliding) {
        // Sliding posture (low silhouette)
        drawRoundRect(
            color = warrior.primaryColor,
            topLeft = Offset(px - 10f * scale, py - 32f * scale),
            size = Size(58f * scale, 28f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * scale)
        )
        // Head
        drawCircle(
            color = Color(0xFFFFD1A4),
            radius = 11f * scale,
            center = Offset(px + 45f * scale, py - 20f * scale)
        )
        // Flowing headband sash behind
        drawLine(
            color = warrior.accentColor,
            start = Offset(px + 40f * scale, py - 22f * scale),
            end = Offset(px - 15f * scale, py - 26f * scale),
            strokeWidth = 4f * scale
        )
    } else {
        // Running or jumping samurai
        val legCycle = sin(engine.distance.value * 0.12f)
        val legOffset = if (engine.isGrounded) legCycle * 14f * scale else 0f

        // Legs
        drawLine(
            color = Color(0xFF1E1E28),
            start = Offset(px + 12f * scale, py - 22f * scale),
            end = Offset(px + 5f * scale + legOffset, py),
            strokeWidth = 6f * scale
        )
        drawLine(
            color = Color(0xFF1E1E28),
            start = Offset(px + 26f * scale, py - 22f * scale),
            end = Offset(px + 32f * scale - legOffset, py),
            strokeWidth = 6f * scale
        )

        // Body / Robe (Torso)
        val bodyPath = Path().apply {
            moveTo(px + 8f * scale, py - 22f * scale)
            lineTo(px + 32f * scale, py - 22f * scale)
            lineTo(px + 28f * scale, py - 52f * scale)
            lineTo(px + 10f * scale, py - 52f * scale)
            close()
        }
        drawPath(bodyPath, color = warrior.primaryColor)

        // Belt sash
        drawRect(
            color = warrior.accentColor,
            topLeft = Offset(px + 8f * scale, py - 32f * scale),
            size = Size(24f * scale, 5f * scale)
        )

        // Head & Kabuto Helmet
        drawCircle(
            color = Color(0xFFFFD1A4),
            radius = 12f * scale,
            center = Offset(px + 20f * scale, py - 58f * scale)
        )
        // Helmet top
        drawArc(
            color = Color(0xFF1E1E28),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(px + 6f * scale, py - 72f * scale),
            size = Size(28f * scale, 24f * scale)
        )
        // Golden helmet crest (horns)
        val hornPath = Path().apply {
            moveTo(px + 20f * scale, py - 66f * scale)
            lineTo(px + 14f * scale, py - 78f * scale)
            lineTo(px + 20f * scale, py - 73f * scale)
            lineTo(px + 26f * scale, py - 78f * scale)
            close()
        }
        drawPath(hornPath, color = Color(0xFFFFB800))

        // Flowing crimson headband fluttering back in wind
        val scarfWave = sin(engine.distance.value * 0.15f) * 6f * scale
        val scarfPath = Path().apply {
            moveTo(px + 10f * scale, py - 60f * scale)
            quadraticTo(
                px - 15f * scale, py - 65f * scale + scarfWave,
                px - 35f * scale, py - 55f * scale - scarfWave
            )
        }
        drawPath(
            scarfPath,
            color = warrior.accentColor,
            style = Stroke(width = 4.5f * scale)
        )
    }

    // Katana Slash Arc Animation
    if (engine.isSlashing) {
        val slashProg = 1f - (engine.slashTimer / engine.slashDuration)
        val arcCenter = Offset(px + 28f * scale, py - 40f * scale)
        val slashRadius = (85f + warrior.slashRangeBonus * 35f) * scale

        // Crescent slash trail
        drawArc(
            brush = Brush.radialGradient(
                colors = listOf(katana.trailColor, katana.bladeColor, Color.Transparent),
                center = arcCenter,
                radius = slashRadius
            ),
            startAngle = -60f + slashProg * 50f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(arcCenter.x - slashRadius, arcCenter.y - slashRadius),
            size = Size(slashRadius * 2, slashRadius * 2),
            style = Stroke(width = 12f * scale)
        )
        // Razor white edge
        drawArc(
            color = Color.White,
            startAngle = -50f + slashProg * 50f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(arcCenter.x - slashRadius, arcCenter.y - slashRadius),
            size = Size(slashRadius * 2, slashRadius * 2),
            style = Stroke(width = 3.5f * scale)
        )
    } else {
        // Idle held blade
        drawLine(
            color = katana.bladeColor,
            start = Offset(px + 25f * scale, py - 35f * scale),
            end = Offset(px + 45f * scale, py - 25f * scale),
            strokeWidth = 3f * scale
        )
    }
}

private fun DrawScope.drawGameEntity(
    entity: ActiveEntity,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val ex = offsetX + (entity.x * scale)
    val ey = offsetY + (entity.y * scale)
    val ew = entity.width * scale
    val eh = entity.height * scale

    if (entity.isSliced) {
        // Draw split halves flying apart!
        val splitDist = entity.sliceProgress * 35f * scale
        // Top half
        rotate(degrees = -15f * entity.sliceProgress, pivot = Offset(ex, ey)) {
            drawRect(
                color = Color(0xFF6B1D2F).copy(alpha = 1f - entity.sliceProgress),
                topLeft = Offset(ex - splitDist * 0.5f, ey - splitDist),
                size = Size(ew, eh * 0.5f)
            )
        }
        // Bottom half
        rotate(degrees = 15f * entity.sliceProgress, pivot = Offset(ex, ey + eh)) {
            drawRect(
                color = Color(0xFF3A101A).copy(alpha = 1f - entity.sliceProgress),
                topLeft = Offset(ex + splitDist * 0.5f, ey + eh * 0.5f + splitDist * 0.4f),
                size = Size(ew, eh * 0.5f)
            )
        }
        return
    }

    when (entity.kind) {
        ObstacleKind.BAMBOO_STAKE -> {
            // Bamboo Spike
            val path = Path().apply {
                moveTo(ex + ew / 2f, ey)
                lineTo(ex + ew, ey + eh)
                lineTo(ex, ey + eh)
                close()
            }
            drawPath(path, color = Color(0xFF4E7037))
            drawLine(color = Color(0xFF6FA14E), start = Offset(ex + 8f * scale, ey + eh * 0.5f), end = Offset(ex + ew - 8f * scale, ey + eh * 0.5f), strokeWidth = 2.5f * scale)
        }
        ObstacleKind.ROLLING_BARREL -> {
            // Rolling Spiked Barrel
            val r = ew / 2f
            val cx = ex + r
            val cy = ey + r
            rotate(degrees = -entity.animTimer * 280f, pivot = Offset(cx, cy)) {
                drawCircle(color = Color(0xFF795548), radius = r, center = Offset(cx, cy))
                drawCircle(color = Color(0xFF4E342E), radius = r * 0.7f, center = Offset(cx, cy))
                // Spikes
                for (angle in 0 until 360 step 60) {
                    val rad = Math.toRadians(angle.toDouble())
                    val sx = cx + (cos(rad) * (r + 5f * scale)).toFloat()
                    val sy = cy + (sin(rad) * (r + 5f * scale)).toFloat()
                    drawCircle(color = Color(0xFFD32F2F), radius = 3.5f * scale, center = Offset(sx, sy))
                }
            }
        }
        ObstacleKind.LOW_BEAM -> {
            // Hanging Torii beam
            drawRoundRect(
                color = Color(0xFFC62828),
                topLeft = Offset(ex, ey),
                size = Size(ew, eh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale)
            )
            // Golden metal brackets
            drawRect(color = Color(0xFFFFB800), topLeft = Offset(ex + 8f * scale, ey), size = Size(10f * scale, eh))
            drawRect(color = Color(0xFFFFB800), topLeft = Offset(ex + ew - 18f * scale, ey), size = Size(10f * scale, eh))
        }
        ObstacleKind.SHADOW_RONIN -> {
            // Shadow Enemy Ninja
            drawRoundRect(
                color = Color(0xFF1A1A22),
                topLeft = Offset(ex + 6f * scale, ey + 18f * scale),
                size = Size(ew - 12f * scale, eh - 18f * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale)
            )
            // Conical Straw Hat
            val hat = Path().apply {
                moveTo(ex + ew / 2f, ey)
                lineTo(ex + ew, ey + 18f * scale)
                lineTo(ex, ey + 18f * scale)
                close()
            }
            drawPath(hat, color = Color(0xFF5D4037))
            // Glowing menacing red eyes
            drawCircle(color = Color(0xFFFF1744), radius = 3f * scale, center = Offset(ex + ew * 0.4f, ey + 26f * scale))
            drawCircle(color = Color(0xFFFF1744), radius = 3f * scale, center = Offset(ex + ew * 0.65f, ey + 26f * scale))
            // Wakizashi blade
            drawLine(color = Color(0xFFE0E0E0), start = Offset(ex + 4f * scale, ey + 42f * scale), end = Offset(ex - 12f * scale, ey + 30f * scale), strokeWidth = 3f * scale)
        }
        ObstacleKind.TENGU_CROW -> {
            // Aerial Crow
            val cy = ey + eh / 2f
            val wingFlap = sin(entity.animTimer * 16f) * 12f * scale
            drawCircle(color = Color(0xFF1E1E28), radius = 10f * scale, center = Offset(ex + ew * 0.6f, cy))
            // Wings
            val wing = Path().apply {
                moveTo(ex + ew * 0.5f, cy)
                lineTo(ex + 8f * scale, cy - 14f * scale + wingFlap)
                lineTo(ex + 18f * scale, cy)
                close()
            }
            drawPath(wing, color = Color(0xFF111118))
            // Red beak / eyes
            drawCircle(color = Color(0xFFFF5252), radius = 2.5f * scale, center = Offset(ex + ew * 0.8f, cy - 2f * scale))
        }
        ObstacleKind.FLYING_SHURIKEN -> {
            val cx = ex + ew / 2f
            val cy = ey + eh / 2f
            val shurikenColor = if (entity.isDeflected) Color(0xFF00E5FF) else Color(0xFFECEFF1)
            rotate(degrees = entity.animTimer * 720f, pivot = Offset(cx, cy)) {
                // 4-point ninja star
                val star = Path().apply {
                    moveTo(cx, cy - ew / 2f)
                    lineTo(cx + 4f * scale, cy - 4f * scale)
                    lineTo(cx + ew / 2f, cy)
                    lineTo(cx + 4f * scale, cy + 4f * scale)
                    lineTo(cx, cy + ew / 2f)
                    lineTo(cx - 4f * scale, cy + 4f * scale)
                    lineTo(cx - ew / 2f, cy)
                    lineTo(cx - 4f * scale, cy - 4f * scale)
                    close()
                }
                drawPath(star, color = shurikenColor)
                drawCircle(color = Color(0xFF263238), radius = 3.5f * scale, center = Offset(cx, cy))
            }
        }
        ObstacleKind.SPIRIT_MON -> {
            // Gold Mon Coin
            val cx = ex + ew / 2f
            val cy = ey + eh / 2f
            drawCircle(color = Color(0xFFFFB800), radius = ew / 2f, center = Offset(cx, cy))
            drawCircle(color = Color(0xFFFFD54F), radius = ew * 0.38f, center = Offset(cx, cy))
            // Square hole in center
            drawRect(
                color = Color(0xFF282538),
                topLeft = Offset(cx - 3.5f * scale, cy - 3.5f * scale),
                size = Size(7f * scale, 7f * scale)
            )
        }
        ObstacleKind.SPIRIT_ORB -> {
            // Azure Spirit Orb
            val cx = ex + ew / 2f
            val cy = ey + eh / 2f
            drawCircle(color = Color(0xFF00E5FF).copy(alpha = 0.3f), radius = ew * 0.7f, center = Offset(cx, cy))
            drawCircle(color = Color(0xFF00E5FF), radius = ew / 2f, center = Offset(cx, cy))
            drawCircle(color = Color.White, radius = ew * 0.25f, center = Offset(cx, cy))
        }
        ObstacleKind.JADE_HEART -> {
            val cx = ex + ew / 2f
            val cy = ey + eh / 2f
            drawCircle(color = Color(0xFF00E676).copy(alpha = 0.3f), radius = ew * 0.7f, center = Offset(cx, cy))
            drawCircle(color = Color(0xFF00E676), radius = ew / 2f, center = Offset(cx, cy))
            drawCircle(color = Color.White, radius = ew * 0.2f, center = Offset(cx - 2f * scale, cy - 2f * scale))
        }
        ObstacleKind.ONI_BOSS -> {
            // Giant Demon Oni
            drawRoundRect(
                color = Color(0xFFB71C1C),
                topLeft = Offset(ex, ey + 25f * scale),
                size = Size(ew, eh - 25f * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f * scale)
            )
            // Golden Horns
            val hornL = Path().apply {
                moveTo(ex + 15f * scale, ey + 25f * scale)
                lineTo(ex + 5f * scale, ey)
                lineTo(ex + 25f * scale, ey + 18f * scale)
                close()
            }
            val hornR = Path().apply {
                moveTo(ex + ew - 15f * scale, ey + 25f * scale)
                lineTo(ex + ew - 5f * scale, ey)
                lineTo(ex + ew - 25f * scale, ey + 18f * scale)
                close()
            }
            drawPath(hornL, color = Color(0xFFFFB800))
            drawPath(hornR, color = Color(0xFFFFB800))
            // Glowing Eyes
            drawCircle(color = Color(0xFFFFEB3B), radius = 6f * scale, center = Offset(ex + ew * 0.35f, ey + 45f * scale))
            drawCircle(color = Color(0xFFFFEB3B), radius = 6f * scale, center = Offset(ex + ew * 0.65f, ey + 45f * scale))
            // Health Bar over Boss
            val hpWidth = ew * (entity.health / 3f)
            drawRect(color = Color(0xFF333333), topLeft = Offset(ex, ey - 14f * scale), size = Size(ew, 8f * scale))
            drawRect(color = Color(0xFFFF1744), topLeft = Offset(ex, ey - 14f * scale), size = Size(hpWidth, 8f * scale))
        }
    }
}

private fun DrawScope.drawParticles(
    engine: GameEngine,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    for (p in engine.particles) {
        val px = offsetX + (p.x * scale)
        val py = offsetY + (p.y * scale)
        val pSize = p.size * scale

        if (p.isPetal) {
            rotate(degrees = p.rotation, pivot = Offset(px, py)) {
                // Sakura Petal shape
                drawOval(
                    color = p.color.copy(alpha = p.alpha),
                    topLeft = Offset(px - pSize, py - pSize * 0.6f),
                    size = Size(pSize * 2f, pSize * 1.2f)
                )
            }
        } else {
            drawCircle(
                color = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                radius = pSize / 2f,
                center = Offset(px, py)
            )
        }
    }
}

private fun DrawScope.drawFloatingCombatTexts(
    engine: GameEngine,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    // Rendered via Canvas lines / markers or overlay in Compose HUD
}

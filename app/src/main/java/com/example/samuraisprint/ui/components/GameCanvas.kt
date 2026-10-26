package com.example.samuraisprint.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.example.samuraisprint.engine.GameEngine
import com.example.samuraisprint.model.ActiveEntity
import com.example.samuraisprint.model.ObstacleKind
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GameCanvas(
    engine: GameEngine,
    modifier: Modifier = Modifier
) {
    // CRITICAL: Observe frameTick StateFlow so Canvas redraws on every 60fps tick!
    val tick by engine.frameTick.collectAsState()

    val textPaintStroke = remember {
        Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
        }
    }
    val textPaintFill = remember {
        Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    var triggeredSwipe = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            if (!triggeredSwipe) {
                                // Instant tap action based on screen zone
                                val w = size.width
                                val h = size.height
                                if (startPos.y < h * 0.68f) {
                                    if (startPos.x < w * 0.32f) {
                                        engine.slide()
                                    } else if (startPos.y < h * 0.38f) {
                                        engine.jump()
                                    } else {
                                        engine.slash()
                                    }
                                }
                            }
                            break
                        }

                        val dx = change.position.x - startPos.x
                        val dy = change.position.y - startPos.y
                        val swipeThreshold = 38f

                        if (!triggeredSwipe && (abs(dx) > swipeThreshold || abs(dy) > swipeThreshold)) {
                            triggeredSwipe = true
                            if (abs(dy) > abs(dx)) {
                                if (dy < 0) {
                                    engine.jump()
                                } else {
                                    engine.slide()
                                }
                            } else {
                                if (dx > 0) {
                                    engine.slash()
                                } else {
                                    engine.slide()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // Read tick inside DrawScope so Compose invalidates draw phase at 60 FPS
        val activeFrame = tick
        if (activeFrame < 0L) return@Canvas

        val scale = size.width / engine.worldWidth
        val groundScreenY = size.height * 0.68f

        val shakeX = if (engine.screenShake > 0f) {
            (Random.nextFloat() * 2f - 1f) * engine.screenShake * scale * 0.6f
        } else 0f
        val shakeY = if (engine.screenShake > 0f) {
            (Random.nextFloat() * 2f - 1f) * engine.screenShake * scale * 0.6f
        } else 0f

        fun toScreenX(wx: Float): Float = wx * scale + shakeX
        fun toScreenY(wy: Float): Float = groundScreenY + (wy - engine.groundY) * scale + shakeY

        // 1. Draw Parallax Feudal Japanese Background
        drawFeudalBackground(engine, scale, groundScreenY, ::toScreenX, ::toScreenY)

        // 2. Draw Ground Track & Control Deck Backdrop
        drawGroundTrack(engine, scale, groundScreenY + shakeY)

        // 3. Draw Entities (Obstacles, Enemies, Boss, Collectibles)
        for (entity in engine.entities) {
            drawGameEntity(
                entity = entity,
                engine = engine,
                scale = scale,
                groundScreenY = groundScreenY + shakeY,
                sx = toScreenX(entity.x),
                sy = toScreenY(entity.y),
                textPaintStroke = textPaintStroke,
                textPaintFill = textPaintFill
            )
        }

        // 4. Draw Player Samurai
        drawSamuraiPlayer(
            engine = engine,
            scale = scale,
            px = toScreenX(engine.playerX),
            py = toScreenY(engine.playerY)
        )

        // 5. Draw Particles (Sakura petals, sparks, dust)
        drawParticles(engine, scale, ::toScreenX, ::toScreenY)

        // 6. Draw Floating Combat Texts directly in world space
        drawFloatingCombatTexts(
            engine = engine,
            scale = scale,
            toScreenX = ::toScreenX,
            toScreenY = ::toScreenY,
            strokePaint = textPaintStroke,
            fillPaint = textPaintFill
        )
    }
}

private fun DrawScope.drawFeudalBackground(
    engine: GameEngine,
    scale: Float,
    groundScreenY: Float,
    toScreenX: (Float) -> Float,
    toScreenY: (Float) -> Float
) {
    val dist = engine.distance.value
    val (skyTop, skyMid, skyBottom, moonColor) = when {
        engine.isDragonSprint -> Quad(
            Color(0xFF3A060E),
            Color(0xFF7A1120),
            Color(0xFFB52B1D),
            Color(0xFFFFEA00)
        )
        dist > 1200f -> Quad(
            Color(0xFF1A0510),
            Color(0xFF3E0A1D),
            Color(0xFF6B0E23),
            Color(0xFFFF2A42)
        )
        dist > 800f -> Quad(
            Color(0xFF0B1120),
            Color(0xFF172542),
            Color(0xFF283A5E),
            Color(0xFFE2E8F0)
        )
        dist > 400f -> Quad(
            Color(0xFF1D0B30),
            Color(0xFF4A154B),
            Color(0xFF9C2758),
            Color(0xFFFFD54F)
        )
        else -> Quad(
            Color(0xFF090C18),
            Color(0xFF171A36),
            Color(0xFF2E234A),
            Color(0xFFFFF3E0)
        )
    }

    // Full-sky gradient above ground
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(skyTop, skyMid, skyBottom),
            startY = 0f,
            endY = groundScreenY
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, groundScreenY)
    )

    // Giant celestial crimson/gold moon
    val moonX = size.width * 0.76f
    val moonY = groundScreenY * 0.30f
    val moonR = 62f * scale
    drawCircle(
        color = moonColor.copy(alpha = 0.16f),
        radius = moonR * 1.45f,
        center = Offset(moonX, moonY)
    )
    drawCircle(
        color = moonColor.copy(alpha = 0.32f),
        radius = moonR * 1.18f,
        center = Offset(moonX, moonY)
    )
    drawCircle(
        color = moonColor,
        radius = moonR,
        center = Offset(moonX, moonY)
    )

    // Far mountains (Layer 1 parallax)
    val farX = toScreenX(engine.bgFarX)
    val spanW = engine.worldWidth * scale
    for (tile in -1..1) {
        val baseX = farX + tile * spanW
        val mountainPath = Path().apply {
            moveTo(baseX, groundScreenY)
            lineTo(baseX, groundScreenY - 120f * scale)
            lineTo(baseX + spanW * 0.25f, groundScreenY - 240f * scale)
            lineTo(baseX + spanW * 0.55f, groundScreenY - 135f * scale)
            lineTo(baseX + spanW * 0.80f, groundScreenY - 265f * scale)
            lineTo(baseX + spanW, groundScreenY - 120f * scale)
            lineTo(baseX + spanW, groundScreenY)
            close()
        }
        drawPath(mountainPath, color = Color(0xFF131124).copy(alpha = 0.88f))
    }

    // Midground Pagodas & Bamboo Silhouettes (Layer 2 parallax)
    val midX = toScreenX(engine.bgMidX)
    for (i in -1..3) {
        val pagodaX = midX + (i * 260f) * scale
        if (pagodaX > -150f * scale && pagodaX < size.width + 150f * scale) {
            drawPagodaSilhouette(pagodaX, groundScreenY - 135f * scale, scale)
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun DrawScope.drawPagodaSilhouette(x: Float, baseY: Float, scale: Float) {
    val darkColor = Color(0xFF0D0B18)
    // Pagoda base tower
    drawRect(
        color = darkColor,
        topLeft = Offset(x - 28f * scale, baseY + 55f * scale),
        size = Size(56f * scale, 82f * scale)
    )
    // Tier 1 roof
    val roof1 = Path().apply {
        moveTo(x - 52f * scale, baseY + 60f * scale)
        lineTo(x + 52f * scale, baseY + 60f * scale)
        lineTo(x + 34f * scale, baseY + 42f * scale)
        lineTo(x - 34f * scale, baseY + 42f * scale)
        close()
    }
    drawPath(roof1, color = darkColor)
    // Tier 2 roof
    val roof2 = Path().apply {
        moveTo(x - 42f * scale, baseY + 36f * scale)
        lineTo(x + 42f * scale, baseY + 36f * scale)
        lineTo(x + 24f * scale, baseY + 20f * scale)
        lineTo(x - 24f * scale, baseY + 20f * scale)
        close()
    }
    drawPath(roof2, color = darkColor)
    // Spire
    drawRect(
        color = darkColor,
        topLeft = Offset(x - 3f * scale, baseY - 12f * scale),
        size = Size(6f * scale, 35f * scale)
    )
    // Glowing pagoda window lantern
    drawCircle(
        color = Color(0xFFFFB800).copy(alpha = 0.5f),
        radius = 4.5f * scale,
        center = Offset(x, baseY + 75f * scale)
    )
}

private fun DrawScope.drawGroundTrack(
    engine: GameEngine,
    scale: Float,
    groundScreenY: Float
) {
    val groundHeight = (size.height - groundScreenY).coerceAtLeast(0f)

    // Subterranean castle stone foundation & control deck area
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF28233C), Color(0xFF151221), Color(0xFF0C0A14)),
            startY = groundScreenY,
            endY = size.height
        ),
        topLeft = Offset(0f, groundScreenY),
        size = Size(size.width, groundHeight)
    )

    // Top turf / wooden bridge deck strip
    drawRect(
        color = Color(0xFF393252),
        topLeft = Offset(0f, groundScreenY),
        size = Size(size.width, 18f * scale)
    )

    // Glowing golden/crimson katana horizon line along the top of the ground
    drawLine(
        color = if (engine.isDragonSprint) Color(0xFFFFD700) else Color(0xFFFFB800),
        start = Offset(0f, groundScreenY),
        end = Offset(size.width, groundScreenY),
        strokeWidth = 3.5f * scale
    )

    // Scrolling wooden bridge planks & stone blocks (Layer 3 parallax)
    val nearX = engine.bgNearX * scale
    for (i in -2..14) {
        val plankX = nearX + (i * 60f) * scale
        if (plankX >= -40f && plankX <= size.width + 40f) {
            drawLine(
                color = Color(0xFF4F4770),
                start = Offset(plankX, groundScreenY + 2f * scale),
                end = Offset(plankX - 18f * scale, groundScreenY + 18f * scale),
                strokeWidth = 3f * scale
            )
        }
    }
}

private fun DrawScope.drawSamuraiPlayer(
    engine: GameEngine,
    scale: Float,
    px: Float,
    py: Float
) {
    if (engine.isInvulnerable && (System.currentTimeMillis() / 70) % 2 == 0L) {
        // Draw translucent ghost outline during invulnerability flash
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = 32f * scale,
            center = Offset(px + 22f * scale, py - 40f * scale)
        )
        return
    }

    val warrior = engine.warrior
    val katana = engine.katana

    // Dragon Sprint blazing aura
    if (engine.isDragonSprint) {
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = 0.35f),
            radius = 62f * scale,
            center = Offset(px + 22f * scale, py - 42f * scale)
        )
        drawCircle(
            color = Color(0xFFFF3D00).copy(alpha = 0.28f),
            radius = 48f * scale,
            center = Offset(px + 22f * scale, py - 42f * scale)
        )
    }

    // Shadow under samurai on the ground
    drawOval(
        color = Color.Black.copy(alpha = 0.4f),
        topLeft = Offset(px - 2f * scale, py - 5f * scale),
        size = Size(48f * scale, 10f * scale)
    )

    if (engine.isSliding) {
        // Low acrobatic power-slide pose
        drawRoundRect(
            color = warrior.primaryColor,
            topLeft = Offset(px - 8f * scale, py - 36f * scale),
            size = Size(64f * scale, 30f * scale),
            cornerRadius = CornerRadius(12f * scale)
        )
        // Armor trim
        drawRoundRect(
            color = warrior.accentColor,
            topLeft = Offset(px + 2f * scale, py - 24f * scale),
            size = Size(42f * scale, 8f * scale),
            cornerRadius = CornerRadius(4f * scale)
        )
        // Head & Kabuto
        drawCircle(
            color = Color(0xFFFFD1A4),
            radius = 12f * scale,
            center = Offset(px + 46f * scale, py - 24f * scale)
        )
        drawArc(
            color = Color(0xFF181824),
            startAngle = 170f,
            sweepAngle = 190f,
            useCenter = true,
            topLeft = Offset(px + 32f * scale, py - 40f * scale),
            size = Size(28f * scale, 24f * scale)
        )
        // Trailing scarf
        drawLine(
            color = warrior.accentColor,
            start = Offset(px + 34f * scale, py - 28f * scale),
            end = Offset(px - 26f * scale, py - 32f * scale),
            strokeWidth = 5f * scale
        )
    } else {
        // Dynamic running or mid-air leaping pose
        val legCycle = sin(engine.distance.value * 0.35f)
        val legOffset = if (engine.isGrounded) legCycle * 16f * scale else 10f * scale

        // Back Leg
        drawLine(
            color = Color(0xFF151520),
            start = Offset(px + 16f * scale, py - 26f * scale),
            end = Offset(px + 6f * scale - legOffset, py),
            strokeWidth = 7f * scale
        )
        // Front Leg
        drawLine(
            color = Color(0xFF232334),
            start = Offset(px + 28f * scale, py - 26f * scale),
            end = Offset(px + 36f * scale + legOffset, py),
            strokeWidth = 7f * scale
        )

        // Samurai Hakama & Armor Torso
        val bodyPath = Path().apply {
            moveTo(px + 6f * scale, py - 24f * scale)
            lineTo(px + 38f * scale, py - 24f * scale)
            lineTo(px + 33f * scale, py - 58f * scale)
            lineTo(px + 11f * scale, py - 58f * scale)
            close()
        }
        drawPath(bodyPath, color = warrior.primaryColor)

        // Shoulder Pauldron & Belt Sash
        drawRect(
            color = warrior.accentColor,
            topLeft = Offset(px + 8f * scale, py - 35f * scale),
            size = Size(28f * scale, 6f * scale)
        )

        // Head
        drawCircle(
            color = Color(0xFFFFD1A4),
            radius = 13f * scale,
            center = Offset(px + 23f * scale, py - 66f * scale)
        )
        // Determined eyes
        drawCircle(
            color = Color(0xFF111118),
            radius = 2.2f * scale,
            center = Offset(px + 29f * scale, py - 66f * scale)
        )

        // Kabuto Helmet
        drawArc(
            color = Color(0xFF181824),
            startAngle = 175f,
            sweepAngle = 190f,
            useCenter = true,
            topLeft = Offset(px + 7f * scale, py - 82f * scale),
            size = Size(32f * scale, 26f * scale)
        )
        // Golden Crescent Maedate (Horns)
        val hornPath = Path().apply {
            moveTo(px + 24f * scale, py - 76f * scale)
            lineTo(px + 16f * scale, py - 90f * scale)
            lineTo(px + 24f * scale, py - 83f * scale)
            lineTo(px + 32f * scale, py - 90f * scale)
            close()
        }
        drawPath(hornPath, color = Color(0xFFFFB800))

        // Flowing Crimson Headband fluttering in the wind
        val scarfWave = sin(engine.distance.value * 0.4f) * 8f * scale
        val scarfPath = Path().apply {
            moveTo(px + 12f * scale, py - 68f * scale)
            quadraticTo(
                px - 14f * scale, py - 74f * scale + scarfWave,
                px - 38f * scale, py - 62f * scale - scarfWave
            )
        }
        drawPath(
            scarfPath,
            color = warrior.accentColor,
            style = Stroke(width = 5f * scale)
        )
    }

    // Katana Blade & Crescent Slash Animation
    if (engine.isSlashing) {
        val slashProg = (1f - (engine.slashTimer / engine.slashDuration)).coerceIn(0f, 1f)
        val arcCenter = Offset(px + 30f * scale, py - 44f * scale)
        val slashRadius = (95f + warrior.slashRangeBonus * 40f) * scale

        // Outer glowing crescent arc
        drawArc(
            brush = Brush.radialGradient(
                colors = listOf(katana.trailColor, katana.bladeColor, Color.Transparent),
                center = arcCenter,
                radius = slashRadius
            ),
            startAngle = -75f + slashProg * 45f,
            sweepAngle = 135f,
            useCenter = false,
            topLeft = Offset(arcCenter.x - slashRadius, arcCenter.y - slashRadius),
            size = Size(slashRadius * 2, slashRadius * 2),
            style = Stroke(width = 16f * scale)
        )
        // Razor white inner blade edge
        drawArc(
            color = Color.White,
            startAngle = -65f + slashProg * 45f,
            sweepAngle = 115f,
            useCenter = false,
            topLeft = Offset(arcCenter.x - slashRadius, arcCenter.y - slashRadius),
            size = Size(slashRadius * 2, slashRadius * 2),
            style = Stroke(width = 4.5f * scale)
        )
    } else {
        // Ready Katana in hand
        val bladeY = if (engine.isSliding) py - 20f * scale else py - 40f * scale
        drawLine(
            color = Color(0xFFFFB800),
            start = Offset(px + 22f * scale, bladeY),
            end = Offset(px + 28f * scale, bladeY - 3f * scale),
            strokeWidth = 5f * scale
        )
        drawLine(
            color = katana.bladeColor,
            start = Offset(px + 28f * scale, bladeY - 3f * scale),
            end = Offset(px + 64f * scale, bladeY - 16f * scale),
            strokeWidth = 3.5f * scale
        )
    }
}

private fun DrawScope.drawGameEntity(
    entity: ActiveEntity,
    engine: GameEngine,
    scale: Float,
    groundScreenY: Float,
    sx: Float,
    sy: Float,
    textPaintStroke: Paint,
    textPaintFill: Paint
) {
    val ew = entity.width * scale
    val eh = entity.height * scale

    if (entity.isSliced) {
        // Dramatic diagonal split-in-half animation!
        val splitDist = entity.sliceProgress * 45f * scale
        val alpha = (1f - entity.sliceProgress).coerceIn(0f, 1f)
        rotate(degrees = -22f * entity.sliceProgress, pivot = Offset(sx, sy)) {
            drawRoundRect(
                color = Color(0xFFD32F2F).copy(alpha = alpha),
                topLeft = Offset(sx - splitDist * 0.4f, sy - splitDist),
                size = Size(ew, eh * 0.48f),
                cornerRadius = CornerRadius(6f * scale)
            )
        }
        rotate(degrees = 22f * entity.sliceProgress, pivot = Offset(sx, sy + eh)) {
            drawRoundRect(
                color = Color(0xFF4A121A).copy(alpha = alpha),
                topLeft = Offset(sx + splitDist * 0.4f, sy + eh * 0.52f + splitDist * 0.3f),
                size = Size(ew, eh * 0.48f),
                cornerRadius = CornerRadius(6f * scale)
            )
        }
        // White slash line between halves
        drawLine(
            color = Color.White.copy(alpha = alpha),
            start = Offset(sx - 10f * scale, sy + eh * 0.5f + 8f * scale),
            end = Offset(sx + ew + 10f * scale, sy + eh * 0.5f - 8f * scale),
            strokeWidth = 3f * scale
        )
        return
    }

    when (entity.kind) {
        ObstacleKind.BAMBOO_STAKE -> {
            // Three sharp bamboo stakes with crimson warning tips
            for (i in 0..2) {
                val bx = sx + i * (ew * 0.33f)
                val bw = ew * 0.30f
                val path = Path().apply {
                    moveTo(bx + bw / 2f, sy)
                    lineTo(bx + bw, sy + eh)
                    lineTo(bx, sy + eh)
                    close()
                }
                drawPath(path, color = Color(0xFF558B2F))
                // Crimson tip
                val tipPath = Path().apply {
                    moveTo(bx + bw / 2f, sy)
                    lineTo(bx + bw * 0.8f, sy + eh * 0.35f)
                    lineTo(bx + bw * 0.2f, sy + eh * 0.35f)
                    close()
                }
                drawPath(tipPath, color = Color(0xFFFF1744))
            }
        }

        ObstacleKind.ROLLING_BARREL -> {
            val r = ew / 2f
            val cx = sx + r
            val cy = sy + r
            rotate(degrees = -entity.animTimer * 320f, pivot = Offset(cx, cy)) {
                drawCircle(color = Color(0xFF6D4C41), radius = r, center = Offset(cx, cy))
                drawCircle(color = Color(0xFF3E2723), radius = r * 0.72f, center = Offset(cx, cy))
                drawCircle(color = Color(0xFFFFB800), radius = r * 0.28f, center = Offset(cx, cy))
                for (angle in 0 until 360 step 45) {
                    val rad = Math.toRadians(angle.toDouble())
                    val px = cx + (cos(rad) * (r + 5f * scale)).toFloat()
                    val py = cy + (sin(rad) * (r + 5f * scale)).toFloat()
                    drawCircle(color = Color(0xFFFF1744), radius = 4f * scale, center = Offset(px, py))
                }
            }
        }

        ObstacleKind.LOW_BEAM -> {
            // Full Torii Gate Support Columns extending down to the ground!
            val pillarWidth = 10f * scale
            drawRect(
                color = Color(0xFF8E1616),
                topLeft = Offset(sx + 4f * scale, sy),
                size = Size(pillarWidth, groundScreenY - sy)
            )
            drawRect(
                color = Color(0xFF8E1616),
                topLeft = Offset(sx + ew - pillarWidth - 4f * scale, sy),
                size = Size(pillarWidth, groundScreenY - sy)
            )
            // Heavy Overhead Crossbeam Barrier
            drawRoundRect(
                color = Color(0xFFD32F2F),
                topLeft = Offset(sx - 6f * scale, sy),
                size = Size(ew + 12f * scale, eh),
                cornerRadius = CornerRadius(6f * scale)
            )
            // Gold Ornamental Brackets
            drawRect(
                color = Color(0xFFFFB800),
                topLeft = Offset(sx + 8f * scale, sy + 6f * scale),
                size = Size(ew - 16f * scale, 8f * scale)
            )
            // Warning "SLIDE ↓" prompt underneath beam
            drawCanvasLabel(
                text = "SLIDE ↓",
                x = sx + ew / 2f,
                y = groundScreenY - 12f * scale,
                textSizePx = 13f * scale,
                color = Color(0xFFFFD54F),
                strokePaint = textPaintStroke,
                fillPaint = textPaintFill
            )
        }

        ObstacleKind.SHADOW_RONIN -> {
            // Enemy Shadow Samurai
            drawRoundRect(
                color = Color(0xFF1F1D2B),
                topLeft = Offset(sx + 6f * scale, sy + 20f * scale),
                size = Size(ew - 12f * scale, eh - 20f * scale),
                cornerRadius = CornerRadius(8f * scale)
            )
            // Crimson armor sash
            drawRect(
                color = Color(0xFFFF1744),
                topLeft = Offset(sx + 8f * scale, sy + 46f * scale),
                size = Size(ew - 16f * scale, 6f * scale)
            )
            // Conical Kasa Straw Hat
            val hat = Path().apply {
                moveTo(sx + ew / 2f, sy)
                lineTo(sx + ew + 6f * scale, sy + 22f * scale)
                lineTo(sx - 6f * scale, sy + 22f * scale)
                close()
            }
            drawPath(hat, color = Color(0xFF5D4037))
            // Glowing Menacing Red Eyes
            drawCircle(color = Color(0xFFFF1744), radius = 3.5f * scale, center = Offset(sx + ew * 0.35f, sy + 29f * scale))
            drawCircle(color = Color(0xFFFF1744), radius = 3.5f * scale, center = Offset(sx + ew * 0.62f, sy + 29f * scale))
            // Drawn Katana pointing left toward player
            drawLine(
                color = Color(0xFFE0E0E0),
                start = Offset(sx + 10f * scale, sy + 48f * scale),
                end = Offset(sx - 22f * scale, sy + 36f * scale),
                strokeWidth = 3.8f * scale
            )
        }

        ObstacleKind.TENGU_CROW -> {
            val cy = sy + eh / 2f
            val wingFlap = sin(entity.animTimer * 18f) * 15f * scale
            drawCircle(color = Color(0xFF1E1E2C), radius = 12f * scale, center = Offset(sx + ew * 0.5f, cy))
            val wing = Path().apply {
                moveTo(sx + ew * 0.5f, cy)
                lineTo(sx + 6f * scale, cy - 16f * scale + wingFlap)
                lineTo(sx + ew - 6f * scale, cy)
                close()
            }
            drawPath(wing, color = Color(0xFF311B47))
            drawCircle(color = Color(0xFFFF1744), radius = 3f * scale, center = Offset(sx + ew * 0.3f, cy - 2f * scale))
        }

        ObstacleKind.FLYING_SHURIKEN -> {
            val cx = sx + ew / 2f
            val cy = sy + eh / 2f
            val inParryRange = !entity.isDeflected && (entity.x - engine.playerX) < 195f && (entity.x - engine.playerX) > -10f
            if (inParryRange) {
                // Draw glowing Parry Window target ring!
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.45f),
                    radius = ew * 0.9f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3f * scale)
                )
            }
            val shurikenColor = if (entity.isDeflected) Color(0xFF00E5FF) else Color(0xFFECEFF1)
            rotate(degrees = entity.animTimer * 760f, pivot = Offset(cx, cy)) {
                val star = Path().apply {
                    moveTo(cx, cy - ew / 2f)
                    lineTo(cx + 5f * scale, cy - 5f * scale)
                    lineTo(cx + ew / 2f, cy)
                    lineTo(cx + 5f * scale, cy + 5f * scale)
                    lineTo(cx, cy + ew / 2f)
                    lineTo(cx - 5f * scale, cy + 5f * scale)
                    lineTo(cx - ew / 2f, cy)
                    lineTo(cx - 5f * scale, cy - 5f * scale)
                    close()
                }
                drawPath(star, color = shurikenColor)
                drawCircle(color = Color(0xFFFF1744), radius = 4f * scale, center = Offset(cx, cy))
            }
        }

        ObstacleKind.SPIRIT_MON -> {
            val cx = sx + ew / 2f
            val cy = sy + eh / 2f
            drawCircle(color = Color(0xFFFFB800).copy(alpha = 0.25f), radius = ew * 0.65f, center = Offset(cx, cy))
            drawCircle(color = Color(0xFFFFB800), radius = ew / 2f, center = Offset(cx, cy))
            drawCircle(color = Color(0xFFFFE082), radius = ew * 0.38f, center = Offset(cx, cy))
            drawRect(
                color = Color(0xFF282538),
                topLeft = Offset(cx - 3.5f * scale, cy - 3.5f * scale),
                size = Size(7f * scale, 7f * scale)
            )
        }

        ObstacleKind.SPIRIT_ORB -> {
            val cx = sx + ew / 2f
            val cy = sy + eh / 2f
            drawCircle(color = Color(0xFF00E5FF).copy(alpha = 0.35f), radius = ew * 0.75f, center = Offset(cx, cy))
            drawCircle(color = Color(0xFF00E5FF), radius = ew / 2f, center = Offset(cx, cy))
            drawCircle(color = Color.White, radius = ew * 0.25f, center = Offset(cx, cy))
        }

        ObstacleKind.JADE_HEART -> {
            val cx = sx + ew / 2f
            val cy = sy + eh / 2f
            drawCircle(color = Color(0xFF00E676).copy(alpha = 0.35f), radius = ew * 0.75f, center = Offset(cx, cy))
            drawCircle(color = Color(0xFF00E676), radius = ew / 2f, center = Offset(cx, cy))
            drawCircle(color = Color.White, radius = ew * 0.22f, center = Offset(cx - 2f * scale, cy - 2f * scale))
        }

        ObstacleKind.ONI_BOSS -> {
            // Towering Demon Oni Warlord
            drawRoundRect(
                color = Color(0xFFB71C1C),
                topLeft = Offset(sx, sy + 24f * scale),
                size = Size(ew, eh - 24f * scale),
                cornerRadius = CornerRadius(14f * scale)
            )
            // Dark Samurai War Armor
            drawRoundRect(
                color = Color(0xFF1F1218),
                topLeft = Offset(sx + 8f * scale, sy + 60f * scale),
                size = Size(ew - 16f * scale, eh - 68f * scale),
                cornerRadius = CornerRadius(8f * scale)
            )
            // Golden Demon Horns
            val hornL = Path().apply {
                moveTo(sx + 16f * scale, sy + 26f * scale)
                lineTo(sx + 4f * scale, sy - 4f * scale)
                lineTo(sx + 28f * scale, sy + 20f * scale)
                close()
            }
            val hornR = Path().apply {
                moveTo(sx + ew - 16f * scale, sy + 26f * scale)
                lineTo(sx + ew - 4f * scale, sy - 4f * scale)
                lineTo(sx + ew - 28f * scale, sy + 20f * scale)
                close()
            }
            drawPath(hornL, color = Color(0xFFFFB800))
            drawPath(hornR, color = Color(0xFFFFB800))
            // Blazing Demon Eyes
            drawCircle(color = Color(0xFFFFEA00), radius = 6.5f * scale, center = Offset(sx + ew * 0.32f, sy + 44f * scale))
            drawCircle(color = Color(0xFFFFEA00), radius = 6.5f * scale, center = Offset(sx + ew * 0.68f, sy + 44f * scale))

            // Boss HP Bar above head
            val hpFraction = (entity.health.toFloat() / entity.maxHealth.toFloat()).coerceIn(0f, 1f)
            drawRoundRect(
                color = Color(0xFF222222),
                topLeft = Offset(sx - 6f * scale, sy - 20f * scale),
                size = Size(ew + 12f * scale, 10f * scale),
                cornerRadius = CornerRadius(5f * scale)
            )
            drawRoundRect(
                color = Color(0xFFFF1744),
                topLeft = Offset(sx - 4f * scale, sy - 18f * scale),
                size = Size((ew + 8f * scale) * hpFraction, 6f * scale),
                cornerRadius = CornerRadius(3f * scale)
            )
        }
    }
}

private fun DrawScope.drawParticles(
    engine: GameEngine,
    scale: Float,
    toScreenX: (Float) -> Float,
    toScreenY: (Float) -> Float
) {
    for (p in engine.particles) {
        val px = toScreenX(p.x)
        val py = toScreenY(p.y)
        val pSize = p.size * scale

        if (p.isPetal) {
            rotate(degrees = p.rotation, pivot = Offset(px, py)) {
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
    toScreenX: (Float) -> Float,
    toScreenY: (Float) -> Float,
    strokePaint: Paint,
    fillPaint: Paint
) {
    for (item in engine.floatingTexts) {
        val sx = toScreenX(item.x)
        val sy = toScreenY(item.y)
        val alphaInt = (item.alpha.coerceIn(0f, 1f) * 255).toInt()
        val textSizePx = 17f * scale * item.scale

        strokePaint.textSize = textSizePx
        strokePaint.alpha = alphaInt

        fillPaint.textSize = textSizePx
        fillPaint.color = item.color.toArgb()
        fillPaint.alpha = alphaInt

        drawContext.canvas.nativeCanvas.drawText(item.text, sx, sy, strokePaint)
        drawContext.canvas.nativeCanvas.drawText(item.text, sx, sy, fillPaint)
    }
}

private fun DrawScope.drawCanvasLabel(
    text: String,
    x: Float,
    y: Float,
    textSizePx: Float,
    color: Color,
    strokePaint: Paint,
    fillPaint: Paint
) {
    strokePaint.textSize = textSizePx
    strokePaint.alpha = 220
    fillPaint.textSize = textSizePx
    fillPaint.color = color.toArgb()
    fillPaint.alpha = 240
    drawContext.canvas.nativeCanvas.drawText(text, x, y, strokePaint)
    drawContext.canvas.nativeCanvas.drawText(text, x, y, fillPaint)
}

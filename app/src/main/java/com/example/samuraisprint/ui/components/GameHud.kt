package com.example.samuraisprint.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.South
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.samuraisprint.audio.SoundManager
import com.example.samuraisprint.engine.GameEngine
import com.example.samuraisprint.ui.theme.SamuraiBg
import com.example.samuraisprint.ui.theme.SamuraiCrimson
import com.example.samuraisprint.ui.theme.SamuraiCyan
import com.example.samuraisprint.ui.theme.SamuraiGold

@Composable
fun GameHud(
    engine: GameEngine,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentHearts by engine.currentHearts.collectAsState()
    val maxHearts by engine.maxHearts.collectAsState()
    val distance by engine.distance.collectAsState()
    val score by engine.score.collectAsState()
    val coins by engine.coinsCollected.collectAsState()
    val combo by engine.comboCount.collectAsState()
    val rank by engine.currentRank.collectAsState()
    val focus by engine.focusMeter.collectAsState()
    val isPaused by engine.isPaused.collectAsState()
    val isGameOver by engine.isGameOver.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // TOP STATUS BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Hearts and Distance
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (i in 1..maxHearts) {
                        val hasHeart = i <= currentHearts
                        Icon(
                            imageVector = if (hasHeart) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Heart",
                            tint = if (hasHeart) SamuraiCrimson else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier
                                .size(26.dp)
                                .padding(end = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "${distance.toInt()} m",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Center: Score & Combo Rank Badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score",
                    color = SamuraiGold,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                if (combo > 1) {
                    Surface(
                        color = rank.color.copy(alpha = 0.22f),
                        border = BorderStroke(1.5.dp, rank.color),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "${rank.label} ${combo}x",
                            color = rank.color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Right: Coins & Pause
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SamuraiGold.copy(alpha = 0.45f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(SamuraiGold, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$coins",
                            color = SamuraiGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { engine.togglePause() },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // BOTTOM ERGONOMIC ARCADE CONTROL DECK (Sits in the lower 30% below ground line)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spirit Focus Bar / Dragon Rush Trigger
            val isDragonReady = focus >= 100f
            if (isDragonReady || engine.isDragonSprint) {
                Button(
                    onClick = { engine.triggerDragonSprint() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (engine.isDragonSprint) SamuraiCrimson else SamuraiGold
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .scale(pulseScale)
                        .testTag("dragon_sprint_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Dragon Sprint",
                        tint = if (engine.isDragonSprint) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (engine.isDragonSprint) "DRAGON RUSH ACTIVE!" else "TAP TO UNLEASH DRAGON RUSH!",
                        fontWeight = FontWeight.Black,
                        color = if (engine.isDragonSprint) Color.White else Color.Black,
                        fontSize = 13.sp
                    )
                }
            } else {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SamuraiCyan.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = SamuraiCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SPIRIT FOCUS",
                            color = SamuraiCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        LinearProgressIndicator(
                            progress = { (focus / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .width(110.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = SamuraiCyan,
                            trackColor = Color(0xFF283044)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${focus.toInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dual-Thumb Arcade Action Row:
            // Left Hand = Movement (SLIDE + JUMP) | Right Hand = Combat (KATANA SLASH / PARRY)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Thumb: Movement Cluster (Slide & Jump side-by-side)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionButton(
                        label = "SLIDE",
                        sublabel = "DUCK / DIVE",
                        icon = Icons.Default.South,
                        color = Color(0xFF3949AB),
                        testTag = "slide_button",
                        size = 74.dp,
                        onPressAction = { engine.slide() }
                    )

                    ActionButton(
                        label = "JUMP",
                        sublabel = "DOUBLE LEAP",
                        icon = Icons.Default.North,
                        color = Color(0xFF00897B),
                        testTag = "jump_button",
                        size = 82.dp,
                        onPressAction = { engine.jump() }
                    )
                }

                // Right Thumb: Primary Katana Slash & Parry Button
                ActionButton(
                    label = "SLASH",
                    sublabel = "STRIKE / PARRY",
                    icon = Icons.Default.ContentCut,
                    color = SamuraiCrimson,
                    testTag = "slash_button",
                    size = 94.dp,
                    onPressAction = { engine.slash() }
                )
            }
        }

        // PAUSE DIALOG OVERLAY
        AnimatedVisibility(
            visible = isPaused,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            var isMutedState by remember { mutableStateOf(SoundManager.isMuted()) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = SamuraiBg,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.5.dp, SamuraiCrimson),
                    modifier = Modifier
                        .padding(32.dp)
                        .width(310.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "BATTLE PAUSED",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { engine.togglePause() },
                            colors = ButtonDefaults.buttonColors(containerColor = SamuraiCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("resume_button")
                        ) {
                            Text("RESUME BATTLE", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                val next = !isMutedState
                                SoundManager.setMuted(next)
                                isMutedState = next
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = if (isMutedState) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Mute Audio"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isMutedState) "UNMUTE AUDIO" else "MUTE AUDIO")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { engine.startRun() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restart")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RESTART RUN")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                engine.isRunning.value = false
                                onNavigateHome()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("RETURN TO SANCTUARY", color = Color(0xFFA0A0B5))
                        }
                    }
                }
            }
        }

        // GAME OVER OVERLAY
        AnimatedVisibility(
            visible = isGameOver,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = SamuraiBg,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(2.dp, SamuraiCrimson),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(0.92f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "HONORABLE DEFEAT",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = SamuraiCrimson,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Every fall strengthens the Ronin spirit",
                            fontSize = 12.sp,
                            color = Color(0xFFA0A0B5),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Surface(
                            color = Color.Black.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                StatRow("Sprint Distance", "${distance.toInt()} m", SamuraiCyan)
                                StatRow("Combat Score", "$score PTS", SamuraiGold)
                                StatRow("Coins Collected", "+$coins", SamuraiGold)
                                StatRow("Enemies Sliced", "${engine.runSlices}", SamuraiCrimson)
                                StatRow("Projectiles Parried", "${engine.runParries}", SamuraiCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { engine.startRun() },
                            colors = ButtonDefaults.buttonColors(containerColor = SamuraiCrimson),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("try_again_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RISE AGAIN", fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                engine.isRunning.value = false
                                onNavigateHome()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("game_over_home_button")
                        ) {
                            Text("RETURN TO SANCTUARY", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFFA0A0B5), fontSize = 13.sp)
        Text(text = value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun ActionButton(
    label: String,
    sublabel: String,
    icon: ImageVector,
    color: Color,
    testTag: String,
    size: Dp = 76.dp,
    onPressAction: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size)
            .scale(if (isPressed) 0.91f else 1.0f)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        if (isPressed) Color.White.copy(alpha = 0.35f) else color,
                        color.copy(alpha = 0.78f)
                    )
                )
            )
            .border(
                width = if (isPressed) 3.dp else 2.dp,
                color = if (isPressed) Color.White else Color.White.copy(alpha = 0.45f),
                shape = CircleShape
            )
            .semantics {
                role = Role.Button
                onClick(label = label) {
                    onPressAction()
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onPressAction()
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    isPressed = false
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(size * 0.34f)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = sublabel,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

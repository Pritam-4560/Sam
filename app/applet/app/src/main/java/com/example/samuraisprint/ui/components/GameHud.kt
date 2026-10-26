package com.example.samuraisprint.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
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
                // Hearts display
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (i in 1..maxHearts) {
                        val hasHeart = i <= currentHearts
                        Icon(
                            imageVector = if (hasHeart) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Heart",
                            tint = if (hasHeart) SamuraiCrimson else Color.Gray,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Distance badge
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "${distance.toInt()}m",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            // Center: Score & Combo Rank Badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score",
                    color = SamuraiGold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                if (combo > 1) {
                    Surface(
                        color = rank.color.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, rank.color),
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
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SamuraiGold.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(SamuraiGold, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
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
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // FOCUS BAR / DRAGON SPRINT BUTTON (Bottom Center)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isDragonReady = focus >= 100f
            if (isDragonReady || engine.isDragonSprint) {
                Button(
                    onClick = { engine.triggerDragonSprint() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (engine.isDragonSprint) SamuraiCrimson else SamuraiGold
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .scale(pulseScale)
                        .testTag("dragon_sprint_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Dragon Sprint",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (engine.isDragonSprint) "DRAGON RUSH!" else "UNLEASH DRAGON",
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        fontSize = 13.sp
                    )
                }
            } else {
                // Focus progress bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SPIRIT FOCUS",
                            color = SamuraiCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${focus.toInt()}%",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { focus / 100f },
                        modifier = Modifier
                            .width(130.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SamuraiCyan,
                        trackColor = Color.DarkGray
                    )
                }
            }
        }

        // ACTION BUTTONS (Ergonomic layout for mobile)
        // Left side: SLIDE / DIVE button
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 28.dp)
        ) {
            ActionButton(
                label = "SLIDE",
                sublabel = "DUCK",
                icon = Icons.Default.South,
                color = Color(0xFF3949AB),
                testTag = "slide_button",
                onClick = { engine.slide() }
            )
        }

        // Right side: JUMP and SLASH buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            ActionButton(
                label = "JUMP",
                sublabel = if (engine.jumpCount == 1) "AIR" else "LEAP",
                icon = Icons.Default.North,
                color = Color(0xFF1E88E5),
                testTag = "jump_button",
                size = 62.dp,
                onClick = { engine.jump() }
            )

            ActionButton(
                label = "SLASH",
                sublabel = "PARRY",
                icon = Icons.Default.Bolt,
                color = SamuraiCrimson,
                testTag = "slash_button",
                size = 76.dp,
                onClick = { engine.slash() }
            )
        }

        // FLOATING COMBAT TEXT DISPLAY
        for (item in engine.floatingTexts) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(item.x.toInt(), item.y.toInt()) }
            ) {
                Text(
                    text = item.text,
                    color = item.color.copy(alpha = item.alpha.coerceIn(0f, 1f)),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.scale(item.scale)
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = SamuraiBg,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SamuraiCrimson),
                    modifier = Modifier
                        .padding(32.dp)
                        .width(300.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PAUSED",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { engine.togglePause() },
                            colors = ButtonDefaults.buttonColors(containerColor = SamuraiCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("resume_button")
                        ) {
                            Text("RESUME BATTLE", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                SoundManager.setMuted(!SoundManager.isMuted())
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (SoundManager.isMuted()) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Mute Audio"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (SoundManager.isMuted()) "UNMUTE AUDIO" else "MUTE AUDIO")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                engine.startRun()
                            },
                            modifier = Modifier.fillMaxWidth()
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
                            modifier = Modifier.fillMaxWidth()
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
                    border = androidx.compose.foundation.BorderStroke(2.dp, SamuraiCrimson),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(0.9f)
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

                        // Stats Grid
                        Surface(
                            color = Color.Black.copy(alpha = 0.4f),
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
                                .height(50.dp)
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
                                .height(46.dp)
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    testTag: String,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0.7f))
                )
            )
            .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(size * 0.36f)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

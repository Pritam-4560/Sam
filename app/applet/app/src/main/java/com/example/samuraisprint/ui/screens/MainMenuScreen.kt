package com.example.samuraisprint.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.samuraisprint.R
import com.example.samuraisprint.audio.SoundManager
import com.example.samuraisprint.data.GameRepository
import com.example.samuraisprint.ui.theme.SamuraiBg
import com.example.samuraisprint.ui.theme.SamuraiCard
import com.example.samuraisprint.ui.theme.SamuraiCrimson
import com.example.samuraisprint.ui.theme.SamuraiCrimsonGlow
import com.example.samuraisprint.ui.theme.SamuraiCyan
import com.example.samuraisprint.ui.theme.SamuraiGold

@Composable
fun MainMenuScreen(
    repository: GameRepository,
    onStartGame: () -> Unit,
    onNavigateDojo: () -> Unit,
    onNavigateArmory: () -> Unit,
    onNavigateTrials: () -> Unit
) {
    val coins by repository.coins.collectAsState()
    val highScore by repository.highScore.collectAsState()
    val bestDistance by repository.bestDistance.collectAsState()
    val totalSlices by repository.totalSlices.collectAsState()
    val totalParries by repository.totalParries.collectAsState()

    var showGuideDialog by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(SoundManager.isMuted()) }
    var isVibEnabled by remember { mutableStateOf(SoundManager.isVibrationEnabled()) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    if (showGuideDialog) {
        CombatGuideDialog(onDismiss = { showGuideDialog = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SamuraiBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_samurai_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, SamuraiCrimson, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SAMURAI SPRINT",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.2.sp,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Coin pill
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SamuraiGold.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            val nextState = !isMuted
                            SoundManager.setMuted(nextState)
                            isMuted = nextState
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Audio Toggle",
                            tint = if (isMuted) Color.Gray else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val nextVib = !isVibEnabled
                            SoundManager.setVibrationEnabled(nextVib)
                            isVibEnabled = nextVib
                            if (nextVib) SoundManager.vibrate(30, 100)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Haptics Toggle",
                            tint = if (isVibEnabled) SamuraiCyan else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // HERO ART BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(230.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, SamuraiCrimson.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_samurai_hero),
                    contentDescription = "Samurai Hero",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark cinematic vignette gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF101017).copy(alpha = 0.4f),
                                    Color(0xFF101017).copy(alpha = 0.95f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Surface(
                        color = SamuraiCrimson.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "WAY OF THE RONIN",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SAMURAI SPRINT",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Acrobatic runner • Katana air slashes • Projectile parrying",
                        fontSize = 11.sp,
                        color = Color(0xFFE0E0F0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // BIG SPRINT NOW BUTTON
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Button(
                    onClick = {
                        SoundManager.playSound(SoundManager.SoundType.SLASH)
                        SoundManager.vibrate(50, 180)
                        onStartGame()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SamuraiCrimson
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .scale(pulseScale)
                        .testTag("play_game_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SPRINT NOW",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // STATS BANNER
            Surface(
                color = SamuraiCard,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatSummaryItem("BEST DISTANCE", "${bestDistance}m", SamuraiCyan)
                    StatSummaryItem("HIGH SCORE", "$highScore PTS", SamuraiGold)
                    StatSummaryItem("ENEMIES SLICED", "$totalSlices", SamuraiCrimson)
                    StatSummaryItem("PARRIES", "$totalParries", Color(0xFF00E676))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // NAVIGATION MENU TILES
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuNavigationCard(
                    title = "DOJO UPGRADES",
                    subtitle = "Enhance vitality, blade reach, focus gathering & luck",
                    icon = Icons.Default.Tune,
                    iconColor = SamuraiCrimson,
                    testTag = "menu_dojo_button",
                    onClick = onNavigateDojo
                )

                MenuNavigationCard(
                    title = "WARRIORS & BLADES",
                    subtitle = "Select Shinobi warriors and forge enchanted katanas",
                    icon = Icons.Default.Bolt,
                    iconColor = SamuraiGold,
                    testTag = "menu_armory_button",
                    onClick = onNavigateArmory
                )

                MenuNavigationCard(
                    title = "HONOR TRIALS",
                    subtitle = "Master trials of blade & speed for extra coins",
                    icon = Icons.Default.EmojiEvents,
                    iconColor = SamuraiCyan,
                    testTag = "menu_trials_button",
                    onClick = onNavigateTrials
                )

                MenuNavigationCard(
                    title = "WAY OF COMBAT",
                    subtitle = "Learn jump, double jump, katana slash, deflect & dragon rush",
                    icon = Icons.Default.HelpOutline,
                    iconColor = Color(0xFFBA68C8),
                    testTag = "menu_guide_button",
                    onClick = { showGuideDialog = true }
                )
            }
        }
    }
}

@Composable
private fun StatSummaryItem(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 9.sp, color = Color(0xFFA0A0B5), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, color = color, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun MenuNavigationCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = SamuraiCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFFA0A0B5),
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

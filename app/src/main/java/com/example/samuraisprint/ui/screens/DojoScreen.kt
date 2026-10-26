package com.example.samuraisprint.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.samuraisprint.model.UpgradeType
import com.example.samuraisprint.ui.theme.SamuraiBg
import com.example.samuraisprint.ui.theme.SamuraiCard
import com.example.samuraisprint.ui.theme.SamuraiCrimson
import com.example.samuraisprint.ui.theme.SamuraiGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DojoScreen(
    repository: GameRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val coins by repository.coins.collectAsState()
    val upgrades by repository.upgradeLevels.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "DOJO UPGRADES",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("dojo_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Coin pill
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SamuraiGold.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(end = 12.dp)
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
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SamuraiBg,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = SamuraiBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Dojo Hero Temple Art
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, SamuraiCrimson.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_dojo_temple),
                        contentDescription = "Dojo Temple",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xFF101017).copy(alpha = 0.9f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Ancient Master's Sanctuary",
                            color = SamuraiGold,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hone your blade, strengthen your body, and attune to spirits.",
                            color = Color(0xFFD0D0E0),
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Upgrade cards
            items(UpgradeType.entries) { upgradeType ->
                val currentLvl = upgrades[upgradeType] ?: 1
                val isMax = currentLvl >= upgradeType.maxLevel
                val cost = if (!isMax) repository.getUpgradeCost(upgradeType, currentLvl) else 0
                val canAfford = coins >= cost && !isMax

                UpgradeCard(
                    upgradeType = upgradeType,
                    currentLevel = currentLvl,
                    cost = cost,
                    isMax = isMax,
                    canAfford = canAfford,
                    onUpgrade = {
                        if (repository.upgradeSkill(upgradeType)) {
                            SoundManager.playSound(SoundManager.SoundType.COIN)
                            SoundManager.vibrate(50, 160)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun UpgradeCard(
    upgradeType: UpgradeType,
    currentLevel: Int,
    cost: Int,
    isMax: Boolean,
    canAfford: Boolean,
    onUpgrade: () -> Unit
) {
    Surface(
        color = SamuraiCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isMax) SamuraiGold.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                when (upgradeType) {
                                    UpgradeType.SLASH_REACH -> SamuraiCrimson.copy(alpha = 0.25f)
                                    UpgradeType.MAX_HEALTH -> Color(0xFFE91E63).copy(alpha = 0.25f)
                                    UpgradeType.FOCUS_CHARGE -> Color(0xFF00E5FF).copy(alpha = 0.25f)
                                    UpgradeType.COIN_MAGNET -> SamuraiGold.copy(alpha = 0.25f)
                                    UpgradeType.COMBO_MASTERY -> Color(0xFF9C27B0).copy(alpha = 0.25f)
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (upgradeType) {
                            UpgradeType.SLASH_REACH -> Icons.Default.ContentCut
                            UpgradeType.MAX_HEALTH -> Icons.Default.Favorite
                            UpgradeType.FOCUS_CHARGE -> Icons.Default.Bolt
                            UpgradeType.COIN_MAGNET -> Icons.Default.Stars
                            UpgradeType.COMBO_MASTERY -> Icons.Default.Speed
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = when (upgradeType) {
                                UpgradeType.SLASH_REACH -> SamuraiCrimson
                                UpgradeType.MAX_HEALTH -> Color(0xFFFF4081)
                                UpgradeType.FOCUS_CHARGE -> Color(0xFF00E5FF)
                                UpgradeType.COIN_MAGNET -> SamuraiGold
                                UpgradeType.COMBO_MASTERY -> Color(0xFFBA68C8)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = upgradeType.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = upgradeType.description,
                            color = Color(0xFFA0A0B5),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Level Dots & Upgrade Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress indicators (1 to 5)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (i in 1..upgradeType.maxLevel) {
                        val isFilled = i <= currentLevel
                        Box(
                            modifier = Modifier
                                .padding(end = 5.dp)
                                .size(width = 24.dp, height = 7.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isFilled) SamuraiCrimson else Color(0xFF333345))
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LV $currentLevel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA0A0B5)
                    )
                }

                // Button
                if (isMax) {
                    Surface(
                        color = SamuraiGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SamuraiGold)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SamuraiGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MASTERED",
                                color = SamuraiGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onUpgrade,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SamuraiCrimson,
                            disabledContainerColor = Color(0xFF333342)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("upgrade_${upgradeType.name.lowercase()}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(SamuraiGold, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$cost",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (canAfford) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

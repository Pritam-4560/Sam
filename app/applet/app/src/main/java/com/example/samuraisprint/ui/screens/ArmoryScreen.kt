package com.example.samuraisprint.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.samuraisprint.audio.SoundManager
import com.example.samuraisprint.data.GameRepository
import com.example.samuraisprint.model.Katana
import com.example.samuraisprint.model.Warrior
import com.example.samuraisprint.ui.theme.SamuraiBg
import com.example.samuraisprint.ui.theme.SamuraiCard
import com.example.samuraisprint.ui.theme.SamuraiCrimson
import com.example.samuraisprint.ui.theme.SamuraiCyan
import com.example.samuraisprint.ui.theme.SamuraiGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmoryScreen(
    repository: GameRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val coins by repository.coins.collectAsState()
    val selectedWarriorId by repository.selectedWarriorId.collectAsState()
    val selectedKatanaId by repository.selectedKatanaId.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "WARRIORS & BLADES",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("armory_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SamuraiBg,
                contentColor = SamuraiCrimson,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = SamuraiCrimson
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "SHINOBI WARRIORS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 0) SamuraiCrimson else Color(0xFFA0A0B5)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "FORGED KATANAS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 1) SamuraiCrimson else Color(0xFFA0A0B5)
                        )
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (selectedTab == 0) {
                    items(repository.warriors) { warrior ->
                        val isSelected = warrior.id == selectedWarriorId
                        val isUnlocked = warrior.isUnlocked || warrior.id == "kage"
                        val canAfford = coins >= warrior.unlockCost

                        WarriorCard(
                            warrior = warrior,
                            isSelected = isSelected,
                            isUnlocked = isUnlocked,
                            canAfford = canAfford,
                            onSelect = {
                                repository.selectWarrior(warrior.id)
                                SoundManager.playSound(SoundManager.SoundType.COIN)
                            },
                            onUnlock = {
                                if (repository.unlockWarrior(warrior)) {
                                    repository.selectWarrior(warrior.id)
                                    SoundManager.playSound(SoundManager.SoundType.DRAGON_SPRINT)
                                    SoundManager.vibrate(60, 200)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                } else {
                    items(repository.katanas) { katana ->
                        val isSelected = katana.id == selectedKatanaId
                        val isUnlocked = katana.isUnlocked || katana.id == "steel"
                        val canAfford = coins >= katana.unlockCost

                        KatanaCard(
                            katana = katana,
                            isSelected = isSelected,
                            isUnlocked = isUnlocked,
                            canAfford = canAfford,
                            onSelect = {
                                repository.selectKatana(katana.id)
                                SoundManager.playSound(SoundManager.SoundType.SLASH)
                            },
                            onUnlock = {
                                if (repository.unlockKatana(katana)) {
                                    repository.selectKatana(katana.id)
                                    SoundManager.playSound(SoundManager.SoundType.DRAGON_SPRINT)
                                    SoundManager.vibrate(60, 200)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WarriorCard(
    warrior: Warrior,
    isSelected: Boolean,
    isUnlocked: Boolean,
    canAfford: Boolean,
    onSelect: () -> Unit,
    onUnlock: () -> Unit
) {
    Surface(
        color = SamuraiCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) SamuraiCrimson else if (isUnlocked) Color.White.copy(alpha = 0.1f) else Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(warrior.accentColor, warrior.primaryColor)
                                ),
                                CircleShape
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = warrior.name.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = warrior.name,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Text(
                            text = warrior.title,
                            color = warrior.accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                if (isSelected) {
                    Surface(
                        color = SamuraiCrimson.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SamuraiCrimson)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = SamuraiCrimson, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ACTIVE", color = SamuraiCrimson, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = warrior.description, color = Color(0xFFA0A0B5), fontSize = 12.sp, lineHeight = 16.sp)

            Spacer(modifier = Modifier.height(12.dp))

            // Stat Meters
            StatBar("Speed", warrior.speedMultiplier / 1.2f, SamuraiCyan)
            Spacer(modifier = Modifier.height(6.dp))
            StatBar("Slash Reach", 0.6f + warrior.slashRangeBonus, SamuraiCrimson)
            Spacer(modifier = Modifier.height(6.dp))
            StatBar("Fortune Pull", warrior.coinMultiplier / 1.5f, SamuraiGold)

            Spacer(modifier = Modifier.height(14.dp))

            if (isUnlocked) {
                if (!isSelected) {
                    Button(
                        onClick = onSelect,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C3A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("select_warrior_${warrior.id}")
                    ) {
                        Text("EQUIP WARRIOR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                Button(
                    onClick = onUnlock,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SamuraiGold,
                        disabledContainerColor = Color(0xFF333342)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("unlock_warrior_${warrior.id}")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = if (canAfford) Color.Black else Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "UNLOCK FOR ${warrior.unlockCost} COINS",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = if (canAfford) Color.Black else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun KatanaCard(
    katana: Katana,
    isSelected: Boolean,
    isUnlocked: Boolean,
    canAfford: Boolean,
    onSelect: () -> Unit,
    onUnlock: () -> Unit
) {
    Surface(
        color = SamuraiCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) katana.trailColor else if (isUnlocked) Color.White.copy(alpha = 0.1f) else Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Glowing Katana blade preview bar
                    Box(
                        modifier = Modifier
                            .size(width = 8.dp, height = 44.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(katana.bladeColor, katana.trailColor)
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(text = katana.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(text = katana.specialEffect, color = katana.trailColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (isSelected) {
                    Surface(
                        color = katana.trailColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, katana.trailColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = katana.trailColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WIELDED", color = katana.trailColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = katana.description, color = Color(0xFFA0A0B5), fontSize = 12.sp, lineHeight = 16.sp)

            Spacer(modifier = Modifier.height(14.dp))

            if (isUnlocked) {
                if (!isSelected) {
                    Button(
                        onClick = onSelect,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C3A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("select_katana_${katana.id}")
                    ) {
                        Text("WIELD BLADE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                Button(
                    onClick = onUnlock,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SamuraiGold,
                        disabledContainerColor = Color(0xFF333342)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("unlock_katana_${katana.id}")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = if (canAfford) Color.Black else Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FORGE FOR ${katana.unlockCost} COINS",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = if (canAfford) Color.Black else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBar(label: String, progress: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFFA0A0B5), fontSize = 11.sp, modifier = Modifier.width(85.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFF2A2A38)
        )
    }
}

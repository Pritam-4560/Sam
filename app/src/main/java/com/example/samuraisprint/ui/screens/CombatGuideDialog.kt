package com.example.samuraisprint.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.South
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.samuraisprint.ui.theme.SamuraiBg
import com.example.samuraisprint.ui.theme.SamuraiCard
import com.example.samuraisprint.ui.theme.SamuraiCrimson
import com.example.samuraisprint.ui.theme.SamuraiCyan
import com.example.samuraisprint.ui.theme.SamuraiGold

@Composable
fun CombatGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SamuraiBg,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, SamuraiCrimson),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "WAY OF COMBAT",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = SamuraiCrimson,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Master the art of the Ronin runner",
                    fontSize = 12.sp,
                    color = Color(0xFFA0A0B5),
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )

                GuideItem(
                    icon = Icons.Default.North,
                    iconBg = Color(0xFF1E88E5),
                    title = "JUMP & DOUBLE JUMP",
                    description = "Tap JUMP to leap over bamboo spike stakes and rolling barrels. Tap again in mid-air to double jump over chasm pits!"
                )

                Spacer(modifier = Modifier.height(10.dp))

                GuideItem(
                    icon = Icons.Default.Bolt,
                    iconBg = SamuraiCrimson,
                    title = "KATANA SLASH",
                    description = "Tap SLASH to slice oncoming shadow ninja ronin, airborne crows, and wooden barricades in your path."
                )

                Spacer(modifier = Modifier.height(10.dp))

                GuideItem(
                    icon = Icons.Default.Shield,
                    iconBg = SamuraiCyan,
                    title = "PARRY & DEFLECT",
                    description = "When ninja projectiles (shurikens) fly toward you, time your SLASH right before impact to DEFLECT them back for massive style points!"
                )

                Spacer(modifier = Modifier.height(10.dp))

                GuideItem(
                    icon = Icons.Default.South,
                    iconBg = Color(0xFF3949AB),
                    title = "SLIDE UNDER HAZARDS",
                    description = "Tap SLIDE to crouch and slide safely underneath burning Torii beams and low hanging banners."
                )

                Spacer(modifier = Modifier.height(10.dp))

                GuideItem(
                    icon = Icons.Default.Bolt,
                    iconBg = SamuraiGold,
                    title = "DRAGON SPRINT",
                    description = "Defeat enemies and collect azure spirit orbs to fill your focus meter. At 100%, unleash DRAGON SPRINT to become invincible and cleave all obstacles!"
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SamuraiCrimson),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("close_guide_button")
                ) {
                    Text("I AM READY", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun GuideItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    description: String
) {
    Surface(
        color = SamuraiCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFFA0A0B5),
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

package com.bitchat.android.features.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitchat.android.ui.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val GlyphBackground = Color(0xFF0B0B0B)
private val GlyphCard = Color(0xFF1A1A1A)
private val GlyphAccent = Color(0xFFFF3B30)
private val GlyphTextPrimary = Color(0xFFFFFFFF)
private val GlyphTextSecondary = Color(0xFFAAAAAA)

enum class SOSType(
    val label: String,
    val subtitle: String,
    val icon: String,
    val accent: Color
) {
    MEDICAL("Medical Emergency", "Broadcast medical distress signal", "🩺", Color(0xFFFF3B30)),
    CRIME("Crime / Safety Threat", "Broadcast safety threat alert", "🚨", Color(0xFFFF9500)),
    DISASTER("Disaster / Attack", "Broadcast disaster emergency", "⚠️", Color(0xFFFFCC00))
}

@Composable
fun SettingsScreen(chatViewModel: ChatViewModel) {
    var sosActive by remember { mutableStateOf(false) }
    var pendingSOSType by remember { mutableStateOf<SOSType?>(null) }
    var cooldownActive by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val sosCardColor by animateColorAsState(
        targetValue = if (sosActive) Color(0xFF2A0A0A) else GlyphCard,
        animationSpec = tween(400),
        label = "sos_card_color"
    )

    // Confirmation dialog
    pendingSOSType?.let { type ->
        AlertDialog(
            onDismissRequest = { pendingSOSType = null },
            containerColor = Color(0xFF1A1A1A),
            title = {
                Text(
                    "${type.icon} ${type.label}",
                    color = type.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    "Send emergency SOS to all nearby mesh devices?\n\nYour location will be broadcast to others.",
                    color = GlyphTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingSOSType = null
                        chatViewModel.triggerSOS()
                        sosActive = true
                        cooldownActive = true
                        cooldownSeconds = 30
                        scope.launch {
                            repeat(30) {
                                delay(1000)
                                cooldownSeconds--
                            }
                            cooldownActive = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = type.accent)
                ) {
                    Text("SEND SOS", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSOSType = null }) {
                    Text("Cancel", color = GlyphTextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlyphBackground)
            .systemBarsPadding()
    ) {
        Text(
            text = "SETTINGS",
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            ),
            color = GlyphTextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick SOS toggle
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = sosCardColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (sosActive) GlyphAccent else Color(0xFF2A2A2A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Emergency SOS",
                                        color = if (sosActive) GlyphAccent else GlyphTextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (sosActive) "Broadcasting emergency + location" else "Broadcast location + emergency alert",
                                        color = GlyphTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Switch(
                                checked = sosActive,
                                onCheckedChange = { enabled ->
                                    sosActive = enabled
                                    if (enabled) chatViewModel.triggerSOS()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GlyphAccent,
                                    uncheckedThumbColor = Color(0xFF555555),
                                    uncheckedTrackColor = Color(0xFF2A2A2A)
                                )
                            )
                        }

                        if (sosActive) {
                            HorizontalDivider(color = GlyphAccent.copy(alpha = 0.3f))
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = GlyphAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "🚨 SOS ACTIVE — Emergency signal sent to mesh",
                                    color = GlyphAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Emergency SOS Types section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "EMERGENCY TYPE",
                        color = GlyphTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                    SettingsCard {
                        Column {
                            SOSType.values().forEachIndexed { index, type ->
                                SOSTypeItem(
                                    type = type,
                                    cooldownActive = cooldownActive,
                                    cooldownSeconds = cooldownSeconds,
                                    onClick = {
                                        if (!cooldownActive) pendingSOSType = type
                                    }
                                )
                                if (index < SOSType.values().size - 1) {
                                    HorizontalDivider(color = Color(0xFF2A2A2A))
                                }
                            }
                        }
                    }
                }
            }

            // AI Features - Always ON
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1A2A1A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = Color(0xFF00C851),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "AI Features",
                                    color = GlyphTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Smart routing, content intelligence, emergency detection",
                                    color = GlyphTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF00C851), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("ACTIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // General Settings
            item {
                SettingsCard {
                    Column {
                        SettingsItem(Icons.Default.Person, "Profile", "Manage your identity")
                        HorizontalDivider(color = Color(0xFF2A2A2A))
                        SettingsItem(Icons.Default.Notifications, "Notifications", "Alert preferences")
                        HorizontalDivider(color = Color(0xFF2A2A2A))
                        SettingsItem(Icons.Default.Security, "Privacy & Security", "Encryption settings")
                        HorizontalDivider(color = Color(0xFF2A2A2A))
                        SettingsItem(Icons.Default.Storage, "Storage", "Manage local data")
                    }
                }
            }

            item {
                SettingsCard {
                    Column {
                        SettingsItem(Icons.Default.Info, "About", "Version & licenses")
                        HorizontalDivider(color = Color(0xFF2A2A2A))
                        SettingsItem(Icons.Default.BugReport, "Debug", "Developer tools")
                    }
                }
            }
        }
    }
}

@Composable
private fun SOSTypeItem(
    type: SOSType,
    cooldownActive: Boolean,
    cooldownSeconds: Int,
    onClick: () -> Unit
) {
    val isDisabled = cooldownActive
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDisabled) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(type.accent.copy(alpha = if (isDisabled) 0.2f else 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(type.icon, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                type.label,
                color = if (isDisabled) GlyphTextSecondary else GlyphTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                type.subtitle,
                color = GlyphTextSecondary,
                fontSize = 12.sp
            )
        }
        if (isDisabled) {
            Text(
                "${cooldownSeconds}s",
                color = GlyphTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = type.accent.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GlyphCard,
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GlyphTextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = GlyphTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = GlyphTextSecondary, fontSize = 12.sp)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF444444),
            modifier = Modifier.size(18.dp)
        )
    }
}

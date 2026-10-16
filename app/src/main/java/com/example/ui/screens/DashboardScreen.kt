package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ChessLensApplication
import com.example.data.AnalysisMode
import com.example.service.ChessLensAccessibilityService
import com.example.service.ChessLensOverlayService
import com.example.ui.theme.ChessLensCyan
import com.example.ui.theme.ChessLensGreen
import com.example.ui.theme.ChessLensRed
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAnalysis: (String?) -> Unit,
    onNavigateToVideo: () -> Unit,
    onNavigateToPuzzles: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onImageImported: (Uri) -> Unit
) {
    val context = LocalContext.current
    val app = ChessLensApplication.instance
    val scope = rememberCoroutineScope()

    val overlayActive by app.preferencesManager.overlayActive.collectAsState(initial = false)
    val isAccessibilityRunning by ChessLensAccessibilityService.isServiceRunning.collectAsState()
    val isOverlayServiceRunning by ChessLensOverlayService.isOverlayRunning.collectAsState()
    val recentHistory by app.historyRepository.allHistory.collectAsState(initial = emptyList())

    // Photo / image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageImported(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ChessLensCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("♞", fontSize = 20.sp, color = ChessLensCyan)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ChessLens",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        containerColor = Slate950
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Health Panel (Green/Red indicators with one-tap fixes)
            HealthPanel(
                isAccessibilityOk = isAccessibilityRunning,
                isOverlayOk = isOverlayServiceRunning || Settings.canDrawOverlays(context),
                isEngineOk = true,
                onFixAccessibility = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } catch (e: Exception) {}
                },
                onFixOverlay = {
                    if (!Settings.canDrawOverlays(context)) {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main "On The Screen" Master Control Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(ChessLensCyan.copy(alpha = 0.4f))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Live Screen Overlay",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isOverlayServiceRunning) "Active • Floating bubble ready" else "Shows arrows on videos & apps",
                                fontSize = 13.sp,
                                color = if (isOverlayServiceRunning) ChessLensGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isOverlayServiceRunning,
                            onCheckedChange = { active ->
                                val intent = Intent(context, ChessLensOverlayService::class.java)
                                if (active) {
                                    context.startService(intent)
                                } else {
                                    intent.action = ChessLensOverlayService.ACTION_STOP
                                    context.startService(intent)
                                }
                                scope.launch {
                                    app.preferencesManager.setOverlayActive(active)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Slate900, checkedTrackColor = ChessLensCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(context, ChessLensOverlayService::class.java).apply {
                                    action = ChessLensOverlayService.ACTION_ANALYZE_NOW
                                }
                                context.startService(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = ChessLensCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Snapshot Screen", fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = ChessLensCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Photo", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Feature Hub
            Text(
                text = "Analysis Hub",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))

            FeatureCard(
                title = "Interactive Analysis Board",
                subtitle = "Study positions, test lines, and view multi-level engine arrows",
                icon = Icons.Default.Layers,
                iconTint = ChessLensCyan,
                onClick = { onNavigateToAnalysis(null) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeatureCard(
                title = "Video Timeline Analysis",
                subtitle = "Timeline scrubber, eval graph over time, and blunder moments",
                icon = Icons.Default.Videocam,
                iconTint = Color(0xFFFBBF24),
                onClick = onNavigateToVideo
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeatureCard(
                title = "Tactical Puzzles & Think-First",
                subtitle = "Sharpen calculation skills with pedagogical hints and scoring",
                icon = Icons.Default.Psychology,
                iconTint = Color(0xFF4ADE80),
                onClick = onNavigateToPuzzles
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Active Engine Levels strip
            Text(
                text = "Engine Levels (Stockfish / Lichess)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LevelBadge(name = "L1 Blue", depth = "d5", color = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                LevelBadge(name = "L2 Teal", depth = "d10", color = Color(0xFF2DD4BF), modifier = Modifier.weight(1f))
                LevelBadge(name = "L3 Green", depth = "d15", color = Color(0xFF4ADE80), modifier = Modifier.weight(1f))
                LevelBadge(name = "Avoid", depth = "Red", color = Color(0xFFF87171), modifier = Modifier.weight(1f))
            }

            // Recent Analysis Card
            if (recentHistory.isNotEmpty()) {
                val latest = recentHistory.first()
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Analysis",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "View All",
                        fontSize = 13.sp,
                        color = ChessLensCyan,
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAnalysis(latest.fen) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Best Move: ${latest.bestMoveSan}",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = latest.explanation,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate800
                        ) {
                            Text(
                                text = "${if (latest.evalCentipawns > 0) "+" else ""}%.1f".format(latest.evalCentipawns / 100f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChessLensCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthPanel(
    isAccessibilityOk: Boolean,
    isOverlayOk: Boolean,
    isEngineOk: Boolean,
    onFixAccessibility: () -> Unit,
    onFixOverlay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "System Health",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HealthItem(
                    label = "Accessibility",
                    isOk = isAccessibilityOk,
                    onClick = if (!isAccessibilityOk) onFixAccessibility else null
                )
                HealthItem(
                    label = "Overlay",
                    isOk = isOverlayOk,
                    onClick = if (!isOverlayOk) onFixOverlay else null
                )
                HealthItem(
                    label = "Engine",
                    isOk = isEngineOk,
                    onClick = null
                )
                HealthItem(
                    label = "Vision",
                    isOk = true,
                    onClick = null
                )
            }
        }
    }
}

@Composable
private fun HealthItem(
    label: String,
    isOk: Boolean,
    onClick: (() -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isOk) ChessLensGreen else ChessLensRed)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isOk) Color.White else ChessLensRed,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate800)
        }
    }
}

@Composable
private fun LevelBadge(name: String, depth: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Slate900)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = depth, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

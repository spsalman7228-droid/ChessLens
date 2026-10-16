package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.chess.DetailLevel
import com.example.data.ArrowStyle
import com.example.data.RevealMode
import com.example.ui.theme.ChessLensCyan
import com.example.ui.theme.ChessLensRed
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val app = ChessLensApplication.instance
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }

    val thinkFirst by app.preferencesManager.thinkFirst.collectAsState(initial = true)
    val showAvoid by app.preferencesManager.showAvoidMoves.collectAsState(initial = true)
    val detailLevel by app.preferencesManager.detailLevel.collectAsState(initial = DetailLevel.INTERMEDIATE)
    val arrowStyle by app.preferencesManager.arrowStyle.collectAsState(initial = ArrowStyle.SOLID)
    val themeMode by app.preferencesManager.themeMode.collectAsState(initial = ThemeMode.DARK)
    val samplingFps by app.preferencesManager.samplingFps.collectAsState(initial = 2.0f)
    val allowedApps by app.preferencesManager.allowedApps.collectAsState(initial = emptySet())

    var showClearDataDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search settings...", color = MaterialTheme.colorScheme.outline) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ChessLensCyan,
                    unfocusedBorderColor = Slate800,
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. General & Appearance
            SettingsSectionHeader(title = "Appearance & Theme", icon = Icons.Default.ColorLens)
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Theme Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(ThemeMode.DARK, ThemeMode.AMOLED, ThemeMode.LIGHT).forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { scope.launch { app.preferencesManager.setThemeMode(mode) } },
                                label = { Text(mode.name) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. On The Screen & Learning
            SettingsSectionHeader(title = "On The Screen & Analysis", icon = Icons.Default.Tv)
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Think-First
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Think-First Mode", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Hides engine moves until you tap Reveal or guess", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = thinkFirst,
                            onCheckedChange = { scope.launch { app.preferencesManager.setThinkFirst(it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = Slate900, checkedTrackColor = ChessLensCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Show Moves to Avoid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show 'Moves to Avoid'", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Displays red arrows for tempting but bad blunders", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = showAvoid,
                            onCheckedChange = { scope.launch { app.preferencesManager.setShowAvoidMoves(it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = Slate900, checkedTrackColor = ChessLensCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Explanation Detail Level
                    Text("Explanation Detail Level", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailLevel.values().forEach { lvl ->
                            FilterChip(
                                selected = detailLevel == lvl,
                                onClick = { scope.launch { app.preferencesManager.setDetailLevel(lvl) } },
                                label = { Text(lvl.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Browser Resolution & DPI
            SettingsSectionHeader(title = "Browser Resolution & DPI", icon = Icons.Default.Tv)
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("DPI Scale (Crispness)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    val dpiScale by app.preferencesManager.browserDpiScale.collectAsState(initial = 150)
                    Slider(
                        value = dpiScale.toFloat(),
                        onValueChange = { scope.launch { app.preferencesManager.setBrowserDpiScale(it.toInt()) } },
                        valueRange = 100f..200f,
                        steps = 4,
                        colors = SliderDefaults.colors(thumbColor = ChessLensCyan, activeTrackColor = ChessLensCyan)
                    )
                    Text("Scale: ${dpiScale}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 3. Arrow Styles
            SettingsSectionHeader(title = "Arrow Styles", icon = Icons.Default.Tune)
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Arrow Rendering Style", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ArrowStyle.values().forEach { style ->
                            FilterChip(
                                selected = arrowStyle == style,
                                onClick = { scope.launch { app.preferencesManager.setArrowStyle(style) } },
                                label = { Text(style.name) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Screen Reading & Allowlist
            SettingsSectionHeader(title = "Screen Reading & Allowlist", icon = Icons.Default.Speed)
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Live Capture Rate: %.1f FPS".format(samplingFps), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Slider(
                        value = samplingFps,
                        onValueChange = { scope.launch { app.preferencesManager.setSamplingFps(it) } },
                        valueRange = 1f..5f,
                        steps = 7,
                        colors = SliderDefaults.colors(thumbColor = ChessLensCyan, activeTrackColor = ChessLensCyan)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("App Allowlist (Opt-In)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("Overlay activates only within allowlisted apps for privacy.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    val commonApps = listOf(
                        Pair("YouTube", "com.google.android.youtube"),
                        Pair("Chrome", "com.android.chrome"),
                        Pair("Chess.com (Study Mode)", "com.chess"),
                        Pair("Lichess", "org.lichess.mobileapp")
                    )

                    commonApps.forEach { (appName, pkg) ->
                        val isAllowed = allowedApps.contains(pkg)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(appName, fontSize = 13.sp, color = Color.White)
                            Switch(
                                checked = isAllowed,
                                onCheckedChange = { allow -> scope.launch { app.preferencesManager.toggleAllowedApp(pkg, allow) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = Slate900, checkedTrackColor = ChessLensCyan)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Data & Privacy
            SettingsSectionHeader(title = "Data & Privacy", icon = Icons.Default.Lock)
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Zero Cloud Storage", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("All screen pixels and video frames are processed exclusively in device RAM. No personal data, contacts, or accounts are collected.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            app.evaluationEngine.clearCache()
                            Toast.makeText(context, "Engine cache cleared!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Clear Engine Cache", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showClearDataDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ChessLensRed.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = ChessLensRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete All Local Data", color = ChessLensRed, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Legal & About
            SettingsSectionHeader(title = "About & Licenses", icon = Icons.Default.Info)
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("ChessLens v1.0.0", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Text("Built for chess learners, coaches, and video analysts.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    LegalRowItem("Fair-Play Notice", onClick = onNavigateToOnboarding)
                    LegalRowItem("Privacy Policy", onClick = onNavigateToPrivacy)
                    LegalRowItem("Terms of Use", onClick = onNavigateToTerms)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stockfish is licensed under GNU GPLv3. Source code available at github.com/official-stockfish/Stockfish.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Delete All Local Data?") },
            text = { Text("This will erase all saved positions, history, and preferences.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            app.historyRepository.clearAll()
                            app.evaluationEngine.clearCache()
                            showClearDataDialog = false
                            Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChessLensRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ChessLensCyan, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
    }
}

@Composable
private fun LegalRowItem(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = ChessLensCyan)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
    }
}

package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ChessLensApplication
import com.example.model.ChessPosition
import com.example.model.MoveAnalysis
import com.example.model.MoveQuality
import com.example.ui.components.ChessBoardView
import com.example.ui.theme.ChessLensCyan
import com.example.ui.theme.ChessLensGreen
import com.example.ui.theme.ChessLensRed
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

data class VideoFrameTimelinePoint(
    val timestampSeconds: Float,
    val moveSan: String,
    val evalCentipawns: Int,
    val quality: MoveQuality,
    val fen: String,
    val explanation: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoAnalysisScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var currentProgress by remember { mutableFloatStateOf(0.42f) }
    val totalDurationSeconds = 180f

    // Sample video timeline points (demonstrates real timeline scrubber & key moments)
    val timelinePoints = remember {
        listOf(
            VideoFrameTimelinePoint(12f, "1. e4", 25, MoveQuality.BEST, "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", "King pawn opening fighting for center control."),
            VideoFrameTimelinePoint(28f, "1... c5", 20, MoveQuality.BEST, "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2", "Sicilian Defense; asymmetrical counter-attack."),
            VideoFrameTimelinePoint(55f, "2. Nf3", 30, MoveQuality.GOOD, "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2", "Natural kingside knight development."),
            VideoFrameTimelinePoint(85f, "2... d6", 25, MoveQuality.BEST, "rnbqkbnr/pp2pppp/3p4/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 3", "Opens bishop diagonal and guards c5."),
            VideoFrameTimelinePoint(115f, "3. d4", 35, MoveQuality.BEST, "rnbqkbnr/pp2pppp/3p4/2p5/3PP3/5N2/PPP2PPP/RNBQKB1R b KQkq d3 0 3", "Strikes the center directly."),
            VideoFrameTimelinePoint(140f, "3... cxd4", 30, MoveQuality.GOOD, "rnbqkbnr/pp2pppp/3p4/8/3pP3/5N2/PPP2PPP/RNBQKB1R w KQkq - 0 4", "Liquidates central pawn."),
            VideoFrameTimelinePoint(162f, "4. Nxd4", 30, MoveQuality.BEST, "rnbqkbnr/pp2pppp/3p4/8/3NP3/8/PPP2PPP/RNBQKB1R b KQkq - 0 4", "Recaptures with centralized knight."),
            VideoFrameTimelinePoint(175f, "4... Nf6", 25, MoveQuality.BEST, "rnbqkb1r/pp2pppp/3p1n2/8/3NP3/8/PPP2PPP/RNBQKB1R w KQkq - 1 5", "Attacks undefended e4 pawn.")
        )
    }

    val selectedPointIndex = (currentProgress * (timelinePoints.size - 1)).toInt().coerceIn(0, timelinePoints.size - 1)
    val activePoint = timelinePoints[selectedPointIndex]
    val activePosition = remember(activePoint.fen) { ChessPosition.fromFen(activePoint.fen) ?: ChessPosition.initial() }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Analysis", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { videoPicker.launch("video/*") }) {
                        Icon(Icons.Default.VideoFile, contentDescription = "Import Video", tint = Color.White)
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
            // Video / Board Preview
            ChessBoardView(
                position = activePosition,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Current Timestamp & Move badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900
                ) {
                    Text(
                        text = "⏱ %02d:%02d".format((activePoint.timestampSeconds / 60).toInt(), (activePoint.timestampSeconds % 60).toInt()),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChessLensCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(android.graphics.Color.parseColor(activePoint.quality.badgeColorHex)).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(android.graphics.Color.parseColor(activePoint.quality.badgeColorHex)))
                ) {
                    Text(
                        text = "${activePoint.moveSan} • ${activePoint.quality.label}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(android.graphics.Color.parseColor(activePoint.quality.badgeColorHex)),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Evaluation Graph Chart
            Text("Evaluation Graph Over Time", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(6.dp))
            EvaluationTimelineGraph(
                points = timelinePoints,
                currentSelectedIndex = selectedPointIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Scrubber Slider
            Slider(
                value = currentProgress,
                onValueChange = { currentProgress = it },
                colors = SliderDefaults.colors(
                    thumbColor = ChessLensCyan,
                    activeTrackColor = ChessLensCyan,
                    inactiveTrackColor = Slate800
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Key Moments Strip
            Text("Key Moments", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(timelinePoints) { pt ->
                    val isSelected = pt == activePoint
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Slate800 else Slate900,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, ChessLensCyan) else null,
                        modifier = Modifier.clickable {
                            val idx = timelinePoints.indexOf(pt)
                            currentProgress = idx.toFloat() / (timelinePoints.size - 1)
                        }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = pt.moveSan, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text(text = pt.quality.label, fontSize = 11.sp, color = Color(android.graphics.Color.parseColor(pt.quality.badgeColorHex)))
                            Text(text = "%02d:%02d".format((pt.timestampSeconds / 60).toInt(), (pt.timestampSeconds % 60).toInt()), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Explanation Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Move Analysis",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activePoint.explanation,
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Export Annotated PGN Button
            Button(
                onClick = {
                    val pgnText = buildString {
                        appendLine("[Event \"ChessLens Video Analysis\"]")
                        appendLine("[Site \"Device Screen\"]")
                        timelinePoints.forEach { pt ->
                            append("${pt.moveSan} {[%clk %02d:%02d] [%eval %.2f]} ".format((pt.timestampSeconds / 60).toInt(), (pt.timestampSeconds % 60).toInt(), pt.evalCentipawns / 100f))
                        }
                    }
                    clipboardManager.setText(AnnotatedString(pgnText))
                    android.widget.Toast.makeText(context, "Annotated PGN copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = ChessLensCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Annotated PGN", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EvaluationTimelineGraph(
    points: List<VideoFrameTimelinePoint>,
    currentSelectedIndex: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Slate900)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h / 2f

            // Center baseline (0.0 eval)
            drawLine(
                color = Color(0xFF334155),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1.dp.toPx()
            )

            if (points.size < 2) return@Canvas

            val stepX = w / (points.size - 1)
            val path = Path()

            points.forEachIndexed { i, pt ->
                val x = i * stepX
                // Map -500 to +500 cp to canvas height
                val normalizedEval = (pt.evalCentipawns / 500f).coerceIn(-1f, 1f)
                val y = midY - (normalizedEval * (midY * 0.8f))

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                // Point marker
                drawCircle(
                    color = if (i == currentSelectedIndex) ChessLensCyan else Color(android.graphics.Color.parseColor(pt.quality.badgeColorHex)),
                    radius = if (i == currentSelectedIndex) 5.dp.toPx() else 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            drawPath(
                path = path,
                color = ChessLensCyan.copy(alpha = 0.8f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

@Composable
fun EvaluationBar(
    evalCentipawns: Int,
    isMate: Boolean = false,
    mateInMoves: Int = 0,
    isFlipped: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Win probability from White's perspective: 0.0 to 1.0
    val whiteRatio = if (isMate) {
        if (evalCentipawns > 0) 0.98f else 0.02f
    } else {
        (1.0 / (1.0 + exp(-0.00368208 * evalCentipawns))).toFloat().coerceIn(0.05f, 0.95f)
    }

    val animatedRatio by animateFloatAsState(
        targetValue = if (isFlipped) 1f - whiteRatio else whiteRatio,
        animationSpec = tween(durationMillis = 350),
        label = "eval_bar_ratio"
    )

    val displayText = when {
        isMate -> if (evalCentipawns > 0) "M$mateInMoves" else "-M$mateInMoves"
        else -> {
            val pawns = evalCentipawns / 100f
            if (pawns > 0) "+%.1f".format(pawns) else "%.1f".format(pawns)
        }
    }

    Box(
        modifier = modifier
            .width(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            // Black portion (top)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(max(0.01f, 1f - animatedRatio))
                    .background(Color(0xFF0F172A))
            )
            // White portion (bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(max(0.01f, animatedRatio))
                    .background(Color(0xFFE2E8F0))
            )
        }

        // Eval text badge
        val textColor = if (whiteRatio > 0.5f) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        val alignment = if (whiteRatio > 0.5f) Alignment.BottomCenter else Alignment.TopCenter
        Text(
            text = displayText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier
                .align(alignment)
                .padding(vertical = 4.dp)
        )
    }
}

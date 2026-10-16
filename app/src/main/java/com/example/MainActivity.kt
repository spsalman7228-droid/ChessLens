package com.example

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.legal.LegalContent
import com.example.ui.screens.AnalysisBoardScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LegalScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PuzzlesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoAnalysisScreen
import com.example.ui.theme.ChessLensTheme
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    private var sharedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            sharedImageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        setContent {
            val app = ChessLensApplication.instance
            val themeMode by app.preferencesManager.themeMode.collectAsState(initial = ThemeMode.DARK)
            val dynamicColor by app.preferencesManager.dynamicColor.collectAsState(initial = false)
            val onboardingDone by app.preferencesManager.onboardingCompleted.collectAsState(initial = true)

            ChessLensTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                ChessLensNavHost(
                    initialOnboardingDone = onboardingDone,
                    sharedImageUri = sharedImageUri,
                    onClearSharedUri = { sharedImageUri = null }
                )
            }
        }
    }
}

@Composable
fun ChessLensNavHost(
    initialOnboardingDone: Boolean,
    sharedImageUri: Uri?,
    onClearSharedUri: () -> Unit
) {
    val navController = rememberNavController()
    val app = ChessLensApplication.instance
    val scope = rememberCoroutineScope()

    // Handle shared image if present
    var detectedSharedFen by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val startDestination = if (initialOnboardingDone) "dashboard" else "onboarding"

    fun processImageUri(uri: Uri) {
        scope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val result = app.boardDetector.detectBoard(bitmap)
                    if (result != null) {
                        val encodedFen = URLEncoder.encode(result.position.toFen(), "UTF-8")
                        navController.navigate("analysis?fen=$encodedFen")
                    } else {
                        Toast.makeText(context, "No chessboard detected in this image", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error analyzing image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    sharedImageUri?.let { uri ->
        processImageUri(uri)
        onClearSharedUri()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onFinished = {
                    scope.launch {
                        app.preferencesManager.setOnboardingDone(true)
                        app.preferencesManager.setFairPlayAck(true)
                    }
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                onNavigateToBrowser = { navController.navigate("browser") },
                onNavigateToAnalysis = { fen ->
                    if (fen != null) {
                        val encoded = URLEncoder.encode(fen, "UTF-8")
                        navController.navigate("analysis?fen=$encoded")
                    } else {
                        navController.navigate("analysis")
                    }
                },
                onNavigateToVideo = { navController.navigate("video") },
                onNavigateToPuzzles = { navController.navigate("puzzles") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToSettings = { navController.navigate("settings") },
                onImageImported = { uri -> processImageUri(uri) }
            )
        }

        composable("browser") {
            com.example.ui.screens.ChessBrowserScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAnalysis = { navController.navigate("analysis") },
                onNavigateToVideo = { navController.navigate("video") },
                onNavigateToPuzzles = { navController.navigate("puzzles") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        composable(
            route = "analysis?fen={fen}",
            arguments = listOf(
                navArgument("fen") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val rawFen = backStackEntry.arguments?.getString("fen")
            val decodedFen = rawFen?.let {
                try { URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { null }
            }
            AnalysisBoardScreen(
                initialFen = decodedFen,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("video") {
            VideoAnalysisScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("puzzles") {
            PuzzlesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("history") {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onSelectPosition = { fen ->
                    val encoded = URLEncoder.encode(fen, "UTF-8")
                    navController.navigate("analysis?fen=$encoded")
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate("privacy_policy") },
                onNavigateToTerms = { navController.navigate("terms_of_use") },
                onNavigateToOnboarding = { navController.navigate("onboarding") }
            )
        }

        composable("privacy_policy") {
            LegalScreen(
                title = "Privacy Policy",
                content = LegalContent.PRIVACY_POLICY,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("terms_of_use") {
            LegalScreen(
                title = "Terms of Use",
                content = LegalContent.TERMS_OF_USE,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

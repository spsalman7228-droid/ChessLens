package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.chess.DetailLevel
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chesslens_settings")

enum class AnalysisMode {
    LIVE,
    SNAPSHOT,
    VIDEO,
    IMPORT
}

enum class RevealMode {
    ALL,
    LEVEL_BY_LEVEL,
    EVAL_FIRST
}

enum class ArrowStyle {
    SOLID,
    DASHED,
    GLOW
}

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_FAIR_PLAY_ACK = booleanPreferencesKey("fair_play_ack")
        val KEY_OVERLAY_ACTIVE = booleanPreferencesKey("overlay_active")
        val KEY_MODE = stringPreferencesKey("mode")
        val KEY_THINK_FIRST = booleanPreferencesKey("think_first")
        val KEY_REVEAL_MODE = stringPreferencesKey("reveal_mode")
        val KEY_DETAIL_LEVEL = stringPreferencesKey("detail_level")
        val KEY_SHOW_AVOID = booleanPreferencesKey("show_avoid")
        val KEY_SAMPLING_FPS = floatPreferencesKey("sampling_fps")
        val KEY_ARROW_STYLE = stringPreferencesKey("arrow_style")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_ALLOWED_APPS = stringSetPreferencesKey("allowed_apps")
        val KEY_FLIP_BOARD = booleanPreferencesKey("flip_board")
        val KEY_AUTO_HIDE_BUBBLE = booleanPreferencesKey("auto_hide_bubble")
        val KEY_BROWSER_DPI_SCALE = intPreferencesKey("browser_dpi_scale")
        val KEY_BROWSER_RESOLUTION_MODE = stringPreferencesKey("browser_resolution_mode")
        val KEY_BROWSER_TEXT_ZOOM = intPreferencesKey("browser_text_zoom")
        val KEY_ALWAYS_ANALYZE_MOVES = booleanPreferencesKey("always_analyze_moves")
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val fairPlayAcknowledged: Flow<Boolean> = context.dataStore.data.map { it[KEY_FAIR_PLAY_ACK] ?: false }
    val overlayActive: Flow<Boolean> = context.dataStore.data.map { it[KEY_OVERLAY_ACTIVE] ?: false }
    val mode: Flow<AnalysisMode> = context.dataStore.data.map {
        try { AnalysisMode.valueOf(it[KEY_MODE] ?: AnalysisMode.SNAPSHOT.name) } catch (e: Exception) { AnalysisMode.SNAPSHOT }
    }
    val thinkFirst: Flow<Boolean> = context.dataStore.data.map { it[KEY_THINK_FIRST] ?: true }
    val revealMode: Flow<RevealMode> = context.dataStore.data.map {
        try { RevealMode.valueOf(it[KEY_REVEAL_MODE] ?: RevealMode.ALL.name) } catch (e: Exception) { RevealMode.ALL }
    }
    val detailLevel: Flow<DetailLevel> = context.dataStore.data.map {
        try { DetailLevel.valueOf(it[KEY_DETAIL_LEVEL] ?: DetailLevel.INTERMEDIATE.name) } catch (e: Exception) { DetailLevel.INTERMEDIATE }
    }
    val showAvoidMoves: Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_AVOID] ?: true }
    val samplingFps: Flow<Float> = context.dataStore.data.map { it[KEY_SAMPLING_FPS] ?: 2.0f }
    val arrowStyle: Flow<ArrowStyle> = context.dataStore.data.map {
        try { ArrowStyle.valueOf(it[KEY_ARROW_STYLE] ?: ArrowStyle.SOLID.name) } catch (e: Exception) { ArrowStyle.SOLID }
    }
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map {
        try { ThemeMode.valueOf(it[KEY_THEME_MODE] ?: ThemeMode.DARK.name) } catch (e: Exception) { ThemeMode.DARK }
    }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[KEY_DYNAMIC_COLOR] ?: false }
    val allowedApps: Flow<Set<String>> = context.dataStore.data.map { it[KEY_ALLOWED_APPS] ?: emptySet() }
    val isFlipped: Flow<Boolean> = context.dataStore.data.map { it[KEY_FLIP_BOARD] ?: false }
    val browserDpiScale: Flow<Int> = context.dataStore.data.map { it[KEY_BROWSER_DPI_SCALE] ?: 150 }
    val browserResolutionMode: Flow<String> = context.dataStore.data.map { it[KEY_BROWSER_RESOLUTION_MODE] ?: "150% Super Sharp" }
    val browserTextZoom: Flow<Int> = context.dataStore.data.map { it[KEY_BROWSER_TEXT_ZOOM] ?: 115 }
    val alwaysAnalyzeMoves: Flow<Boolean> = context.dataStore.data.map { it[KEY_ALWAYS_ANALYZE_MOVES] ?: true }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setFairPlayAck(ack: Boolean) {
        context.dataStore.edit { it[KEY_FAIR_PLAY_ACK] = ack }
    }

    suspend fun setOverlayActive(active: Boolean) {
        context.dataStore.edit { it[KEY_OVERLAY_ACTIVE] = active }
    }

    suspend fun setMode(mode: AnalysisMode) {
        context.dataStore.edit { it[KEY_MODE] = mode.name }
    }

    suspend fun setThinkFirst(enabled: Boolean) {
        context.dataStore.edit { it[KEY_THINK_FIRST] = enabled }
    }

    suspend fun setRevealMode(mode: RevealMode) {
        context.dataStore.edit { it[KEY_REVEAL_MODE] = mode.name }
    }

    suspend fun setDetailLevel(level: DetailLevel) {
        context.dataStore.edit { it[KEY_DETAIL_LEVEL] = level.name }
    }

    suspend fun setShowAvoidMoves(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_AVOID] = show }
    }

    suspend fun setSamplingFps(fps: Float) {
        context.dataStore.edit { it[KEY_SAMPLING_FPS] = fps }
    }

    suspend fun setArrowStyle(style: ArrowStyle) {
        context.dataStore.edit { it[KEY_ARROW_STYLE] = style.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun toggleAllowedApp(packageName: String, allow: Boolean) {
        context.dataStore.edit {
            val current = (it[KEY_ALLOWED_APPS] ?: emptySet()).toMutableSet()
            if (allow) current.add(packageName) else current.remove(packageName)
            it[KEY_ALLOWED_APPS] = current
        }
    }

    suspend fun setFlipped(flipped: Boolean) {
        context.dataStore.edit { it[KEY_FLIP_BOARD] = flipped }
    }

    suspend fun setBrowserDpiScale(dpiScale: Int) {
        context.dataStore.edit { it[KEY_BROWSER_DPI_SCALE] = dpiScale }
    }

    suspend fun setBrowserResolutionMode(mode: String) {
        context.dataStore.edit { it[KEY_BROWSER_RESOLUTION_MODE] = mode }
    }

    suspend fun setBrowserTextZoom(textZoom: Int) {
        context.dataStore.edit { it[KEY_BROWSER_TEXT_ZOOM] = textZoom }
    }

    suspend fun setAlwaysAnalyzeMoves(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ALWAYS_ANALYZE_MOVES] = enabled }
    }
}

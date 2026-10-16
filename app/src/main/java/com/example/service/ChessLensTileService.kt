package com.example.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.MainActivity

class ChessLensTileService : TileService() {

    override fun onClick() {
        super.onClick()
        // If accessibility service is running, trigger screenshot analysis
        val accessibility = ChessLensAccessibilityService.instance
        if (accessibility != null) {
            val intent = Intent(this, ChessLensOverlayService::class.java).apply {
                action = ChessLensOverlayService.ACTION_ANALYZE_NOW
            }
            startService(intent)
        } else {
            // Open main app to prompt accessibility setup
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivityAndCollapse(intent)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val isServiceActive = ChessLensAccessibilityService.isServiceRunning.value
        tile.state = if (isServiceActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Analyze Screen"
        tile.updateTile()
    }
}

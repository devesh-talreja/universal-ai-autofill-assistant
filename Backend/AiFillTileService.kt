package com.example.smartautofiller.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class AiFillTileService : TileService() {

    companion object {
        const val ACTION_TOGGLE = "com.example.smartautofiller.TILE_TOGGLE"
        const val ACTION_OPEN_ACCESSIBILITY = "com.example.smartautofiller.OPEN_ACCESSIBILITY"

        // Tile update karo bahar se bhi
        fun requestTileUpdate(context: Context) {
            requestListeningState(context, android.content.ComponentName(context, AiFillTileService::class.java))
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        val prefs = getSharedPreferences("autofill_prefs", MODE_PRIVATE)
        val isBubbleEnabled = prefs.getBoolean("bubble_enabled", false)
        val accessibilityOn = SmartAccessibilityService.instance != null

        if (!accessibilityOn) {
            // Accessibility settings open karo
            openAccessibilitySettings()
        } else {
            // Bubble toggle karo
            val newState = !isBubbleEnabled
            prefs.edit().putBoolean("bubble_enabled", newState).apply()
            SmartAccessibilityService.instance?.setBubbleVisible(newState)
            updateTile()
        }
    }

    private fun openAccessibilitySettings() {
        try {
            // Direct SmartAiBubble settings pe le jao
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val bundle = android.os.Bundle()
                bundle.putString(
                    ":settings:fragment_args_key",
                    "com.example.smartautofiller/.service.SmartAccessibilityService"
                )
                putExtra(":settings:show_fragment_args", bundle)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ — PendingIntent use karo
                val pendingIntent = PendingIntent.getActivity(
                    this, 1001, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            // Fallback — general accessibility settings
            val fallback = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this, 1002, fallback,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                startActivityAndCollapse(fallback)
            }
        }
    }

    fun updateTile() {
        val tile = qsTile ?: return
        val prefs = getSharedPreferences("autofill_prefs", MODE_PRIVATE)
        val isBubbleEnabled = prefs.getBoolean("bubble_enabled", false)
        val accessibilityOn = SmartAccessibilityService.instance != null

        when {
            !accessibilityOn -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "AI Fill"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Tap to setup"
                }
            }
            isBubbleEnabled -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "AI Fill"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Bubble ON"
                }
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "AI Fill"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Bubble OFF"
                }
            }
        }
        tile.updateTile()
    }

    override fun onTileAdded() { super.onTileAdded(); updateTile() }
    override fun onTileRemoved() { super.onTileRemoved() }
    override fun onStopListening() { super.onStopListening() }
}
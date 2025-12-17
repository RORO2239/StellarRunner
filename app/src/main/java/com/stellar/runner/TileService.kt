package com.stellar.runner

import android.annotation.TargetApi
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

@TargetApi(Build.VERSION_CODES.N)
class TileService : TileService() {
    
    private val prefs: SharedPreferences by lazy { 
        getSharedPreferences("tile", MODE_PRIVATE) 
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        
        val tile = qsTile ?: return
        val command = prefs.getString("content", null).orEmpty()
        val command1 = prefs.getString("content1", null).orEmpty()
        val isSwitchMode = prefs.getBoolean("switch", false)

        when {
            command.isEmpty() -> openSettings()
            !isSwitchMode -> executeCommand(command)
            else -> handleSwitchMode(tile, command, command1)
        }
    }

    private fun updateTileState() {
        qsTile?.apply {
            val hasCommand = !prefs.getString("content", null).isNullOrEmpty()
            
            label = if (hasCommand) "点击执行命令" else "长按设置命令"
            state = if (hasCommand) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            
            updateTile()
        }
    }

    private fun openSettings() {
        startActivityAndCollapse(
            Intent(this, TileSetting::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun executeCommand(command: String) {
        startActivityAndCollapse(
            Intent(this, ExecNoDisplay::class.java).apply {
                putExtra("content", command)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun handleSwitchMode(tile: Tile, commandOn: String, commandOff: String) {
        val isActive = tile.state == Tile.STATE_ACTIVE
        val commandToExecute = if (isActive) commandOff else commandOn
        
        executeCommand(commandToExecute)
        
        tile.state = if (isActive) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.updateTile()
    }
}

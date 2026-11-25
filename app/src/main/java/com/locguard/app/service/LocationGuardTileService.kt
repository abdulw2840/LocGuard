package com.locguard.app.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.locguard.app.LocGuardApp
import com.locguard.app.monitor.GuardPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LocationGuardTileService : TileService() {

    private val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collectJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        collectJob?.cancel()
        collectJob = tileScope.launch {
            LocGuardApp.instance.guardEngine.state.collect { state ->
                val tile = qsTile ?: return@collect
                when (state.phase) {
                    GuardPhase.PAUSED -> {
                        tile.state = Tile.STATE_INACTIVE
                        tile.label = "LocGuard"
                        tile.subtitle = "Paused"
                    }
                    GuardPhase.COUNTING_DOWN -> {
                        tile.state = Tile.STATE_ACTIVE
                        tile.label = "LocGuard"
                        tile.subtitle = "Off in ${state.remainingSeconds}s"
                    }
                    GuardPhase.PROTECTED_WHITELISTED -> {
                        tile.state = Tile.STATE_ACTIVE
                        tile.label = "LocGuard"
                        tile.subtitle = "Protected"
                    }
                    GuardPhase.SCREEN_OFF_GRACE -> {
                        tile.state = Tile.STATE_ACTIVE
                        tile.label = "LocGuard"
                        tile.subtitle = "Closing..."
                    }
                    GuardPhase.IDLE_LOCATION_OFF -> {
                        tile.state = Tile.STATE_ACTIVE
                        tile.label = "LocGuard"
                        tile.subtitle = "Location Off"
                    }
                }
                tile.updateTile()
            }
        }
    }

    override fun onStopListening() {
        collectJob?.cancel()
        collectJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val app = LocGuardApp.instance
        tileScope.launch {
            val state = app.guardEngine.state.value
            if (state.isLocationEnabled) {
                app.guardEngine.turnOffNow()
            } else {
                val settings = app.preferences.settingsFlow.first()
                app.preferences.setGuardEnabled(!settings.guardEnabled)
            }
        }
    }

    override fun onDestroy() {
        tileScope.cancel()
        super.onDestroy()
    }
}

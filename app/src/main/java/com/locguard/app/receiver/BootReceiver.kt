package com.locguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.locguard.app.LocGuardApp
import com.locguard.app.service.LocationGuardService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = LocGuardApp.instance.preferences.settingsFlow.first()
                    if (settings.guardEnabled) {
                        LocationGuardService.start(context)
                    }
                } finally {
                    pending.finish()
                }
            }
        }
    }
}

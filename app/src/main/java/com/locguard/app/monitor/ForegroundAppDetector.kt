package com.locguard.app.monitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.provider.Settings
import com.locguard.app.privilege.PrivilegeManager
import com.locguard.app.service.GuardAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ForegroundAppDetector(
    private val context: Context,
    private val privilegeManager: PrivilegeManager
) {
    private val usageStatsManager by lazy {
        context.getSystemService(UsageStatsManager::class.java)
    }

    private val _accessibilityPackage = MutableStateFlow<String?>(null)
    val accessibilityPackage: StateFlow<String?> = _accessibilityPackage.asStateFlow()

    fun onAccessibilityWindowChanged(packageName: String) {
        if (packageName.isNotBlank() && packageName != "com.android.systemui") {
            _accessibilityPackage.value = packageName
        }
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val target = "${context.packageName}/${GuardAccessibilityService::class.java.name}"
        val shortTarget = "${context.packageName}/.service.GuardAccessibilityService"
        return enabledServices.contains(target) || enabledServices.contains(shortTarget)
    }

    suspend fun getCurrentForegroundPackage(): String? = withContext(Dispatchers.IO) {
        if (isAccessibilityServiceEnabled()) {
            _accessibilityPackage.value?.let { return@withContext it }
        }

        if (privilegeManager.hasUsageStatsPermission()) {
            queryUsageStatsForeground()?.let { return@withContext it }
        }

        privilegeManager.getForegroundPackagePrivileged()
    }

    private fun queryUsageStatsForeground(): String? {
        val now = System.currentTimeMillis()
        val events = usageStatsManager?.queryEvents(now - 60_000L, now) ?: return null
        val event = UsageEvents.Event()
        var latestPkg: String? = null
        var latestTs = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED &&
                event.timeStamp >= latestTs
            ) {
                latestTs = event.timeStamp
                latestPkg = event.packageName
            }
        }
        return latestPkg
    }
}

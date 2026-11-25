package com.locguard.app.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.locguard.app.LocGuardApp
import com.locguard.app.data.GuardLogEntry
import com.locguard.app.data.GuardSettings
import com.locguard.app.data.LogCategory
import com.locguard.app.data.PrivilegePreference
import com.locguard.app.monitor.GuardRuntimeState
import com.locguard.app.privilege.PrivilegeStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val icon: ImageBitmap?
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LocGuardApp

    val settings: StateFlow<GuardSettings> = app.preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GuardSettings())

    val runtimeState: StateFlow<GuardRuntimeState> = app.guardEngine.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GuardRuntimeState())

    val privilegeStatus: StateFlow<PrivilegeStatus> = app.privilegeManager.status
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrivilegeStatus())

    val logs: StateFlow<List<GuardLogEntry>> = app.logRepository.logs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(true)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    init {
        refreshPrivileges()
        loadInstalledApps()
    }

    fun refreshPrivileges() {
        viewModelScope.launch {
            app.privilegeManager.refreshStatus()
        }
    }

    fun requestShizukuPermission() {
        app.privilegeManager.requestShizukuPermission()
    }

    fun grantPermissionsViaPrivilege() {
        viewModelScope.launch {
            val ok = app.privilegeManager.grantRequiredPermissions()
            app.logRepository.add(
                LogCategory.PRIVILEGE,
                if (ok) "Permissions Auto-Granted" else "Permission Grant Failed",
                if (ok) "UsageStats & Secure Settings granted via ${privilegeStatus.value.activeBackend.displayName}"
                else "Ensure Root or Shizuku is active"
            )
        }
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        app.startActivity(intent)
    }

    fun setGuardEnabled(enabled: Boolean) {
        viewModelScope.launch { app.preferences.setGuardEnabled(enabled) }
    }

    fun setAutoEnableForWhitelisted(enabled: Boolean) {
        viewModelScope.launch { app.preferences.setAutoEnableForWhitelisted(enabled) }
    }

    fun setAutoDisableOnScreenOff(enabled: Boolean) {
        viewModelScope.launch { app.preferences.setAutoDisableOnScreenOff(enabled) }
    }

    fun setScreenOffGraceSeconds(seconds: Int) {
        viewModelScope.launch { app.preferences.setScreenOffGraceSeconds(seconds) }
    }

    fun setTimeoutSeconds(seconds: Int) {
        viewModelScope.launch { app.preferences.setTimeoutSeconds(seconds) }
    }

    fun setPrivilegePreference(pref: PrivilegePreference) {
        viewModelScope.launch { app.preferences.setPrivilegePreference(pref) }
    }

    fun setShowCountdownNotification(show: Boolean) {
        viewModelScope.launch { app.preferences.setShowCountdownNotification(show) }
    }

    fun toggleWhitelistedPackage(packageName: String) {
        viewModelScope.launch { app.preferences.toggleWhitelistedPackage(packageName) }
    }

    fun turnOffLocationNow() {
        app.guardEngine.turnOffNow()
    }

    fun extendTimer() {
        app.guardEngine.extendTimer(60)
    }

    fun clearLogs() {
        app.logRepository.clear()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingApps.value = true
            val pm = app.packageManager
            val apps = pm.getInstalledApplications(0)
                .filter { it.packageName != app.packageName }
                .mapNotNull { info ->
                    val hasLauncher = pm.getLaunchIntentForPackage(info.packageName) != null
                    val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (!hasLauncher && isSystem) return@mapNotNull null

                    val label = runCatching { pm.getApplicationLabel(info).toString() }
                        .getOrDefault(info.packageName)

                    InstalledAppInfo(
                        packageName = info.packageName,
                        label = label,
                        isSystem = isSystem,
                        icon = null // load icons lazily below
                    )
                }
                .sortedBy { it.label.lowercase() }

            // Show list immediately without icons
            _installedApps.value = apps
            _isLoadingApps.value = false

            // Load icons in background batches of 20
            val withIcons = apps.toMutableList()
            for (i in withIcons.indices) {
                val info = withIcons[i]
                if (info.icon == null) {
                    val icon = runCatching {
                        val appInfo = pm.getApplicationInfo(info.packageName, 0)
                        pm.getApplicationIcon(appInfo).toBitmap(72, 72).asImageBitmap()
                    }.getOrNull()
                    withIcons[i] = info.copy(icon = icon)
                }
                if ((i + 1) % 20 == 0) {
                    _installedApps.value = withIcons.toList()
                }
            }
            _installedApps.value = withIcons.toList()
        }
    }
}

package com.locguard.app.privilege

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.IBinder
import android.os.Process
import android.provider.Settings
import com.locguard.app.BuildConfig
import com.locguard.app.IPrivilegedService
import com.locguard.app.data.PrivilegePreference
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

enum class ActiveBackend(val displayName: String) {
    ROOT("Root (libsu daemon)"),
    SHIZUKU("Shizuku UserService"),
    WRITE_SECURE("Secure Settings"),
    NONE("No Privilege")
}

data class PrivilegeStatus(
    val isRootAvailable: Boolean = false,
    val isShizukuBinderAlive: Boolean = false,
    val isShizukuPermissionGranted: Boolean = false,
    val isShizukuServiceBound: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val hasWriteSecureSettings: Boolean = false,
    val activeBackend: ActiveBackend = ActiveBackend.NONE
)

class PrivilegeManager(private val context: Context) {

    private val _status = MutableStateFlow(PrivilegeStatus())
    val status: StateFlow<PrivilegeStatus> = _status.asStateFlow()

    @Volatile
    private var shizukuService: IPrivilegedService? = null

    @Volatile
    private var currentPreference: PrivilegePreference = PrivilegePreference.AUTO

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, ShizukuUserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("privileged")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            shizukuService = IPrivilegedService.Stub.asInterface(service)
            _status.update { it.copy(isShizukuServiceBound = shizukuService != null) }
            recomputeActiveBackend()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shizukuService = null
            _status.update { it.copy(isShizukuServiceBound = false) }
            recomputeActiveBackend()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        _status.update { it.copy(isShizukuBinderAlive = true) }
        checkAndBindShizuku()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        shizukuService = null
        _status.update {
            it.copy(
                isShizukuBinderAlive = false,
                isShizukuServiceBound = false
            )
        }
        recomputeActiveBackend()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            _status.update { it.copy(isShizukuPermissionGranted = granted) }
            if (granted) {
                bindShizukuService()
            }
            recomputeActiveBackend()
        }

    fun initialize() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }

    fun destroy() {
        runCatching {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            if (shizukuService != null) {
                Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            }
        }
    }

    fun updatePreference(preference: PrivilegePreference) {
        currentPreference = preference
        recomputeActiveBackend()
    }

    suspend fun refreshStatus() = withContext(Dispatchers.IO) {
        val rootGranted = runCatching { Shell.getShell().isRoot }.getOrDefault(false)
        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val shizukuGranted = if (binderAlive) {
            runCatching {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
        } else {
            false
        }

        val usageStats = hasUsageStatsPermission()
        val writeSecure = context.checkSelfPermission(
            android.Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED

        _status.update {
            it.copy(
                isRootAvailable = rootGranted,
                isShizukuBinderAlive = binderAlive,
                isShizukuPermissionGranted = shizukuGranted,
                isShizukuServiceBound = shizukuService != null,
                hasUsageStatsPermission = usageStats,
                hasWriteSecureSettings = writeSecure
            )
        }

        if (binderAlive && shizukuGranted && shizukuService == null) {
            bindShizukuService()
        }
        recomputeActiveBackend()
    }

    fun requestShizukuPermission() {
        if (!Shizuku.pingBinder()) return
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            bindShizukuService()
        } else if (!Shizuku.shouldShowRequestPermissionRationale()) {
            Shizuku.requestPermission(1001)
        } else {
            Shizuku.requestPermission(1001)
        }
    }

    private fun checkAndBindShizuku() {
        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        _status.update { it.copy(isShizukuPermissionGranted = granted) }
        if (granted && shizukuService == null) {
            bindShizukuService()
        }
        recomputeActiveBackend()
    }

    private fun bindShizukuService() {
        runCatching {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        }
    }

    private fun recomputeActiveBackend() {
        val s = _status.value
        val backend = when (currentPreference) {
            PrivilegePreference.ROOT -> when {
                s.isRootAvailable -> ActiveBackend.ROOT
                s.hasWriteSecureSettings -> ActiveBackend.WRITE_SECURE
                else -> ActiveBackend.NONE
            }
            PrivilegePreference.SHIZUKU -> when {
                s.isShizukuServiceBound -> ActiveBackend.SHIZUKU
                s.hasWriteSecureSettings -> ActiveBackend.WRITE_SECURE
                else -> ActiveBackend.NONE
            }
            PrivilegePreference.AUTO -> when {
                s.isRootAvailable -> ActiveBackend.ROOT
                s.isShizukuServiceBound -> ActiveBackend.SHIZUKU
                s.hasWriteSecureSettings -> ActiveBackend.WRITE_SECURE
                else -> ActiveBackend.NONE
            }
        }
        _status.update { it.copy(activeBackend = backend) }
    }

    suspend fun setLocationEnabled(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val backend = _status.value.activeBackend
        val state = if (enabled) "true" else "false"
        val mode = if (enabled) 3 else 0

        val success = when (backend) {
            ActiveBackend.ROOT -> {
                val res = Shell.cmd("cmd location set-location-enabled $state").exec()
                if (res.isSuccess) true
                else Shell.cmd("settings put secure location_mode $mode").exec().isSuccess
            }
            ActiveBackend.SHIZUKU -> {
                runCatching { shizukuService?.setLocationEnabled(enabled) ?: false }
                    .getOrDefault(false)
            }
            ActiveBackend.WRITE_SECURE -> {
                runCatching {
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.LOCATION_MODE,
                        mode
                    )
                }.getOrDefault(false)
            }
            ActiveBackend.NONE -> false
        }
        success
    }

    fun isLocationEnabledSystem(): Boolean {
        val lm = context.getSystemService(LocationManager::class.java)
        return lm?.isLocationEnabled ?: false
    }

    suspend fun getForegroundPackagePrivileged(): String? = withContext(Dispatchers.IO) {
        when (_status.value.activeBackend) {
            ActiveBackend.ROOT -> {
                val res = Shell.cmd(
                    "dumpsys activity activities | grep -E 'topResumedActivity|mResumedActivity' | head -n 1"
                ).exec()
                if (!res.isSuccess || res.out.isEmpty()) return@withContext null
                parsePackageFromDumpsys(res.out.first())
            }
            ActiveBackend.SHIZUKU -> {
                runCatching { shizukuService?.getForegroundPackage() }.getOrNull()
            }
            else -> null
        }
    }

    suspend fun grantRequiredPermissions(): Boolean = withContext(Dispatchers.IO) {
        val pkg = context.packageName
        val ok = when (_status.value.activeBackend) {
            ActiveBackend.ROOT -> {
                val r = Shell.cmd(
                    "appops set $pkg GET_USAGE_STATS allow",
                    "pm grant $pkg android.permission.WRITE_SECURE_SETTINGS",
                    "pm grant $pkg android.permission.POST_NOTIFICATIONS"
                ).exec()
                r.isSuccess
            }
            ActiveBackend.SHIZUKU -> {
                runCatching { shizukuService?.grantRuntimePermissions(pkg) ?: false }
                    .getOrDefault(false)
            }
            else -> false
        }
        refreshStatus()
        ok
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun parsePackageFromDumpsys(line: String): String? {
        val regex = Regex("""([a-zA-Z0-9_]+(?:\.[a-zA-Z0-9_]+)+)/""")
        return regex.find(line)?.groupValues?.getOrNull(1)
    }
}

package com.locguard.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.locguard.app.service.LocationGuardService
import com.locguard.app.ui.MainViewModel
import com.locguard.app.ui.components.KaruiPivotHeader
import com.locguard.app.ui.screens.AppsScreen
import com.locguard.app.ui.screens.HomeScreen
import com.locguard.app.ui.screens.LogsScreen
import com.locguard.app.ui.screens.ThemesScreen
import com.locguard.app.ui.theme.LocalKaruiPalette
import com.locguard.app.ui.theme.LocalUiScale
import com.locguard.app.ui.theme.LocGuardTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Service already runs cleanly */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        LocationGuardService.start(this)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            LocGuardTheme(
                themeName = settings.themeName,
                uiScale = settings.uiScale
            ) {
                LocGuardRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPrivileges()
    }
}

@Composable
private fun LocGuardRoot(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val runtimeState by viewModel.runtimeState.collectAsStateWithLifecycle()
    val privilegeStatus by viewModel.privilegeStatus.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()

    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("guard", "apps", "logs", "themes")

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val headerHeight = statusTop + (104 * scale).dp
    val screenPadding = PaddingValues(top = headerHeight, bottom = navBottom)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
    ) {
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(tween(120)).togetherWith(fadeOut(tween(100)))
            },
            label = "karuiTabTransition"
        ) { tab ->
            when (tab) {
                0 -> HomeScreen(
                    settings = settings,
                    runtimeState = runtimeState,
                    privilegeStatus = privilegeStatus,
                    onToggleGuard = viewModel::setGuardEnabled,
                    onToggleAutoEnable = viewModel::setAutoEnableForWhitelisted,
                    onToggleScreenOff = viewModel::setAutoDisableOnScreenOff,
                    onScreenOffGraceChange = viewModel::setScreenOffGraceSeconds,
                    onToggleCountdownNotif = viewModel::setShowCountdownNotification,
                    onTimeoutChange = viewModel::setTimeoutSeconds,
                    onPrivilegePrefChange = viewModel::setPrivilegePreference,
                    onTurnOffNow = viewModel::turnOffLocationNow,
                    onExtendTimer = viewModel::extendTimer,
                    onRefreshPrivileges = viewModel::refreshPrivileges,
                    onRequestShizuku = viewModel::requestShizukuPermission,
                    onAutoGrantPermissions = viewModel::grantPermissionsViaPrivilege,
                    contentPadding = screenPadding
                )
                1 -> AppsScreen(
                    apps = installedApps,
                    whitelistedPackages = settings.whitelistedPackages,
                    isLoading = isLoadingApps,
                    onTogglePackage = viewModel::toggleWhitelistedPackage,
                    contentPadding = screenPadding
                )
                2 -> LogsScreen(
                    logs = logs,
                    autoDisableCount = settings.autoDisableCount,
                    onClearLogs = viewModel::clearLogs,
                    contentPadding = screenPadding
                )
                3 -> ThemesScreen(
                    currentTheme = settings.themeName,
                    currentScale = settings.uiScale,
                    onSelectTheme = viewModel::setThemeName,
                    onSelectScale = viewModel::setUiScale,
                    contentPadding = screenPadding
                )
            }
        }

        KaruiPivotHeader(
            title = "locguard",
            tabs = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            topInset = statusTop,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

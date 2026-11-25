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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.locguard.app.service.LocationGuardService
import com.locguard.app.ui.MainViewModel
import com.locguard.app.ui.components.LiquidAuroraBackground
import com.locguard.app.ui.components.LiquidGlassBottomBar
import com.locguard.app.ui.components.NavTabItem
import com.locguard.app.ui.screens.AppsScreen
import com.locguard.app.ui.screens.HomeScreen
import com.locguard.app.ui.screens.LogsScreen
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
            LocGuardTheme {
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

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val navItems = listOf(
        NavTabItem("Guard", Icons.Rounded.Shield),
        NavTabItem("Apps", Icons.Rounded.Apps),
        NavTabItem("Logs", Icons.Rounded.History)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidAuroraBackground()

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { _ ->
            val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val screenPadding = androidx.compose.foundation.layout.PaddingValues(
                top = statusTop,
                bottom = navBottom
            )

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(tween(220)).togetherWith(fadeOut(tween(180)))
                },
                label = "tabTransition"
            ) { tab ->
                when (tab) {
                    0 -> HomeScreen(
                        settings = settings,
                        runtimeState = runtimeState,
                        privilegeStatus = privilegeStatus,
                        onToggleGuard = viewModel::setGuardEnabled,
                        onToggleAutoEnable = viewModel::setAutoEnableForWhitelisted,
                        onToggleScreenOff = viewModel::setAutoDisableOnScreenOff,
                        onToggleCountdownNotif = viewModel::setShowCountdownNotification,
                        onTimeoutChange = viewModel::setTimeoutSeconds,
                        onPrivilegePrefChange = viewModel::setPrivilegePreference,
                        onTurnOffNow = viewModel::turnOffLocationNow,
                        onExtendTimer = viewModel::extendTimer,
                        onRefreshPrivileges = viewModel::refreshPrivileges,
                        onRequestShizuku = viewModel::requestShizukuPermission,
                        onAutoGrantPermissions = viewModel::grantPermissionsViaPrivilege,
                        onOpenAccessibility = viewModel::openAccessibilitySettings,
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
                }
            }
        }

        LiquidGlassBottomBar(
            items = navItems,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 14.dp,
                    start = 24.dp,
                    end = 24.dp
                )
                .widthIn(max = 420.dp)
        )
    }
}

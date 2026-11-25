package com.locguard.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.locguard.app.data.GuardSettings
import com.locguard.app.data.PrivilegePreference
import com.locguard.app.monitor.GuardPhase
import com.locguard.app.monitor.GuardRuntimeState
import com.locguard.app.privilege.ActiveBackend
import com.locguard.app.privilege.PrivilegeStatus
import com.locguard.app.ui.components.LiquidGlassCard
import com.locguard.app.ui.components.LiquidStatusOrb

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    settings: GuardSettings,
    runtimeState: GuardRuntimeState,
    privilegeStatus: PrivilegeStatus,
    onToggleGuard: (Boolean) -> Unit,
    onToggleAutoEnable: (Boolean) -> Unit,
    onToggleScreenOff: (Boolean) -> Unit,
    onToggleCountdownNotif: (Boolean) -> Unit,
    onTimeoutChange: (Int) -> Unit,
    onPrivilegePrefChange: (PrivilegePreference) -> Unit,
    onTurnOffNow: () -> Unit,
    onExtendTimer: () -> Unit,
    onRefreshPrivileges: () -> Unit,
    onRequestShizuku: () -> Unit,
    onAutoGrantPermissions: () -> Unit,
    onOpenAccessibility: () -> Unit,
    contentPadding: PaddingValues
) {
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "LocGuard",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${settings.whitelistedPackages.size} whitelisted apps • ${settings.autoDisableCount} auto-offs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Hero Liquid Status Orb Card
        item {
            LiquidGlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LiquidStatusOrb(state = runtimeState)

                    AnimatedVisibility(
                        visible = runtimeState.isLocationEnabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTurnOffNow()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GpsOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Turn Off Now")
                            }

                            if (runtimeState.phase == GuardPhase.COUNTING_DOWN) {
                                FilledTonalButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onExtendTimer()
                                    },
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+1m")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Privilege & Battery Efficiency Card
        item {
            LiquidGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privilege & Battery Engine",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    IconButton(onClick = onRefreshPrivileges) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                StatusPillRow(
                    label = "Active Backend",
                    value = privilegeStatus.activeBackend.displayName,
                    isHealthy = privilegeStatus.activeBackend != ActiveBackend.NONE
                )
                StatusPillRow(
                    label = "Root (libsu)",
                    value = if (privilegeStatus.isRootAvailable) "Granted" else "Not Detected",
                    isHealthy = privilegeStatus.isRootAvailable
                )
                StatusPillRow(
                    label = "Shizuku",
                    value = when {
                        privilegeStatus.isShizukuServiceBound -> "Bound & Ready"
                        privilegeStatus.isShizukuBinderAlive && !privilegeStatus.isShizukuPermissionGranted -> "Tap to Authorize"
                        privilegeStatus.isShizukuBinderAlive -> "Connecting..."
                        else -> "Not Running"
                    },
                    isHealthy = privilegeStatus.isShizukuServiceBound,
                    onClick = if (privilegeStatus.isShizukuBinderAlive && !privilegeStatus.isShizukuPermissionGranted) {
                        onRequestShizuku
                    } else null
                )
                StatusPillRow(
                    label = "UsageStats Access",
                    value = if (privilegeStatus.hasUsageStatsPermission) "Granted" else "Missing",
                    isHealthy = privilegeStatus.hasUsageStatsPermission
                )
                StatusPillRow(
                    label = "Zero-Poll Watcher (A11y)",
                    value = if (runtimeState.isAccessibilityActive) "Active (0% Poll)" else "Optional • Tap to Enable",
                    isHealthy = runtimeState.isAccessibilityActive,
                    onClick = onOpenAccessibility
                )

                if (!privilegeStatus.hasUsageStatsPermission && privilegeStatus.activeBackend != ActiveBackend.NONE) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAutoGrantPermissions()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Grant Permissions via ${privilegeStatus.activeBackend.displayName}")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Backend Priority",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PrivilegePreference.entries.forEach { pref ->
                        FilterChip(
                            selected = settings.privilegePreference == pref,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPrivilegePrefChange(pref)
                            },
                            label = { Text(pref.label) }
                        )
                    }
                }
            }
        }

        // Timeout & Automation Behavior Card
        item {
            LiquidGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryChargingFull,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Automation & Timeout",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-Off Delay",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = formatDuration(settings.timeoutSeconds),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = settings.timeoutSeconds.toFloat(),
                    onValueChange = { onTimeoutChange(it.toInt()) },
                    valueRange = 5f..600f,
                    steps = 0
                )

                val presets = listOf(15, 30, 60, 120, 300)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { sec ->
                        FilterChip(
                            selected = settings.timeoutSeconds == sec,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTimeoutChange(sec)
                            },
                            label = { Text(formatDuration(sec)) }
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                SettingToggleRow(
                    title = "Auto-Enable for Whitelisted Apps",
                    subtitle = "Automatically turn Location ON when opening a whitelisted app",
                    checked = settings.autoEnableForWhitelisted,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleAutoEnable(it)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Auto-Off on Screen Off",
                    subtitle = "Pause watchers immediately & close location 5s after screen locks",
                    checked = settings.autoDisableOnScreenOff,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleScreenOff(it)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Live Countdown Notification",
                    subtitle = "Show remaining seconds with quick Turn Off & +1m actions",
                    checked = settings.showCountdownNotification,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleCountdownNotif(it)
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusPillRow(
    label: String,
    value: String,
    isHealthy: Boolean,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (isHealthy) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (isHealthy) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isHealthy) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatDuration(seconds: Int): String {
    return if (seconds < 60) "${seconds}s"
    else {
        val m = seconds / 60
        val s = seconds % 60
        if (s == 0) "${m}m" else "${m}m ${s}s"
    }
}

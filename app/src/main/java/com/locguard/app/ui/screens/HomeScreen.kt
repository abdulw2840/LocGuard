package com.locguard.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locguard.app.data.GuardSettings
import com.locguard.app.data.PrivilegePreference
import com.locguard.app.monitor.GuardPhase
import com.locguard.app.monitor.GuardRuntimeState
import com.locguard.app.privilege.ActiveBackend
import com.locguard.app.privilege.PrivilegeStatus
import com.locguard.app.ui.components.KaruiBox
import com.locguard.app.ui.components.KaruiButton
import com.locguard.app.ui.components.KaruiChip
import com.locguard.app.ui.components.KaruiDashedDivider
import com.locguard.app.ui.components.KaruiStatusHero
import com.locguard.app.ui.components.KaruiSubtleButton
import com.locguard.app.ui.components.KaruiSwitch
import com.locguard.app.ui.theme.LocalKaruiPalette
import com.locguard.app.ui.theme.LocalUiScale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    settings: GuardSettings,
    runtimeState: GuardRuntimeState,
    privilegeStatus: PrivilegeStatus,
    onToggleGuard: (Boolean) -> Unit,
    onToggleAutoEnable: (Boolean) -> Unit,
    onToggleScreenOff: (Boolean) -> Unit,
    onScreenOffGraceChange: (Int) -> Unit,
    onToggleCountdownNotif: (Boolean) -> Unit,
    onTimeoutChange: (Int) -> Unit,
    onPrivilegePrefChange: (PrivilegePreference) -> Unit,
    onTurnOffNow: () -> Unit,
    onExtendTimer: () -> Unit,
    onRefreshPrivileges: () -> Unit,
    onRequestShizuku: () -> Unit,
    onAutoGrantPermissions: () -> Unit,
    contentPadding: PaddingValues
) {
    val haptic = LocalHapticFeedback.current
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "${settings.whitelistedPackages.size} whitelisted apps • ${settings.autoDisableCount} auto-offs",
                color = palette.gray,
                fontFamily = FontFamily.Monospace,
                fontSize = (12 * scale).sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        // Status Box
        item {
            KaruiBox(title = "Guard Status") {
                KaruiStatusHero(state = runtimeState)

                Spacer(modifier = Modifier.height(12.dp))
                KaruiDashedDivider()
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Enable Location Guard",
                    subtitle = "Monitor foreground apps and auto-shutoff GPS",
                    checked = settings.guardEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleGuard(it)
                    }
                )

                AnimatedVisibility(
                    visible = runtimeState.isLocationEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KaruiButton(
                            text = "Turn Off Now",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTurnOffNow()
                            },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )

                        if (runtimeState.phase == GuardPhase.COUNTING_DOWN) {
                            KaruiSubtleButton(
                                text = "+1m",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onExtendTimer()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Privilege & Battery Engine Box
        item {
            KaruiBox(title = "Privilege & Battery Engine") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Backend Diagnostics",
                        color = palette.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = (14 * scale).sp
                    )
                    KaruiSubtleButton(
                        text = "Refresh",
                        onClick = onRefreshPrivileges
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                StatusRow(
                    label = "Active Backend",
                    value = privilegeStatus.activeBackend.displayName,
                    isHealthy = privilegeStatus.activeBackend != ActiveBackend.NONE
                )
                StatusRow(
                    label = "Root (KSU / Magisk)",
                    value = if (privilegeStatus.isRootAvailable) "GRANTED" else "NONE",
                    isHealthy = privilegeStatus.isRootAvailable
                )
                StatusRow(
                    label = "Shizuku",
                    value = when {
                        privilegeStatus.isShizukuServiceBound -> "READY"
                        privilegeStatus.isShizukuBinderAlive && !privilegeStatus.isShizukuPermissionGranted -> "AUTHORIZE"
                        privilegeStatus.isShizukuBinderAlive -> "CONNECTING"
                        else -> "OFFLINE"
                    },
                    isHealthy = privilegeStatus.isShizukuServiceBound,
                    onClick = if (privilegeStatus.isShizukuBinderAlive && !privilegeStatus.isShizukuPermissionGranted) {
                        onRequestShizuku
                    } else null
                )
                StatusRow(
                    label = "UsageStats Access",
                    value = if (privilegeStatus.hasUsageStatsPermission) "GRANTED" else "MISSING",
                    isHealthy = privilegeStatus.hasUsageStatsPermission
                )

                if (!privilegeStatus.hasUsageStatsPermission && privilegeStatus.activeBackend != ActiveBackend.NONE) {
                    Spacer(modifier = Modifier.height(10.dp))
                    KaruiButton(
                        text = "Auto-Grant via ${privilegeStatus.activeBackend.displayName}",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAutoGrantPermissions()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                KaruiDashedDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "PRIVILEGE METHOD",
                    color = palette.gray,
                    fontSize = (11 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PrivilegePreference.entries.forEach { pref ->
                        KaruiChip(
                            label = pref.label,
                            selected = settings.privilegePreference == pref,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPrivilegePrefChange(pref)
                            }
                        )
                    }
                }
            }
        }

        // Timeout & Automation Behavior Box
        item {
            KaruiBox(title = "Automation & Timeout") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-Off Delay",
                        color = palette.text,
                        fontWeight = FontWeight.Medium,
                        fontSize = (15 * scale).sp
                    )
                    Text(
                        text = formatDuration(settings.timeoutSeconds),
                        color = palette.accent,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = (16 * scale).sp
                    )
                }

                Slider(
                    value = settings.timeoutSeconds.toFloat(),
                    onValueChange = { onTimeoutChange(it.toInt()) },
                    valueRange = 5f..600f,
                    colors = SliderDefaults.colors(
                        thumbColor = palette.accent,
                        activeTrackColor = palette.accent,
                        inactiveTrackColor = palette.surface
                    )
                )

                val presets = listOf(15, 30, 60, 120, 300)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { sec ->
                        KaruiChip(
                            label = formatDuration(sec),
                            selected = settings.timeoutSeconds == sec,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTimeoutChange(sec)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                KaruiDashedDivider()
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Auto-Enable for Whitelisted Apps",
                    subtitle = "Turn Location ON when opening a whitelisted app",
                    checked = settings.autoEnableForWhitelisted,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleAutoEnable(it)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
                KaruiDashedDivider()
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Auto-Off on Screen Off",
                    subtitle = "Pause normal timer & close location after lock",
                    checked = settings.autoDisableOnScreenOff,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleScreenOff(it)
                    }
                )

                AnimatedVisibility(
                    visible = settings.autoDisableOnScreenOff,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Screen-Off Delay",
                                color = palette.text,
                                fontWeight = FontWeight.Medium,
                                fontSize = (14 * scale).sp
                            )
                            Text(
                                text = formatDuration(settings.screenOffGraceSeconds),
                                color = palette.accent,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = (14 * scale).sp
                            )
                        }
                        Slider(
                            value = settings.screenOffGraceSeconds.toFloat(),
                            onValueChange = { onScreenOffGraceChange(it.toInt()) },
                            valueRange = 5f..120f,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.accent,
                                activeTrackColor = palette.accent,
                                inactiveTrackColor = palette.surface
                            )
                        )
                        val screenPresets = listOf(5, 15, 30, 60)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            screenPresets.forEach { sec ->
                                KaruiChip(
                                    label = formatDuration(sec),
                                    selected = settings.screenOffGraceSeconds == sec,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onScreenOffGraceChange(sec)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                KaruiDashedDivider()
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Live Countdown Notification",
                    subtitle = "Show remaining seconds with Turn Off & +1m",
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
private fun StatusRow(
    label: String,
    value: String,
    isHealthy: Boolean,
    onClick: (() -> Unit)? = null
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.text.copy(alpha = 0.85f),
            fontSize = (13 * scale).sp
        )
        Box(
            modifier = Modifier
                .background(if (isHealthy) palette.accent else palette.surface)
                .border(1.dp, palette.border, RectangleShape)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = value,
                color = if (isHealthy) palette.accentTxt else palette.gray,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = (11 * scale).sp
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
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = palette.text,
                fontWeight = FontWeight.Medium,
                fontSize = (14 * scale).sp
            )
            Text(
                text = subtitle,
                color = palette.gray,
                fontSize = (12 * scale).sp
            )
        }
        KaruiSwitch(checked = checked, onCheckedChange = onCheckedChange)
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

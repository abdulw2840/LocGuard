package com.locguard.app.monitor

import android.content.Context
import android.os.PowerManager
import com.locguard.app.data.GuardLogRepository
import com.locguard.app.data.GuardPreferences
import com.locguard.app.data.GuardSettings
import com.locguard.app.data.LogCategory
import com.locguard.app.privilege.PrivilegeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class GuardPhase {
    IDLE_LOCATION_OFF,
    PROTECTED_WHITELISTED,
    COUNTING_DOWN,
    SCREEN_OFF_GRACE,
    PAUSED
}

data class GuardRuntimeState(
    val isLocationEnabled: Boolean = false,
    val isScreenOn: Boolean = true,
    val currentForegroundApp: String? = null,
    val phase: GuardPhase = GuardPhase.IDLE_LOCATION_OFF,
    val remainingSeconds: Int = 0,
    val totalCountdownSeconds: Int = 60,
    val isAccessibilityActive: Boolean = false
)

class LocationGuardEngine(
    private val context: Context,
    private val preferences: GuardPreferences,
    private val privilegeManager: PrivilegeManager,
    private val foregroundDetector: ForegroundAppDetector,
    private val logRepository: GuardLogRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(GuardRuntimeState())
    val state: StateFlow<GuardRuntimeState> = _state.asStateFlow()

    @Volatile
    private var currentSettings = GuardSettings()

    @Volatile
    private var isStarted = false

    private var countdownJob: Job? = null
    private var adaptiveWatcherJob: Job? = null
    private var screenOffJob: Job? = null
    private var reEvaluateJob: Job? = null

    @Volatile
    private var pausedRemainingSeconds: Int? = null
    @Volatile
    private var pausedTotalSeconds: Int? = null

    fun start() {
        if (isStarted) return
        isStarted = true

        val pm = context.getSystemService(PowerManager::class.java)
        val screenOn = pm?.isInteractive ?: true
        val locOn = privilegeManager.isLocationEnabledSystem()
        val a11y = foregroundDetector.isAccessibilityServiceEnabled()

        _state.update {
            it.copy(
                isScreenOn = screenOn,
                isLocationEnabled = locOn,
                isAccessibilityActive = a11y
            )
        }

        scope.launch {
            preferences.settingsFlow.collectLatest { settings ->
                val prev = currentSettings
                currentSettings = settings
                privilegeManager.updatePreference(settings.privilegePreference)

                if (!settings.guardEnabled) {
                    stopAllTimers()
                    _state.update {
                        it.copy(phase = GuardPhase.PAUSED, remainingSeconds = 0)
                    }
                } else {
                    if (!prev.guardEnabled || prev.timeoutSeconds != settings.timeoutSeconds) {
                        reEvaluateState(reason = "Settings updated")
                    } else {
                        updateAdaptiveWatcher()
                    }
                }
            }
        }

        scope.launch {
            foregroundDetector.accessibilityPackage.collectLatest { pkg ->
                if (pkg != null && isStarted) {
                    onForegroundPackageObserved(pkg)
                }
            }
        }

        logRepository.add(
            LogCategory.INFO,
            "LocGuard Engine Active",
            "Battery-optimized event-driven guard initialized"
        )
        reEvaluateState(reason = "Engine start")
    }

    fun onLocationModeChanged(enabled: Boolean) {
        val wasEnabled = _state.value.isLocationEnabled
        _state.update { it.copy(isLocationEnabled = enabled) }
        if (!enabled) {
            stopAllTimers()
            _state.update {
                it.copy(
                    phase = GuardPhase.IDLE_LOCATION_OFF,
                    remainingSeconds = 0
                )
            }
        } else if (!wasEnabled) {
            // Fresh Location ON transition: guarantee countdown resets to full default timeout
            countdownJob?.cancel()
            countdownJob = null
        }
        if (wasEnabled != enabled) {
            logRepository.add(
                LogCategory.INFO,
                if (enabled) "Location Turned ON" else "Location Turned OFF",
                "System location state changed"
            )
        }
        reEvaluateState(reason = "Location mode changed")
    }

    fun onScreenStateChanged(screenOn: Boolean) {
        _state.update {
            it.copy(
                isScreenOn = screenOn,
                isAccessibilityActive = foregroundDetector.isAccessibilityServiceEnabled()
            )
        }

        if (!screenOn) {
            // Zero-wakeup battery optimization: immediately halt foreground polling when screen is off
            adaptiveWatcherJob?.cancel()
            adaptiveWatcherJob = null

            val settings = currentSettings
            if (settings.guardEnabled && _state.value.isLocationEnabled) {
                if (_state.value.phase == GuardPhase.COUNTING_DOWN && _state.value.remainingSeconds > 0) {
                    pausedRemainingSeconds = _state.value.remainingSeconds
                    pausedTotalSeconds = _state.value.totalCountdownSeconds
                    countdownJob?.cancel()
                    countdownJob = null
                }
                if (settings.autoDisableOnScreenOff) {
                    scheduleScreenOffDisable(settings.screenOffGraceSeconds)
                }
            }
        } else {
            screenOffJob?.cancel()
            screenOffJob = null
            reEvaluateState(reason = "Screen turned on")
        }
    }

    fun turnOffNow() {
        stopAllTimers()
        _state.update {
            it.copy(
                phase = GuardPhase.IDLE_LOCATION_OFF,
                remainingSeconds = 0
            )
        }
        scope.launch {
            val ok = privilegeManager.setLocationEnabled(false)
            stopAllTimers()
            if (ok) {
                preferences.recordAutoDisable()
                logRepository.add(
                    LogCategory.AUTO_DISABLE,
                    "Location Disabled Manually",
                    "Instant shutoff triggered via LocGuard"
                )
            }
            _state.update {
                it.copy(
                    isLocationEnabled = privilegeManager.isLocationEnabledSystem(),
                    phase = GuardPhase.IDLE_LOCATION_OFF,
                    remainingSeconds = 0
                )
            }
        }
    }

    fun extendTimer(extraSeconds: Int = 60) {
        if (!_state.value.isLocationEnabled || !currentSettings.guardEnabled) return
        val newRemaining = (_state.value.remainingSeconds + extraSeconds).coerceAtMost(3600)
        startCountdown(newRemaining, newRemaining.coerceAtLeast(currentSettings.timeoutSeconds))
        logRepository.add(
            LogCategory.TIMER,
            "Timer Extended (+${extraSeconds}s)",
            "Countdown adjusted to ${newRemaining}s"
        )
    }

    private fun reEvaluateState(reason: String) {
        val settings = currentSettings
        if (!settings.guardEnabled) {
            stopAllTimers()
            _state.update { it.copy(phase = GuardPhase.PAUSED, remainingSeconds = 0) }
            return
        }

        val locEnabled = privilegeManager.isLocationEnabledSystem()
        val a11yActive = foregroundDetector.isAccessibilityServiceEnabled()
        _state.update {
            it.copy(
                isLocationEnabled = locEnabled,
                isAccessibilityActive = a11yActive
            )
        }

        if (!locEnabled) {
            countdownJob?.cancel()
            countdownJob = null
            _state.update {
                it.copy(
                    phase = GuardPhase.IDLE_LOCATION_OFF,
                    remainingSeconds = 0
                )
            }
            updateAdaptiveWatcher()
            return
        }

        reEvaluateJob?.cancel()
        reEvaluateJob = scope.launch {
            val fgPkg = foregroundDetector.getCurrentForegroundPackage()
            _state.update { it.copy(currentForegroundApp = fgPkg) }

            val isWhitelisted = fgPkg != null && settings.whitelistedPackages.contains(fgPkg)
            if (isWhitelisted) {
                pausedRemainingSeconds = null
                pausedTotalSeconds = null
                countdownJob?.cancel()
                countdownJob = null
                _state.update {
                    it.copy(
                        phase = GuardPhase.PROTECTED_WHITELISTED,
                        remainingSeconds = 0
                    )
                }
            } else {
                val resumeSec = pausedRemainingSeconds
                val resumeTotal = pausedTotalSeconds ?: settings.timeoutSeconds
                pausedRemainingSeconds = null
                pausedTotalSeconds = null
                if (resumeSec != null && resumeSec > 0) {
                    startCountdown(resumeSec, resumeTotal)
                } else if (countdownJob?.isActive != true) {
                    startCountdown(settings.timeoutSeconds, settings.timeoutSeconds)
                }
            }
            updateAdaptiveWatcher()
        }
    }

    private fun onForegroundPackageObserved(pkg: String) {
        val prevPkg = _state.value.currentForegroundApp
        if (pkg == prevPkg) return
        _state.update { it.copy(currentForegroundApp = pkg) }

        val settings = currentSettings
        if (!settings.guardEnabled) return

        val isWhitelisted = settings.whitelistedPackages.contains(pkg)
        val locEnabled = _state.value.isLocationEnabled

        if (isWhitelisted) {
            countdownJob?.cancel()
            countdownJob = null
            screenOffJob?.cancel()
            screenOffJob = null

            if (!locEnabled && settings.autoEnableForWhitelisted) {
                scope.launch {
                    val ok = privilegeManager.setLocationEnabled(true)
                    if (ok) {
                        logRepository.add(
                            LogCategory.AUTO_ENABLE,
                            "Location Auto-Enabled",
                            "Whitelisted app opened: $pkg"
                        )
                        _state.update {
                            it.copy(
                                isLocationEnabled = true,
                                phase = GuardPhase.PROTECTED_WHITELISTED,
                                remainingSeconds = 0
                            )
                        }
                    }
                }
            } else if (locEnabled) {
                _state.update {
                    it.copy(
                        phase = GuardPhase.PROTECTED_WHITELISTED,
                        remainingSeconds = 0
                    )
                }
            }
        } else if (locEnabled && countdownJob?.isActive != true) {
            startCountdown(settings.timeoutSeconds, settings.timeoutSeconds)
        }
    }

    private fun startCountdown(seconds: Int, total: Int) {
        countdownJob?.cancel()
        _state.update {
            it.copy(
                phase = GuardPhase.COUNTING_DOWN,
                remainingSeconds = seconds,
                totalCountdownSeconds = total
            )
        }

        countdownJob = scope.launch {
            var remaining = seconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _state.update { it.copy(remainingSeconds = remaining) }
            }

            if (!isActive || !currentSettings.guardEnabled) return@launch

            // Final verification before shutting off location
            val fgPkg = foregroundDetector.getCurrentForegroundPackage()
            if (fgPkg != null && currentSettings.whitelistedPackages.contains(fgPkg)) {
                _state.update {
                    it.copy(
                        currentForegroundApp = fgPkg,
                        phase = GuardPhase.PROTECTED_WHITELISTED,
                        remainingSeconds = 0
                    )
                }
                return@launch
            }

            val disabled = privilegeManager.setLocationEnabled(false)
            if (disabled) {
                preferences.recordAutoDisable()
                logRepository.add(
                    LogCategory.AUTO_DISABLE,
                    "Location Auto-Disabled",
                    "Timeout (${total}s) expired with no whitelisted app active"
                )
                _state.update {
                    it.copy(
                        isLocationEnabled = false,
                        phase = GuardPhase.IDLE_LOCATION_OFF,
                        remainingSeconds = 0
                    )
                }
                updateAdaptiveWatcher()
            }
        }
    }

    private fun scheduleScreenOffDisable(graceSeconds: Int) {
        screenOffJob?.cancel()
        _state.update {
            it.copy(
                phase = GuardPhase.SCREEN_OFF_GRACE,
                remainingSeconds = graceSeconds,
                totalCountdownSeconds = graceSeconds.coerceAtLeast(1)
            )
        }
        screenOffJob = scope.launch {
            var remaining = graceSeconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                if (!_state.value.isScreenOn) {
                    _state.update { it.copy(remainingSeconds = remaining) }
                }
            }
            if (!isActive || _state.value.isScreenOn || !currentSettings.guardEnabled) return@launch
            pausedRemainingSeconds = null
            pausedTotalSeconds = null
            if (privilegeManager.isLocationEnabledSystem()) {
                val ok = privilegeManager.setLocationEnabled(false)
                if (ok) {
                    preferences.recordAutoDisable()
                    logRepository.add(
                        LogCategory.POWER,
                        "Screen-Off Location Shutoff",
                        "Disabled location ${graceSeconds}s after screen turned off"
                    )
                }
            }
            _state.update {
                it.copy(
                    isLocationEnabled = false,
                    phase = GuardPhase.IDLE_LOCATION_OFF,
                    remainingSeconds = 0
                )
            }
        }
    }

    /**
     * Runs ONLY while screen is ON AND either:
     * - Location is currently ON (to detect entering/leaving whitelisted apps), OR
     * - Auto-Enable is ON with non-empty whitelist and AccessibilityService is NOT active.
     * Completely zero wakeups when screen is OFF or when Location is OFF & Auto-Enable is OFF.
     */
    private fun updateAdaptiveWatcher() {
        val state = _state.value
        val settings = currentSettings
        val a11yActive = foregroundDetector.isAccessibilityServiceEnabled()

        val needsPolling = settings.guardEnabled &&
            state.isScreenOn &&
            !a11yActive &&
            (state.isLocationEnabled || (settings.autoEnableForWhitelisted && settings.whitelistedPackages.isNotEmpty()))

        if (!needsPolling) {
            adaptiveWatcherJob?.cancel()
            adaptiveWatcherJob = null
            return
        }

        if (adaptiveWatcherJob?.isActive == true) return

        adaptiveWatcherJob = scope.launch {
            while (isActive) {
                val pkg = foregroundDetector.getCurrentForegroundPackage()
                if (pkg != null) {
                    onForegroundPackageObserved(pkg)
                }
                val intervalMs = if (_state.value.isLocationEnabled) 3000L else 4000L
                delay(intervalMs)
            }
        }
    }

    private fun stopAllTimers() {
        pausedRemainingSeconds = null
        pausedTotalSeconds = null
        reEvaluateJob?.cancel()
        reEvaluateJob = null
        countdownJob?.cancel()
        countdownJob = null
        screenOffJob?.cancel()
        screenOffJob = null
        adaptiveWatcherJob?.cancel()
        adaptiveWatcherJob = null
    }
}

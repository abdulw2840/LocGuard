package com.locguard.app

import android.app.Application
import com.locguard.app.data.GuardLogRepository
import com.locguard.app.data.GuardPreferences
import com.locguard.app.monitor.ForegroundAppDetector
import com.locguard.app.monitor.LocationGuardEngine
import com.locguard.app.privilege.PrivilegeManager
import com.topjohnwu.superuser.Shell

class LocGuardApp : Application() {

    lateinit var preferences: GuardPreferences
        private set
    lateinit var logRepository: GuardLogRepository
        private set
    lateinit var privilegeManager: PrivilegeManager
        private set
    lateinit var foregroundDetector: ForegroundAppDetector
        private set
    lateinit var guardEngine: LocationGuardEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setTimeout(10)
        )

        preferences = GuardPreferences(this)
        logRepository = GuardLogRepository()
        privilegeManager = PrivilegeManager(this).apply { initialize() }
        foregroundDetector = ForegroundAppDetector(this, privilegeManager)
        guardEngine = LocationGuardEngine(
            context = this,
            preferences = preferences,
            privilegeManager = privilegeManager,
            foregroundDetector = foregroundDetector,
            logRepository = logRepository
        )
    }

    companion object {
        lateinit var instance: LocGuardApp
            private set
    }
}

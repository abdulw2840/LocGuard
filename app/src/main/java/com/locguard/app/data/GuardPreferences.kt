package com.locguard.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "locguard_prefs")

enum class PrivilegePreference(val label: String) {
    ROOT("Root (KSU / Magisk)"),
    SHIZUKU("Shizuku")
}

data class GuardSettings(
    val guardEnabled: Boolean = true,
    val autoEnableForWhitelisted: Boolean = true,
    val autoDisableOnScreenOff: Boolean = true,
    val screenOffGraceSeconds: Int = 5,
    val timeoutSeconds: Int = 60,
    val privilegePreference: PrivilegePreference = PrivilegePreference.ROOT,
    val showCountdownNotification: Boolean = true,
    val whitelistedPackages: Set<String> = emptySet(),
    val autoDisableCount: Int = 0,
    val lastDisabledTimestamp: Long = 0L,
    val themeName: String = "gruvbox",
    val uiScale: Float = 1.0f
)

class GuardPreferences(private val context: Context) {

    private object Keys {
        val GUARD_ENABLED = booleanPreferencesKey("guard_enabled")
        val AUTO_ENABLE_WHITELISTED = booleanPreferencesKey("auto_enable_whitelisted")
        val AUTO_DISABLE_SCREEN_OFF = booleanPreferencesKey("auto_disable_screen_off")
        val SCREEN_OFF_GRACE_SECONDS = intPreferencesKey("screen_off_grace_seconds")
        val TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
        val PRIVILEGE_PREF = stringPreferencesKey("privilege_pref")
        val SHOW_COUNTDOWN_NOTIF = booleanPreferencesKey("show_countdown_notif")
        val WHITELISTED_PACKAGES = stringSetPreferencesKey("whitelisted_packages")
        val AUTO_DISABLE_COUNT = intPreferencesKey("auto_disable_count")
        val LAST_DISABLED_TS = longPreferencesKey("last_disabled_ts")
        val THEME_NAME = stringPreferencesKey("theme_name")
        val UI_SCALE = floatPreferencesKey("ui_scale")
    }

    val settingsFlow: Flow<GuardSettings> = context.dataStore.data.map { prefs ->
        val prefName = prefs[Keys.PRIVILEGE_PREF] ?: PrivilegePreference.ROOT.name
        val privilegePref = runCatching { PrivilegePreference.valueOf(prefName) }
            .getOrDefault(PrivilegePreference.ROOT)

        GuardSettings(
            guardEnabled = prefs[Keys.GUARD_ENABLED] ?: true,
            autoEnableForWhitelisted = prefs[Keys.AUTO_ENABLE_WHITELISTED] ?: true,
            autoDisableOnScreenOff = prefs[Keys.AUTO_DISABLE_SCREEN_OFF] ?: true,
            screenOffGraceSeconds = (prefs[Keys.SCREEN_OFF_GRACE_SECONDS] ?: 5).coerceIn(5, 300),
            timeoutSeconds = (prefs[Keys.TIMEOUT_SECONDS] ?: 60).coerceIn(5, 1800),
            privilegePreference = privilegePref,
            showCountdownNotification = prefs[Keys.SHOW_COUNTDOWN_NOTIF] ?: true,
            whitelistedPackages = prefs[Keys.WHITELISTED_PACKAGES] ?: emptySet(),
            autoDisableCount = prefs[Keys.AUTO_DISABLE_COUNT] ?: 0,
            lastDisabledTimestamp = prefs[Keys.LAST_DISABLED_TS] ?: 0L,
            themeName = prefs[Keys.THEME_NAME] ?: "gruvbox",
            uiScale = (prefs[Keys.UI_SCALE] ?: 1.0f).coerceIn(0.85f, 1.3f)
        )
    }

    suspend fun setGuardEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GUARD_ENABLED] = enabled }
    }

    suspend fun setAutoEnableForWhitelisted(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_ENABLE_WHITELISTED] = enabled }
    }

    suspend fun setAutoDisableOnScreenOff(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_DISABLE_SCREEN_OFF] = enabled }
    }

    suspend fun setScreenOffGraceSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.SCREEN_OFF_GRACE_SECONDS] = seconds.coerceIn(5, 300) }
    }

    suspend fun setTimeoutSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.TIMEOUT_SECONDS] = seconds.coerceIn(5, 1800) }
    }

    suspend fun setPrivilegePreference(pref: PrivilegePreference) {
        context.dataStore.edit { it[Keys.PRIVILEGE_PREF] = pref.name }
    }

    suspend fun setShowCountdownNotification(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_COUNTDOWN_NOTIF] = show }
    }

    suspend fun toggleWhitelistedPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.WHITELISTED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (!current.add(packageName)) {
                current.remove(packageName)
            }
            prefs[Keys.WHITELISTED_PACKAGES] = current
        }
    }

    suspend fun recordAutoDisable() {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_DISABLE_COUNT] = (prefs[Keys.AUTO_DISABLE_COUNT] ?: 0) + 1
            prefs[Keys.LAST_DISABLED_TS] = System.currentTimeMillis()
        }
    }

    suspend fun setThemeName(themeName: String) {
        context.dataStore.edit { it[Keys.THEME_NAME] = themeName }
    }

    suspend fun setUiScale(scale: Float) {
        context.dataStore.edit { it[Keys.UI_SCALE] = scale.coerceIn(0.85f, 1.3f) }
    }
}

package com.locguard.app.privilege

import com.locguard.app.IPrivilegedService
import kotlin.system.exitProcess

class ShizukuUserService : IPrivilegedService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun setLocationEnabled(enabled: Boolean): Boolean {
        val state = if (enabled) "true" else "false"
        val mode = if (enabled) "3" else "0"
        val result = exec("cmd location set-location-enabled $state")
        if (result.exitCode == 0) {
            return true
        }
        return exec("settings put secure location_mode $mode").exitCode == 0
    }

    override fun isLocationEnabled(): Boolean {
        val cmdRes = exec("cmd location is-location-enabled")
        if (cmdRes.exitCode == 0) {
            val out = cmdRes.output.trim()
            if (out == "true" || out == "false") {
                return out == "true"
            }
        }
        val modeRes = exec("settings get secure location_mode")
        return modeRes.output.trim().toIntOrNull()?.let { it != 0 } ?: false
    }

    override fun getForegroundPackage(): String? {
        val res = exec("dumpsys activity activities | grep -E 'topResumedActivity|mResumedActivity' | head -n 1")
        if (res.exitCode != 0 || res.output.isBlank()) return null
        return parsePackageFromDumpsys(res.output)
    }

    override fun grantRuntimePermissions(packageName: String): Boolean {
        val r1 = exec("appops set $packageName GET_USAGE_STATS allow")
        val r2 = exec("pm grant $packageName android.permission.WRITE_SECURE_SETTINGS")
        exec("pm grant $packageName android.permission.POST_NOTIFICATIONS")
        return r1.exitCode == 0 || r2.exitCode == 0
    }

    override fun runCommand(command: String): String {
        return exec(command).output
    }

    private data class ExecResult(val exitCode: Int, val output: String)

    private fun exec(cmd: String): ExecResult {
        return try {
            val process = ProcessBuilder("sh", "-c", cmd)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val code = process.waitFor()
            ExecResult(code, output)
        } catch (t: Throwable) {
            ExecResult(-1, t.message ?: "error")
        }
    }

    private fun parsePackageFromDumpsys(line: String): String? {
        val regex = Regex("""([a-zA-Z0-9_]+(?:\.[a-zA-Z0-9_]+)+)/""")
        return regex.find(line)?.groupValues?.getOrNull(1)
    }
}

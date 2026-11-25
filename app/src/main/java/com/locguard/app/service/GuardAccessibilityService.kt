package com.locguard.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.locguard.app.LocGuardApp

class GuardAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        LocGuardApp.instance.foregroundDetector.onAccessibilityWindowChanged(pkg)
    }

    override fun onInterrupt() {
        // No-op
    }
}

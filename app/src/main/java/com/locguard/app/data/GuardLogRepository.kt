package com.locguard.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class LogCategory {
    AUTO_DISABLE,
    AUTO_ENABLE,
    TIMER,
    POWER,
    PRIVILEGE,
    INFO
}

data class GuardLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val category: LogCategory,
    val title: String,
    val detail: String
)

class GuardLogRepository {
    private val _logs = MutableStateFlow<List<GuardLogEntry>>(emptyList())
    val logs: StateFlow<List<GuardLogEntry>> = _logs.asStateFlow()

    fun add(category: LogCategory, title: String, detail: String) {
        val entry = GuardLogEntry(
            category = category,
            title = title,
            detail = detail
        )
        _logs.update { current ->
            (listOf(entry) + current).take(120)
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}

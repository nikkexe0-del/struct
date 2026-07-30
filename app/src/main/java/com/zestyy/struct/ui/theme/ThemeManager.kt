package com.zestyy.struct.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode { DARK, LIGHT, GREEN }

/**
 * Simple SharedPreferences-backed theme choice, process-wide via StateFlow so any screen can
 * both read the current theme and change it (e.g. a settings sheet) and have it apply instantly
 * everywhere without threading state through the nav graph.
 */
object ThemeManager {
    private const val PREFS = "struct_prefs"
    private const val KEY_THEME = "theme_mode"

    private val _mode = MutableStateFlow(AppThemeMode.DARK)
    val mode: StateFlow<AppThemeMode> = _mode.asStateFlow()

    fun init(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_THEME, null)
        _mode.value = saved?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() } ?: AppThemeMode.DARK
    }

    fun setMode(context: Context, mode: AppThemeMode) {
        _mode.value = mode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, mode.name).apply()
    }
}

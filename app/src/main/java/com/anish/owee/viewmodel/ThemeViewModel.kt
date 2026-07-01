package com.anish.owee.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.anish.owee.data.local.PreferenceManager
import com.anish.owee.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferenceManager = PreferenceManager(application)
    
    private val _themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(preferenceManager.getThemeMode())
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        preferenceManager.saveThemeMode(mode.name)
    }
}

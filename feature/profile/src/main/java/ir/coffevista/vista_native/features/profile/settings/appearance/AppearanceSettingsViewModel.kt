package ir.coffevista.vista_native.features.profile.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.datastore.SettingsPreferenceStore
import ir.coffevista.vista_native.core.datastore.SettingsPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(
    private val store: SettingsPreferenceStore,
) : ViewModel() {
    val preferences: StateFlow<SettingsPreferences> = store.preferences.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsPreferences(),
    )

    fun setThemeMode(value: String) = viewModelScope.launch {
        store.update { it.copy(themeMode = value) }
    }

    fun setReduceMotion(value: Boolean) = viewModelScope.launch {
        store.update { it.copy(reduceMotion = value) }
    }

    fun setChatEntryMode(value: String) = viewModelScope.launch {
        if (value !in setOf("adaptive", "minimal", "off")) return@launch
        store.update { it.copy(chatEntryMode = value) }
    }

    fun setEmojiStyle(value: String) = viewModelScope.launch {
        if (value !in setOf("custom", "system")) return@launch
        store.update { it.copy(emojiStyle = value) }
    }

    fun setLocale(value: String) = viewModelScope.launch {
        if (value !in setOf("fa", "en", "ar")) return@launch
        store.update { it.copy(locale = value) }
    }

    fun setAutoplayMedia(value: Boolean) = viewModelScope.launch {
        store.update { it.copy(autoplayMedia = value) }
    }

    fun setMediaQuality(value: String) = viewModelScope.launch {
        store.update { it.copy(mediaQuality = value) }
    }

    fun setVideoDataSaver(value: Boolean) = viewModelScope.launch {
        store.update { it.copy(videoDataSaver = value) }
    }
}

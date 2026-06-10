package com.omnimiko.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omnimiko.data.settings.AppSettings
import com.omnimiko.data.settings.SettingsRepository
import com.omnimiko.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    fun setTemperature(value: Float) = viewModelScope.launch { repository.setTemperature(value) }
    fun setMaxIterations(value: Int) = viewModelScope.launch { repository.setMaxIterations(value) }
    fun setAutoApprove(value: Boolean) = viewModelScope.launch { repository.setAutoApproveTools(value) }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(container.settingsRepository) as T
    }
}

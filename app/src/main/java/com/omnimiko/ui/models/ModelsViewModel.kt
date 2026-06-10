package com.omnimiko.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omnimiko.di.AppContainer
import com.omnimiko.llm.model.CatalogModel
import com.omnimiko.llm.model.DownloadState
import com.omnimiko.llm.model.LocalModel
import com.omnimiko.llm.model.ModelCatalog
import com.omnimiko.llm.model.ModelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelsUiState(
    val catalog: List<CatalogModel> = ModelCatalog.defaults,
    val installed: List<LocalModel> = emptyList(),
    val activeModelId: String? = null,
    val downloads: Map<String, DownloadState> = emptyMap(),
    val busy: Boolean = false,
)

class ModelsViewModel(private val modelManager: ModelManager) : ViewModel() {

    private val _state = MutableStateFlow(ModelsUiState())
    val state: StateFlow<ModelsUiState> = _state.asStateFlow()

    init {
        refreshInstalled()
        viewModelScope.launch {
            modelManager.activeModel.collect { active ->
                _state.update { it.copy(activeModelId = active?.id) }
            }
        }
    }

    fun refreshInstalled() {
        _state.update { it.copy(installed = modelManager.installedModels()) }
    }

    fun download(model: CatalogModel) {
        viewModelScope.launch {
            modelManager.download(model).collect { progress ->
                _state.update { it.copy(downloads = it.downloads + (model.id to progress)) }
                if (progress is DownloadState.Completed) refreshInstalled()
            }
        }
    }

    fun activate(model: LocalModel) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            runCatching { modelManager.activate(model) }
            _state.update { it.copy(busy = false) }
        }
    }

    fun deactivate() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            modelManager.deactivate()
            _state.update { it.copy(busy = false) }
        }
    }

    fun delete(model: LocalModel) {
        if (modelManager.delete(model)) refreshInstalled()
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ModelsViewModel(container.modelManager) as T
    }
}

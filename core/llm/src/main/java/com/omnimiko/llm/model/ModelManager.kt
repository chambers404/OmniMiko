package com.omnimiko.llm.model

import com.omnimiko.llm.engine.EngineOptions
import com.omnimiko.llm.engine.InferenceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Owns the lifecycle of on-device models: discovery of downloaded files, loading
 * the active model into the [InferenceEngine], and exposing the current state to
 * the UI. One model is "active" at a time to keep memory pressure manageable.
 */
class ModelManager(
    private val modelsDir: File,
    private val engine: InferenceEngine,
    private val downloader: ModelDownloader,
) {
    private val _activeModel = MutableStateFlow<LocalModel?>(null)
    val activeModel: StateFlow<LocalModel?> = _activeModel.asStateFlow()

    /** Models already present on disk, matched back to the catalog by file name. */
    fun installedModels(): List<LocalModel> {
        if (!modelsDir.exists()) return emptyList()
        val byFileName = ModelCatalog.defaults.associateBy { it.fileName }
        return modelsDir.listFiles { f -> f.isFile && !f.name.endsWith(".part") }
            ?.mapNotNull { file ->
                val catalog = byFileName[file.name] ?: return@mapNotNull null
                LocalModel(
                    id = catalog.id,
                    displayName = catalog.displayName,
                    absolutePath = file.absolutePath,
                    format = catalog.format,
                    sizeBytes = file.length(),
                    contextTokens = catalog.contextTokens,
                    supportsTools = catalog.supportsTools,
                )
            }
            .orEmpty()
    }

    fun download(model: CatalogModel): Flow<DownloadState> = downloader.download(model)

    suspend fun activate(model: LocalModel) {
        engine.load(
            modelPath = model.absolutePath,
            options = EngineOptions(maxContextTokens = model.contextTokens),
        )
        _activeModel.value = model
    }

    suspend fun deactivate() {
        engine.unload()
        _activeModel.value = null
    }

    fun delete(model: LocalModel): Boolean {
        if (_activeModel.value?.id == model.id) return false
        return File(model.absolutePath).delete()
    }
}

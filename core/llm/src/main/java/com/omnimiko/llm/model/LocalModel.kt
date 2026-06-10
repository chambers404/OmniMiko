package com.omnimiko.llm.model

/** A model that has been downloaded to device storage. */
data class LocalModel(
    val id: String,
    val displayName: String,
    val absolutePath: String,
    val format: ModelFormat,
    val sizeBytes: Long,
    val contextTokens: Int,
    val supportsTools: Boolean,
)

/** Download progress for a catalog model. */
sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : DownloadState {
        val fraction: Float get() = if (totalBytes <= 0) 0f else downloadedBytes.toFloat() / totalBytes
    }
    data class Completed(val model: LocalModel) : DownloadState
    data class Failed(val message: String) : DownloadState
}

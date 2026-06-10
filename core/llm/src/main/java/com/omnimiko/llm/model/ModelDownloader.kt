package com.omnimiko.llm.model

import com.omnimiko.common.log.OmniLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Streams a catalog model to disk, emitting [DownloadState] progress. Writes to a
 * `.part` file and atomically renames on success so a partial download is never
 * mistaken for a usable model.
 */
class ModelDownloader(
    private val modelsDir: File,
    private val client: OkHttpClient = OkHttpClient(),
) {

    fun download(model: CatalogModel): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0, model.approxSizeBytes))
        modelsDir.mkdirs()
        val target = File(modelsDir, model.fileName)
        val part = File(modelsDir, model.fileName + ".part")

        val request = Request.Builder().url(model.downloadUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit(DownloadState.Failed("HTTP ${response.code}"))
                return@flow
            }
            val body = response.body ?: run {
                emit(DownloadState.Failed("Empty response body"))
                return@flow
            }
            val total = body.contentLength().takeIf { it > 0 } ?: model.approxSizeBytes
            var written = 0L

            body.byteStream().use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        written += read
                        emit(DownloadState.Downloading(written, total))
                    }
                }
            }
        }

        if (!part.renameTo(target)) {
            emit(DownloadState.Failed("Could not finalize download"))
            return@flow
        }
        OmniLog.i(TAG, "Downloaded ${model.id} -> ${target.absolutePath}")
        emit(DownloadState.Completed(model.toLocalModel(target)))
    }.flowOn(Dispatchers.IO)

    private fun CatalogModel.toLocalModel(file: File) = LocalModel(
        id = id,
        displayName = displayName,
        absolutePath = file.absolutePath,
        format = format,
        sizeBytes = file.length(),
        contextTokens = contextTokens,
        supportsTools = supportsTools,
    )

    private companion object {
        const val TAG = "ModelDownloader"
    }
}

package com.omnimiko.ui.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omnimiko.di.AppContainer
import com.omnimiko.llm.model.CatalogModel
import com.omnimiko.llm.model.DownloadState
import com.omnimiko.llm.model.LocalModel

@Composable
fun ModelsScreen(container: AppContainer) {
    val viewModel: ModelsViewModel = viewModel(factory = ModelsViewModel.Factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val installedIds = state.installed.associateBy { it.id }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("On-device models", style = MaterialTheme.typography.titleLarge)
            Text(
                "Everything runs locally. Download a model once, then activate it to chat offline.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        items(state.catalog, key = { it.id }) { model ->
            ModelCard(
                model = model,
                installed = installedIds[model.id],
                isActive = state.activeModelId == model.id,
                download = state.downloads[model.id],
                busy = state.busy,
                onDownload = { viewModel.download(model) },
                onActivate = { installedIds[model.id]?.let(viewModel::activate) },
                onDeactivate = viewModel::deactivate,
                onDelete = { installedIds[model.id]?.let(viewModel::delete) },
            )
        }
    }
}

@Composable
private fun ModelCard(
    model: CatalogModel,
    installed: LocalModel?,
    isActive: Boolean,
    download: DownloadState?,
    busy: Boolean,
    onDownload: () -> Unit,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(model.displayName, style = MaterialTheme.typography.bodyLarge)
                if (isActive) {
                    Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(model.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${model.format} · ~${model.approxSizeBytes / 1_000_000} MB · ctx ${model.contextTokens}" +
                    if (model.supportsTools) " · tools" else "",
                style = MaterialTheme.typography.labelSmall,
            )

            when (val d = download) {
                is DownloadState.Downloading -> {
                    LinearProgressIndicator(progress = { d.fraction }, modifier = Modifier.fillMaxWidth())
                    Text("${(d.fraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                }
                is DownloadState.Failed -> Text("Failed: ${d.message}", color = MaterialTheme.colorScheme.error)
                else -> Unit
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    installed == null -> Button(onClick = onDownload, enabled = download !is DownloadState.Downloading) {
                        Text(if (download is DownloadState.Downloading) "Downloading…" else "Download")
                    }
                    isActive -> OutlinedButton(onClick = onDeactivate, enabled = !busy) { Text("Unload") }
                    else -> Button(onClick = onActivate, enabled = !busy) { Text("Load") }
                }
                if (installed != null && !isActive) {
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
            }
        }
    }
}

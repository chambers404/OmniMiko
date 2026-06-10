package com.omnimiko.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omnimiko.di.AppContainer

@Composable
fun SettingsScreen(container: AppContainer) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(container))
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        Column {
            Text("Temperature: ${"%.2f".format(settings.temperature)}", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = settings.temperature,
                onValueChange = viewModel::setTemperature,
                valueRange = 0f..1.5f,
            )
            Text(
                "Lower is more focused and deterministic; higher is more creative.",
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Divider()

        Column {
            Text("Max agent iterations: ${settings.maxIterations}", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = settings.maxIterations.toFloat(),
                onValueChange = { viewModel.setMaxIterations(it.toInt()) },
                valueRange = 1f..16f,
                steps = 14,
            )
            Text(
                "How many think→act cycles the agent may run before stopping.",
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Divider()

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Auto-approve tools", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Skip the approval prompt for tools that change files or access the network. " +
                        "Convenient, but less safe.",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Switch(checked = settings.autoApproveTools, onCheckedChange = viewModel::setAutoApprove)
        }

        Divider()
        Text(
            "OmniMiko runs models entirely on your device. Conversations and model " +
                "weights never leave the phone unless a tool you approve requires it.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

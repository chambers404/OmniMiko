package com.omnimiko.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.omnimiko.agent.core.Orchestrator
import com.omnimiko.agent.tools.CalculatorTool
import com.omnimiko.agent.tools.CurrentTimeTool
import com.omnimiko.agent.tools.ToolRegistry
import com.omnimiko.agent.tools.WebFetchTool
import com.omnimiko.agent.tools.WorkspaceFileSystem
import com.omnimiko.data.conversation.ConversationRepository
import com.omnimiko.data.conversation.OmniDatabase
import com.omnimiko.data.settings.SettingsRepository
import com.omnimiko.llm.engine.InferenceEngine
import com.omnimiko.llm.engine.MediaPipeLlmEngine
import com.omnimiko.llm.engine.MockLlmEngine
import com.omnimiko.llm.model.ModelDownloader
import com.omnimiko.llm.model.ModelManager
import okhttp3.OkHttpClient
import java.io.File

private val Context.settingsDataStore by preferencesDataStore(name = "omnimiko_settings")

/**
 * Manual dependency-injection container. The whole object graph is small and
 * lives for the app's lifetime, so a hand-wired container keeps build times low
 * and the wiring obvious — no annotation processor needed.
 *
 * Swap [createEngine] to choose the inference backend. By default we use the real
 * on-device [MediaPipeLlmEngine]; set [useMockEngine] (e.g. in a debug build with
 * no model present) to fall back to [MockLlmEngine] so the app is fully usable
 * for development without gigabytes of weights.
 */
class AppContainer(
    private val appContext: Context,
    useMockEngine: Boolean = false,
) {
    val modelsDir: File = File(appContext.filesDir, "models").apply { mkdirs() }
    val workspaceDir: File = File(appContext.filesDir, "workspace").apply { mkdirs() }

    private val httpClient: OkHttpClient = OkHttpClient()

    val engine: InferenceEngine =
        if (useMockEngine) MockLlmEngine() else MediaPipeLlmEngine(appContext)

    val modelManager: ModelManager =
        ModelManager(modelsDir, engine, ModelDownloader(modelsDir, httpClient))

    private val database: OmniDatabase = OmniDatabase.build(appContext)

    val conversationRepository: ConversationRepository =
        ConversationRepository(database.conversationDao(), database.messageDao())

    val settingsRepository: SettingsRepository =
        SettingsRepository(appContext.settingsDataStore)

    /** Default tool set. Add new [com.omnimiko.agent.tools.Tool]s here. */
    val toolRegistry: ToolRegistry = ToolRegistry(
        buildList {
            add(CurrentTimeTool())
            add(CalculatorTool())
            add(WebFetchTool(httpClient))
            addAll(WorkspaceFileSystem(workspaceDir).tools())
        },
    )

    val orchestrator: Orchestrator = Orchestrator(engine, toolRegistry)
}

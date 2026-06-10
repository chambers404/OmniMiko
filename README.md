# OmniMiko

**A true local LLM agentic orchestrator for Android.**

OmniMiko brings a desktop-class agentic coding/assistant experience — the kind
you get from tools like Claude or OpenClaw — entirely onto your phone. Models run
**on-device**. Your prompts, conversations, and the model weights themselves never
leave the device unless a tool you explicitly approve needs the network.

> Status: **scaffold / v0.1.0.** The architecture, agent loop, tool framework,
> UI, and persistence are in place and unit-tested. Plug in an on-device model
> (or use the built-in mock engine) to run it.

---

## What it does

- 🧠 **On-device inference** — runs quantized LLMs locally via a backend-agnostic
  `InferenceEngine` (default: MediaPipe LLM Inference; designed to also host a
  llama.cpp/GGUF JNI backend).
- 🤖 **Agentic orchestration** — a ReAct-style loop that lets the model *think →
  call tools → observe → repeat* until it can answer, streamed live to the UI.
- 🧰 **Tools** — calculator, current time, sandboxed workspace file read/write/list,
  and web fetch. Adding a tool is one class + one registry line.
- 🔐 **Human-in-the-loop approvals** — state-changing and network tools pause the
  run for explicit user consent (toggleable).
- 💬 **Full chat UI** — Jetpack Compose, streaming tokens, tool-activity bubbles,
  conversation history, model manager, and settings.
- 💾 **Durable sessions** — conversations and learned memories persisted in Room so
  a multi-step run survives the app being killed.

## Architecture

OmniMiko is a multi-module Gradle project. Dependencies point **downward** only:

```
app  ──►  core:agent  ──►  core:llm  ──►  core:common
  │            │                            ▲
  └──►  core:data  ───────────────────────-─┘
```

| Module        | Responsibility                                                        |
|---------------|-----------------------------------------------------------------------|
| `core:common` | Shared domain types: `ChatMessage`, `ToolSpec/Call/Result`, `AgentEvent`, `OmniResult`. |
| `core:llm`    | `InferenceEngine` abstraction + `MediaPipeLlmEngine`/`MockLlmEngine`, prompt formatting, model catalog/download/lifecycle. |
| `core:agent`  | The `Orchestrator` loop, `ToolRegistry`, built-in tools, system-prompt + tool-call parsing. |
| `core:data`   | Room database, conversation/memory repositories, settings DataStore.  |
| `app`         | Compose UI, ViewModels, manual DI (`AppContainer`), foreground service.|

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the agent loop in detail.

## Build

Requires the **Android SDK** (API 35) and JDK 17+. Open in Android Studio
(Ladybug or newer) or build from the CLI:

```bash
# Point Gradle at your SDK
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew :app:assembleDebug      # build the debug APK (uses the mock engine)
./gradlew test                    # run JVM unit tests
./gradlew :app:installDebug       # install on a connected device/emulator
```

The **debug** build uses `MockLlmEngine` so the whole app is usable end-to-end
without downloading gigabytes of weights. The **release** build uses real
on-device inference (`MediaPipeLlmEngine`).

> Note: this repo intentionally does **not** vendor model weights. Use the
> in-app **Models** tab to download one, or configure `ModelCatalog` with your
> own redistributable bundle URLs.

## Running a real model

1. Build & install a release (or flip `useMockEngine = false` in `OmniMikoApp`).
2. Open **Models**, pick a model, and tap **Download** → **Load**.
3. Chat. Watch it call tools live; approve any that touch files or the network.

Supported formats behind `InferenceEngine`:
- **MediaPipe `.task`** bundles (Gemma, Phi, …) — works today.
- **GGUF** via llama.cpp JNI — wire a `LlamaCppEngine : InferenceEngine` (the
  interface is ready; the native backend is the next milestone).

## Adding a tool

```kotlin
class MyTool : Tool {
    override val spec = ToolSpec(
        name = "my_tool",
        description = "What it does, for the model.",
        parameters = listOf(ParameterSpec("arg", "string", "…")),
        requiresApproval = false,
    )
    override suspend fun execute(callId: String, arguments: JsonObject): ToolResult =
        success(callId, "result")
}
```

Register it in `AppContainer.toolRegistry`. It's now advertised to the model and
callable in the loop — no other changes required.

## License

MIT — see [LICENSE](LICENSE).

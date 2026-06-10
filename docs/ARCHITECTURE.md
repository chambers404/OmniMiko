# OmniMiko Architecture

This document explains how the pieces fit together and, most importantly, how the
**agent loop** turns a plain chat model into a tool-using agent that runs on a
phone.

## Design goals

1. **Local-first / private.** Inference and orchestration happen on-device. The
   only egress is tools the user explicitly approves (e.g. `web_fetch`).
2. **Backend-agnostic inference.** The agent never knows which runtime produced a
   token. Swapping MediaPipe for llama.cpp touches one class.
3. **Model-agnostic tool calling.** Works even with models that have no native
   function-calling, by teaching the protocol in the system prompt and parsing
   the output.
4. **Resilient.** A single tool failure, a denied approval, or the app being
   killed mid-run must not lose the conversation.

## Module graph

```
        ┌─────────────┐
        │     app     │  Compose UI · ViewModels · DI · foreground service
        └──────┬──────┘
   ┌───────────┼─────────────┐
   ▼           ▼             ▼
core:agent  core:data    (uses both)
   │
   ▼
core:llm
   │
   ▼
core:common   (leaf — pure domain types, no Android deps beyond logging)
```

Strict downward dependencies keep build times low and make each layer testable in
isolation (the agent loop is unit-tested against `MockLlmEngine`, no device
needed).

## The inference abstraction

`InferenceEngine` (in `core:llm`) is the seam:

```kotlin
interface InferenceEngine {
    val isReady: Boolean
    suspend fun load(modelPath: String, options: EngineOptions = EngineOptions())
    suspend fun unload()
    fun generate(messages: List<ChatMessage>, params: GenerationParams): Flow<String>
}
```

- `MediaPipeLlmEngine` wraps Google's on-device LLM Inference API (`.task` bundles).
- `MockLlmEngine` is deterministic and dependency-free — used by the debug build,
  Compose previews, and unit tests.
- A future `LlamaCppEngine` slots in here to run GGUF via JNI.

`generate` returns a cold `Flow<String>` of token deltas. Cancelling the collector
cancels native generation (via `callbackFlow { … awaitClose { session.close() } }`).

## The agent loop

`Orchestrator.run(history, config, approvalGate): Flow<AgentEvent>` implements a
ReAct-style loop:

```
build system prompt (persona + tool schemas + <tool_call> protocol)
seed transcript = [system] + history
repeat up to maxIterations:
    stream one assistant turn  ───────────────► emit Token deltas
    parse <tool_call> blocks out of the text
    if no tool calls:
        emit Completed(finalMessage); stop
    for each tool call:
        if tool.requiresApproval and approvals enforced:
            emit ApprovalRequired ; await approvalGate
            if denied: feed a "denied" tool result back; continue
        run tool ──► emit ToolCallStarted / ToolCallFinished
        append the tool result to the transcript (role = TOOL)
    loop (model now sees the tool output and continues)
```

Everything the UI needs is expressed as an `AgentEvent`:
`Token`, `AssistantMessage`, `ToolCallStarted/Finished`, `ApprovalRequired`,
`Status`, `Completed`, `Error`. The ViewModel collects this flow and reduces it
into immutable UI state.

### Tool-call protocol

The system prompt instructs the model to emit exactly:

```
<tool_call>{"name": "<tool>", "arguments": { ... }}</tool_call>
```

`ToolCallParser` extracts these blocks (tolerating code fences, whitespace, and a
missing closing tag at end-of-stream) and returns the remaining prose plus the
structured calls. `</tool_call>` is registered as a stop sequence so generation
halts the moment a call is complete and the loop can act without waiting for more
tokens.

### Approvals (human-in-the-loop)

Tools whose `spec.requiresApproval` is `true` (e.g. `write_file`, `web_fetch`)
suspend the loop. The orchestrator exposes a `ApprovalGate` functional interface;
`ChatViewModel` implements it by surfacing a `PendingApproval` into UI state and
resolving a `CompletableDeferred<Boolean>` when the user taps Allow/Deny.

## Tools

`Tool` is a tiny contract: a declarative `ToolSpec` (rendered into the prompt) and
a `suspend execute(callId, arguments): ToolResult`. Tools must never throw — they
return an error `ToolResult` so the loop can recover and the model can react.

Built-ins:

| Tool           | Approval | Notes                                                    |
|----------------|----------|----------------------------------------------------------|
| `current_time` | no       | Grounds plans in real time.                              |
| `calculator`   | no       | Self-contained recursive-descent evaluator (no eval).    |
| `read_file`    | no       | Confined to the workspace sandbox.                       |
| `write_file`   | yes      | Path-validated against workspace escape.                 |
| `list_files`   | no       | Workspace listing.                                       |
| `web_fetch`    | yes      | The only egress; strips HTML, truncates to a budget.     |

`WorkspaceFileSystem` canonicalizes every path and rejects anything resolving
outside the workspace root, so a confused or adversarial model cannot read or
overwrite arbitrary device files.

## Persistence

`core:data` uses Room for conversations, messages (tool calls stored as JSON so a
turn round-trips exactly), and a `memories` table for durable facts. Settings live
in a Preferences DataStore. Because the transcript is persisted per message, a
multi-step run can resume after process death.

## Threading & lifecycle

- Generation and tool I/O run on `Dispatchers.Default`/`IO`; the loop is driven
  inside `viewModelScope`, so navigating away cancels it cleanly.
- `AgentForegroundService` keeps the process alive for long runs while the app is
  backgrounded (the foreground-service notification only owns lifetime; the work
  stays in coroutines).

## Extension points / roadmap

- **`LlamaCppEngine`** — GGUF inference via JNI for the widest model selection.
- **MCP client tool** — let OmniMiko call external Model Context Protocol servers
  as just another `Tool`.
- **Vision / multimodal** — extend `ChatMessage` with attachments once a
  multimodal on-device model backend is wired.
- **Sub-agents** — the `Orchestrator` is reentrant; a `spawn_agent` tool could run
  a nested loop for decomposition.

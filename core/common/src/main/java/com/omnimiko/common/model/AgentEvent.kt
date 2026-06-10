package com.omnimiko.common.model

/**
 * Events streamed from the agent loop to the UI as a run progresses. The UI
 * renders these incrementally (token deltas, tool activity, plan steps) so the
 * experience matches a desktop agent: you watch it think and act.
 */
sealed interface AgentEvent {
    /** A streamed chunk of assistant text. */
    data class Token(val text: String) : AgentEvent

    /** The model finished an assistant turn (full text included). */
    data class AssistantMessage(val message: ChatMessage) : AgentEvent

    /** The model requested a tool; emitted before execution. */
    data class ToolCallStarted(val call: ToolCall) : AgentEvent

    /** A tool finished executing. */
    data class ToolCallFinished(val result: ToolResult) : AgentEvent

    /** A tool requires explicit user approval before it can run. */
    data class ApprovalRequired(val call: ToolCall, val spec: ToolSpec) : AgentEvent

    /** High-level status for the activity indicator. */
    data class Status(val phase: Phase, val detail: String = "") : AgentEvent

    /** The run completed normally. */
    data class Completed(val finalMessage: ChatMessage) : AgentEvent

    /** The run failed or was aborted. */
    data class Error(val message: String, val cause: Throwable? = null) : AgentEvent

    enum class Phase { PLANNING, GENERATING, CALLING_TOOL, WAITING_APPROVAL, DONE }
}

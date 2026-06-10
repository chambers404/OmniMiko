package com.omnimiko.agent.tools

import com.omnimiko.common.model.ParameterSpec
import com.omnimiko.common.model.ToolResult
import com.omnimiko.common.model.ToolSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Filesystem tools confined to a sandbox directory (the agent's "workspace").
 * Every path is resolved and validated to stay inside [root], so a malicious or
 * confused model cannot read or clobber arbitrary device files.
 */
class WorkspaceFileSystem(private val root: File) {

    init {
        root.mkdirs()
    }

    /** Resolve [relative] against root, rejecting any escape via `..` or symlinks. */
    fun resolveInside(relative: String): File? {
        val candidate = File(root, relative).canonicalFile
        val base = root.canonicalFile
        return if (candidate.path == base.path || candidate.path.startsWith(base.path + File.separator)) {
            candidate
        } else {
            null
        }
    }

    fun tools(): List<Tool> = listOf(ReadFileTool(this), WriteFileTool(this), ListFilesTool(this))
}

private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.content

class ReadFileTool(private val fs: WorkspaceFileSystem) : Tool {
    override val spec = ToolSpec(
        name = "read_file",
        description = "Read a UTF-8 text file from the agent workspace.",
        parameters = listOf(
            ParameterSpec("path", "string", "Workspace-relative file path."),
        ),
    )

    override suspend fun execute(callId: String, arguments: JsonObject): ToolResult =
        withContext(Dispatchers.IO) {
            val path = arguments.str("path") ?: return@withContext failure(callId, "Missing 'path'.")
            val file = fs.resolveInside(path)
                ?: return@withContext failure(callId, "Path escapes workspace: $path")
            if (!file.exists()) return@withContext failure(callId, "No such file: $path")
            val text = file.readText()
            success(callId, if (text.length > MAX) text.take(MAX) + "\n…[truncated]" else text)
        }

    private companion object { const val MAX = 100_000 }
}

class WriteFileTool(private val fs: WorkspaceFileSystem) : Tool {
    override val spec = ToolSpec(
        name = "write_file",
        description = "Create or overwrite a UTF-8 text file in the agent workspace.",
        parameters = listOf(
            ParameterSpec("path", "string", "Workspace-relative file path."),
            ParameterSpec("content", "string", "Full file contents to write."),
        ),
        requiresApproval = true,
    )

    override suspend fun execute(callId: String, arguments: JsonObject): ToolResult =
        withContext(Dispatchers.IO) {
            val path = arguments.str("path") ?: return@withContext failure(callId, "Missing 'path'.")
            val content = arguments.str("content") ?: return@withContext failure(callId, "Missing 'content'.")
            val file = fs.resolveInside(path)
                ?: return@withContext failure(callId, "Path escapes workspace: $path")
            file.parentFile?.mkdirs()
            file.writeText(content)
            success(callId, "Wrote ${content.length} chars to $path")
        }
}

class ListFilesTool(private val fs: WorkspaceFileSystem) : Tool {
    override val spec = ToolSpec(
        name = "list_files",
        description = "List files and directories under a workspace path.",
        parameters = listOf(
            ParameterSpec("path", "string", "Workspace-relative directory (use \".\" for root).", required = false),
        ),
    )

    override suspend fun execute(callId: String, arguments: JsonObject): ToolResult =
        withContext(Dispatchers.IO) {
            val path = arguments.str("path") ?: "."
            val dir = fs.resolveInside(path)
                ?: return@withContext failure(callId, "Path escapes workspace: $path")
            if (!dir.exists()) return@withContext failure(callId, "No such directory: $path")
            val listing = dir.listFiles()?.sortedBy { it.name }?.joinToString("\n") { f ->
                val kind = if (f.isDirectory) "dir " else "file"
                "$kind  ${f.name}${if (f.isFile) " (${f.length()} bytes)" else ""}"
            } ?: "(empty)"
            success(callId, listing.ifBlank { "(empty)" })
        }
}

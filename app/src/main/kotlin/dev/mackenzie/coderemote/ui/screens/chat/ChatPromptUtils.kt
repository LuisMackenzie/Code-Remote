package dev.mackenzie.coderemote.ui.screens.chat

import dev.mackenzie.coderemote.data.api.PromptPart
import dev.mackenzie.coderemote.domain.model.Message

/** Format a token count to a human-readable string (e.g., 1.2k, 45.3k, 1.2M). */
internal fun formatTokenCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
        else -> count.toString()
    }
}

internal fun formatAssistantErrorMessage(error: Message.Assistant.ErrorInfo?): String? {
    if (error == null) return null
    val raw = error.message.ifBlank { error.name }
    return raw.ifBlank { null }
}

/**
 * Splits raw input text into a list of [PromptPart] objects.
 * Text around confirmed @file mentions becomes type="text" parts,
 * and each @file mention becomes a type="file" part with a file:// URL.
 */
internal fun buildPromptParts(
    text: String,
    confirmedPaths: Set<String>,
    sessionDirectory: String?
): List<PromptPart> {
    if (confirmedPaths.isEmpty()) {
        val trimmed = text.trim()
        return if (trimmed.isEmpty()) emptyList()
        else listOf(PromptPart(type = "text", text = trimmed))
    }

    // Find all confirmed @path mentions with their positions
    data class Mention(val start: Int, val end: Int, val path: String)
    val mentions = mutableListOf<Mention>()

    for (path in confirmedPaths) {
        val needle = "@$path"
        var searchFrom = 0
        while (true) {
            val idx = text.indexOf(needle, searchFrom)
            if (idx == -1) break
            val endIdx = idx + needle.length
            // Boundary check: next char must be whitespace, end-of-string, or @
            if (endIdx < text.length) {
                val next = text[endIdx]
                if (!next.isWhitespace() && next != '@') {
                    searchFrom = endIdx
                    continue
                }
            }
            mentions.add(Mention(idx, endIdx, path))
            searchFrom = endIdx
        }
    }

    if (mentions.isEmpty()) {
        val trimmed = text.trim()
        return if (trimmed.isEmpty()) emptyList()
        else listOf(PromptPart(type = "text", text = trimmed))
    }

    // Sort by position
    mentions.sortBy { it.start }

    val parts = mutableListOf<PromptPart>()
    var cursor = 0

    for (mention in mentions) {
        // Add text before this mention
        if (mention.start > cursor) {
            val segment = text.substring(cursor, mention.start).trim()
            if (segment.isNotEmpty()) {
                parts.add(PromptPart(type = "text", text = segment))
            }
        }
        // Add file part
        val isDir = mention.path.endsWith("/")
        val absPath = if (sessionDirectory != null) "$sessionDirectory/${mention.path}" else mention.path
        val displayName = mention.path.trimEnd('/').substringAfterLast('/')
        parts.add(
            PromptPart(
                type = "file",
                path = mention.path,
                mime = if (isDir) "application/x-directory" else "text/plain",
                url = "file:///$absPath",
                filename = displayName
            )
        )
        cursor = mention.end
    }

    // Trailing text
    if (cursor < text.length) {
        val segment = text.substring(cursor).trim()
        if (segment.isNotEmpty()) {
            parts.add(PromptPart(type = "text", text = segment))
        }
    }

    return parts
}

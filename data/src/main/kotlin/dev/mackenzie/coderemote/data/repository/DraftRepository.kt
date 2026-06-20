package dev.mackenzie.coderemote.data.repository

import dev.mackenzie.coderemote.data.AppLogger
import dev.mackenzie.coderemote.data.FileStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DraftRepository"
private const val DRAFTS_FILE = "session_drafts.json"

/**
 * A single draft: text + attachment URIs + confirmed @file paths for a session.
 */
@Serializable
data class Draft(
    val text: String = "",
    val imageUris: List<String> = emptyList(),
    val confirmedFilePaths: List<String> = emptyList(),
    val selectedAgent: String? = null,
    val selectedVariant: String? = null,
) {
    val isEmpty: Boolean
        get() = text.isBlank() &&
                imageUris.isEmpty() &&
                confirmedFilePaths.isEmpty() &&
                selectedAgent.isNullOrBlank() &&
                selectedVariant.isNullOrBlank()
}

/**
 * Persists per-session message drafts (text + attachment URIs + @file mentions)
 * so they survive navigation, app restarts, and WebUI detours.
 *
 * Storage: JSON file via [FileStorage]. Kept simple — no Room dependency.
 */
@Singleton
class DraftRepository @Inject constructor(
    private val fileStorage: FileStorage,
    private val logger: AppLogger
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** In-memory cache, loaded lazily. */
    private var drafts: MutableMap<String, Draft>? = null

    private fun ensureLoaded(): MutableMap<String, Draft> {
        drafts?.let { return it }
        val loaded = try {
            val content = fileStorage.readText(DRAFTS_FILE)
            if (content.isNullOrBlank()) {
                mutableMapOf()
            } else {
                json.decodeFromString<Map<String, Draft>>(content).toMutableMap()
            }
        } catch (e: Exception) {
            logger.w(TAG, "Failed to load drafts, starting fresh: ${e.message}")
            mutableMapOf()
        }
        drafts = loaded
        return loaded
    }

    /**
     * Get the draft for a session (or null if none exists).
     */
    fun getDraft(sessionId: String): Draft? {
        val d = ensureLoaded()[sessionId]
        return if (d != null && !d.isEmpty) d else null
    }

    /**
     * Save a draft for a session. Removes the entry if the draft is empty.
     */
    fun saveDraft(sessionId: String, draft: Draft) {
        val map = ensureLoaded()
        if (draft.isEmpty) {
            map.remove(sessionId)
        } else {
            map[sessionId] = draft
        }
        persist(map)
    }

    /**
     * Clear the draft for a session (e.g. after sending).
     */
    fun clearDraft(sessionId: String) {
        val map = ensureLoaded()
        if (map.remove(sessionId) != null) {
            persist(map)
        }
    }

    private fun persist(map: Map<String, Draft>) {
        try {
            fileStorage.writeText(DRAFTS_FILE, json.encodeToString(map))
        } catch (e: Exception) {
            logger.e(TAG, "Failed to persist drafts: ${e.message}")
        }
    }
}

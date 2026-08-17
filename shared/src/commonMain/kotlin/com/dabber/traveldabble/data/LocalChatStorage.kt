package com.dabber.traveldabble.data

import com.dabber.traveldabble.model.LocalChatMessage
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local-only chat storage with in-memory cache and SharedPreferences persistence.
 * Safe across Android and all KMP platforms without filesystem permission exceptions.
 */
object LocalChatStorage {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val kvSettings by lazy {
        try {
            Settings()
        } catch (_: Exception) {
            null
        }
    }

    private val inMemoryCache = mutableMapOf<String, MutableList<LocalChatMessage>>()

    private fun storageKey(tripId: String) = "chat_messages_$tripId"

    /**
     * Load all messages for a trip
     */
    fun loadMessages(tripId: String): List<LocalChatMessage> {
        val cached = inMemoryCache[tripId]
        if (cached != null) return cached.toList()

        return try {
            val stored = kvSettings?.getStringOrNull(storageKey(tripId))
            if (!stored.isNullOrBlank()) {
                val list = json.decodeFromString<List<LocalChatMessage>>(stored)
                inMemoryCache[tripId] = list.toMutableList()
                list
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Save a new message to local storage
     */
    fun saveMessage(message: LocalChatMessage) {
        val list = inMemoryCache.getOrPut(message.tripId) {
            loadMessages(message.tripId).toMutableList()
        }
        list.add(message)

        try {
            val serialized = json.encodeToString(list)
            kvSettings?.putString(storageKey(message.tripId), serialized)
        } catch (_: Exception) {
            // Never crash the UI on storage failure
        }
    }

    /**
     * Clear all messages for a trip
     */
    fun clearMessages(tripId: String) {
        inMemoryCache.remove(tripId)
        try {
            kvSettings?.remove(storageKey(tripId))
        } catch (_: Exception) {}
    }

    /**
     * Delete a specific message
     */
    fun deleteMessage(tripId: String, messageId: String) {
        val list = inMemoryCache.getOrPut(tripId) {
            loadMessages(tripId).toMutableList()
        }
        list.removeAll { it.id == messageId }

        try {
            val serialized = json.encodeToString(list)
            kvSettings?.putString(storageKey(tripId), serialized)
        } catch (_: Exception) {}
    }

    /**
     * Get message count for a trip
     */
    fun getMessageCount(tripId: String): Int {
        return loadMessages(tripId).size
    }
}

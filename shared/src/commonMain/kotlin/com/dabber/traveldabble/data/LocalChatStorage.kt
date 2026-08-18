package com.dabber.traveldabble.data

import com.dabber.traveldabble.model.LocalChatMessage
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ChatConversation(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessagePreview: String? = null,
    val messageCount: Int = 0,
)

/**
 * Local-only chat storage supporting multiple conversations with in-memory cache
 * and SharedPreferences persistence.
 */
object LocalChatStorage {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val kvSettings by lazy {
        try {
            Settings()
        } catch (_: Throwable) {
            null
        }
    }

    private const val CONVERSATIONS_KEY = "chat_conversations_index"
    private const val ACTIVE_CONV_KEY = "active_chat_conversation_id"

    private val inMemoryCache = mutableMapOf<String, MutableList<LocalChatMessage>>()
    private var conversationsCache: MutableList<ChatConversation>? = null

    private fun storageKey(conversationId: String) = "chat_messages_$conversationId"

    /**
     * Get all active conversations sorted by most recently active.
     */
    fun loadConversations(): List<ChatConversation> {
        conversationsCache?.let { return it.sortedByDescending { c -> c.updatedAt } }

        val stored = try {
            kvSettings?.getStringOrNull(CONVERSATIONS_KEY)
        } catch (_: Throwable) {
            null
        }

        val list = if (!stored.isNullOrBlank()) {
            try {
                json.decodeFromString<List<ChatConversation>>(stored).toMutableList()
            } catch (_: Throwable) {
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        if (list.isEmpty()) {
            // Seed a default conversation
            val defaultConv = ChatConversation(
                id = "general",
                title = "Travel Planning",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastMessagePreview = "Xin chào! I'm your Vietnam travel copilot.",
                messageCount = loadMessages("general").size,
            )
            list.add(defaultConv)
            saveConversationsList(list)
        }

        conversationsCache = list
        return list.sortedByDescending { it.updatedAt }
    }

    private fun saveConversationsList(list: List<ChatConversation>) {
        conversationsCache = list.toMutableList()
        try {
            val serialized = json.encodeToString(list)
            kvSettings?.putString(CONVERSATIONS_KEY, serialized)
        } catch (_: Throwable) {}
    }

    /**
     * Create a new conversation and return it.
     */
    fun createConversation(title: String = "New Chat"): ChatConversation {
        val list = loadConversations().toMutableList()
        val newId = "conv_" + System.currentTimeMillis()
        val conv = ChatConversation(
            id = newId,
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastMessagePreview = null,
            messageCount = 0,
        )
        list.add(0, conv)
        saveConversationsList(list)
        setActiveConversationId(newId)
        return conv
    }

    /**
     * Rename an existing conversation.
     */
    fun updateConversationTitle(conversationId: String, newTitle: String) {
        val list = loadConversations().toMutableList()
        val index = list.indexOfFirst { it.id == conversationId }
        if (index != -1) {
            list[index] = list[index].copy(title = newTitle, updatedAt = System.currentTimeMillis())
            saveConversationsList(list)
        }
    }

    /**
     * Delete a conversation and all its messages.
     */
    fun deleteConversation(conversationId: String) {
        val list = loadConversations().toMutableList()
        list.removeAll { it.id == conversationId }
        clearMessages(conversationId)

        if (list.isEmpty()) {
            // Keep at least one conversation
            val defaultConv = ChatConversation(
                id = "general",
                title = "Travel Planning",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastMessagePreview = null,
                messageCount = 0,
            )
            list.add(defaultConv)
        }

        saveConversationsList(list)
        if (getActiveConversationId() == conversationId) {
            setActiveConversationId(list.first().id)
        }
    }

    /**
     * Get or set active conversation ID.
     */
    fun getActiveConversationId(): String {
        val stored = try {
            kvSettings?.getStringOrNull(ACTIVE_CONV_KEY)
        } catch (_: Throwable) {
            null
        }
        return stored?.ifBlank { "general" } ?: "general"
    }

    fun setActiveConversationId(id: String) {
        try {
            kvSettings?.putString(ACTIVE_CONV_KEY, id)
        } catch (_: Throwable) {}
    }

    /**
     * Load all messages for a conversation/trip
     */
    fun loadMessages(conversationId: String): List<LocalChatMessage> {
        val cached = inMemoryCache[conversationId]
        if (cached != null) return cached.toList()

        return try {
            val stored = kvSettings?.getStringOrNull(storageKey(conversationId))
            if (!stored.isNullOrBlank()) {
                val list = json.decodeFromString<List<LocalChatMessage>>(stored)
                inMemoryCache[conversationId] = list.toMutableList()
                list
            } else {
                emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Save a new message to local storage
     */
    fun saveMessage(message: LocalChatMessage) {
        val convId = message.tripId
        val list = inMemoryCache.getOrPut(convId) {
            loadMessages(convId).toMutableList()
        }
        list.add(message)

        try {
            val serialized = json.encodeToString(list)
            kvSettings?.putString(storageKey(convId), serialized)
        } catch (_: Throwable) {}

        // Update conversation summary
        val conversations = loadConversations().toMutableList()
        val index = conversations.indexOfFirst { it.id == convId }
        val preview = message.text.take(80).replace("\n", " ")
        if (index != -1) {
            val existing = conversations[index]
            var title = existing.title
            if (title == "New Chat" && message.senderId != "ai") {
                title = message.text.take(30).trim()
            }
            conversations[index] = existing.copy(
                title = title,
                updatedAt = System.currentTimeMillis(),
                lastMessagePreview = preview,
                messageCount = list.size,
            )
            saveConversationsList(conversations)
        } else {
            val newConv = ChatConversation(
                id = convId,
                title = if (message.senderId != "ai") message.text.take(30).trim() else "Travel Planning",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastMessagePreview = preview,
                messageCount = list.size,
            )
            conversations.add(0, newConv)
            saveConversationsList(conversations)
        }
    }

    /**
     * Clear all messages for a conversation
     */
    fun clearMessages(conversationId: String) {
        inMemoryCache.remove(conversationId)
        try {
            kvSettings?.remove(storageKey(conversationId))
        } catch (_: Throwable) {}
    }

    /**
     * Delete a specific message
     */
    fun deleteMessage(conversationId: String, messageId: String) {
        val list = inMemoryCache.getOrPut(conversationId) {
            loadMessages(conversationId).toMutableList()
        }
        list.removeAll { it.id == messageId }

        try {
            val serialized = json.encodeToString(list)
            kvSettings?.putString(storageKey(conversationId), serialized)
        } catch (_: Throwable) {}
    }

    /**
     * Get message count for a conversation
     */
    fun getMessageCount(conversationId: String): Int {
        return loadMessages(conversationId).size
    }

    /**
     * Clear all conversations and all chat history
     */
    fun clearAll() {
        inMemoryCache.clear()
        conversationsCache?.clear()
        try {
            val convs = loadConversations()
            convs.forEach { c -> kvSettings?.remove(storageKey(c.id)) }
            kvSettings?.remove(CONVERSATIONS_KEY)
            kvSettings?.remove(ACTIVE_CONV_KEY)
        } catch (_: Throwable) {}
    }
}

package com.dabber.traveldabble.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import com.dabber.traveldabble.ui.navigation.ScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.AiModelOption
import com.dabber.traveldabble.data.AiResult
import com.dabber.traveldabble.data.AiService
import com.dabber.traveldabble.data.AuthState
import com.dabber.traveldabble.data.ChatConversation
import com.dabber.traveldabble.data.LocalChatStorage
import com.dabber.traveldabble.data.SettingsState
import com.dabber.traveldabble.data.ToolExecutionEvent
import com.dabber.traveldabble.data.ToolResult
import com.dabber.traveldabble.model.LocalChatMessage
import com.dabber.traveldabble.ui.components.GlassButton
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.GlassIconButton
import com.dabber.traveldabble.ui.components.TropicalLogo
import com.dabber.traveldabble.ui.components.bounceClick
import com.dabber.traveldabble.ui.glass.GlassIntensity
import com.dabber.traveldabble.ui.glass.glass
import com.dabber.traveldabble.ui.theme.AuroraBlue
import com.dabber.traveldabble.ui.theme.AuroraGold
import com.dabber.traveldabble.ui.theme.AuroraTeal
import com.dabber.traveldabble.ui.theme.AuroraViolet
import kotlinx.coroutines.launch

/**
 * AI Chat screen with multiple conversation support, conversation drawer,
 * and direct redirect to AI Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    tripId: String = "general",
    onNavigate: (screen: String, tripId: String?) -> Unit = { _, _ -> },
) {
    val chatSuggestions = listOf(
        "Plan 3 days in Hanoi & Ha Long",
        "Best street food spots in Hoi An",
        "How to do the Ha Giang loop?",
        "Create a 4-day trip to Da Nang",
        "Must-see spots in Ninh Binh",
    )

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Conversation management
    var conversations by remember { mutableStateOf(LocalChatStorage.loadConversations()) }
    var activeConversationId by remember { mutableStateOf(LocalChatStorage.getActiveConversationId()) }
    var showConversationsDrawer by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<ChatConversation?>(null) }

    // Active conversation messages
    val messages = remember { mutableStateListOf<LocalChatMessage>() }

    fun refreshMessages(convId: String) {
        messages.clear()
        messages.addAll(LocalChatStorage.loadMessages(convId))
    }

    LaunchedEffect(activeConversationId) {
        refreshMessages(activeConversationId)
    }

    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var aiStatus by remember { mutableStateOf<String?>(null) }
    var availableModels by remember { mutableStateOf<List<AiModelOption>>(AiService.DEFAULT_AI_MODELS) }

    // Tool execution tracking
    val toolEvents = remember { mutableStateListOf<ToolEventDisplay>() }
    var currentToolName by remember { mutableStateOf<String?>(null) }

    // Check AI health and fetch live models on mount
    LaunchedEffect(Unit) {
        ScrollState.show()
        val health = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            AiService.checkHealth()
        }
        aiStatus = when {
            health.serverKeyConfigured -> null
            AuthState.openRouterApiKey != null -> null
            else -> "No AI key configured. Configure your OpenRouter key in AI Settings."
        }
        val liveModels = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            AiService.fetchModels()
        }
        if (liveModels.isNotEmpty()) {
            availableModels = liveModels
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size, toolEvents.size) {
        val totalItems = messages.size + toolEvents.size
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    val activeConversation = conversations.find { it.id == activeConversationId }
        ?: conversations.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Conversation Drawer Toggle Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .glass(RoundedCornerShape(12.dp), GlassIntensity.Standard)
                    .clickable { showConversationsDrawer = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    androidx.compose.material.icons.Icons.Filled.Forum,
                    contentDescription = "Conversations",
                    tint = AuroraTeal,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Active Conversation Title & Model Subtitle
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showConversationsDrawer = true },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        activeConversation?.title ?: "Travel Copilot",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val activeModelName = availableModels.find { it.id == SettingsState.selectedAiModel }?.name
                    ?: SettingsState.selectedAiModel.substringAfterLast("/").replace(":free", " (Free)").replace("-", " ")
                val modeText = when {
                    AuthState.openRouterApiKey != null -> "BYOK • $activeModelName"
                    else -> "Server AI • $activeModelName"
                }
                Text(
                    modeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Quick New Chat Action
            GlassIconButton(
                icon = Icons.Filled.Add,
                contentDescription = "New Conversation",
                onClick = {
                    val newConv = LocalChatStorage.createConversation("New Chat")
                    conversations = LocalChatStorage.loadConversations()
                    activeConversationId = newConv.id
                    refreshMessages(newConv.id)
                },
            )

            // Direct Redirect to AI Settings
            GlassIconButton(
                icon = Icons.Filled.Settings,
                contentDescription = "AI Settings",
                onClick = {
                    onNavigate("ai_settings", null)
                },
            )
        }

        // AI Status Banner
        aiStatus?.let { status ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = { onNavigate("ai_settings", null) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = AuroraTeal, modifier = Modifier.size(16.dp))
                    Text(
                        status,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Configure →",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AuroraTeal,
                    )
                }
            }
        }

        // Messages + Tool Events Interleaved
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Empty State Hero Card
            if (messages.isEmpty() && toolEvents.isEmpty() && !isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TropicalLogo(size = 64.dp, showBackground = true)
                        Text(
                            "Travel Copilot",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Ask anything about Vietnam itineraries, food, destinations, weather, and packing tips.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }
            }

            // Interleave messages and tool events
            val allItems = buildList {
                var toolIdx = 0
                for (msg in messages) {
                    while (toolIdx < toolEvents.size && toolEvents[toolIdx].timestamp <= msg.timestamp) {
                        add(ChatItem.Tool(toolEvents[toolIdx]))
                        toolIdx++
                    }
                    add(ChatItem.Message(msg))
                }
                while (toolIdx < toolEvents.size) {
                    add(ChatItem.Tool(toolEvents[toolIdx]))
                    toolIdx++
                }
            }

            items(allItems, key = { it.id }) { item ->
                when (item) {
                    is ChatItem.Message -> ChatBubble(item.message)
                    is ChatItem.Tool -> ToolEventCard(item.event, onNavigate = onNavigate)
                }
            }

            // Typing indicator
            if (isLoading) {
                item {
                    TypingIndicator(currentToolName = currentToolName)
                }
            }
        }

        // Suggestion Chips (when conversation is short or empty)
        if (messages.size <= 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chatSuggestions.forEach { suggestion ->
                    GlassChip(
                        label = suggestion,
                        tint = AuroraTeal,
                        onClick = {
                            input = suggestion
                        },
                    )
                }
            }
        }

        // Dock clearance calculation: 88.dp when keyboard is closed, 10.dp when keyboard is up
        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        val isKeyboardOpen = imeBottom > 0.dp
        val bottomDockPadding = if (isKeyboardOpen) 10.dp else 88.dp

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = bottomDockPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .glass(RoundedCornerShape(24.dp), GlassIntensity.Standard)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (input.isEmpty()) {
                    Text(
                        "Ask Travel Copilot…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                )
            }

            // Send button
            val canSend = input.isNotBlank() && !isLoading
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) Brush.linearGradient(listOf(AuroraTeal, AuroraBlue))
                        else Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f)))
                    )
                    .clickable(enabled = canSend) {
                        val text = input.trim()
                        input = ""
                        val userMsg = LocalChatMessage(
                            id = "msg_" + System.currentTimeMillis(),
                            tripId = activeConversationId,
                            senderId = AuthState.currentUser?.id ?: "user",
                            senderName = AuthState.currentUser?.displayName ?: "Traveler",
                            text = text,
                            timestamp = System.currentTimeMillis(),
                        )
                        messages.add(userMsg)
                        LocalChatStorage.saveMessage(userMsg)
                        conversations = LocalChatStorage.loadConversations()

                        isLoading = true
                        currentToolName = null
                        toolEvents.clear()

                        scope.launch {
                            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                AiService.sendMessage(
                                    tripId = activeConversationId,
                                    userMessage = text,
                                    byokKey = AuthState.openRouterApiKey,
                                    model = SettingsState.selectedAiModel,
                                    onToolExecuted = { event ->
                                        when (event) {
                                            is ToolExecutionEvent.Started -> {
                                                currentToolName = event.toolName
                                            }
                                            is ToolExecutionEvent.Completed -> {
                                                val display = ToolEventDisplay(
                                                    id = "tool_${event.toolName}_${System.currentTimeMillis()}",
                                                    toolName = event.toolName,
                                                    status = when (event.result) {
                                                        is ToolResult.Success -> ToolStatus.SUCCESS
                                                        is ToolResult.Error -> ToolStatus.ERROR
                                                    },
                                                    message = when (val r = event.result) {
                                                        is ToolResult.Success -> r.message
                                                        is ToolResult.Error -> r.message
                                                    },
                                                    timestamp = System.currentTimeMillis(),
                                                    navigationTarget = when (val r = event.result) {
                                                        is ToolResult.Success -> r.navigateTo
                                                        else -> null
                                                    },
                                                    navigateTripId = when (val r = event.result) {
                                                        is ToolResult.Success -> r.navigateTripId
                                                        else -> null
                                                    },
                                                )
                                                toolEvents.add(display)
                                            }
                                        }
                                    }
                                )
                            }

                            isLoading = false
                            currentToolName = null

                            when (result) {
                                is AiResult.Success -> {
                                    val aiMsg = LocalChatMessage(
                                        id = "ai_" + System.currentTimeMillis(),
                                        tripId = activeConversationId,
                                        senderId = "ai",
                                        senderName = "Travel Copilot",
                                        text = result.content,
                                        timestamp = System.currentTimeMillis(),
                                    )
                                    messages.add(aiMsg)
                                    LocalChatStorage.saveMessage(aiMsg)
                                    conversations = LocalChatStorage.loadConversations()
                                }
                                is AiResult.Error -> {
                                    val errorMsg = LocalChatMessage(
                                        id = "ai_err_" + System.currentTimeMillis(),
                                        tripId = activeConversationId,
                                        senderId = "ai",
                                        senderName = "Travel Copilot",
                                        text = "⚠️ ${result.message}",
                                        timestamp = System.currentTimeMillis(),
                                    )
                                    messages.add(errorMsg)
                                    LocalChatStorage.saveMessage(errorMsg)
                                    conversations = LocalChatStorage.loadConversations()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    // Conversations Modal Bottom Sheet
    if (showConversationsDrawer) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showConversationsDrawer = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Chat Conversations",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    GlassButton(
                        label = "New Chat",
                        icon = Icons.Filled.Add,
                        onClick = {
                            val newConv = LocalChatStorage.createConversation("New Chat")
                            conversations = LocalChatStorage.loadConversations()
                            activeConversationId = newConv.id
                            refreshMessages(newConv.id)
                            showConversationsDrawer = false
                        },
                        accent = true,
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        val isActive = conv.id == activeConversationId
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                activeConversationId = conv.id
                                LocalChatStorage.setActiveConversationId(conv.id)
                                refreshMessages(conv.id)
                                showConversationsDrawer = false
                            },
                            contentPadding = 12.dp,
                            intensity = if (isActive) GlassIntensity.Prominent else GlassIntensity.Standard,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isActive) AuroraTeal else Color.Transparent),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        conv.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium),
                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    conv.lastMessagePreview?.let { preview ->
                                        Text(
                                            preview,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                if (conversations.size > 1) {
                                    GlassIconButton(
                                        icon = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete conversation",
                                        onClick = {
                                            conversationToDelete = conv
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Conversation Confirmation Dialog
    conversationToDelete?.let { conv ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete \"${conv.title}\"? All its messages will be removed.") },
            confirmButton = {
                GlassButton(
                    label = "Delete",
                    onClick = {
                        LocalChatStorage.deleteConversation(conv.id)
                        conversations = LocalChatStorage.loadConversations()
                        activeConversationId = LocalChatStorage.getActiveConversationId()
                        refreshMessages(activeConversationId)
                        conversationToDelete = null
                    },
                    accent = true,
                )
            },
            dismissButton = {
                GlassButton(
                    label = "Cancel",
                    onClick = { conversationToDelete = null },
                )
            },
        )
    }
}

private sealed class ChatItem(val id: String) {
    data class Message(val message: LocalChatMessage) : ChatItem(message.id)
    data class Tool(val event: ToolEventDisplay) : ChatItem(event.id)
}

data class ToolEventDisplay(
    val id: String,
    val toolName: String,
    val status: ToolStatus,
    val message: String,
    val timestamp: Long,
    val navigationTarget: String? = null,
    val navigateTripId: String? = null,
)

enum class ToolStatus { RUNNING, SUCCESS, ERROR }

@Composable
private fun ToolEventCard(
    event: ToolEventDisplay,
    onNavigate: (screen: String, tripId: String?) -> Unit,
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        intensity = GlassIntensity.Standard,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (event.status) {
                ToolStatus.RUNNING -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = AuroraTeal,
                )
                ToolStatus.SUCCESS -> Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = AuroraTeal,
                    modifier = Modifier.size(16.dp),
                )
                ToolStatus.ERROR -> Text(
                    "!",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    toolDisplayName(event.toolName),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    event.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            event.navigationTarget?.let { target ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .glass(RoundedCornerShape(8.dp), GlassIntensity.Standard)
                        .clickable { onNavigate(target, event.navigateTripId) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Link, null, tint = AuroraTeal, modifier = Modifier.size(12.dp))
                        Text("View", style = MaterialTheme.typography.labelSmall, color = AuroraTeal)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator(currentToolName: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
                .glass(RoundedCornerShape(20.dp), GlassIntensity.Standard)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                "Travel Copilot",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                currentToolName?.let { "Using ${toolDisplayName(it)}..." } ?: "Thinking...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChatBubble(message: LocalChatMessage) {
    val isAi = message.senderId == "ai"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isAi) 6.dp else 20.dp,
                        bottomEnd = if (isAi) 20.dp else 6.dp,
                    )
                )
                .then(
                    if (isAi) {
                        Modifier.glass(RoundedCornerShape(20.dp), GlassIntensity.Standard)
                    } else {
                        Modifier.background(Brush.linearGradient(listOf(AuroraTeal.copy(alpha = 0.85f), AuroraBlue.copy(alpha = 0.85f))))
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isAi) {
                Text(
                    message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isAi) MaterialTheme.colorScheme.onSurface else Color.White,
            )
        }
    }
}

private fun toolDisplayName(name: String): String = when (name) {
    "weather_forecast" -> "Checking Weather"
    "seasonal_recommendations" -> "Seasonal Advice"
    "travel_advisory" -> "Travel Advisory"
    "local_events" -> "Finding Events"
    "itinerary_templates" -> "Loading Itinerary"
    "compare_destinations" -> "Comparing Places"
    "search_destinations" -> "Searching Catalog"
    "create_trip" -> "Creating Trip"
    "update_trip" -> "Updating Trip"
    "delete_trip" -> "Deleting Trip"
    "add_place_to_itinerary" -> "Adding Place"
    "remove_place_from_itinerary" -> "Removing Place"
    "navigate_to" -> "Navigating"
    "show_trip" -> "Opening Trip"
    "get_my_trips" -> "Loading Trips"
    "get_trip_detail" -> "Loading Details"
    "get_my_profile" -> "Loading Profile"
    else -> name.replace("_", " ").replaceFirstChar { it.uppercase() }
}

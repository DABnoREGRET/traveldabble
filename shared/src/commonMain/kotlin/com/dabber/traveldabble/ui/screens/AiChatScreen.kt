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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.AVAILABLE_AI_MODELS
import com.dabber.traveldabble.data.AiModelOption
import com.dabber.traveldabble.data.AiResult
import com.dabber.traveldabble.data.AiService
import com.dabber.traveldabble.data.AuthState
import com.dabber.traveldabble.data.LocalChatStorage
import com.dabber.traveldabble.data.SettingsState
import com.dabber.traveldabble.data.ToolExecutionEvent
import com.dabber.traveldabble.data.ToolResult
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.bounceClick
import com.dabber.traveldabble.ui.glass.GlassIntensity
import com.dabber.traveldabble.ui.glass.glass
import com.dabber.traveldabble.model.LocalChatMessage
import com.dabber.traveldabble.ui.theme.AuroraBlue
import com.dabber.traveldabble.ui.theme.AuroraGold
import com.dabber.traveldabble.ui.theme.AuroraTeal
import com.dabber.traveldabble.ui.theme.AuroraViolet
import kotlinx.coroutines.launch

/**
 * AI Chat screen with tool-calling support.
 * The AI can search destinations, manage trips, and navigate the app.
 */
@Composable
fun AiChatScreen(
    tripId: String = "general",
    onNavigate: (screen: String, tripId: String?) -> Unit = { _, _ -> },
) {
    val chatSuggestions = listOf(
        "Plan a trip to Tokyo",
        "Show my trips",
        "Find beaches in Vietnam",
        "Create a 3-day trip to Da Nang",
        "What should I do in Hanoi?",
    )

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Load messages from local storage
    val messages = remember {
        mutableStateListOf<LocalChatMessage>().apply {
            addAll(LocalChatStorage.loadMessages(tripId))
        }
    }

    var input by remember { mutableStateOf("") }
    var counter by remember { mutableStateOf(messages.size) }
    var isLoading by remember { mutableStateOf(false) }
    var showApiKeyPrompt by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }
    var aiStatus by remember { mutableStateOf<String?>(null) }
    var availableModels by remember { mutableStateOf(AVAILABLE_AI_MODELS) }

    // Tool execution tracking
    val toolEvents = remember { mutableStateListOf<ToolEventDisplay>() }
    var currentToolName by remember { mutableStateOf<String?>(null) }

    // Check AI health and fetch live models on mount
    LaunchedEffect(Unit) {
        val health = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            AiService.checkHealth()
        }
        aiStatus = when {
            health.serverKeyConfigured -> null
            AuthState.openRouterApiKey != null -> null
            else -> "No AI key configured. Add your OpenRouter key in Profile settings."
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AuroraTeal, AuroraViolet))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Travel Copilot", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                val activeModelName = AVAILABLE_AI_MODELS.find { it.id == AuthState.selectedAiModel }?.name ?: "Gemma 4 26B (Free)"
                val modeText = when {
                    AuthState.openRouterApiKey != null -> "BYOK • $activeModelName"
                    else -> "Server AI • $activeModelName"
                }
                Text(modeText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            // BYOK key button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .glass(RoundedCornerShape(16.dp), GlassIntensity.Standard)
                    .clickable { showApiKeyPrompt = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Key,
                    contentDescription = "API Key",
                    tint = if (AuthState.openRouterApiKey != null) AuroraTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // AI status banner
        aiStatus?.let { status ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Key, null, tint = AuroraTeal, modifier = Modifier.size(14.dp))
                    Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Messages + tool events interleaved
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Interleave messages and tool events
            val allItems = buildList {
                var msgIdx = 0
                var toolIdx = 0
                // Simple interleaving: messages first, then tools after each message batch
                while (msgIdx < messages.size || toolIdx < toolEvents.size) {
                    if (msgIdx < messages.size) {
                        add(ConversationItem.Message(messages[msgIdx]))
                        msgIdx++
                    }
                    // Add any tool events that belong after this message
                    while (toolIdx < toolEvents.size) {
                        add(ConversationItem.ToolEvent(toolEvents[toolIdx]))
                        toolIdx++
                    }
                }
            }

            items(allItems) { item ->
                when (item) {
                    is ConversationItem.Message -> ChatBubble(item.message)
                    is ConversationItem.ToolEvent -> ToolEventCard(item.event)
                }
            }

            if (isLoading) {
                item {
                    TypingIndicator(currentToolName)
                }
            }
        }

        // Suggestions
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chatSuggestions.forEach { suggestion ->
                GlassChip(
                    label = suggestion,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { input = suggestion },
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 110.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .glass(RoundedCornerShape(24.dp), GlassIntensity.Standard)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
            ) {
                if (input.isEmpty()) {
                    Text("Ask anything...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f))
                }
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .bounceClick(pressedScale = 0.90f) {
                        val userText = input.trim()
                        if (userText.isBlank() || isLoading) return@bounceClick

                        // Save user message locally
                        counter++
                        val userMessage = LocalChatMessage(
                            id = "u$counter",
                            tripId = tripId,
                            senderId = AuthState.currentUser?.id ?: "guest",
                            senderName = AuthState.currentUser?.displayName ?: "Guest",
                            text = userText,
                            timestamp = System.currentTimeMillis(),
                        )
                        LocalChatStorage.saveMessage(userMessage)
                        messages.add(userMessage)
                        input = ""
                        isLoading = true
                        toolEvents.clear()
                        currentToolName = null

                        // Send to AI service with tool execution callback
                        scope.launch {
                            try {
                                val result = AiService.sendMessage(
                                    tripId = tripId,
                                    userMessage = userText,
                                    byokKey = AuthState.openRouterApiKey,
                                    onToolExecuted = { event ->
                                        when (event) {
                                            is ToolExecutionEvent.Started -> {
                                                currentToolName = event.toolName
                                                toolEvents.add(ToolEventDisplay(
                                                    toolName = event.toolName,
                                                    status = ToolEventStatus.Running,
                                                    args = event.args?.toString()?.take(100),
                                                ))
                                            }
                                            is ToolExecutionEvent.Completed -> {
                                                currentToolName = null
                                                val lastIdx = toolEvents.lastIndex
                                                if (lastIdx >= 0) {
                                                    toolEvents[lastIdx] = toolEvents[lastIdx].copy(
                                                        status = when (event.result) {
                                                            is ToolResult.Success -> ToolEventStatus.Success
                                                            is ToolResult.Error -> ToolEventStatus.Error
                                                        },
                                                        message = event.result.message,
                                                    )
                                                }
                                                // Handle navigation
                                                if (event.result is ToolResult.Success) {
                                                    val navigateTo = event.result.navigateTo
                                                    val navigateTripId = event.result.navigateTripId
                                                    if (navigateTo != null) {
                                                        onNavigate(navigateTo, navigateTripId)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                                isLoading = false
                                currentToolName = null
                                counter++
                                val aiMessage = LocalChatMessage(
                                    id = "a$counter",
                                    tripId = tripId,
                                    senderId = "ai",
                                    senderName = "Travel Copilot",
                                    text = when (result) {
                                        is AiResult.Success -> result.content
                                        is AiResult.Error -> "Sorry, I couldn't process that: ${result.message}"
                                    },
                                    timestamp = System.currentTimeMillis(),
                                )
                                LocalChatStorage.saveMessage(aiMessage)
                                messages.add(aiMessage)
                            } catch (e: Exception) {
                                isLoading = false
                                currentToolName = null
                                counter++
                                val errMessage = LocalChatMessage(
                                    id = "a$counter",
                                    tripId = tripId,
                                    senderId = "ai",
                                    senderName = "Travel Copilot",
                                    text = "Sorry, I encountered an issue: ${e.message ?: "Connection error"}. Please check your connection and try again.",
                                    timestamp = System.currentTimeMillis(),
                                )
                                LocalChatStorage.saveMessage(errMessage)
                                messages.add(errMessage)
                            }
                        }
                    }
                    .clip(CircleShape)
                    .background(
                        if (isLoading) Brush.linearGradient(listOf(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
                        else Brush.linearGradient(listOf(AuroraTeal, AuroraBlue))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    // API Key prompt dialog
    if (showApiKeyPrompt) {
        ApiKeyDialog(
            currentKey = AuthState.openRouterApiKey,
            currentModel = AuthState.selectedAiModel,
            models = availableModels,
            onDismiss = { showApiKeyPrompt = false },
            onSave = { key, model ->
                SettingsState.updateOpenRouterApiKey(key)
                SettingsState.updateSelectedAiModel(model)
                showApiKeyPrompt = false
                aiStatus = if (key.isNullOrBlank()) {
                    scope.launch {
                        val health = AiService.checkHealth()
                        if (!health.serverKeyConfigured) {
                            aiStatus = "No AI key configured. Add your OpenRouter key in Profile settings."
                        } else {
                            aiStatus = null
                        }
                    }
                    null
                } else null
            },
        )
    }
}

// --- Conversation item types for interleaving ---

private sealed class ConversationItem {
    data class Message(val message: LocalChatMessage) : ConversationItem()
    data class ToolEvent(val event: ToolEventDisplay) : ConversationItem()
}

// --- Tool event display ---

private data class ToolEventDisplay(
    val toolName: String,
    val status: ToolEventStatus,
    val message: String? = null,
    val args: String? = null,
)

private enum class ToolEventStatus { Running, Success, Error }

private fun toolDisplayName(toolName: String): String = when (toolName) {
    "get_my_trips" -> "Loading your trips"
    "get_trip_detail" -> "Loading trip details"
    "create_trip" -> "Creating trip"
    "update_trip" -> "Updating trip"
    "delete_trip" -> "Deleting trip"
    "add_place_to_itinerary" -> "Adding to itinerary"
    "remove_place_from_itinerary" -> "Removing from itinerary"
    "navigate_to" -> "Navigating"
    "show_trip" -> "Opening trip"
    "get_my_profile" -> "Loading profile"
    "search_destinations" -> "Searching destinations"
    else -> toolName.replace("_", " ")
}

// --- UI Components ---

@Composable
private fun ToolEventCard(event: ToolEventDisplay) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A2332).copy(alpha = 0.6f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (event.status) {
                ToolEventStatus.Running -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = AuroraTeal,
                    )
                }
                ToolEventStatus.Success -> {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = AuroraGold,
                        modifier = Modifier.size(14.dp),
                    )
                }
                ToolEventStatus.Error -> {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Column {
                Text(
                    toolDisplayName(event.toolName),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                event.message?.let { msg ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (event.status == ToolEventStatus.Error) Color(0xFFFF6B6B) else AuroraGold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeyDialog(
    currentKey: String?,
    currentModel: String,
    models: List<AiModelOption> = AVAILABLE_AI_MODELS,
    onDismiss: () -> Unit,
    onSave: (String?, String) -> Unit,
) {
    var keyInput by remember { mutableStateOf(currentKey ?: "") }
    var selectedModel by remember { mutableStateOf(currentModel) }
    var showModelDropdown by remember { mutableStateOf(false) }
    var modelSearchQuery by remember { mutableStateOf("") }

    val filteredModels = remember(models, modelSearchQuery) {
        if (modelSearchQuery.isBlank()) models
        else models.filter {
            it.name.contains(modelSearchQuery, ignoreCase = true) ||
            it.id.contains(modelSearchQuery, ignoreCase = true) ||
            it.description.contains(modelSearchQuery, ignoreCase = true)
        }
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("AI Travel Copilot Configuration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Configure your Bring-Your-Own-Key (BYOK) OpenRouter API key and preferred AI model. Leave the key empty to use the server's default AI.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("OpenRouter API Key", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(12.dp), GlassIntensity.Standard)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                BasicTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (keyInput.isEmpty()) {
                    Text("sk-or-v1-...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }

            Text("AI Model Selection", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            val currentModelObj = models.find { it.id == selectedModel }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(12.dp), GlassIntensity.Standard)
                    .clickable { showModelDropdown = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = currentModelObj?.name ?: selectedModel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (currentModelObj?.isFree == true) {
                                Text(
                                    "FREE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuroraTeal,
                                )
                            }
                        }
                        currentModelObj?.description?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = "Select AI model",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                DropdownMenu(
                    expanded = showModelDropdown,
                    onDismissRequest = { showModelDropdown = false },
                ) {
                    filteredModels.forEach { modelOption ->
                        DropdownMenuItem(
                            text = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            modelOption.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (modelOption.id == selectedModel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (modelOption.isFree) {
                                            Text(
                                                "FREE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AuroraTeal,
                                            )
                                        }
                                    }
                                    Text(
                                        modelOption.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                selectedModel = modelOption.id
                                showModelDropdown = false
                            },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip(label = "Clear Key", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable {
                    keyInput = ""
                    onSave(null, selectedModel)
                })
                GlassChip(label = "Save", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable {
                    onSave(keyInput.takeIf { it.isNotBlank() }, selectedModel)
                })
                GlassChip(label = "Cancel", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { onDismiss() })
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
                        Modifier.background(Brush.linearGradient(listOf(AuroraTeal.copy(alpha = 0.8f), AuroraBlue.copy(alpha = 0.8f))))
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

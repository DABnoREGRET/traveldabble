package com.dabber.traveldabble.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.AiModelOption
import com.dabber.traveldabble.data.AiService
import com.dabber.traveldabble.data.ApiClient
import com.dabber.traveldabble.data.AuthState
import com.dabber.traveldabble.data.DEFAULT_BASE_URL
import com.dabber.traveldabble.data.SettingsState
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassIconButton
import com.dabber.traveldabble.ui.theme.AuroraTeal
import com.dabber.traveldabble.ui.theme.Danger
import kotlinx.coroutines.launch

@Composable
fun AccountSettingsScreen(onBack: () -> Unit) {
    val user = AuthState.currentUser

    // Server connection dialog state — hoisted to composable scope
    var showServerDialog by remember { mutableStateOf(false) }
    var serverInput by remember { mutableStateOf(SettingsState.customServerUrl ?: "") }
    var connectionStatus by remember { mutableStateOf<Boolean?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    // AI BYOK & Model selection dialog state
    var showAiDialog by remember { mutableStateOf(false) }
    var aiKeyInput by remember { mutableStateOf(SettingsState.openRouterApiKey ?: "") }
    var selectedAiModel by remember { mutableStateOf(SettingsState.selectedAiModel) }
    var showModelDropdown by remember { mutableStateOf(false) }
    var availableModels by remember { mutableStateOf<List<AiModelOption>>(AiService.DEFAULT_AI_MODELS) }

    LaunchedEffect(Unit) {
        val liveModels = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            AiService.fetchModels()
        }
        if (liveModels.isNotEmpty()) {
            availableModels = liveModels
        }
    }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassIconButton(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
            Text(
                "Account",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Profile card
            GlassCard(contentPadding = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary,
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            user?.displayName?.firstOrNull()?.uppercase() ?: "G",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            user?.displayName ?: "Guest User",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            user?.email ?: "Local-first storage active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Backend Server Connection
            GlassCard(contentPadding = 14.dp) {
                AccountAction(
                    label = "Backend Server Connection",
                    description = SettingsState.customServerUrl?.let { "Connected to $it" }
                        ?: "Default: $DEFAULT_BASE_URL",
                    onClick = {
                        serverInput = SettingsState.customServerUrl ?: ""
                        connectionStatus = null
                        showServerDialog = true
                    },
                )
            }

            // AI API Key & Model (BYOK)
            GlassCard(contentPadding = 14.dp) {
                val activeModelName = availableModels.find { it.id == SettingsState.selectedAiModel }?.name
                    ?: SettingsState.selectedAiModel.substringAfterLast("/").replace(":free", " (Free)").replace("-", " ")
                AccountAction(
                    label = "AI Travel Copilot & BYOK",
                    description = SettingsState.openRouterApiKey?.let { "Custom Key • $activeModelName" }
                        ?: "Server AI • $activeModelName",
                    onClick = {
                        aiKeyInput = SettingsState.openRouterApiKey ?: ""
                        selectedAiModel = SettingsState.selectedAiModel
                        showAiDialog = true
                    },
                )
            }

            // Danger zone
            Spacer(Modifier.height(8.dp))
            Text(
                "Account Management",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GlassCard(contentPadding = 14.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountAction(
                        label = if (AuthState.isGuestMode) "Clear Local Data" else "Sign Out",
                        description = "Return to guest state or switch user accounts",
                        onClick = { AuthState.onLogout() },
                    )
                }
            }
        }
    }

    // Server Connection Dialog — rendered at composable root, not inside Column content lambda
    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("Backend Server URL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Configure the Ktor backend server endpoint (e.g. Render, Railway, or VPS).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = serverInput,
                        onValueChange = { v ->
                            serverInput = v
                            connectionStatus = null
                        },
                        label = { Text("Server URL") },
                        placeholder = { Text("https://your-service.onrender.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (connectionStatus != null) {
                        Text(
                            if (connectionStatus == true) "✓ Connected successfully (/health responded ok)"
                            else "✗ Could not connect to server /health endpoint",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (connectionStatus == true) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        SettingsState.updateCustomServerUrl(serverInput)
                        showServerDialog = false
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            serverInput = ""
                            SettingsState.updateCustomServerUrl(null)
                            showServerDialog = false
                        },
                    ) {
                        Text("Reset")
                    }
                    TextButton(
                        enabled = !isTesting,
                        onClick = {
                            isTesting = true
                            coroutineScope.launch {
                                connectionStatus = ApiClient.testConnection(serverInput)
                                isTesting = false
                            }
                        },
                    ) {
                        Text(if (isTesting) "Testing..." else "Test")
                    }
                }
            },
        )
    }

    // AI BYOK & Model Dialog
    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("AI Copilot & BYOK") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter your OpenRouter API key to use your own AI credits, or leave it blank to use the server's AI. Choose your preferred AI model below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = aiKeyInput,
                        onValueChange = { aiKeyInput = it },
                        label = { Text("OpenRouter API Key") },
                        placeholder = { Text("sk-or-v1-...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        "AI Model Selection",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    val currentModelObj = availableModels.find { it.id == selectedAiModel }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showModelDropdown = true }
                            .padding(vertical = 4.dp),
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
                                        text = currentModelObj?.name ?: selectedAiModel,
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
                            availableModels.forEach { modelOption ->
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
                                                    color = if (modelOption.id == selectedAiModel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
                                        selectedAiModel = modelOption.id
                                        showModelDropdown = false
                                    },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        SettingsState.updateOpenRouterApiKey(aiKeyInput.takeIf { it.isNotBlank() })
                        SettingsState.updateSelectedAiModel(selectedAiModel)
                        showAiDialog = false
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            aiKeyInput = ""
                            SettingsState.updateOpenRouterApiKey(null)
                            SettingsState.updateSelectedAiModel("google/gemma-4-26b-a4b-it:free")
                            showAiDialog = false
                        },
                    ) {
                        Text("Reset Key")
                    }
                    TextButton(onClick = { showAiDialog = false }) {
                        Text("Cancel")
                    }
                }
            },
        )
    }
}

@Composable
private fun AccountAction(label: String, description: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            description?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

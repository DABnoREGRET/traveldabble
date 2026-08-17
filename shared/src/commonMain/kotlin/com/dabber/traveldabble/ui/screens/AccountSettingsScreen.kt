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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.dabber.traveldabble.data.ApiClient
import com.dabber.traveldabble.data.AuthState
import com.dabber.traveldabble.data.DEFAULT_BASE_URL
import com.dabber.traveldabble.data.SettingsState
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassIconButton
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

            // AI API Key
            GlassCard(contentPadding = 14.dp) {
                AccountAction(
                    label = "AI Travel Copilot Key (BYOK)",
                    description = SettingsState.openRouterApiKey?.let { "Custom OpenRouter key configured" }
                        ?: "Using default local / server AI",
                    onClick = { /* Open API key */ },
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

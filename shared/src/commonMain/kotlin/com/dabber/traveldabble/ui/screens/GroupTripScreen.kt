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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.Repository
import com.dabber.traveldabble.model.TripMember
import com.dabber.traveldabble.ui.components.GlassButton
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.GlassIconButton
import com.dabber.traveldabble.ui.glass.GlassIntensity
import com.dabber.traveldabble.ui.glass.glass
import com.dabber.traveldabble.ui.theme.Danger
import kotlinx.coroutines.launch

@Composable
fun GroupTripScreen(
    tripId: String,
    tripTitle: String,
    onBack: () -> Unit,
) {
    var members by remember { mutableStateOf<List<TripMember>>(emptyList()) }
    var inviteCode by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tripId) {
        members = Repository.getTripMembers(tripId)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassIconButton(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Trip Members",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    tripTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlassChip(
                label = "Generate Invite Code",
                selected = true,
                onClick = {
                    scope.launch {
                        val code = Repository.generateInviteCode(tripId)
                        inviteCode = code
                    }
                },
            )
            GlassChip(
                label = "Join Another Trip",
                selected = false,
                onClick = { showJoinDialog = true },
            )
        }

        // Show generated invite code
        inviteCode?.let { code ->
            Spacer(Modifier.height(12.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                intensity = GlassIntensity.Subtle,
                contentPadding = 14.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Invite Code",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            code,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copy code",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                // Code copied
                            },
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Members list
        Text(
            "Members (${members.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Loading members…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (members.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "No members yet. Share your invite code above to collaborate!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(members) { member ->
                    MemberCard(
                        member = member,
                        isOwner = member.role == "owner",
                        onRemove = {
                            scope.launch {
                                Repository.removeMember(tripId, member.userId)
                                members = Repository.getTripMembers(tripId)
                            }
                        },
                    )
                }
            }
        }
    }

    // Join dialog
    if (showJoinDialog) {
        JoinTripDialog(
            code = joinInput,
            onCodeChange = { joinInput = it.uppercase().filter { ch -> ch.isLetterOrDigit() } },
            onJoin = {
                if (joinInput.trim().length >= 3) {
                    scope.launch {
                        Repository.joinTrip(joinInput.trim())
                        members = Repository.getTripMembers(tripId)
                        showJoinDialog = false
                        joinInput = ""
                    }
                }
            },
            onDismiss = { showJoinDialog = false },
        )
    }
}

@Composable
private fun MemberCard(
    member: TripMember,
    isOwner: Boolean,
    onRemove: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        intensity = GlassIntensity.Standard,
        contentPadding = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOwner) {
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                        } else {
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary))
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    member.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    member.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            GlassChip(
                label = member.role.replaceFirstChar { it.uppercase() },
                selected = isOwner,
            )

            if (!isOwner) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remove member",
                    tint = Danger.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onRemove() },
                )
            }
        }
    }
}

@Composable
private fun JoinTripDialog(
    code: String,
    onCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
    onDismiss: () -> Unit,
) {
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        GlassCard(
            modifier = Modifier
                .padding(24.dp)
                .clickable { /* consume click */ },
            intensity = GlassIntensity.Prominent,
            contentPadding = 20.dp,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Join Trip",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Enter the 6-character invite code shared by the trip organizer:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Code input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glass(RoundedCornerShape(12.dp), GlassIntensity.Subtle)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        if (code.isEmpty()) {
                            Text(
                                "e.g. VN8821",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        BasicTextField(
                            value = code,
                            onValueChange = {
                                onCodeChange(it)
                                if (error != null) error = null
                            },
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (error != null) {
                        Text(
                            error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Danger,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GlassButton(
                        label = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    GlassButton(
                        label = "Join Trip",
                        onClick = {
                            if (code.trim().length < 3) {
                                error = "Please enter a valid invite code"
                            } else {
                                onJoin()
                            }
                        },
                        accent = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

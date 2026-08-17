package com.dabber.traveldabble.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.ApiClient
import com.dabber.traveldabble.data.ApiError
import com.dabber.traveldabble.data.AuthState
import com.dabber.traveldabble.ui.components.GlassButton
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.TropicalLogo
import com.dabber.traveldabble.ui.glass.GlassIntensity
import com.dabber.traveldabble.ui.glass.glass
import com.dabber.traveldabble.ui.theme.Danger
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
) {
    var isRegister by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var displayNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }

    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun validate(): Boolean {
        var isValid = true

        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (email.trim().isEmpty()) {
            emailError = "Email address is required"
            isValid = false
        } else if (!email.trim().matches(emailRegex)) {
            emailError = "Enter a valid email address (e.g. name@example.com)"
            isValid = false
        } else {
            emailError = null
        }

        if (password.isEmpty()) {
            passwordError = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordError = null
        }

        if (isRegister) {
            if (username.trim().length < 3) {
                usernameError = "Username must be at least 3 characters"
                isValid = false
            } else {
                usernameError = null
            }

            if (displayName.trim().length < 2) {
                displayNameError = "Please enter your name"
                isValid = false
            } else {
                displayNameError = null
            }
        }

        return isValid
    }

    fun submit() {
        if (!validate() || loading) return
        generalError = null
        loading = true
        scope.launch {
            try {
                val response = if (isRegister) {
                    ApiClient.register(username.trim(), email.trim(), password, displayName.trim())
                } else {
                    ApiClient.login(email.trim(), password)
                }
                AuthState.onLoginSuccess(response)
                onSuccess()
            } catch (e: ResponseException) {
                generalError = runCatching { e.response.body<ApiError>().error }
                    .getOrDefault(e.message ?: "Authentication failed")
            } catch (e: Exception) {
                generalError = e.message ?: "Could not connect to server. You can continue using local mode."
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TropicalLogo(size = 52.dp, showBackground = true)
            Column {
                Text(
                    "Travel Dabble",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Local-first by default",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            "Sign in optionally to sync trips across devices and collaborate.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            if (isRegister) "Create your account" else "Sign in to account",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassChip(
                label = "Sign In",
                selected = !isRegister,
                onClick = {
                    isRegister = false
                    generalError = null
                },
            )
            GlassChip(
                label = "Register",
                selected = isRegister,
                onClick = {
                    isRegister = true
                    generalError = null
                },
            )
        }

        if (isRegister) {
            AuthTextField(
                value = username,
                onValueChange = {
                    username = it
                    if (usernameError != null) usernameError = null
                },
                label = "Username *",
                placeholder = "traveler_vn",
                icon = Icons.Filled.Person,
                errorMessage = usernameError,
            )
            AuthTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                    if (displayNameError != null) displayNameError = null
                },
                label = "Display name *",
                placeholder = "Alex Nguyen",
                icon = Icons.Filled.Person,
                errorMessage = displayNameError,
            )
        }

        AuthTextField(
            value = email,
            onValueChange = {
                email = it
                if (emailError != null) emailError = null
            },
            label = "Email address *",
            placeholder = "you@example.com",
            icon = Icons.Filled.Email,
            errorMessage = emailError,
        )

        AuthTextField(
            value = password,
            onValueChange = {
                password = it
                if (passwordError != null) passwordError = null
            },
            label = "Password *",
            placeholder = "At least 6 characters",
            icon = Icons.Filled.Lock,
            isPassword = true,
            showPassword = showPassword,
            onTogglePasswordVisibility = { showPassword = !showPassword },
            errorMessage = passwordError,
        )

        generalError?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = Danger,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        GlassButton(
            label = if (loading) "Processing…" else if (isRegister) "Create account" else "Sign in",
            onClick = ::submit,
            accent = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Skip / Continue in Local Mode",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { onSuccess() }
                .padding(12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    errorMessage: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (errorMessage != null) Danger else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glass(
                    shape = RoundedCornerShape(16.dp),
                    intensity = GlassIntensity.Standard,
                    tint = if (errorMessage != null) Danger.copy(alpha = 0.08f) else Color.Transparent,
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (errorMessage != null) Danger else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (isPassword && onTogglePasswordVisibility != null) {
                IconButton(
                    onClick = onTogglePasswordVisibility,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = "Toggle password visibility",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        if (errorMessage != null) {
            Text(
                errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Danger,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

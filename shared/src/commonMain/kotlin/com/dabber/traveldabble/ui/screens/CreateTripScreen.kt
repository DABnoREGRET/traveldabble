package com.dabber.traveldabble.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.Repository
import com.dabber.traveldabble.ui.components.GlassButton
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.GlassIconButton
import com.dabber.traveldabble.ui.glass.GlassIntensity
import com.dabber.traveldabble.ui.glass.glass
import com.dabber.traveldabble.ui.theme.Danger
import kotlinx.coroutines.launch

@Composable
fun CreateTripScreen(onBack: () -> Unit, onCreated: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Vietnam") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var travelers by remember { mutableStateOf("2") }

    var titleError by remember { mutableStateOf<String?>(null) }
    var destinationError by remember { mutableStateOf<String?>(null) }
    var countryError by remember { mutableStateOf<String?>(null) }
    var startDateError by remember { mutableStateOf<String?>(null) }
    var endDateError by remember { mutableStateOf<String?>(null) }
    var travelersError by remember { mutableStateOf<String?>(null) }

    var submitting by remember { mutableStateOf(false) }
    var generalError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val quickDestinations = listOf(
        "Hanoi & Ha Long" to "Hanoi & Ha Long Discovery",
        "Hoi An & Da Nang" to "Central Vietnam Coast",
        "Ha Giang" to "Ha Giang Loop Adventure",
        "Ho Chi Minh City" to "Saigon & Mekong Delta",
        "Ninh Binh" to "Ninh Binh Karsts",
        "Phong Nha" to "Phong Nha Cave Expedition",
    )

    fun validate(): Boolean {
        var isValid = true

        if (title.trim().length < 2) {
            titleError = "Trip title must be at least 2 characters"
            isValid = false
        } else {
            titleError = null
        }

        if (destination.trim().length < 2) {
            destinationError = "Please enter a destination"
            isValid = false
        } else {
            destinationError = null
        }

        if (country.trim().isEmpty()) {
            countryError = "Please enter a country"
            isValid = false
        } else {
            countryError = null
        }

        if (startDate.trim().isEmpty()) {
            startDateError = "Start date is required (e.g. Oct 15)"
            isValid = false
        } else {
            startDateError = null
        }

        if (endDate.trim().isEmpty()) {
            endDateError = "End date is required (e.g. Oct 22)"
            isValid = false
        } else {
            endDateError = null
        }

        val travelerCount = travelers.toIntOrNull()
        if (travelerCount == null || travelerCount < 1 || travelerCount > 99) {
            travelersError = "Enter 1 to 99 travelers"
            isValid = false
        } else {
            travelersError = null
        }

        return isValid
    }

    fun submit() {
        if (!validate() || submitting) return

        generalError = null
        submitting = true
        scope.launch {
            val created = Repository.createTrip(
                title = title.trim(),
                destination = destination.trim(),
                country = country.trim(),
                startDate = startDate.trim(),
                endDate = endDate.trim(),
                travelers = travelers.toIntOrNull() ?: 1,
            )
            submitting = false
            if (created != null) {
                onCreated()
            } else {
                generalError = "Could not create trip. Please check your inputs and try again."
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
                Text(
                    "New trip",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Quick suggestions
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Popular Destinations",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        quickDestinations.forEach { (dest, defaultTitle) ->
                            GlassChip(
                                label = dest,
                                selected = destination == dest,
                                onClick = {
                                    destination = dest
                                    if (title.isEmpty() || quickDestinations.any { it.second == title }) {
                                        title = defaultTitle
                                    }
                                    country = "Vietnam"
                                    destinationError = null
                                    titleError = null
                                },
                            )
                        }
                    }
                }

                GlassTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (titleError != null) titleError = null
                    },
                    label = "Trip title *",
                    placeholder = "e.g. Hanoi & Ha Long Explorer",
                    errorMessage = titleError,
                )

                GlassTextField(
                    value = destination,
                    onValueChange = {
                        destination = it
                        if (destinationError != null) destinationError = null
                    },
                    label = "Destination *",
                    placeholder = "City or region (e.g. Da Nang)",
                    errorMessage = destinationError,
                )

                GlassTextField(
                    value = country,
                    onValueChange = {
                        country = it
                        if (countryError != null) countryError = null
                    },
                    label = "Country *",
                    placeholder = "Vietnam",
                    errorMessage = countryError,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassTextField(
                        value = startDate,
                        onValueChange = {
                            startDate = it
                            if (startDateError != null) startDateError = null
                        },
                        label = "Start date *",
                        placeholder = "Oct 15",
                        errorMessage = startDateError,
                        modifier = Modifier.weight(1f),
                    )
                    GlassTextField(
                        value = endDate,
                        onValueChange = {
                            endDate = it
                            if (endDateError != null) endDateError = null
                        },
                        label = "End date *",
                        placeholder = "Oct 22",
                        errorMessage = endDateError,
                        modifier = Modifier.weight(1f),
                    )
                }

                GlassTextField(
                    value = travelers,
                    onValueChange = {
                        travelers = it.filter(Char::isDigit)
                        if (travelersError != null) travelersError = null
                    },
                    label = "Number of travelers *",
                    placeholder = "2",
                    errorMessage = travelersError,
                )

                generalError?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Danger,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassButton(
                        label = if (submitting) "Creating…" else "Create trip",
                        onClick = ::submit,
                        accent = true,
                        modifier = Modifier.weight(1f),
                    )
                    GlassButton(
                        label = "Cancel",
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (errorMessage != null) Danger else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glass(
                    shape = RoundedCornerShape(16.dp),
                    intensity = GlassIntensity.Standard,
                    tint = if (errorMessage != null) Danger.copy(alpha = 0.08f) else Color.Transparent,
                )
                .padding(horizontal = 16.dp, vertical = 13.dp),
        ) {
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
                modifier = Modifier.fillMaxWidth(),
            )
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

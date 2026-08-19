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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.dabber.traveldabble.data.ApiClient
import com.dabber.traveldabble.data.Repository
import com.dabber.traveldabble.ui.components.GlassButton
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.GlassIconButton
import com.dabber.traveldabble.ui.glass.GlassIntensity
import com.dabber.traveldabble.ui.glass.glass
import com.dabber.traveldabble.ui.theme.Danger
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
internal object FutureOrPresentSelectableDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val todayEpochDays = Clock.System.now().toLocalDateTime(TimeZone.UTC).date.toEpochDays()
        val pickedEpochDays = Instant.fromEpochMilliseconds(utcTimeMillis).toLocalDateTime(TimeZone.UTC).date.toEpochDays()
        return pickedEpochDays >= todayEpochDays
    }

    override fun isSelectableYear(year: Int): Boolean {
        val currentYear = Clock.System.now().toLocalDateTime(TimeZone.UTC).year
        return year >= currentYear
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal class MinDateSelectableDates(private val minEpochDays: Int) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val pickedEpochDays = Instant.fromEpochMilliseconds(utcTimeMillis).toLocalDateTime(TimeZone.UTC).date.toEpochDays()
        return pickedEpochDays >= minEpochDays
    }

    override fun isSelectableYear(year: Int): Boolean {
        val currentYear = Clock.System.now().toLocalDateTime(TimeZone.UTC).year
        return year >= currentYear
    }
}

private fun formatDateFromMillis(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val localDate = instant.toLocalDateTime(TimeZone.UTC).date
    val monthName = when (localDate.monthNumber) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> ""
    }
    return "$monthName ${localDate.dayOfMonth}, ${localDate.year}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(onBack: () -> Unit, onCreated: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Vietnam") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var travelers by remember { mutableStateOf("2") }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

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

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val startParsed = Repository.parseDateStringToLocalDate(startDate)
        if (startDate.trim().isEmpty()) {
            startDateError = "Please select a start date"
            isValid = false
        } else if (startParsed != null && startParsed < today) {
            startDateError = "Start date cannot be in the past"
            isValid = false
        } else {
            startDateError = null
        }

        val endParsed = Repository.parseDateStringToLocalDate(endDate)
        if (endDate.trim().isEmpty()) {
            endDateError = "Please select an end date"
            isValid = false
        } else if (startParsed != null && endParsed != null && endParsed < startParsed) {
            endDateError = "End date must be on or after start date"
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
                onCreated(created.id)
            } else {
                generalError = "Could not create trip. Please check your network or server connection."
            }
        }
    }

    // Material 3 Date Picker Dialog for Start Date (Future & Present only)
    if (showStartDatePicker) {
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = nowMillis,
            selectableDates = FutureOrPresentSelectableDates,
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            startDate = formatDateFromMillis(it)
                            startDateError = null
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material 3 Date Picker Dialog for End Date (>= Start Date)
    if (showEndDatePicker) {
        val startLocalDate = Repository.parseDateStringToLocalDate(startDate)
        val nowLocalDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val minEpochDays = startLocalDate?.toEpochDays() ?: nowLocalDate.toEpochDays()
        val initialEndMillis = startLocalDate?.let {
            Instant.fromEpochMilliseconds(it.toEpochDays() * 24L * 60 * 60 * 1000).toEpochMilliseconds() + (3L * 24 * 60 * 60 * 1000)
        } ?: (Clock.System.now().toEpochMilliseconds() + (7L * 24 * 60 * 60 * 1000))

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEndMillis,
            selectableDates = MinDateSelectableDates(minEpochDays),
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            endDate = formatDateFromMillis(it)
                            endDateError = null
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
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

                // Date Selection with Material Date Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DateSelectorField(
                        value = startDate,
                        label = "Start date *",
                        placeholder = "Select date",
                        errorMessage = startDateError,
                        modifier = Modifier.weight(1f),
                        onClick = { showStartDatePicker = true },
                    )
                    DateSelectorField(
                        value = endDate,
                        label = "End date *",
                        placeholder = "Select date",
                        errorMessage = endDateError,
                        modifier = Modifier.weight(1f),
                        onClick = { showEndDatePicker = true },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
private fun DateSelectorField(
    value: String,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    onClick: () -> Unit,
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
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = value.ifEmpty { placeholder },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = "Pick Date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
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

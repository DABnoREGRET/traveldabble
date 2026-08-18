package com.dabber.traveldabble.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.dabber.traveldabble.data.Repository
import com.dabber.traveldabble.model.Expense
import com.dabber.traveldabble.ui.components.GlassButton
import com.dabber.traveldabble.ui.components.GlassCard
import com.dabber.traveldabble.ui.components.GlassChip
import com.dabber.traveldabble.ui.components.GlassIconButton
import com.dabber.traveldabble.ui.components.ProgressTrack
import com.dabber.traveldabble.ui.glass.GlassIntensity
import com.dabber.traveldabble.ui.mock.Trip
import com.dabber.traveldabble.ui.mock.toUi
import com.dabber.traveldabble.ui.theme.AuroraTeal
import com.dabber.traveldabble.ui.theme.Danger
import com.dabber.traveldabble.ui.theme.JadeGreen
import com.dabber.traveldabble.ui.theme.LotusRed
import com.dabber.traveldabble.ui.theme.MekongOrange
import com.dabber.traveldabble.ui.theme.SilkViolet
import com.dabber.traveldabble.ui.theme.TempleGold
import kotlinx.coroutines.launch

private val categoryColors = listOf(JadeGreen, MekongOrange, TempleGold, SilkViolet, LotusRed)

@Composable
fun BudgetScreen(tripId: String, onBack: () -> Unit) {
    var trip by remember { mutableStateOf<Trip?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var expenseTitle by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf("Food") }
    var expenseDate by remember { mutableStateOf("Today") }
    var budgetTargetInput by remember { mutableStateOf("") }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    val scope = rememberCoroutineScope()

    fun loadTripData() {
        scope.launch {
            trip = Repository.getTrip(tripId)?.toUi()
        }
    }

    LaunchedEffect(tripId) {
        trip = Repository.getTrip(tripId)?.toUi()
    }

    val loadedTrip = trip

    if (loadedTrip == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Loading trip budget…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val budget = loadedTrip.budget
    val totalSpent = budget.spent

    val displayCategories = remember(budget) {
        if (budget.categories.isNotEmpty()) {
            budget.categories
        } else if (budget.expenses.isNotEmpty()) {
            budget.expenses.groupBy { it.category }
                .map { (cat, exps) -> cat to exps.sumOf { it.amount } }
        } else {
            listOf(
                "Lodging" to budget.total * 0.4,
                "Food" to budget.total * 0.25,
                "Transport" to budget.total * 0.2,
                "Activities" to budget.total * 0.15
            )
        }
    }

    if (showEditBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showEditBudgetDialog = false },
            title = { Text("Set Total Budget Target") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Set your total spending budget for ${loadedTrip.title}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = budgetTargetInput,
                        onValueChange = { budgetTargetInput = it },
                        label = { Text("Total Budget (USD)") },
                        placeholder = { Text("1500.0") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTarget = budgetTargetInput.toDoubleOrNull() ?: 0.0
                        if (newTarget > 0.0) {
                            scope.launch {
                                Repository.updateTripBudget(tripId, newTarget)
                                loadTripData()
                            }
                            showEditBudgetDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (expenseToDelete != null) {
        val exp = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense?") },
            text = {
                Text(
                    "Remove '${exp.title}' (${exp.amount.toInt()} USD) from your trip budget?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            Repository.removeExpenseFromTrip(tripId, exp.id)
                            loadTripData()
                            expenseToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddExpenseDialog) {
        val categories = listOf("Food", "Lodging", "Transport", "Activities", "Other")
        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("Log Expense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = expenseTitle,
                        onValueChange = { expenseTitle = it },
                        label = { Text("Description") },
                        placeholder = { Text("e.g. Pho dinner in Old Quarter") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = expenseAmount,
                        onValueChange = { expenseAmount = it },
                        label = { Text("Amount (USD)") },
                        placeholder = { Text("25.0") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = expenseDate,
                        onValueChange = { expenseDate = it },
                        label = { Text("Date") },
                        placeholder = { Text("e.g. Today or Oct 15") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        categories.take(3).forEach { cat ->
                            GlassChip(
                                label = cat,
                                selected = expenseCategory == cat,
                                onClick = { expenseCategory = cat },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        categories.drop(3).forEach { cat ->
                            GlassChip(
                                label = cat,
                                selected = expenseCategory == cat,
                                onClick = { expenseCategory = cat },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = expenseAmount.toDoubleOrNull() ?: 0.0
                        if (expenseTitle.isNotBlank() && amount > 0.0) {
                            scope.launch {
                                Repository.addExpenseToTrip(
                                    tripId = tripId,
                                    title = expenseTitle.trim(),
                                    category = expenseCategory,
                                    amount = amount,
                                    date = expenseDate.trim().ifBlank { "Today" },
                                )
                                loadTripData()
                            }
                            expenseTitle = ""
                            expenseAmount = ""
                            expenseDate = "Today"
                            showAddExpenseDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
                Column {
                    Text(
                        "Budget & Expenses",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        loadedTrip.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Summary Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                contentPadding = 18.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Total Spent",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${totalSpent.toInt()} USD",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "Total Budget",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            GlassIconButton(
                                icon = Icons.Filled.Edit,
                                contentDescription = "Edit Budget Target",
                                onClick = {
                                    budgetTargetInput = budget.total.toInt().toString()
                                    showEditBudgetDialog = true
                                },
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(
                            "${budget.total.toInt()} USD",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                val fraction = if (budget.total > 0) (totalSpent / budget.total).toFloat().coerceIn(0f, 1f) else 0f
                ProgressTrack(fraction = fraction, color = AuroraTeal)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${((fraction) * 100).toInt()}% of budget used",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val remaining = (budget.total - totalSpent).coerceAtLeast(0.0)
                    Text(
                        "${remaining.toInt()} USD remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Category Breakdown Section
        item {
            Text(
                "Spending by Category",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (displayCategories.isNotEmpty()) {
            val totalCat = displayCategories.sumOf { it.second }
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    contentPadding = 16.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        ) {
                            displayCategories.forEachIndexed { index, (_, amount) ->
                                val weight = if (totalCat > 0) (amount / totalCat).toFloat().coerceAtLeast(0.01f) else 1f
                                Box(
                                    modifier = Modifier
                                        .weight(weight)
                                        .fillMaxHeight()
                                        .background(categoryColors[index % categoryColors.size]),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            displayCategories.forEachIndexed { index, (cat, amount) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(categoryColors[index % categoryColors.size], RoundedCornerShape(2.dp)),
                                    )
                                    Text(
                                        "$cat: ${amount.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(displayCategories.size) { index ->
                val (cat, amount) = displayCategories[index]
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    intensity = GlassIntensity.Subtle,
                    contentPadding = 14.dp,
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                cat,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "${amount.toInt()} USD",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        ProgressTrack(
                            fraction = if (budget.total > 0) ((amount / budget.total) * 2f).toFloat().coerceIn(0f, 1f) else 0f,
                            color = categoryColors[index % categoryColors.size],
                        )
                    }
                }
            }
        }

        // Logged Expenses Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Logged Expenses (${budget.expenses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                GlassButton(
                    label = "+ Add",
                    icon = Icons.Filled.Add,
                    onClick = { showAddExpenseDialog = true },
                    accent = true,
                )
            }
        }

        // Empty state when no expenses logged
        if (budget.expenses.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    intensity = GlassIntensity.Subtle,
                    contentPadding = 20.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "No expenses logged yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Tap \"+ Add\" above to log meals, transit, lodging, or activities.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            items(budget.expenses) { expense ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    intensity = GlassIntensity.Subtle,
                    contentPadding = 14.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                expense.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "${expense.category}  •  ${expense.date}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "${expense.amount.toInt()} USD",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            IconButton(
                                onClick = { expenseToDelete = expense },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete Expense",
                                    tint = Danger.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

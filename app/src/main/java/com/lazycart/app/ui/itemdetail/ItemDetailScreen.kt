package com.lazycart.app.ui.itemdetail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.lazycart.app.domain.model.ConsumptionEvent
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.model.PredictionConfidence
import com.lazycart.app.domain.model.RestockPrediction
import com.lazycart.app.ui.theme.GoldenAmber
import com.lazycart.app.ui.theme.GoldenAmberContainer
import com.lazycart.app.ui.theme.SuccessGreen
import com.lazycart.app.ui.theme.SuccessGreenContainer
import com.lazycart.app.ui.theme.WarningOrange
import com.lazycart.app.ui.theme.WarningOrangeContainer
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Detailed view of a pantry item displaying quantity, category, restock predictions,
 * expiry alerts, and full consumption history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: Long,
    navController: NavHostController,
    onNavigateToEdit: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ItemDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var isLogBottomSheetVisible by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<ConsumptionEvent?>(null) }
    var showMarkFinishedConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        viewModel.setItemId(itemId)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.item?.name ?: "Item Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Item"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        val item = uiState.item

        if (uiState.isLoading && item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Item not found or deleted",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Quantity Section
                item {
                    QuantitySection(
                        item = item,
                        onMarkFinishedClick = { showMarkFinishedConfirmation = true }
                    )
                }

                // 2. Prediction Card
                item {
                    RestockPredictionCard(
                        prediction = uiState.prediction,
                        onLogFirstUsage = { isLogBottomSheetVisible = true }
                    )
                }

                // 3. Add to Shopping List Button
                item {
                    AddToShoppingListButton(
                        isAdded = uiState.isAddedToShoppingList,
                        onAddClick = { viewModel.addToShoppingList() }
                    )
                }

                // 4. Expiry Section (if applicable)
                if (item.expiryDate != null) {
                    item {
                        ExpiryCard(item = item, expiryDate = item.expiryDate)
                    }
                }

                // 5. Consumption History Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Consumption History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalButton(
                            onClick = { isLogBottomSheetVisible = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Log Usage", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // 6. Consumption History List or Empty State
                if (uiState.consumptionHistory.isEmpty()) {
                    item {
                        EmptyConsumptionHistoryState(
                            onLogUsage = { isLogBottomSheetVisible = true }
                        )
                    }
                } else {
                    items(items = uiState.consumptionHistory, key = { it.id }) { event ->
                        ConsumptionEventItem(
                            event = event,
                            onDeleteClick = { eventToDelete = event }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet to Log Consumption
    if (isLogBottomSheetVisible && uiState.item != null) {
        ItemDetailLogBottomSheet(
            item = uiState.item!!,
            onDismiss = { isLogBottomSheetVisible = false },
            onConfirm = { quantity, note ->
                viewModel.logConsumption(
                    quantity = quantity,
                    unit = uiState.item!!.unit,
                    note = note
                )
                isLogBottomSheetVisible = false
            }
        )
    }

    // Confirmation dialog before deleting consumption event
    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(text = "Delete Usage Entry") },
            text = {
                Text(
                    text = "Are you sure you want to remove this log of ${formatQuantity(event.quantityConsumed)} ${event.unit.abbreviation}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEvent(event)
                        eventToDelete = null
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog to mark item as finished
    if (showMarkFinishedConfirmation) {
        AlertDialog(
            onDismissRequest = { showMarkFinishedConfirmation = false },
            title = { Text(text = "Mark as Finished") },
            text = {
                Text(text = "Mark \"${uiState.item?.name}\" as finished? It will be archived from your active pantry.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markAsFinished()
                        showMarkFinishedConfirmation = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkFinishedConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Quantity section card displaying the current stock level and quick 'Mark as Finished' button.
 */
@Composable
private fun QuantitySection(
    item: PantryItem,
    onMarkFinishedClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = item.category.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (item.priceHint > 0f) {
                    Text(
                        text = "₹${formatQuantity(item.priceHint)} / ${item.unit.abbreviation}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatQuantity(item.quantity),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.quantity <= 0f || item.isFinished) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.unit.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!item.isFinished && item.quantity > 0f) {
                    OutlinedButton(
                        onClick = onMarkFinishedClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Mark as finished", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Finished",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Restock prediction card showing confidence badge, restock date, and explanation.
 */
@Composable
private fun RestockPredictionCard(
    prediction: RestockPrediction?,
    onLogFirstUsage: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Restock Prediction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val confidence = prediction?.confidence ?: PredictionConfidence.INSUFFICIENT_DATA
                val (badgeColor, textColor, label) = when (confidence) {
                    PredictionConfidence.HIGH -> Triple(SuccessGreenContainer, SuccessGreen, "HIGH CONFIDENCE")
                    PredictionConfidence.MEDIUM -> Triple(GoldenAmberContainer, GoldenAmber, "MEDIUM CONFIDENCE")
                    PredictionConfidence.LOW -> Triple(WarningOrangeContainer, WarningOrange, "LOW CONFIDENCE")
                    PredictionConfidence.INSUFFICIENT_DATA -> Triple(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        "INSUFFICIENT DATA"
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (prediction == null || prediction.confidence == PredictionConfidence.INSUFFICIENT_DATA) {
                Text(
                    text = prediction?.explanation
                        ?: "Log your consumption events to enable intelligent restock predictions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onLogFirstUsage,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Log First Usage")
                }
            } else {
                val restockDate = prediction.estimatedRestockDate
                if (restockDate != null) {
                    Text(
                        text = "Estimated Restock Date",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(restockDate),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                prediction.daysRemaining?.let { days ->
                    val roundedDays = Math.round(days)
                    val daysStr = when {
                        roundedDays <= 0 -> "Restock recommended today"
                        roundedDays == 1 -> "1 day remaining"
                        else -> "$roundedDays days remaining"
                    }
                    Text(
                        text = daysStr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (roundedDays <= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = prediction.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Button to add item to the shopping list with toggle feedback.
 */
@Composable
private fun AddToShoppingListButton(
    isAdded: Boolean,
    onAddClick: () -> Unit
) {
    if (isAdded) {
        FilledTonalButton(
            onClick = {},
            enabled = false,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Added to Shopping List ✓",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add to Shopping List",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Expiry date information card with calculated days remaining.
 */
@Composable
private fun ExpiryCard(
    item: PantryItem,
    expiryDate: LocalDate
) {
    val isExpired = item.isExpired()
    val isExpiringSoon = item.isExpiringSoon(withinDays = 3)

    val (cardColor, tintColor) = when {
        isExpired -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        isExpiringSoon -> WarningOrangeContainer to WarningOrange
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) to MaterialTheme.colorScheme.onSurfaceVariant
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpired || isExpiringSoon) Icons.Default.Warning else Icons.Default.Schedule,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Expiration",
                    style = MaterialTheme.typography.labelMedium,
                    color = tintColor
                )
                Text(
                    text = formatExpiryDescription(expiryDate),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tintColor
                )
            }
        }
    }
}

/**
 * Single card row for a consumption event.
 */
@Composable
private fun ConsumptionEventItem(
    event: ConsumptionEvent,
    onDeleteClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${formatQuantity(event.quantityConsumed)} ${event.unit.abbreviation}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatInstant(event.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Empty state when no consumption events are present.
 */
@Composable
private fun EmptyConsumptionHistoryState(
    onLogUsage: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No usage logged yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Log daily consumption to teach LazyCart your habits and predict restocks.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onLogUsage,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Log Consumption")
            }
        }
    }
}

/**
 * Modal bottom sheet for logging consumption on the Item Detail screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailLogBottomSheet(
    item: PantryItem,
    onDismiss: () -> Unit,
    onConfirm: (Float, String) -> Unit
) {
    var quantityText by remember(item.id) {
        val initial = if (item.quantity >= 1f) "1" else formatQuantity(item.quantity)
        mutableStateOf(initial)
    }
    var sliderValue by remember(item.id) {
        val initialFloat = if (item.quantity >= 1f) 1f else item.quantity
        mutableFloatStateOf(initialFloat)
    }
    var noteText by remember { mutableStateOf("") }
    val maxQuantity = item.quantity.coerceAtLeast(0.1f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Log Consumption",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "In pantry: ${formatQuantity(item.quantity)} ${item.unit.abbreviation}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val parsedQty = quantityText.toFloatOrNull() ?: 0f
            val isError = parsedQty > item.quantity || (quantityText.isNotEmpty() && parsedQty <= 0f)

            OutlinedTextField(
                value = quantityText,
                onValueChange = { newText ->
                    quantityText = newText
                    val parsed = newText.toFloatOrNull()
                    if (parsed != null && parsed in 0f..maxQuantity) {
                        sliderValue = parsed
                    }
                },
                label = { Text("Quantity consumed") },
                suffix = { Text(item.unit.abbreviation) },
                singleLine = true,
                isError = isError,
                supportingText = if (parsedQty > item.quantity) {
                    { Text("Cannot exceed available stock (${formatQuantity(item.quantity)})", color = MaterialTheme.colorScheme.error) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Slider
            Text(
                text = "Adjust: ${formatQuantity(sliderValue)} ${item.unit.abbreviation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = sliderValue.coerceIn(0f, maxQuantity),
                onValueChange = { newValue ->
                    sliderValue = newValue
                    quantityText = formatQuantity(newValue)
                },
                valueRange = 0f..maxQuantity,
                modifier = Modifier.fillMaxWidth()
            )

            // Preset Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.25f, 0.5f, 1f).filter { it <= maxQuantity }.forEach { amount ->
                    AssistChip(
                        onClick = {
                            sliderValue = amount
                            quantityText = formatQuantity(amount)
                        },
                        label = { Text("+${formatQuantity(amount)} ${item.unit.abbreviation}") }
                    )
                }
                AssistChip(
                    onClick = {
                        sliderValue = item.quantity
                        quantityText = formatQuantity(item.quantity)
                    },
                    label = { Text("All (${formatQuantity(item.quantity)})") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Optional Note
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note (optional)") },
                placeholder = { Text("e.g., Breakfast smoothie, dinner") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                val isValid = parsedQty > 0f && parsedQty <= item.quantity
                Button(
                    onClick = {
                        if (isValid) {
                            onConfirm(parsedQty, noteText.trim())
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}

private fun formatDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
}

private fun formatInstant(instant: java.time.Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.getDefault())
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}

private fun formatQuantity(quantity: Float): String {
    return if (quantity % 1f == 0f) {
        quantity.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", quantity)
    }
}

private fun formatExpiryDescription(expiryDate: LocalDate): String {
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(today, expiryDate)
    return when {
        days < 0 -> "Expired ${-days} day${if (-days != 1L) "s" else ""} ago (${formatDate(expiryDate)})"
        days == 0L -> "Expires today (${formatDate(expiryDate)})"
        days == 1L -> "Expires tomorrow (${formatDate(expiryDate)})"
        else -> "Expires in $days days (${formatDate(expiryDate)})"
    }
}

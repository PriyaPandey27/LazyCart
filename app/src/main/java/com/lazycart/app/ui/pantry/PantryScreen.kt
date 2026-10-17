package com.lazycart.app.ui.pantry

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lazycart.app.domain.model.ItemCategory
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.model.PredictionConfidence
import com.lazycart.app.domain.model.RestockPrediction
import com.lazycart.app.ui.navigation.Screen
import com.lazycart.app.ui.theme.SuccessGreen
import com.lazycart.app.ui.theme.SuccessGreenContainer
import com.lazycart.app.ui.theme.WarningOrange
import com.lazycart.app.ui.theme.WarningOrangeContainer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Main Pantry screen displaying active groceries, categories, restock predictions,
 * swipe-to-delete actions, and quick consumption logging.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(
    navController: NavHostController,
    onNavigateToAddItem: () -> Unit,
    onNavigateToItemDetail: (Long) -> Unit,
    viewModel: PantryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var itemToDelete by remember { mutableStateOf<PantryItem?>(null) }
    var itemToLog by remember { mutableStateOf<PantryItem?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(message = error)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Pantry",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddItem,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Item"
                )
            }
        },
        bottomBar = {
            PantryBottomNavBar(navController = navController)
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search pantry items...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // 2. Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All" chip
                item {
                    val isAllSelected = uiState.selectedCategory == null
                    FilterChip(
                        selected = isAllSelected,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("All") },
                        leadingIcon = if (isAllSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null
                    )
                }

                // Per-category chips
                items(ItemCategory.values()) { category ->
                    val isSelected = uiState.selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.selectCategory(if (isSelected) null else category)
                        },
                        label = { Text(category.displayName) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null
                    )
                }
            }

            // 3. Content Area: Loading, Empty, or List of Items
            when {
                uiState.isLoading && uiState.items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                uiState.items.isEmpty() -> {
                    if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) {
                        EmptySearchState(
                            searchQuery = uiState.searchQuery,
                            onClearFilters = {
                                viewModel.updateSearchQuery("")
                                viewModel.selectCategory(null)
                            }
                        )
                    } else {
                        EmptyPantryState(
                            onAddItem = onNavigateToAddItem
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = uiState.items, key = { it.id }) { item ->
                            PantryItemCard(
                                item = item,
                                onClick = { onNavigateToItemDetail(item.id) },
                                onLogClick = { itemToLog = item },
                                onDeleteRequest = { itemToDelete = item }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation AlertDialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(text = "Delete Item")
            },
            text = {
                Text(text = "Are you sure you want to remove \"${item.name}\" from your pantry?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(item)
                        itemToDelete = null
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
                TextButton(onClick = { itemToDelete = null }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Log Consumption ModalBottomSheet
    itemToLog?.let { item ->
        LogConsumptionBottomSheet(
            item = item,
            onConfirm = { quantityConsumed ->
                viewModel.logConsumption(
                    itemId = item.id,
                    quantity = quantityConsumed,
                    unit = item.unit
                )
                itemToLog = null
            },
            onDismiss = { itemToLog = null }
        )
    }
}

/**
 * Card representing a single pantry item. Displays quantity, category,
 * restock prediction badge, expiry indicator, and swipe-to-dismiss behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantryItemCard(
    item: PantryItem,
    onClick: () -> Unit,
    onLogClick: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDeleteRequest()
            }
            false // Keep card in place until confirmed via AlertDialog
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart ||
                dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val color by animateColorAsState(
                targetValue = if (isSwiping) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                label = "swipeBgColor"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .background(color, shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isSwiping) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        modifier = modifier
    ) {
        ElevatedCard(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Row: Item Name and Category Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = item.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Middle Row: Current Quantity and Quick 'Log' Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatQuantity(item.quantity),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.unit.abbreviation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = onLogClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Log", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Row: Prediction Badge and Expiry Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prediction badge
                    PredictionBadge(
                        prediction = item.prediction,
                        onTapToLog = onLogClick
                    )

                    // Expiry date (if specified)
                    item.expiryDate?.let { expiryDate ->
                        ExpiryBadge(item = item, expiryDate = expiryDate)
                    }
                }
            }
        }
    }
}

/**
 * Badge displaying restock predictions or prompt to log usage.
 */
@Composable
private fun PredictionBadge(
    prediction: RestockPrediction?,
    onTapToLog: () -> Unit
) {
    if (prediction == null || prediction.confidence == PredictionConfidence.INSUFFICIENT_DATA) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            onClick = onTapToLog
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Tap to log usage",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        val days = prediction.daysRemaining?.let { Math.round(it) } ?: 0
        val (containerColor, contentColor) = when {
            days <= 2 -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
            days in 3..5 -> WarningOrangeContainer to WarningOrange
            else -> SuccessGreenContainer to SuccessGreen
        }
        val daysText = if (days <= 1) "1 day left" else "$days days left"

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = containerColor
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
            }
        }
    }
}

/**
 * Badge displaying the expiry status with warning colors when expiring soon.
 */
@Composable
private fun ExpiryBadge(
    item: PantryItem,
    expiryDate: LocalDate
) {
    val isExpired = item.isExpired()
    val isExpiringSoon = item.isExpiringSoon(withinDays = 3)

    val (badgeText, tintColor) = when {
        isExpired -> "Expired: ${formatDate(expiryDate)}" to MaterialTheme.colorScheme.error
        isExpiringSoon -> "Exp: ${formatDate(expiryDate)}" to WarningOrange
        else -> "Exp: ${formatDate(expiryDate)}" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isExpired || isExpiringSoon) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            color = tintColor,
            fontWeight = if (isExpired || isExpiringSoon) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Bottom sheet allowing user to enter quantity consumed with a slider, presets, or text input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogConsumptionBottomSheet(
    item: PantryItem,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityText by remember(item.id) {
        val initialVal = if (item.quantity >= 1f) "1" else formatQuantity(item.quantity)
        mutableStateOf(initialVal)
    }
    var sliderValue by remember(item.id) {
        val initialFloat = if (item.quantity >= 1f) 1f else item.quantity
        mutableFloatStateOf(initialFloat)
    }
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "In pantry: ${formatQuantity(item.quantity)} ${item.unit.abbreviation}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Text input for quantity
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
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Slider for quick adjustment
            Text(
                text = "Adjust quantity: ${formatQuantity(sliderValue)} ${item.unit.abbreviation}",
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

            // Preset quick-chips
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
                            onConfirm(parsedQty)
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

/**
 * Empty state shown when user has no items in the pantry.
 */
@Composable
private fun EmptyPantryState(
    onAddItem: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Kitchen,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No items yet. Add your first grocery!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Keep track of stock, predict restocks, and reduce food waste.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddItem,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Add Item")
            }
        }
    }
}

/**
 * Empty state shown when search query or category filter returns no results.
 */
@Composable
private fun EmptySearchState(
    searchQuery: String,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No items match your search",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) {
                    "No results found for \"$searchQuery\". Try checking the spelling or resetting filters."
                } else {
                    "No items found in this category."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
                onClick = onClearFilters,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Clear filters")
            }
        }
    }
}

/**
 * Consistent bottom navigation bar displaying Home, Pantry, Shopping, and Settings.
 */
@Composable
private fun PantryBottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        val navItems = listOf(
            Triple(Screen.Home.route, "Home", Icons.Default.Home),
            Triple(Screen.Pantry.route, "Pantry", Icons.Default.Kitchen),
            Triple(Screen.Shopping.route, "Shopping", Icons.Default.ShoppingCart),
            Triple(Screen.Settings.route, "Settings", Icons.Default.Settings)
        )

        navItems.forEach { (route, label, icon) ->
            val selected = currentRoute == route
            NavigationBarItem(
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(text = label) },
                selected = selected,
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

private fun formatDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
}

private fun formatQuantity(quantity: Float): String {
    return if (quantity % 1f == 0f) {
        quantity.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", quantity)
    }
}

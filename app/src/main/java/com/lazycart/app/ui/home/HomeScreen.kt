package com.lazycart.app.ui.home

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lazycart.app.domain.model.ItemCategory
import com.lazycart.app.domain.model.ItemUnit
import com.lazycart.app.domain.model.PantryItem
import com.lazycart.app.domain.model.PredictionConfidence
import com.lazycart.app.domain.model.RestockPrediction
import com.lazycart.app.ui.navigation.Screen
import com.lazycart.app.ui.navigation.bottomNavItems
import com.lazycart.app.ui.theme.ErrorContainer
import com.lazycart.app.ui.theme.ErrorRed
import com.lazycart.app.ui.theme.LazyCartTheme
import com.lazycart.app.ui.theme.SuccessGreen
import com.lazycart.app.ui.theme.SuccessGreenContainer
import com.lazycart.app.ui.theme.WarningOrange
import com.lazycart.app.ui.theme.WarningOrangeContainer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Main dashboard screen displaying greetings, high-level summary cards,
 * items predicted to run out soon, items approaching expiration, quick-action FAB,
 * and bottom navigation.
 */
@Composable
fun HomeScreen(
    navController: NavHostController,
    onNavigateToAddItem: () -> Unit,
    onNavigateToItemDetail: (Long) -> Unit,
    onNavigateToShopping: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        uiState = uiState,
        onNavigateToAddItem = onNavigateToAddItem,
        onNavigateToItemDetail = onNavigateToItemDetail,
        onNavigateToShopping = onNavigateToShopping,
        onLogConsumption = viewModel::logConsumption,
        onDismissError = viewModel::dismissError,
        bottomBar = {
            BottomNavBar(navController = navController)
        }
    )
}

/**
 * Stateless content presentation for HomeScreen to allow previews and UI testing.
 */
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onNavigateToAddItem: () -> Unit,
    onNavigateToItemDetail: (Long) -> Unit,
    onNavigateToShopping: () -> Unit,
    onLogConsumption: (Long, Float, String) -> Unit,
    onDismissError: () -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMsg ->
            val result = snackbarHostState.showSnackbar(
                message = errorMsg,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                onDismissError()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showLogDialog = true },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Restaurant,
                        contentDescription = "Log Consumption"
                    )
                },
                text = { Text("Log Consumption") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Greeting Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = uiState.greeting,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.todayDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 2. Summary Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Total Items",
                            count = uiState.totalItemCount.toString(),
                            subtitle = "In your pantry",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "Restock Soon",
                            count = uiState.itemsRunningSoon.size.toString(),
                            subtitle = if (uiState.itemsRunningSoon.isEmpty()) "All stocked up" else "Items low",
                            containerColor = if (uiState.itemsRunningSoon.isNotEmpty()) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (uiState.itemsRunningSoon.isNotEmpty()) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. Section: Restock Soon (Horizontal scroll or cheerful empty state)
                item {
                    SectionHeader(
                        title = "Restock Soon",
                        count = uiState.itemsRunningSoon.size
                    )
                }

                item {
                    if (uiState.itemsRunningSoon.isEmpty()) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = SuccessGreenContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Your pantry is well stocked!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "No items predicted to run out in the next 5 days.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.itemsRunningSoon, key = { it.id }) { item ->
                                PantryItemMiniCard(
                                    item = item,
                                    onClick = { onNavigateToItemDetail(item.id) }
                                )
                            }
                        }
                    }
                }

                // 4. Section: Expiring Soon
                item {
                    SectionHeader(
                        title = "Expiring Soon",
                        count = uiState.itemsExpiringSoon.size
                    )
                }

                item {
                    if (uiState.itemsExpiringSoon.isEmpty()) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "All items are fresh!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "No items expiring within the next 3 days.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.itemsExpiringSoon.forEach { item ->
                                ExpiryAlertCard(
                                    item = item,
                                    onClick = { onNavigateToItemDetail(item.id) }
                                )
                            }
                        }
                    }
                }

                // 5. Shopping List Action Card & Button
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        onClick = onNavigateToShopping
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.ShoppingCart,
                                            contentDescription = "Shopping List",
                                            tint = MaterialTheme.colorScheme.onSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Shopping List",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Review items needing purchase or restock",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = onNavigateToShopping,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Open")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        LogConsumptionDialog(
            items = uiState.allItems,
            onDismiss = { showLogDialog = false },
            onConfirm = { itemId, qty, note ->
                onLogConsumption(itemId, qty, note)
            },
            onNavigateToAddItem = onNavigateToAddItem
        )
    }
}

/**
 * Stat summary card for the top dashboard grid.
 */
@Composable
private fun SummaryCard(
    title: String,
    count: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize
                ),
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.75f)
            )
        }
    }
}

/**
 * Section header with an optional pill badge indicating item count.
 */
@Composable
private fun SectionHeader(
    title: String,
    count: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (count != null && count > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact card representing a pantry item predicted to run low soon.
 * Displays name, quantity + unit, and urgency-colored days left badge:
 * - Red (< 2 days)
 * - Orange (< 5 days)
 * - Green (otherwise)
 */
@Composable
private fun PantryItemMiniCard(
    item: PantryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysRemaining = item.prediction?.daysRemaining
    val (badgeBg, badgeText, badgeLabel) = when {
        daysRemaining == null -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Estimating"
        )
        daysRemaining < 1f -> Triple(ErrorContainer, ErrorRed, "< 1 day left")
        daysRemaining < 2f -> Triple(ErrorContainer, ErrorRed, "1 day left")
        daysRemaining < 5f -> Triple(WarningOrangeContainer, WarningOrange, "${daysRemaining.roundToInt()} days left")
        else -> Triple(SuccessGreenContainer, SuccessGreen, "${daysRemaining.roundToInt()} days left")
    }

    ElevatedCard(
        modifier = modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = item.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatQuantity(item.quantity)} ${item.unit.abbreviation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = badgeLabel,
                    color = badgeText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * List row item alerting the user that a pantry item is near expiry.
 */
@Composable
private fun ExpiryAlertCard(
    item: PantryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ErrorContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = "Expiry alert",
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.category.displayName} • ${formatQuantity(item.quantity)} ${item.unit.abbreviation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ErrorContainer,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = formatExpiryText(item.expiryDate),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "View item details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Quick action dialog for recording consumption without leaving the Home screen.
 */
@Composable
private fun LogConsumptionDialog(
    items: List<PantryItem>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Float, String) -> Unit,
    onNavigateToAddItem: () -> Unit
) {
    if (items.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Log Consumption") },
            text = { Text("There are no active items in your pantry. Add items to your pantry first to record consumption.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToAddItem()
                    }
                ) {
                    Text("Add Item")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
        return
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var quantityText by remember { mutableStateOf("1") }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val selectedItem = items.getOrNull(selectedIndex) ?: items.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Consumption",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select an item to record usage and update predictions:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Item picker dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = "${selectedItem.name} (${formatQuantity(selectedItem.quantity)} ${selectedItem.unit.abbreviation} left)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pantry Item") },
                        trailingIcon = {
                            IconButton(onClick = { expandedDropdown = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown,
                                    contentDescription = "Select Item"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedDropdown = true }
                    )
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        items.forEachIndexed { index, item ->
                            DropdownMenuItem(
                                text = {
                                    Text("${item.name} (${formatQuantity(item.quantity)} ${item.unit.abbreviation})")
                                },
                                onClick = {
                                    selectedIndex = index
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                // Quantity consumed
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it
                        errorMessage = null
                    },
                    label = { Text("Quantity Consumed (${selectedItem.unit.displayName})") },
                    placeholder = { Text("e.g. 1") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. Weekly cooking") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toFloatOrNull()
                    if (qty == null || qty <= 0f) {
                        errorMessage = "Please enter a valid positive quantity"
                        return@Button
                    }
                    onConfirm(selectedItem.id, qty, noteText.trim())
                    onDismiss()
                }
            ) {
                Text("Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Bottom navigation bar supporting the 4 main destinations:
 * Home, Pantry, Shopping, Settings.
 */
@Composable
private fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { screen ->
            val (icon, label) = when (screen) {
                Screen.Home -> Icons.Outlined.Home to "Home"
                Screen.Pantry -> Icons.Outlined.Kitchen to "Pantry"
                Screen.Shopping -> Icons.Outlined.ShoppingCart to "Shopping"
                Screen.Settings -> Icons.Outlined.Settings to "Settings"
                else -> Icons.Outlined.Home to ""
            }

            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(text = label) }
            )
        }
    }
}

/**
 * Formats decimal quantity cleanly: prints integer if whole, or 1 decimal place.
 */
private fun formatQuantity(quantity: Float): String {
    return if (quantity % 1f == 0f) {
        quantity.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", quantity)
    }
}

/**
 * Returns human-friendly expiry string relative to current date.
 */
private fun formatExpiryText(expiryDate: LocalDate?, today: LocalDate = LocalDate.now()): String {
    if (expiryDate == null) return "No date"
    val days = ChronoUnit.DAYS.between(today, expiryDate)
    return when {
        days < 0 -> "Expired"
        days == 0L -> "Expires today"
        days == 1L -> "Expires tomorrow"
        else -> "Expires in $days days"
    }
}

// ── Compose Previews ────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Home Screen - Active Items")
@Composable
private fun HomeScreenPreview() {
    val sampleItems = listOf(
        PantryItem(
            id = 1L,
            name = "Whole Milk",
            category = ItemCategory.DAIRY,
            quantity = 0.5f,
            unit = ItemUnit.LITRES,
            purchaseDate = LocalDate.now().minusDays(5),
            expiryDate = LocalDate.now().plusDays(2),
            prediction = RestockPrediction(
                daysRemaining = 1.2f,
                estimatedRestockDate = LocalDate.now().plusDays(1),
                averageDailyConsumption = 0.4f,
                confidence = PredictionConfidence.HIGH,
                explanation = "Based on recent consumption",
                observationWindowDays = 30
            )
        ),
        PantryItem(
            id = 2L,
            name = "Eggs",
            category = ItemCategory.PROTEIN,
            quantity = 4f,
            unit = ItemUnit.PIECES,
            purchaseDate = LocalDate.now().minusDays(7),
            expiryDate = LocalDate.now().plusDays(1),
            prediction = RestockPrediction(
                daysRemaining = 3.5f,
                estimatedRestockDate = LocalDate.now().plusDays(3),
                averageDailyConsumption = 1f,
                confidence = PredictionConfidence.MEDIUM,
                explanation = "Based on recent consumption",
                observationWindowDays = 30
            )
        )
    )

    val sampleState = HomeUiState(
        greeting = "Good afternoon",
        todayDate = "Saturday, Oct 17",
        totalItemCount = 14,
        itemsRunningSoon = sampleItems,
        itemsExpiringSoon = sampleItems,
        isLoading = false,
        errorMessage = null,
        allItems = sampleItems
    )

    LazyCartTheme {
        HomeScreenContent(
            uiState = sampleState,
            onNavigateToAddItem = {},
            onNavigateToItemDetail = {},
            onNavigateToShopping = {},
            onLogConsumption = { _, _, _ -> },
            onDismissError = {},
            bottomBar = {
                BottomNavBar(navController = rememberNavController())
            }
        )
    }
}

@Preview(showBackground = true, name = "Home Screen - Well Stocked Empty State")
@Composable
private fun HomeScreenEmptyPreview() {
    val sampleState = HomeUiState(
        greeting = "Good morning",
        todayDate = "Saturday, Oct 17",
        totalItemCount = 5,
        itemsRunningSoon = emptyList(),
        itemsExpiringSoon = emptyList(),
        isLoading = false,
        errorMessage = null,
        allItems = emptyList()
    )

    LazyCartTheme {
        HomeScreenContent(
            uiState = sampleState,
            onNavigateToAddItem = {},
            onNavigateToItemDetail = {},
            onNavigateToShopping = {},
            onLogConsumption = { _, _, _ -> },
            onDismissError = {},
            bottomBar = {
                BottomNavBar(navController = rememberNavController())
            }
        )
    }
}

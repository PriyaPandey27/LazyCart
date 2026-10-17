package com.lazycart.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lazycart.app.ui.navigation.Screen
import com.lazycart.app.ui.theme.SuccessGreen
import com.lazycart.app.ui.theme.WarningOrange

/** Shared bottom navigation bar used across all root screens. */
@Composable
fun LazyCartBottomNav(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavItem.all.forEach { navItem ->
            NavigationBarItem(
                selected = currentRoute == navItem.screen.route,
                onClick = {
                    navController.navigate(navItem.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = navItem.icon,
                        contentDescription = navItem.label
                    )
                },
                label = { Text(navItem.label) }
            )
        }
    }
}

private data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
) {
    companion object {
        val all = listOf(
            NavItem(Screen.Home, "Home", Icons.Outlined.Home),
            NavItem(Screen.Pantry, "Pantry", Icons.Outlined.Kitchen),
            NavItem(Screen.Shopping, "Shopping", Icons.Outlined.ShoppingCart),
            NavItem(Screen.Settings, "Settings", Icons.Outlined.Settings)
        )
    }
}

/** Urgency badge colour based on days remaining. */
@Composable
fun urgencyColor(daysRemaining: Float?): Color {
    if (daysRemaining == null) return MaterialTheme.colorScheme.outlineVariant
    return when {
        daysRemaining < 2f -> MaterialTheme.colorScheme.error
        daysRemaining < 5f -> WarningOrange
        else -> SuccessGreen
    }
}

/** Pill-shaped badge for urgency display. */
@Composable
fun UrgencyBadge(
    daysRemaining: Float?,
    modifier: Modifier = Modifier
) {
    val color = urgencyColor(daysRemaining)
    val label = when {
        daysRemaining == null -> "No data"
        daysRemaining < 1f -> "Out soon"
        daysRemaining < 2f -> "1 day left"
        else -> "${daysRemaining.toInt()} days left"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/** Section header used throughout the app for consistency. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        trailingContent?.invoke()
    }
}

/** Full-screen loading indicator. */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.semantics {
            contentDescription = "Loading"
        })
    }
}

/** Generic empty state composable. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(16.dp))
            action()
        }
    }
}

/** Category chip colour helper. */
@Composable
fun categoryBadgeColors(categoryName: String): Pair<Color, Color> {
    val bg = when (categoryName) {
        "DAIRY" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "GRAINS" -> Color(0xFFFFF8E1) to Color(0xFFE65100)
        "PRODUCE" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "PROTEIN" -> Color(0xFFFCE4EC) to Color(0xFFC62828)
        "BEVERAGES" -> Color(0xFFE8EAF6) to Color(0xFF283593)
        "CONDIMENTS" -> Color(0xFFFFF3E0) to Color(0xFFBF360C)
        "SNACKS" -> Color(0xFFF3E5F5) to Color(0xFF6A1B9A)
        "HOUSEHOLD" -> Color(0xFFF5F5F5) to Color(0xFF424242)
        else -> Color(0xFFF5F5F5) to Color(0xFF616161)
    }
    return bg
}

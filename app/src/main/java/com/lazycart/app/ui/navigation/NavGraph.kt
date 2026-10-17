package com.lazycart.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lazycart.app.ui.addedit.AddEditItemScreen
import com.lazycart.app.ui.home.HomeScreen
import com.lazycart.app.ui.itemdetail.ItemDetailScreen
import com.lazycart.app.ui.onboarding.OnboardingScreen
import com.lazycart.app.ui.pantry.PantryScreen
import com.lazycart.app.ui.settings.SettingsScreen
import com.lazycart.app.ui.shopping.ShoppingListScreen

/** Type-safe navigation destinations. */
sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Pantry : Screen("pantry")
    object Shopping : Screen("shopping")
    object Settings : Screen("settings")
    object AddItem : Screen("add_item")
    object ItemDetail : Screen("item_detail/{itemId}") {
        fun createRoute(itemId: Long) = "item_detail/$itemId"
    }
    object EditItem : Screen("edit_item/{itemId}") {
        fun createRoute(itemId: Long) = "edit_item/$itemId"
    }
}

/** Bottom nav tabs (the 4 root destinations). */
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Pantry,
    Screen.Shopping,
    Screen.Settings
)

@Composable
fun LazyCartNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                onNavigateToAddItem = { navController.navigate(Screen.AddItem.route) },
                onNavigateToItemDetail = { id -> navController.navigate(Screen.ItemDetail.createRoute(id)) },
                onNavigateToShopping = {
                    navController.navigate(Screen.Shopping.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Screen.Pantry.route) {
            PantryScreen(
                navController = navController,
                onNavigateToAddItem = { navController.navigate(Screen.AddItem.route) },
                onNavigateToItemDetail = { id -> navController.navigate(Screen.ItemDetail.createRoute(id)) }
            )
        }

        composable(Screen.Shopping.route) {
            ShoppingListScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        composable(Screen.AddItem.route) {
            AddEditItemScreen(
                itemId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ItemDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
            ItemDetailScreen(
                itemId = itemId,
                navController = navController,
                onNavigateToEdit = { navController.navigate(Screen.EditItem.createRoute(itemId)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditItem.route,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
            AddEditItemScreen(
                itemId = itemId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

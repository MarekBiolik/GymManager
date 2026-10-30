package com.gymmanager.android.nutrition

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymmanager.android.nutrition.charts.ChartsScreen
import com.gymmanager.android.nutrition.charts.ChartsViewModel
import com.gymmanager.android.nutrition.charts.ChartsViewModelFactory
import com.gymmanager.android.nutrition.diary.DiaryScreen
import com.gymmanager.android.nutrition.diary.DiaryViewModel
import com.gymmanager.android.nutrition.diary.DiaryViewModelFactory
import com.gymmanager.android.nutrition.foods.FoodsScreen
import com.gymmanager.android.nutrition.foods.FoodsViewModel
import com.gymmanager.android.nutrition.foods.FoodsViewModelFactory
import com.gymmanager.android.nutrition.settings.NutritionSettingsScreen
import com.gymmanager.android.nutrition.settings.NutritionSettingsViewModel
import com.gymmanager.android.nutrition.settings.NutritionSettingsViewModelFactory
import com.gymmanager.android.sync.AndroidNetworkStatus
import com.gymmanager.android.sync.HelloSyncViewModel
import com.gymmanager.android.sync.HelloSyncViewModelFactory
import com.gymmanager.android.theme.GymTokens
import com.gymmanager.core.GymManagerServices
import com.gymmanager.core.application.account.DeleteAccount

enum class NutritionTab(
    val route: String,
    val label: String,
) {
    Diary("diary", "Diary"),
    Foods("foods", "Foods"),
    Charts("charts", "Charts"),
    Settings("settings", "Settings"),
}

@Composable
fun NutritionShell(
    userId: String,
    services: GymManagerServices,
    network: AndroidNetworkStatus,
    deleteAccount: DeleteAccount,
    onAccountDeleted: () -> Unit,
    onSignOut: () -> Unit,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: NutritionTab.Diary.route

    Scaffold(
        containerColor = GymTokens.ColorBgCanvas,
        bottomBar = {
            NavigationBar(containerColor = GymTokens.ColorBgSurface) {
                NutritionTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector =
                                    when (tab) {
                                        NutritionTab.Diary -> Icons.AutoMirrored.Filled.List
                                        NutritionTab.Foods -> Icons.Default.Restaurant
                                        NutritionTab.Charts -> Icons.Default.BarChart
                                        NutritionTab.Settings -> Icons.Default.Settings
                                    },
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NutritionTab.Diary.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(NutritionTab.Diary.route) {
                val vm: DiaryViewModel =
                    viewModel(
                        key = "diary-$userId",
                        factory =
                            DiaryViewModelFactory(
                                userId = userId,
                                services = services,
                                network = network,
                            ),
                    )
                DiaryScreen(viewModel = vm)
            }
            composable(NutritionTab.Foods.route) {
                val vm: FoodsViewModel =
                    viewModel(
                        key = "foods-$userId",
                        factory =
                            FoodsViewModelFactory(
                                userId = userId,
                                services = services,
                                network = network,
                            ),
                    )
                FoodsScreen(viewModel = vm)
            }
            composable(NutritionTab.Charts.route) {
                val vm: ChartsViewModel =
                    viewModel(
                        key = "charts-$userId",
                        factory =
                            ChartsViewModelFactory(
                                userId = userId,
                                services = services,
                                network = network,
                            ),
                    )
                ChartsScreen(viewModel = vm)
            }
            composable(NutritionTab.Settings.route) {
                val settingsVm: NutritionSettingsViewModel =
                    viewModel(
                        key = "settings-$userId",
                        factory =
                            NutritionSettingsViewModelFactory(
                                userId = userId,
                                services = services,
                                network = network,
                            ),
                    )
                val helloVm: HelloSyncViewModel =
                    viewModel(
                        key = "hello-$userId",
                        factory =
                            HelloSyncViewModelFactory(
                                userId = userId,
                                profiles = services.profiles,
                                measurements = services.measurements,
                                network = network,
                            ),
                    )
                NutritionSettingsScreen(
                    viewModel = settingsVm,
                    helloSyncViewModel = helloVm,
                    deleteAccount = deleteAccount,
                    onAccountDeleted = onAccountDeleted,
                    onSignOut = onSignOut,
                )
            }
        }
    }
}

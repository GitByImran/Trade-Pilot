package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.BacktestScreen
import com.example.ui.screens.CoinDetailScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PaperTradingScreen
import com.example.ui.screens.SignalsHistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BinanceGold

sealed class Screen(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Dashboard : Screen("dashboard", "Markets", Icons.Filled.ShowChart, Icons.Outlined.ShowChart)
    object Signals : Screen("signals", "Signals", Icons.Filled.QueryStats, Icons.Outlined.QueryStats)
    object Paper : Screen("paper", "Portfolio", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
    object Backtest : Screen("backtest", "Backtest", Icons.Filled.History, Icons.Outlined.History)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Signals,
    Screen.Paper,
    Screen.Backtest,
    Screen.Settings
)

@Composable
fun TradePilotApp(
    viewModel: TradePilotViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 600.dp
        val showBottomBar = !isExpanded && bottomNavItems.any { it.route == currentRoute }

        if (isExpanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.testTag("nav_rail"),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label, fontSize = 11.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = BinanceGold,
                                selectedTextColor = BinanceGold,
                                indicatorColor = BinanceGold.copy(alpha = 0.15f)
                            )
                        )
                    }
                }

                AppNavHost(
                    navController = navController,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_nav_bar"),
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                        ) {
                            bottomNavItems.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = screen.label
                                        )
                                    },
                                    label = { Text(screen.label, fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = BinanceGold,
                                        selectedTextColor = BinanceGold,
                                        indicatorColor = BinanceGold.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                AppNavHost(
                    navController = navController,
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    viewModel: TradePilotViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToCoin = { symbol ->
                    viewModel.selectSymbol(symbol)
                    navController.navigate("coin/$symbol")
                },
                onNavigateToPaper = {
                    navController.navigate(Screen.Paper.route)
                }
            )
        }

        composable(Screen.Signals.route) {
            SignalsHistoryScreen(
                viewModel = viewModel,
                onSelectSymbol = { symbol ->
                    navController.navigate("coin/$symbol")
                }
            )
        }

        composable(Screen.Paper.route) {
            PaperTradingScreen(viewModel = viewModel)
        }

        composable(Screen.Backtest.route) {
            BacktestScreen(viewModel = viewModel)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = viewModel)
        }

        composable(
            route = "coin/{symbol}",
            arguments = listOf(navArgument("symbol") { type = NavType.StringType })
        ) { backStackEntry ->
            val symbol = backStackEntry.arguments?.getString("symbol") ?: "BTCUSDT"
            CoinDetailScreen(
                symbol = symbol,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

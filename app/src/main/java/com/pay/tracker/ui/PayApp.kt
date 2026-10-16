package com.pay.tracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pay.tracker.ui.navigation.PayBottomNavigationBar
import com.pay.tracker.ui.navigation.Screen
import com.pay.tracker.ui.screens.DashboardScreen
import com.pay.tracker.ui.screens.HistoryScreen
import com.pay.tracker.ui.screens.ReviewQueueScreen
import com.pay.tracker.ui.screens.SimulatorScreen
import com.pay.tracker.ui.screens.SettingsScreen
import com.pay.tracker.ui.viewmodel.DashboardViewModel
import com.pay.tracker.ui.viewmodel.HistoryViewModel
import com.pay.tracker.ui.viewmodel.PayViewModelFactory
import com.pay.tracker.ui.viewmodel.ReviewQueueViewModel
import com.pay.tracker.ui.viewmodel.SettingsViewModel
import com.pay.tracker.ui.viewmodel.SimulatorViewModel

@Composable
fun PayApp(
    viewModelFactory: PayViewModelFactory,
    initialRoute: String? = null,
    modifier: Modifier = Modifier
) {
    var currentRoute by rememberSaveable { mutableStateOf(initialRoute ?: Screen.Dashboard.route) }

    LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrBlank()) {
            currentRoute = initialRoute
        }
    }

    // System back gesture navigation
    BackHandler(enabled = currentRoute != Screen.Dashboard.route) {
        if (currentRoute == Screen.Simulator.route) {
            currentRoute = Screen.Settings.route
        } else {
            currentRoute = Screen.Dashboard.route
        }
    }

    val dashboardViewModel: DashboardViewModel = viewModel(factory = viewModelFactory)
    val reviewQueueViewModel: ReviewQueueViewModel = viewModel(factory = viewModelFactory)
    val historyViewModel: HistoryViewModel = viewModel(factory = viewModelFactory)
    val simulatorViewModel: SimulatorViewModel = viewModel(factory = viewModelFactory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)

    val pendingTransactions by reviewQueueViewModel.pendingTransactions.collectAsState()

    com.pay.tracker.ui.components.BotanicalBackground(
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                if (currentRoute != Screen.Simulator.route) {
                    PayBottomNavigationBar(
                        currentRoute = currentRoute,
                        pendingCount = pendingTransactions.size,
                        onNavigateToRoute = { route -> currentRoute = route }
                    )
                }
            }
        ) { innerPadding ->
        // Pass bottom padding to inner screens so they don't clip beneath the NavigationBar
        val screenModifier = Modifier
            .fillMaxSize()
            .padding(bottom = innerPadding.calculateBottomPadding())

        when (currentRoute) {
            Screen.Dashboard.route -> {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToReview = { currentRoute = Screen.ReviewQueue.route },
                    onNavigateToHistory = { currentRoute = Screen.History.route },
                    modifier = screenModifier
                )
            }
            Screen.ReviewQueue.route -> {
                ReviewQueueScreen(
                    viewModel = reviewQueueViewModel,
                    modifier = screenModifier
                )
            }
            Screen.History.route -> {
                HistoryScreen(
                    viewModel = historyViewModel,
                    modifier = screenModifier
                )
            }
            Screen.Simulator.route -> {
                SimulatorScreen(
                    viewModel = simulatorViewModel,
                    onNavigateBack = { currentRoute = Screen.Settings.route },
                    modifier = screenModifier
                )
            }
            Screen.Settings.route -> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToSimulator = { currentRoute = Screen.Simulator.route },
                    modifier = screenModifier
                )
            }
            else -> {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToReview = { currentRoute = Screen.ReviewQueue.route },
                    onNavigateToHistory = { currentRoute = Screen.History.route },
                    modifier = screenModifier
                )
            }
        }
    }
}
}


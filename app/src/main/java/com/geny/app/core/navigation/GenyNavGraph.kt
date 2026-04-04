package com.geny.app.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.geny.app.domain.model.AuthState
import com.geny.app.presentation.agent.AgentDetailScreen
import com.geny.app.presentation.agent.AgentDetailViewModel
import com.geny.app.presentation.auth.AuthScreen
import com.geny.app.presentation.auth.AuthViewModel
import com.geny.app.presentation.chat.ChatListScreen
import com.geny.app.presentation.chat.ChatListViewModel
import com.geny.app.presentation.chat.ChatRoomScreen
import com.geny.app.presentation.chat.ChatRoomViewModel
import com.geny.app.presentation.dashboard.DashboardScreen
import com.geny.app.presentation.dashboard.DashboardViewModel
import com.geny.app.presentation.settings.SettingsScreen
import com.geny.app.presentation.settings.SettingsViewModel
import com.geny.app.presentation.vtuber.VTuberViewerScreen
import com.geny.app.presentation.vtuber.VTuberViewerViewModel

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard.route, "Agents", Icons.Filled.Dashboard),
    BottomNavItem(Screen.ChatList.route, "Chat", Icons.Filled.Chat),
    BottomNavItem(Screen.Settings.route, "Settings", Icons.Filled.Settings)
)

@Composable
fun GenyNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    // Auth screen handles everything: URL + login + auto-login
    // Only go to Dashboard if already authenticated
    val startDestination = when (authState.authState) {
        is AuthState.Authenticated -> Screen.Dashboard.route
        else -> Screen.Auth.route
    }

    NavHost(
        navController = navController,
        startDestination = if (authState.isLoading) Screen.Auth.route else startDestination
    ) {
        // Auth Screen
        composable(Screen.Auth.route) {
            AuthScreen(
                viewModel = authViewModel,
                onAuthenticated = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onNeedServerSetup = {
                    // URL is now part of auth screen, no-op
                }
            )
        }

        // Main screens with bottom nav
        composable(Screen.Dashboard.route) {
            MainScaffold(navController, Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = hiltViewModel()
                DashboardScreen(
                    viewModel = viewModel,
                    onAgentClick = { agentId ->
                        navController.navigate(Screen.AgentDetail.createRoute(agentId))
                    }
                )
            }
        }

        composable(Screen.ChatList.route) {
            MainScaffold(navController, Screen.ChatList.route) {
                val viewModel: ChatListViewModel = hiltViewModel()
                ChatListScreen(
                    viewModel = viewModel,
                    onRoomClick = { roomId ->
                        navController.navigate(Screen.ChatRoom.createRoute(roomId))
                    }
                )
            }
        }

        composable(Screen.Settings.route) {
            MainScaffold(navController, Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = if (navController.previousBackStackEntry != null) {
                        { navController.popBackStack() }
                    } else null,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Detail screens (no bottom nav)
        composable(
            route = Screen.AgentDetail.route,
            arguments = listOf(navArgument("agentId") { type = NavType.StringType })
        ) {
            val viewModel: AgentDetailViewModel = hiltViewModel()
            AgentDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onVTuberClick = { agentId ->
                    navController.navigate(Screen.VTuberViewer.createRoute(agentId))
                }
            )
        }

        composable(
            route = Screen.ChatRoom.route,
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) {
            val viewModel: ChatRoomViewModel = hiltViewModel()
            ChatRoomScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.VTuberViewer.route,
            arguments = listOf(navArgument("agentId") { type = NavType.StringType })
        ) {
            val viewModel: VTuberViewerViewModel = hiltViewModel()
            VTuberViewerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun MainScaffold(
    navController: NavHostController,
    currentRoute: String,
    content: @Composable () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination == item.route,
                        onClick = {
                            if (currentDestination != item.route) {
                                navController.navigate(item.route) {
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
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}

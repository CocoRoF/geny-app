package com.geny.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.geny.app.presentation.home.AppDrawerContent
import com.geny.app.presentation.home.HomeScreen
import com.geny.app.presentation.home.HomeViewModel
import com.geny.app.presentation.memory.OpsidianScreen
import com.geny.app.presentation.memory.OpsidianViewModel
import com.geny.app.presentation.settings.SettingsScreen
import com.geny.app.presentation.settings.SettingsViewModel
import com.geny.app.presentation.vtuber.VTuberViewerScreen
import com.geny.app.presentation.vtuber.VTuberViewerViewModel
import kotlinx.coroutines.launch

@Composable
fun GenyNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    val startDestination = when (authState.authState) {
        is AuthState.Authenticated -> Screen.Home.route
        else -> Screen.Auth.route
    }

    // Home state for drawer — shared across VTuber routes
    val homeViewModel: HomeViewModel = hiltViewModel()
    val homeState by homeViewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Determine if drawer should be active
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isVTuberRoute = currentRoute == Screen.Home.route ||
            currentRoute == Screen.VTuberViewer.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isVTuberRoute,
        drawerContent = {
            if (isVTuberRoute) {
                AppDrawerContent(
                    agents = homeState.vtuberAgents,
                    selectedAgentId = homeState.selectedAgentId,
                    onSelectAgent = { agentId ->
                        homeViewModel.selectAgent(agentId)
                        navController.navigate(Screen.VTuberViewer.createRoute(agentId)) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    },
                    onNavigateToAgents = {
                        navController.navigate(Screen.Dashboard.route)
                        scope.launch { drawerState.close() }
                    },
                    onNavigateToChatRooms = {
                        navController.navigate(Screen.ChatList.route)
                        scope.launch { drawerState.close() }
                    },
                    onNavigateToOpsidian = {
                        navController.navigate(Screen.Opsidian.route)
                        scope.launch { drawerState.close() }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = if (authState.isLoading) Screen.Auth.route else startDestination
        ) {
            // Auth
            composable(Screen.Auth.route) {
                AuthScreen(
                    viewModel = authViewModel,
                    onAuthenticated = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    },
                    onNeedServerSetup = { }
                )
            }

            // Home — gateway that auto-navigates to VTuber viewer
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onAgentReady = { agentId ->
                        navController.navigate(Screen.VTuberViewer.createRoute(agentId)) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAgents = {
                        navController.navigate(Screen.Dashboard.route)
                    }
                )
            }

            // VTuber Viewer — main content
            composable(
                route = Screen.VTuberViewer.route,
                arguments = listOf(navArgument("agentId") { type = NavType.StringType })
            ) {
                val viewModel: VTuberViewerViewModel = hiltViewModel()
                VTuberViewerScreen(
                    viewModel = viewModel,
                    onBack = { scope.launch { drawerState.open() } },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            // Dashboard (All Agents) — secondary
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = hiltViewModel()
                DashboardScreen(
                    viewModel = viewModel,
                    onAgentClick = { agentId ->
                        navController.navigate(Screen.AgentDetail.createRoute(agentId))
                    },
                    onVTuberClick = { agentId ->
                        homeViewModel.selectAgent(agentId)
                        navController.navigate(Screen.VTuberViewer.createRoute(agentId)) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Chat List — secondary
            composable(Screen.ChatList.route) {
                val viewModel: ChatListViewModel = hiltViewModel()
                ChatListScreen(
                    viewModel = viewModel,
                    onRoomClick = { roomId ->
                        navController.navigate(Screen.ChatRoom.createRoute(roomId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Settings — secondary
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Agent Detail
            composable(
                route = Screen.AgentDetail.route,
                arguments = listOf(navArgument("agentId") { type = NavType.StringType })
            ) {
                val viewModel: AgentDetailViewModel = hiltViewModel()
                AgentDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onVTuberClick = { agentId ->
                        homeViewModel.selectAgent(agentId)
                        navController.navigate(Screen.VTuberViewer.createRoute(agentId)) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    },
                    onMemoryClick = {
                        navController.navigate(Screen.Opsidian.route)
                    }
                )
            }

            // Chat Room
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

            // Opsidian (unified memory browser)
            composable(Screen.Opsidian.route) {
                val viewModel: OpsidianViewModel = hiltViewModel()
                OpsidianScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

        }
    }
}

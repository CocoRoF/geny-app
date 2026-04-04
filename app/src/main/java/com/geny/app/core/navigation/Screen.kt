package com.geny.app.core.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Dashboard : Screen("dashboard")
    data object ChatList : Screen("chat")
    data object Settings : Screen("settings")

    data object AgentDetail : Screen("agent/{agentId}") {
        fun createRoute(agentId: String) = "agent/$agentId"
    }

    data object ChatRoom : Screen("chat/{roomId}") {
        fun createRoute(roomId: String) = "chat/$roomId"
    }

    data object VTuberViewer : Screen("vtuber/{agentId}") {
        fun createRoute(agentId: String) = "vtuber/$agentId"
    }
}

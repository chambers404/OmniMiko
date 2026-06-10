package com.omnimiko.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.omnimiko.di.AppContainer
import com.omnimiko.ui.chat.ChatScreen
import com.omnimiko.ui.conversations.ConversationsScreen
import com.omnimiko.ui.models.ModelsScreen
import com.omnimiko.ui.settings.SettingsScreen

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Chat("chat", "Chat", Icons.AutoMirrored.Filled.Chat),
    History("history", "History", Icons.Filled.History),
    Models("models", "Models", Icons.Filled.Memory),
    Settings("settings", "Settings", Icons.Filled.Settings),
}

@Composable
fun OmniMikoRoot(container: AppContainer) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Dest.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = current?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Chat.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Dest.Chat.route) { ChatScreen(container) }
            composable(Dest.History.route) {
                ConversationsScreen(
                    container = container,
                    onOpenConversation = { navController.navigate(Dest.Chat.route) },
                )
            }
            composable(Dest.Models.route) { ModelsScreen(container) }
            composable(Dest.Settings.route) { SettingsScreen(container) }
        }
    }
}

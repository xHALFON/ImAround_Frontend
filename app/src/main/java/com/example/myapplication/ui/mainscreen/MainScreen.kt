package com.example.myapplication.ui.mainscreen

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.myapplication.ui.components.ChatScreen
//import com.example.myapplication.ui.components.ScanScreen
import com.example.myapplication.ui.search.SearchScreen
import com.example.myapplication.ui.profile.ProfileScreen

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Profile : BottomNavItem("profileTab", Icons.Filled.AccountCircle, "Profile")
    object Scan : BottomNavItem("scanTab", Icons.Filled.QrCodeScanner, "Scan")
    object Chat : BottomNavItem("chatTab", Icons.Filled.Chat, "Chat")
}

@Composable
fun MainScreen(navController: NavHostController) {
    val bottomNavController = rememberNavController()
    val items = listOf(
        BottomNavItem.Chat,
        BottomNavItem.Scan,
        BottomNavItem.Profile

    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentDestination = bottomNavController.currentBackStackEntryAsState().value?.destination
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.route == item.route,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Profile.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Profile.route) {
                Log.d("InfoTrack", "MainScreen: Clicked Profile Button")
                ProfileScreen(navController)
            }
            composable(BottomNavItem.Scan.route) {
                SearchScreen(navController)
            }
            composable(BottomNavItem.Chat.route) {
                ChatScreen(navController)
            }
        }
    }
}

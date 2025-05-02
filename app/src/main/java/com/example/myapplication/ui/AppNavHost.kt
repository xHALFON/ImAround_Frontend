package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.example.myapplication.ui.login.LoginScreen
import com.example.myapplication.ui.mainscreen.MainScreen
import com.example.myapplication.ui.profile.ProfileScreen
import com.example.myapplication.ui.register.RegisterScreen
import com.example.myapplication.ui.search.SearchScreen
import com.example.myapplication.ui.hobbies.HobbySelectionScreen
import com.example.myapplication.ui.hobbies.HobbyViewModel



@Composable
fun AppNavHost(navController: NavHostController) {
    // Create shared HobbyViewModel to persist selections between screens
    val hobbyViewModel: HobbyViewModel = viewModel()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController=navController,hobbyViewModel = hobbyViewModel) }
        composable("hobby_selection") { HobbySelectionScreen(navController = navController, viewModel = hobbyViewModel)}
        composable("main") { MainScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
        composable("search") { SearchScreen(navController) }

    }
}
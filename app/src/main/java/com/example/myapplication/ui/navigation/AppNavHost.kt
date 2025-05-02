package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.example.myapplication.ui.login.LoginScreen
import com.example.myapplication.ui.mainscreen.MainScreen
import com.example.myapplication.ui.profile.ProfileScreen
import com.example.myapplication.ui.register.RegisterScreen
import com.example.myapplication.ui.search.SearchScreen


@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("main") { MainScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
        composable("search") { SearchScreen(navController) }

    }
}
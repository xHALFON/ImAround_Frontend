package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.ui.login.LoginScreen
import com.example.myapplication.ui.mainscreen.MainScreen
import com.example.myapplication.ui.profile.ProfileScreen
import com.example.myapplication.ui.register.RegisterScreen
import com.example.myapplication.ui.search.SearchScreen
import com.example.myapplication.ui.search.SearchViewModel
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppNavHost(
    navController: NavHostController,
    searchViewModel: SearchViewModel? = null
) {
    val context = LocalContext.current
    val sessionManager = SessionManager(context)

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(navController)
        }

        composable("register") {
            RegisterScreen(navController)
        }

        composable("main") {
            MainScreen(navController)
        }

        composable("profile") {
            ProfileScreen(navController)
        }

        composable("search") {
            // טעינת מאצ'ים בכל פעם שהמשתמש נכנס למסך החיפוש
            searchViewModel?.let { viewModel ->
                LaunchedEffect(Unit) {
                    if (sessionManager.getUserId() != null) {
                        viewModel.loadMatches()
                    }
                }

                SearchScreen(navController = navController, viewModel = viewModel)
            } ?: SearchScreen(navController = navController) // במקרה ש-viewModel הוא null
        }
    }
}
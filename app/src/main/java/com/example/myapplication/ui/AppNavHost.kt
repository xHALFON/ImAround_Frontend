package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.ui.login.LoginScreen
import com.example.myapplication.ui.mainscreen.MainScreen
import com.example.myapplication.ui.profile.ProfileScreen
import com.example.myapplication.ui.register.RegisterScreen
import com.example.myapplication.ui.search.SearchScreen
import com.example.myapplication.ui.hobbies.HobbySelectionScreen
import com.example.myapplication.ui.hobbies.HobbyViewModel
import com.example.myapplication.ui.search.SearchViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.ui.profile.EditProfileScreen
import com.example.myapplication.ui.profile.ProfileViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    searchViewModel: SearchViewModel? = null
) {
    val context = LocalContext.current
    val sessionManager = SessionManager(context)
    val hobbyViewModel: HobbyViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()


    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(navController)
        }

        composable("register") {
            RegisterScreen(navController, hobbyViewModel = hobbyViewModel)
        }
        composable("hobby_selection") {
            HobbySelectionScreen(
                navController = navController,
                viewModel = hobbyViewModel,
                onSaveComplete = {
                    // 👇 This is optional but helps trigger recomposition
                    profileViewModel.userProfile.value =
                        profileViewModel.userProfile.value?.copy(
                            hobbies = hobbyViewModel.getSelectedHobbies()
                        )
                }
            )
        }





        composable("main") {
            MainScreen(navController)
        }

        composable("profile") {
            ProfileScreen(navController)
        }

        composable("edit_profile") {
            val user by profileViewModel.userProfile.observeAsState()
            user?.let {
                EditProfileScreen(
                    navController = navController,
                    user = it,
                    viewModel = profileViewModel,
                    hobbyViewModel = hobbyViewModel // 👈 pass shared instance
                )
            }
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
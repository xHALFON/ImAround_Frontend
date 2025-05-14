package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.model.UserResponse
import com.example.myapplication.ui.chat.ChatViewModel
import com.example.myapplication.ui.chat.ChatDetailScreen
import com.example.myapplication.ui.chat.compose.ChatListScreen
import com.example.myapplication.ui.hobbies.HobbySelectionScreen
import com.example.myapplication.ui.hobbies.HobbyViewModel
import com.example.myapplication.ui.login.LoginScreen
import com.example.myapplication.ui.mainscreen.MainScreen
import com.example.myapplication.ui.profile.EditProfileScreen
import com.example.myapplication.ui.profile.ProfileScreen
import com.example.myapplication.ui.profile.ProfileViewModel
import com.example.myapplication.ui.register.RegisterScreen
import com.example.myapplication.ui.search.SearchScreen
import com.example.myapplication.ui.search.SearchViewModel
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson

@Composable
fun AppNavHost(
    navController: NavHostController,
    searchViewModel: SearchViewModel? = null
) {
    val context = LocalContext.current
    val sessionManager = SessionManager(context)
    val hobbyViewModel: HobbyViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()

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
            SearchScreen(navController)
        }

        // Add new chat screens
        composable("chat_list") {
            ChatListScreen(
                onChatSelected = { matchId, chatPartner ->
                    // שנה ל
                    val safeMatchId = matchId ?: ""
                    val firstName = chatPartner.firstName ?: "User"
                    val lastName = chatPartner.lastName ?: ""
                    val userId = chatPartner._id ?: ""
                    val email = chatPartner.email ?: ""
                    val avatar = chatPartner.avatar ?: ""

                    navController.navigate("chat_detail/$safeMatchId/$userId/$firstName/$lastName")
                },
                sessionManager = sessionManager,
                viewModel = chatViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(
            route = "chat_detail/{matchId}/{userId}/{firstName}/{lastName}",
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType },
                navArgument("firstName") { type = NavType.StringType },
                navArgument("lastName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val firstName = backStackEntry.arguments?.getString("firstName") ?: "User"
            val lastName = backStackEntry.arguments?.getString("lastName") ?: ""

            // יצירת אובייקט חדש במקום המרה מJSON
            val chatPartner = UserResponse(
                _id = userId,
                firstName = firstName,
                lastName = lastName,
                email = "",
                avatar = ""
            )

            // המשך כרגיל
            ChatDetailScreen(
                matchId = matchId,
                chatPartner = chatPartner,
                onBackClick = { navController.popBackStack() },
                sessionManager = sessionManager,
                viewModel = chatViewModel
            )
        }
    }
}
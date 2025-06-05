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
import com.example.myapplication.ui.login.CompleteProfileScreen  // 🆕 הוסף את זה
import com.example.myapplication.ui.register.RegisterScreen
import com.example.myapplication.ui.search.SearchScreen
import com.example.myapplication.ui.search.SearchViewModel
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import java.net.URLEncoder
import java.net.URLDecoder

@Composable
fun AppNavHost(
    navController: NavHostController,
    searchViewModel: SearchViewModel? = null,
    startDestination: String = "login"
) {
    val context = LocalContext.current
    val sessionManager = SessionManager(context)
    val hobbyViewModel: HobbyViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(navController)
        }

        composable("register") {
            RegisterScreen(navController, hobbyViewModel = hobbyViewModel)
        }

        // 🆕 הוסף את המסך החדש הזה:
        composable("complete_profile") {
            CompleteProfileScreen(
                navController = navController,
                hobbyViewModel = hobbyViewModel  // משתמש באותו HobbyViewModel
            )
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
        composable("main_with_chat") {
            MainScreen(navController, startWithChatTab = true)
        }
        // Add new chat screens
        composable("chat_list") {
            ChatListScreen(
                onChatSelected = { matchId, chatPartner ->
                    val safeMatchId = matchId ?: ""
                    val firstName = URLEncoder.encode(chatPartner.firstName ?: "", "UTF-8")
                    val lastName = URLEncoder.encode(chatPartner.lastName ?: "", "UTF-8")
                    val userId = chatPartner._id ?: ""
                    val avatar = URLEncoder.encode(chatPartner.avatar ?: "", "UTF-8")

                    navController.navigate("chat_detail/$safeMatchId/$userId/$firstName/$lastName/$avatar")
                },
                sessionManager = sessionManager,
                viewModel = chatViewModel,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(
            route = "chat_detail/{matchId}/{userId}/{firstName}/{lastName}/{avatar}",
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType },
                navArgument("firstName") { type = NavType.StringType },
                navArgument("lastName") { type = NavType.StringType },
                navArgument("avatar") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val firstName = URLDecoder.decode(backStackEntry.arguments?.getString("firstName") ?: "", "UTF-8")
            val lastName = URLDecoder.decode(backStackEntry.arguments?.getString("lastName") ?: "", "UTF-8")
            val avatar = URLDecoder.decode(backStackEntry.arguments?.getString("avatar") ?: "", "UTF-8")

            val chatPartner = UserResponse(
                _id = userId,
                firstName = firstName,
                lastName = lastName,
                email = "",
                avatar = avatar
            )

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
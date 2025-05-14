package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.data.network.SocketManager
import com.example.myapplication.ui.AppNavHost
import com.example.myapplication.ui.chat.ChatViewModel
import com.example.myapplication.ui.search.SearchViewModel
import com.example.myapplication.ui.theme.SimpleLoginScreenTheme
import io.socket.client.Socket

class MainActivity : ComponentActivity() {

    private lateinit var searchViewModel: SearchViewModel
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // יצירת שירותים
        sessionManager = SessionManager(this)
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        // טעינת מאצ'ים מיד כשהאפליקציה עולה אם המשתמש מחובר
        if (sessionManager.getUserId() != null) {
            searchViewModel.loadMatches()
            // Also initialize the chat system
            SocketManager.getInstance().init("http://10.0.2.2:3000")
        }

        setContent {
            SimpleLoginScreenTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    searchViewModel = searchViewModel
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SimpleLoginScreenTheme {
        AppNavHost(navController = rememberNavController())
    }
}

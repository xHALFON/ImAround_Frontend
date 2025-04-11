package com.example.myapplication.ui.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.ui.components.AccountQueryComponent
import com.example.myapplication.ui.components.BottomComponent
import com.example.myapplication.ui.components.HeadingTextComponent
import com.example.myapplication.ui.components.MyTextFieldComponent
import com.example.myapplication.ui.components.NormalTextComponent
import com.example.myapplication.ui.components.PasswordTextFieldComponent
import com.example.myapplication.ui.login.LoginViewModel
import com.example.myapplication.ui.theme.AccentColor
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R

@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authResponse by viewModel.authResponse.observeAsState()
    val errorMessage by viewModel.errorMessage.observeAsState()

    // תגובה להתחברות מוצלחת
    LaunchedEffect(authResponse) {
        authResponse?.let {
            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
            navController.navigate("main")
        }
    }

    // הצגת שגיאה
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(28.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.iamaround_logo_new),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(width = 300.dp, height = 300.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(0.dp))

            Column {
                MyTextFieldComponent(
                    labelValue = "Email",
                    icon = Icons.Outlined.Email,
                    value = email,
                    onValueChange = { email = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                PasswordTextFieldComponent(
                    labelValue = "Password",
                    icon = Icons.Outlined.Lock,
                    value = password,
                    onValueChange = { password = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            BottomComponent(
                textQuery = "Don't have an account? ",
                textClickable = "Register",
                action = "Login",
                navController = navController,
                onActionClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.loginUser(email, password)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            AccountQueryComponent(
                textQuery = "Don't have an account? ",
                textClickable = "Register",

                navController = navController
            )
        }
    }
}

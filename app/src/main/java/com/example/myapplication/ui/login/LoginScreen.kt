package com.example.myapplication.ui.login

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.R
import com.example.myapplication.ui.components.MyTextFieldComponent
import com.example.myapplication.ui.components.PasswordTextFieldComponent

// Modern Color Palette - Same as Profile Screen
object ModernColors {
    val PrimaryBlue = Color(0xFF2563EB)
    val SecondaryBlue = Color(0xFF3B82F6)
    val LightBlue = Color(0xFFDBEAFE)
    val SoftGray = Color(0xFFF8FAFC)
    val MediumGray = Color(0xFF64748B)
    val DarkGray = Color(0xFF1E293B)
    val Success = Color(0xFF10B981)
    val Danger = Color(0xFFEF4444)
    val Warning = Color(0xFFF59E0B)
    val CardBackground = Color(0xFFFFFFFF)
    val DividerColor = Color(0xFFE2E8F0)
}

@Composable
fun LoginScreen(
    navController: NavHostController,
    loginViewModel: LoginViewModel = viewModel(),
    googleSignInViewModel: GoogleSignInViewModel = viewModel()
) {
    val context = LocalContext.current

    // Regular login state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Regular login observables
    val authResponse by loginViewModel.authResponse.observeAsState()
    val loginErrorMessage by loginViewModel.errorMessage.observeAsState()

    // Google Sign-In observables
    val googleAuthResponse by googleSignInViewModel.authResponse.observeAsState()
    val googleErrorMessage by googleSignInViewModel.errorMessage.observeAsState()
    val isGoogleLoading by googleSignInViewModel.isLoading.observeAsState(false)

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        googleSignInViewModel.handleSignInResult(task, context)
    }

    // Handle regular login success
    LaunchedEffect(authResponse) {
        authResponse?.let {
            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
            navController.navigate("main") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    // Handle Google Sign-In success
    LaunchedEffect(googleAuthResponse) {
        googleAuthResponse?.let { response ->
            Toast.makeText(context, "Welcome ${response.firstName}!", Toast.LENGTH_SHORT).show()

            if (response.needsCompletion) {
                navController.navigate("complete_profile") {
                    popUpTo("login") { inclusive = true }
                }
            } else {
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }

    // Handle errors
    LaunchedEffect(loginErrorMessage) {
        loginErrorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(googleErrorMessage) {
        googleErrorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    // Modern Background with subtle gradient
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ModernColors.SoftGray,
                            Color.White
                        ),
                        startY = 0f,
                        endY = 1000f
                    )
                )
        ) {
            // הוספת גלילה אנכית
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // זה התיקון העיקרי!
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(32.dp))

                // App Logo
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.iamaround_logo_new),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(width = 260.dp, height = 260.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Welcome text with modern typography
                Text(
                    text = "Welcome Back!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ModernColors.DarkGray,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sign in to continue your journey",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = ModernColors.MediumGray,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.3.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Email & Password Fields
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MyTextFieldComponent(
                        labelValue = "Email",
                        icon = Icons.Outlined.Email,
                        value = email,
                        onValueChange = { email = it }
                    )

                    PasswordTextFieldComponent(
                        labelValue = "Password",
                        icon = Icons.Outlined.Lock,
                        value = password,
                        onValueChange = { password = it }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Modern Login Button
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        } else {
                            loginViewModel.loginUser(email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ModernColors.PrimaryBlue
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Text(
                        text = "Login",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Modern Divider with "OR"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = ModernColors.DividerColor,
                        thickness = 1.dp
                    )
                    Text(
                        text = "OR",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ModernColors.MediumGray,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        letterSpacing = 1.sp
                    )
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = ModernColors.DividerColor,
                        thickness = 1.dp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Modern Google Sign-In Button
                OutlinedButton(
                    onClick = {
                        try {
                            val googleSignInClient = googleSignInViewModel.getGoogleSignInClient(context)
                            val signInIntent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(signInIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Google Sign-In setup error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isGoogleLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = ModernColors.DarkGray
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        ModernColors.DividerColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 6.dp
                    )
                ) {
                    if (isGoogleLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ModernColors.PrimaryBlue,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Signing in...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.3.sp
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Google Icon
                            Icon(
                                painter = painterResource(id = R.drawable.google_svg),
                                contentDescription = "Google",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Modern Register Link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account? ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = ModernColors.MediumGray,
                        letterSpacing = 0.2.sp
                    )
                    TextButton(
                        onClick = { navController.navigate("register") },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Register",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ModernColors.PrimaryBlue,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Modern Footer text
                Text(
                    text = "By continuing, you agree to our Terms of Service\nand Privacy Policy",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = ModernColors.MediumGray.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    letterSpacing = 0.2.sp
                )

                // רווח נוסף בתחתית כדי לוודא שהתוכן לא נחתך
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
package com.example.myapplication.ui.register

import android.app.DatePickerDialog
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.ui.components.BottomComponent
import com.example.myapplication.ui.components.CheckboxComponent
import com.example.myapplication.ui.components.DatePickerField
import com.example.myapplication.ui.components.DateTextFieldComponent
import com.example.myapplication.ui.components.HeadingTextComponent
import com.example.myapplication.ui.components.MyTextFieldComponent
import com.example.myapplication.ui.components.NormalTextComponent
import com.example.myapplication.ui.components.PasswordTextFieldComponent
import java.util.*
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.sp

@Composable
fun RegisterScreen(
    navController: NavHostController,
    viewModel: RegisterViewModel = viewModel()
) {
    val context = LocalContext.current

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    val authResponse by viewModel.authResponse.observeAsState()
    val errorMessage by viewModel.errorMessage.observeAsState()

    // בחירת תמונה
    val imagePickerLauncher = rememberLauncherForActivityResult(GetContent()) { uri ->
        imageUri = uri
        if (uri != null) {
            showPreviewDialog = true
        }
    }

    // תאריך לידה
    fun showDatePicker() {
        Log.d("RegisterScreen", "showDatePicker() called") // ✅ כאן
        val calendar = Calendar.getInstance()
        DatePickerDialog(context,
            { _, y, m, d -> dob = "$d/${m + 1}/$y" },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ניהול תגובות
    LaunchedEffect(authResponse) {
        authResponse?.let {
            Toast.makeText(context, "Registered successfully!", Toast.LENGTH_SHORT).show()
            navController.navigate("login")
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    if (showPreviewDialog && imageUri != null) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Profile Picture Preview") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showPreviewDialog = false },
                        modifier = Modifier
                            .fillMaxWidth(0.6f) // 60% מרוחב הדיאלוג
                            .height(48.dp)
                    ) {
                        Text(text = "Save", fontSize = 18.sp)
                    }
                }
            },
            confirmButton = {}, // ננטרל את ברירת המחדל
        )
    }

    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(28.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NormalTextComponent(value = "Hello there,")
            HeadingTextComponent(value = "Create an Account")
            Spacer(modifier = Modifier.height(25.dp))

            Column {
                MyTextFieldComponent(
                    labelValue = "First Name",
                    icon = Icons.Outlined.Person,
                    value = firstName,
                    onValueChange = { firstName = it }
                )
                Spacer(modifier = Modifier.height(10.dp))
                MyTextFieldComponent(
                    labelValue = "Last Name",
                    icon = Icons.Outlined.Person,
                    value = lastName,
                    onValueChange = { lastName = it }
                )
                Spacer(modifier = Modifier.height(10.dp))
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
                Spacer(modifier = Modifier.height(10.dp))

                // תאריך לידה
                DatePickerField(
                    label = "Date of Birth",
                    value = dob,
                    onClick = { showDatePicker() }
                )
                Spacer(modifier = Modifier.height(10.dp))

                // העלאת תמונה
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upload Profile Picture")
                }


                Spacer(modifier = Modifier.height(10.dp))

                CheckboxComponent()

                Spacer(modifier = Modifier.height(10.dp))

                BottomComponent(
                    textQuery = "Already have an account? ",
                    textClickable = "Login",
                    action = "Register",
                    navController = navController,
                    onActionClick = {
                        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank() || dob.isBlank()) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.registerUser(firstName, lastName, email, password, dob, imageUri)
                        }
                    }
                )
            }
        }
    }
}



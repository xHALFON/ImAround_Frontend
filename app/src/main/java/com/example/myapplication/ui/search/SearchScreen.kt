package com.example.myapplication.ui.search

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val users by viewModel.usersResponse.observeAsState(emptyList())
    val error by viewModel.errorMessage.observeAsState()

    // הרשאות בלוטות'
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }
        if (granted) {
            viewModel.startSearch()
        } else {
            Toast.makeText(context, "יש לאשר הרשאות Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Button(
            onClick = { permissionLauncher.launch(requiredPermissions) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("חיפוש משתמשים קרובים")
        }

        Spacer(modifier = Modifier.height(16.dp))


        if (users.isEmpty()) {
            Text("תוצאות סריקה יופיעו כאן")
        } else {
            users.forEach { user ->
                Image(
                    painter = rememberAsyncImagePainter(user.avatar),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(140.dp)

                )

                Text("שם: ${user.firstName} ${user.lastName}\nאימייל: ${user.email}")
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

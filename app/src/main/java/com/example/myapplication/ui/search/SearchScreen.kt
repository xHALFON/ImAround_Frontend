/*
package com.example.myapplication.ui.search

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester.Companion.createRefs
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ChainStyle
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
*/

package com.example.myapplication.ui.search

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.HeartBroken
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
import androidx.constraintlayout.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.R
import com.example.myapplication.data.model.UserResponse

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val users by viewModel.usersResponse.observeAsState(emptyList())
    val error by viewModel.errorMessage.observeAsState()

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

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { permissionLauncher.launch(requiredPermissions) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("חיפוש משתמשים קרובים")
        }

        if (users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("תוצאות סריקה יופיעו כאן")
            }
        } else {
            // Show top user (could be enhanced with paging/swipe)
            val topUser = users.first()
            TinderProfileScreen(
                user = topUser
            )
        }
    }
}

@Composable
fun TinderProfileScreen(user: UserResponse) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray)
    ) {
        val (profileImage, nameText, locationText, dislikeIcon, superLikeIcon, likeIcon) = createRefs()

        createHorizontalChain(
            dislikeIcon,
            superLikeIcon,
            likeIcon,
            chainStyle = ChainStyle.SpreadInside
        )

        val guideBottom = createGuidelineFromBottom(48.dp)

        // Async Coil Image (instead of static drawable)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .constrainAs(profileImage) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
        ) {
            AsyncImage(
                model = user.avatar,
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize()
            )
            GradientOverlay()
        }

        NameAndEmail(
            name = "${user.firstName} ${user.lastName}",
            email = user.email,
            modifier = Modifier.constrainAs(nameText) {
                end.linkTo(parent.end)
                start.linkTo(parent.start)
                bottom.linkTo(guideBottom, margin = 100.dp)
            }
        )

        IconButton(
            onClick = { /* TODO: Handle Dislike */ },
            modifier = Modifier.constrainAs(dislikeIcon) {
                bottom.linkTo(guideBottom)
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.HeartBroken,
                contentDescription = "Dislike",
                tint = Color.Magenta,
                modifier = Modifier.size(50.dp)
            )
        }

        IconButton(
            onClick = { /* TODO: Handle Super Like */ },
            modifier = Modifier.constrainAs(superLikeIcon) {
                bottom.linkTo(guideBottom)
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = "Super Like",
                tint = Color.Yellow,
                modifier = Modifier.size(50.dp)
            )
        }

        IconButton(
            onClick = { /* TODO: Handle Like */ },
            modifier = Modifier.constrainAs(likeIcon) {
                bottom.linkTo(guideBottom)
            }
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Like",
                tint = Color.Red,
                modifier = Modifier.size(50.dp)
            )
        }
    }
}

@Composable
private fun NameAndEmail(name: String, email: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = name,
            fontSize = 28.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = email,
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GradientOverlay() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(fraction = 0.5f)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black
                    )
                )
            )
    )
}



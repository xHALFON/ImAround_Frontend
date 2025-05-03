package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun CustomBottomNavigationBar(
    currentRoute: String?,
    onItemSelected: (String) -> Unit,
    onFabClick: () -> Unit
) {
    val items = listOf(
        BottomNavItem.Chat,
        BottomNavItem.Profile
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {
        // Top curved part of the navigation bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(70.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            shadowElevation = 8.dp,
            color = Color.White
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // First item (Chat)
                NavItem(
                    item = items[0],
                    isSelected = currentRoute == items[0].route,
                    onItemSelected = onItemSelected,
                    modifier = Modifier.weight(1f)
                )

                // Center empty space for FAB
                Spacer(modifier = Modifier.weight(1f))

                // Last item (Profile)
                NavItem(
                    item = items[1],
                    isSelected = currentRoute == items[1].route,
                    onItemSelected = onItemSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Floating action button (Scan)
        FloatingActionButton(
            onClick = {
                // Just call onItemSelected to navigate to the scan route
                onItemSelected(BottomNavItem.Scan.route)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
                .size(60.dp)
                .zIndex(1f),
            containerColor = Color(0xFF6F75E8),
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(
                imageVector = BottomNavItem.Scan.icon,
                contentDescription = BottomNavItem.Scan.label,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
    ) {
        val tint = if (isSelected) Color(0xFF6F75E8) else Color.Gray

        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = item.label,
            color = tint,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Profile : BottomNavItem("profileTab", Icons.Filled.AccountCircle, "Profile")
    object Scan : BottomNavItem("scanTab", Icons.Filled.QrCodeScanner, "Scan")
    object Chat : BottomNavItem("chatTab", Icons.Filled.Chat, "Chat")
}
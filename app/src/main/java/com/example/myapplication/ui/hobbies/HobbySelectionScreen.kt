package com.example.myapplication.ui.hobbies

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

/**
 * Data class representing a hobby item with its visual properties
 */
data class HobbyItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

/**
 * The full-screen hobby selection screen
 * Can be used both during registration and from settings
 */
@Composable
fun HobbySelectionScreen(
    navController: NavHostController,
    viewModel: HobbyViewModel = viewModel(),
    onSaveComplete: (() -> Unit)? = null
) {
    val selectedHobbies by viewModel.selectedHobbies.observeAsState(emptyList())
    val scrollState = rememberScrollState()
    val maxSelection = 5

    // Define hobby items with icons and bolder, more vibrant colors
    val hobbies = listOf(
        HobbyItem("gaming", "Gaming", Icons.Filled.SportsEsports, Color(0xFFBDBDBD)),
        HobbyItem("dancing", "Dancing", Icons.Filled.MusicNote, Color(0xFFFF8A80)),
        HobbyItem("language", "Language", Icons.Filled.Translate, Color(0xFF80DEEA)),
        HobbyItem("music", "Music", Icons.Filled.MusicNote, Color(0xFFCE93D8)),
        HobbyItem("movie", "Movie", Icons.Filled.Movie, Color(0xFFF48FB1)),
        HobbyItem("photography", "Photography", Icons.Filled.PhotoCamera, Color(0xFFCFD8DC)),
        HobbyItem("architecture", "Architecture", Icons.Filled.Architecture, Color(0xFFA5D6A7)),
        HobbyItem("fashion", "Fashion", Icons.Filled.Checkroom, Color(0xFFF48FB1)),
        HobbyItem("book", "Book", Icons.Filled.MenuBook, Color(0xFFCE93D8)),
        HobbyItem("writing", "Writing", Icons.Filled.Create, Color(0xFF80CBC4)),
        HobbyItem("nature", "Nature", Icons.Filled.Park, Color(0xFFC5E1A5)),
        HobbyItem("painting", "Painting", Icons.Filled.Palette, Color(0xFFFFF59D)),
        HobbyItem("football", "Football", Icons.Filled.SportsSoccer, Color(0xFFB0BEC5)),
        HobbyItem("people", "People", Icons.Filled.People, Color(0xFFFFF59D)),
        HobbyItem("animals", "Animals", Icons.Filled.Pets, Color(0xFFCE93D8)),
        HobbyItem("fitness", "Gym & Fitness", Icons.Filled.FitnessCenter, Color(0xFFFFF59D))
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            navController.popBackStack()
                            onSaveComplete?.invoke()
                        }
                )

                Text(
                    text = "Select up to $maxSelection interests",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF4A148C),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Hobbies grid - arranged to match your design
            val chunkedHobbies = hobbies.chunked(2)

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                chunkedHobbies.forEach { rowHobbies ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowHobbies.forEach { hobby ->
                            val isSelected = selectedHobbies.contains(hobby.id)
                            val isSelectable = selectedHobbies.size < maxSelection || isSelected

                            HobbyChip(
                                hobby = hobby,
                                isSelected = isSelected,
                                enabled = isSelectable,
                                modifier = Modifier.weight(1f),
                                onSelected = { viewModel.toggleHobbySelection(hobby.id) }
                            )
                        }

                        // Fill with empty spaces if row is not complete
                        if (rowHobbies.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Progress indicator at the bottom
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${selectedHobbies.size}/$maxSelection",
                    fontSize = 16.sp,
                    color = Color(0xFF4A148C)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = selectedHobbies.size.toFloat() / maxSelection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFBA68C8),
                    trackColor = Color(0xFFE1BEE7)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save button
            Button(
                onClick = {
                    onSaveComplete?.invoke() // 👈 Move this after popBackStack
                    navController.popBackStack()
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedHobbies.isNotEmpty()) Color(0xFF4A148C) else Color.Gray
                ),
                enabled = selectedHobbies.isNotEmpty(),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("Save", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun HobbyChip(
    hobby: HobbyItem,
    isSelected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSelected: () -> Unit
) {
    // Always use the hobby's color when selected
    val backgroundColor = if (isSelected) hobby.color else Color.White
    // Always use the hobby's color for border when selected
    val borderColor = if (isSelected) hobby.color else Color.LightGray
    // Text is always black for better readability
    val textColor = Color.Black.copy(alpha = if (isSelected) 1f else 0.7f)
    // Icon color is always based on the hobby color for consistent identity
    val iconColor = if (isSelected) Color.Black else hobby.color.copy(alpha = 0.8f)

    // Adjust opacity to show disabled state if needed
    val alpha = if (enabled) 1f else 0.6f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor.copy(alpha = alpha))
            .border(1.dp, borderColor.copy(alpha = alpha), RoundedCornerShape(20.dp))
            .clickable(enabled = enabled) { onSelected() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = hobby.icon,
            contentDescription = hobby.name,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = hobby.name,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp
        )
    }
}
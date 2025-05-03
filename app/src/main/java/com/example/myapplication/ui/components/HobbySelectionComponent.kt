package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class HobbyItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun HobbySelectionDialog(
    show: Boolean,
    maxSelection: Int = 5,
    selectedHobbies: List<String>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onHobbySelected: (String) -> Unit
) {
    if (!show) return

    // Define hobby items with icons and colors
    val hobbies = listOf(
        HobbyItem("gaming", "Gaming", Icons.Filled.SportsEsports, Color(0xFFE0E0E0)),
        HobbyItem("dancing", "Dancing", Icons.Filled.MusicNote, Color(0xFFFFEBEE)),
        HobbyItem("language", "Language", Icons.Filled.Translate, Color(0xFFE0F7FA)),
        HobbyItem("music", "Music", Icons.Filled.MusicNote, Color(0xFFE1BEE7)),
        HobbyItem("movie", "Movie", Icons.Filled.Movie, Color(0xFFF8BBD0)),
        HobbyItem("photography", "Photography", Icons.Filled.PhotoCamera, Color(0xFFE0E0E0)),
        HobbyItem("architecture", "Architecture", Icons.Filled.Architecture, Color(0xFFE0E0E0)),
        HobbyItem("fashion", "Fashion", Icons.Filled.Checkroom, Color(0xFFF8BBD0)),
        HobbyItem("book", "Book", Icons.Filled.MenuBook, Color(0xFFE1BEE7)),
        HobbyItem("writing", "Writing", Icons.Filled.Create, Color(0xFFE0E0E0)),
        HobbyItem("nature", "Nature", Icons.Filled.Park, Color(0xFFDCEDC8)),
        HobbyItem("painting", "Painting", Icons.Filled.Palette, Color(0xFFFFF9C4)),
        HobbyItem("football", "Football", Icons.Filled.SportsSoccer, Color(0xFFE0E0E0)),
        HobbyItem("people", "People", Icons.Filled.People, Color(0xFFFFF9C4)),
        HobbyItem("animals", "Animals", Icons.Filled.Pets, Color(0xFFE1BEE7)),
        HobbyItem("fitness", "Gym & Fitness", Icons.Filled.FitnessCenter, Color(0xFFFFF9C4))
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
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
                            .clickable { onDismiss() }
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

                Spacer(modifier = Modifier.height(24.dp))

                // Hobbies grid - arranged to match your design
                val chunkedHobbies = hobbies.chunked(3)

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    chunkedHobbies.forEach { rowHobbies ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                    onSelected = { onHobbySelected(hobby.id) }
                                )
                            }

                            // Fill with empty spaces if row is not complete
                            if (rowHobbies.size < 3) {
                                repeat(3 - rowHobbies.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress indicator
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

                Spacer(modifier = Modifier.height(24.dp))

                // Continue button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedHobbies.isNotEmpty()) Color(0xFF4A148C) else Color.Gray
                    ),
                    enabled = selectedHobbies.isNotEmpty(),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Continue", fontSize = 16.sp)
                }
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
    val backgroundColor = if (isSelected) hobby.color else Color.White
    val borderColor = if (isSelected) hobby.color else Color.LightGray
    val textColor = if (isSelected) Color.Black else Color.Gray
    val iconColor = if (isSelected) Color.Black else Color.Gray

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
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
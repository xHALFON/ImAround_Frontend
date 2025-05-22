package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GenderInterestSelector(
    selectedGender: String,
    onGenderSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Interested In",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF4A148C),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GenderOption(
                text = "Male",
                icon = Icons.Default.Male,
                isSelected = selectedGender == "Male",
                onClick = { onGenderSelected("Male") },
                modifier = Modifier.weight(1f)
            )

            GenderOption(
                text = "Female",
                icon = Icons.Default.Female,
                isSelected = selectedGender == "Female",
                onClick = { onGenderSelected("Female") },
                modifier = Modifier.weight(1f)
            )

            GenderOption(
                text = "Both",
                icon = null, // Custom icon handling for "Both"
                isSelected = selectedGender == "Both",
                onClick = { onGenderSelected("Both") },
                modifier = Modifier.weight(1f),
                isBoth = true
            )
        }
    }
}

@Composable
fun GenderOption(
    text: String,
    icon: ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBoth: Boolean = false
) {
    val backgroundColor = if (isSelected) Color(0xFF6F75E8).copy(alpha = 0.2f) else Color.White
    val borderColor = if (isSelected) Color(0xFF6F75E8) else Color.LightGray

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isSelected) Color(0xFF6F75E8) else Color.LightGray.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(percent = 50)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isBoth) {
                // Custom rendering for "Both" option
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Male,
                        contentDescription = "Male",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Female,
                        contentDescription = "Female",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF6F75E8) else Color.DarkGray
        )
    }
}
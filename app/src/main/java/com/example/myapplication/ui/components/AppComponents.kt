package com.example.myapplication.ui.components


import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.R
import com.example.myapplication.ui.profile.CardBackgroundColor
import com.example.myapplication.ui.profile.PrimaryColor
import com.example.myapplication.ui.profile.TextPrimaryColor
import com.example.myapplication.ui.profile.TextSecondaryColor
import com.example.myapplication.ui.theme.AccentColor
import com.example.myapplication.ui.theme.GrayColor
import com.example.myapplication.ui.theme.Secondary
import com.example.myapplication.ui.theme.TextColor




@Composable
fun NormalTextComponent(value: String) {
    Text(
        text = value,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        style = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Normal
        ),
        color = TextColor,
        textAlign = TextAlign.Center
    )
}

@Composable
fun HeadingTextComponent(value: String) {
    Text(
        text = value,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(),
        style = TextStyle(
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Normal
        ),
        color = TextColor,
        textAlign = TextAlign.Center
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTextFieldComponent(
    labelValue: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,     // ✅ Added for multi-line support
    maxLines: Int = 1               // ✅ Added for multi-line support
) {
    OutlinedTextField(
        label = { Text(text = labelValue) },
        value = value,
        onValueChange = onValueChange,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = AccentColor,
            unfocusedIndicatorColor = GrayColor,
            focusedLeadingIconColor = AccentColor,
            unfocusedLeadingIconColor = GrayColor,
            focusedLabelColor = AccentColor,
            unfocusedLabelColor = GrayColor
        ),
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
        readOnly = readOnly,
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = "input icon")
        },
        keyboardOptions = KeyboardOptions.Default,
        singleLine = singleLine,    // ✅ Added for multi-line support
        maxLines = maxLines         // ✅ Added for multi-line support
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordTextFieldComponent(
    labelValue: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        label = { Text(text = labelValue) },
        value = value,
        onValueChange = onValueChange,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = AccentColor,
            unfocusedIndicatorColor = GrayColor,
            focusedLeadingIconColor = AccentColor,
            unfocusedLeadingIconColor = GrayColor,
            focusedLabelColor = AccentColor,
            unfocusedLabelColor = GrayColor
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = "password icon")
        },
        trailingIcon = {
            val iconImage = if (isPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff
            val description = if (isPasswordVisible) "Show Password" else "Hide Password"
            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                Icon(imageVector = iconImage, contentDescription = description)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
    )
}

@Composable
fun CheckboxComponent() {
    var isChecked by remember {
        mutableStateOf(false)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(56.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = {
                isChecked = it
            }
        )
        ClickableTextComponent()
    }
}
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) PrimaryColor else TextSecondaryColor.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = if (enabled) TextSecondaryColor else TextSecondaryColor.copy(alpha = 0.5f)
                )

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = if (enabled) TextPrimaryColor else TextSecondaryColor.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
                )
            }
        }
    }
}

@Composable
fun ClickableTextComponent() {
    val initialText = "By continuing you accept our "
    val privacyPolicyText = "Privacy Policy"
    val andText = " and "
    val termOfUseText = "Term of Use"

    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = TextColor)) {
            append(initialText)
        }
        withStyle(style = SpanStyle(color = Secondary)) {
            pushStringAnnotation(tag = privacyPolicyText, annotation = privacyPolicyText)
            append(privacyPolicyText)
        }
        withStyle(style = SpanStyle(color = TextColor)) {
            append(andText)
        }
        withStyle(style = SpanStyle(color = Secondary)) {
            pushStringAnnotation(tag = termOfUseText, annotation = termOfUseText)
            append(termOfUseText)
        }
        append(".")
    }

    ClickableText(text = annotatedString, onClick = {
        annotatedString.getStringAnnotations(it, it)
            .firstOrNull()?.also { annotation ->
                Log.d("ClickableTextComponent", "You have Clicked ${annotation.tag}")
            }
    })
}

@Composable
fun BottomComponent(
    textQuery: String,
    textClickable: String,
    action: String,
    navController: NavHostController,
    onActionClick: () -> Unit // ✅ NEW
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onActionClick, // ✅ השתמש ב-callback
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(listOf(Secondary, AccentColor)),
                            shape = RoundedCornerShape(50.dp)
                        )
                        .fillMaxWidth()
                        .heightIn(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = action, color = Color.White, fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    thickness = 1.dp,
                    color = GrayColor
                )
                Text(
                    text = "Or",
                    modifier = Modifier.padding(10.dp),
                    fontSize = 20.sp
                )
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    thickness = 1.dp,
                    color = GrayColor
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { /*TODO*/ },
                    colors = ButtonDefaults.buttonColors(Color.Transparent),
                    modifier = Modifier
                        .padding(4.dp)
                        .border(
                            width = 2.dp,
                            color = Color(android.graphics.Color.parseColor("#d2d2d2")),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.google_svg),
                        contentDescription = "Google Logo",
                        modifier = Modifier
                            .size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = { /*TODO*/ },
                    colors = ButtonDefaults.buttonColors(Color.Transparent),
                    modifier = Modifier
                        .padding(4.dp)
                        .border(
                            width = 2.dp,
                            color = Color(android.graphics.Color.parseColor("#d2d2d2")),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.facebook_svg),
                        contentDescription = "Google Logo",
                        modifier = Modifier
                            .size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(15.dp))
            AccountQueryComponent(textQuery, textClickable, navController)
        }
    }
}

@Composable
fun AccountQueryComponent(
    textQuery: String,
    textClickable: String,
    navController: NavHostController
) {
    val annonatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = TextColor, fontSize = 15.sp)) {
            append(textQuery)
        }
        withStyle(style = SpanStyle(color = Secondary, fontSize = 15.sp)) {
            pushStringAnnotation(tag = textClickable, annotation = textClickable)
            append(textClickable)
        }
    }

    ClickableText(text = annonatedString, onClick = {
        annonatedString.getStringAnnotations(it, it)
            .firstOrNull()?.also { annonation ->
                if (annonation.item == "Login") {
                    navController.navigate("Login")
                } else if (annonation.item == "Register") {
                    navController.navigate("Register")
                }
            }
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    // כדי למנוע focus ולשמור על click בלבד
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    OutlinedTextField(
        value = value,
        onValueChange = {}, // read-only
        label = { Text(label) },
        readOnly = true,   // ✅ לא ניתן להקליד
        enabled = true,    // ✅ נשאר בעיצוב רגיל
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.medium, // בדיוק כמו השדות שלך
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = "Calendar Icon"
            )
        },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = AccentColor,
            unfocusedIndicatorColor = GrayColor,
            focusedLeadingIconColor = AccentColor,
            unfocusedLeadingIconColor = GrayColor,
            focusedLabelColor = AccentColor,
            unfocusedLabelColor = GrayColor
        )
    )
}



@Composable
fun ScanScreen(navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Scan Screen")
    }
}












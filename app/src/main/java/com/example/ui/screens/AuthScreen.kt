package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.auth.AuthManager
import com.example.util.ImageCompressor
import com.example.ui.theme.WhatsAppBackground
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTealDark
import com.localsathi.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    var selectedProfileUri by remember { mutableStateOf<Uri?>(null) }

    // Use TextFieldValue to preserve composition and selection state across IME events.
    var fullNameValue by remember { mutableStateOf(TextFieldValue("")) }
    var emailValue by remember { mutableStateOf(TextFieldValue("")) }
    var phoneValue by remember { mutableStateOf(TextFieldValue("")) }
    var passwordValue by remember { mutableStateOf(TextFieldValue("")) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Gallery Picker for circular profile photo (zero-permission Photo Picker)
    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedProfileUri = uri
            errorMessage = null
        }
    }

    val inputFieldTextStyle = TextStyle(
        color = Color(0xFF000000),
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    )

    val inputFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF000000),
        unfocusedTextColor = Color(0xFF000000),
        focusedContainerColor = Color(0xFFFFFFFF),
        unfocusedContainerColor = Color(0xFFFFFFFF),
        cursorColor = Color(0xFF000000),
        focusedBorderColor = WhatsAppTealDark,
        unfocusedBorderColor = Color(0xFF90A4AE),
        focusedLabelColor = WhatsAppTealDark,
        unfocusedLabelColor = Color(0xFF424242),
        focusedLeadingIconColor = WhatsAppTealDark,
        unfocusedLeadingIconColor = WhatsAppTealDark,
        focusedTrailingIconColor = Color(0xFF333333),
        unfocusedTrailingIconColor = Color(0xFF666666)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WhatsAppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D47A1)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "Lokal Post Logo",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Lokal Post",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = WhatsAppTealDark
            )

            Text(
                text = "आस-पास की तस्वीरें (5 किमी दायरा)",
                fontSize = 14.sp,
                color = Color(0xFF424242),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Single Sign-Up Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "साइन अप (Sign Up)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppTealDark,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "खाता बनाएं या पहले से मौजूद होने पर सीधे जुड़ें",
                        fontSize = 12.sp,
                        color = Color(0xFF616161),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Circular "Photo Chuno" Profile Picker
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9))
                            .border(2.dp, WhatsAppTealDark, CircleShape)
                            .clickable {
                                profilePhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("profile_photo_picker"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedProfileUri != null) {
                            AsyncImage(
                                model = selectedProfileUri,
                                contentDescription = "Selected Profile Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Photo Chuno",
                                    tint = WhatsAppTealDark,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Photo Chuno",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppTealDark,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (selectedProfileUri != null) "प्रोफ़ाइल फोटो चुनी गई (बदलने के लिए टैप करें)"
                        else "गैलरी से प्रोफ़ाइल फोटो चुनें",
                        fontSize = 12.sp,
                        color = Color(0xFF555555),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. पूरा नाम (Full Name)
                    OutlinedTextField(
                        value = fullNameValue,
                        onValueChange = { newValue ->
                            fullNameValue = newValue
                            errorMessage = null
                        },
                        textStyle = inputFieldTextStyle,
                        label = { Text("पूरा नाम (Full Name)") },
                        placeholder = { Text("जैसे: राहुल शर्मा", color = Color(0xFF9E9E9E)) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_full_name_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = inputFieldColors
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. जीमेल (Gmail)
                    OutlinedTextField(
                        value = emailValue,
                        onValueChange = { newValue ->
                            emailValue = newValue
                            errorMessage = null
                        },
                        textStyle = inputFieldTextStyle,
                        label = { Text("जीमेल (Gmail)") },
                        placeholder = { Text("जैसे: yourname@gmail.com", color = Color(0xFF9E9E9E)) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = inputFieldColors
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. फोन नंबर (Phone Number)
                    OutlinedTextField(
                        value = phoneValue,
                        onValueChange = { newValue ->
                            phoneValue = newValue
                            errorMessage = null
                        },
                        textStyle = inputFieldTextStyle,
                        label = { Text("फोन नंबर (Phone Number)") },
                        placeholder = { Text("जैसे: 9876543210", color = Color(0xFF9E9E9E)) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_phone_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = inputFieldColors
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. पासवर्ड (Password)
                    OutlinedTextField(
                        value = passwordValue,
                        onValueChange = { newValue ->
                            passwordValue = newValue
                            errorMessage = null
                        },
                        textStyle = inputFieldTextStyle,
                        label = { Text("पासवर्ड (Password)") },
                        placeholder = { Text("पासवर्ड दर्ज करें", color = Color(0xFF9E9E9E)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = inputFieldColors
                    )

                    // Error Message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFD32F2F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Button (No OTP)
                    Button(
                        onClick = {
                            val fullName = fullNameValue.text
                            val email = emailValue.text
                            val phone = phoneValue.text
                            val password = passwordValue.text

                            if (fullName.isBlank()) {
                                errorMessage = "कृपया अपना पूरा नाम लिखें"
                                return@Button
                            }
                            if (email.isBlank() || !email.contains("@")) {
                                errorMessage = "कृपया सही जीमेल दर्ज करें"
                                return@Button
                            }
                            if (phone.isBlank()) {
                                errorMessage = "कृपया फोन नंबर दर्ज करें"
                                return@Button
                            }
                            if (password.length < 4) {
                                errorMessage = "पासवर्ड कम से कम 4 अक्षरों का होना चाहिए"
                                return@Button
                            }

                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                // Compress selected profile photo Bitmap at 50% JPEG quality -> Base64
                                val profileBase64 = withContext(Dispatchers.IO) {
                                    selectedProfileUri?.let { uri ->
                                        ImageCompressor.compressProfilePhotoToBase64(context, uri)
                                    } ?: ""
                                }
                                val (success, err) = authManager.signUpOrLogin(
                                    fullName = fullName,
                                    email = email,
                                    phone = phone,
                                    password = password,
                                    profilePhotoBase64 = profileBase64
                                )
                                isLoading = false
                                if (success) {
                                    onAuthSuccess()
                                } else {
                                    errorMessage = err ?: "साइन अप में समस्या आई"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button"),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "साइन अप करें (Sign Up)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "बिना OTP के सीधे लॉगिन व साइन-अप",
                        fontSize = 12.sp,
                        color = Color(0xFF757575),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

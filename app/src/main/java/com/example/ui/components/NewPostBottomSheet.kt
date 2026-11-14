package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.auth.AuthManager
import com.example.data.location.UserLocation
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTealDark
import java.io.File

private fun createTempCameraUri(context: Context): Uri {
    val imageFile = File(context.cacheDir, "lokal_post_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPostBottomSheet(
    sheetState: SheetState,
    isHindi: Boolean,
    userLocation: UserLocation?,
    isUploading: Boolean,
    onDismiss: () -> Unit,
    onSubmitPost: (authorName: String, caption: String, imageUri: Uri) -> Unit
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val savedName = remember { AuthManager(context).getUserProfile().fullName }
    var authorNameValue by remember {
        mutableStateOf(
            TextFieldValue(
                savedName.ifBlank { if (isHindi) "आस-पास का साथी" else "Nearby Neighbor" }
            )
        )
    }
    var captionValue by remember { mutableStateOf(TextFieldValue("")) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Direct Camera launcher via FileProvider
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val capturedUri = pendingCameraUri
        if (success && capturedUri != null) {
            selectedImageUri = capturedUri
            validationError = null
        }
    }

    val openCameraDirectly: () -> Unit = {
        try {
            val uri = createTempCameraUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            validationError = if (isHindi) {
                "कैमरा खोलने में समस्या आई: ${e.localizedMessage ?: "उपलब्ध नहीं"}"
            } else {
                "Could not open camera: ${e.localizedMessage ?: "Unavailable"}"
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCameraDirectly()
        } else {
            validationError = if (isHindi) {
                "फोटो खींचने के लिए कृपया कैमरा अनुमति दें।"
            } else {
                "Please grant Camera permission to take a photo."
            }
        }
    }

    val requestAndLaunchCamera: () -> Unit = {
        val hasCameraPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPerm) {
            openCameraDirectly()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Android Photo Picker launcher (zero permission, compliant with Play Policy)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            validationError = null
        } else if (selectedImageUri == null) {
            // If gallery has no photos or picker closed without selection, prompt camera fallback
            validationError = if (isHindi) {
                "गैलरी में कोई फोटो नहीं मिली? सीधे 'कैमरा खोलें' बटन दबाएं।"
            } else {
                "No photo selected from gallery? Tap 'Open Camera' directly."
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "नया फोटो पोस्ट करें" else "Post a New Photo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhatsAppTealDark
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Photo Selection Area (Direct Camera + Gallery fallback)
            if (selectedImageUri == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF0F2F5))
                        .border(1.5.dp, WhatsAppTealDark.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { requestAndLaunchCamera() }
                        .padding(16.dp)
                        .testTag("select_photo_button"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(WhatsAppGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Open Camera",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isHindi) "कैमरा खोलें और तुरंत फोटो लें" else "Open Camera & Take Photo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppTealDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { requestAndLaunchCamera() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_camera_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppTealDark,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "कैमरा" else "Camera",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } catch (_: Exception) {
                                    requestAndLaunchCamera()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_gallery_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = WhatsAppTealDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "गैलरी" else "Gallery",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppTealDark
                            )
                        }
                    }
                }
            } else {
                // Image preview with Camera / Gallery change buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE5E7EB))
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Photo",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(20.dp))
                                .clickable { requestAndLaunchCamera() }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isHindi) "कैमरा" else "Camera",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        Row(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(20.dp))
                                .clickable {
                                    try {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    } catch (_: Exception) {
                                        requestAndLaunchCamera()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isHindi) "गैलरी" else "Gallery",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (validationError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = validationError ?: "",
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Author Name Field
            OutlinedTextField(
                value = authorNameValue,
                onValueChange = { authorNameValue = it },
                textStyle = TextStyle(
                    color = Color(0xFF000000),
                    fontSize = 16.sp
                ),
                label = { Text(if (isHindi) "आपका नाम / पहचान" else "Your Name / Tag") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("author_name_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF000000),
                    unfocusedTextColor = Color(0xFF000000),
                    focusedContainerColor = Color(0xFFFFFFFF),
                    unfocusedContainerColor = Color(0xFFFFFFFF),
                    cursorColor = Color(0xFF000000),
                    focusedBorderColor = WhatsAppTealDark,
                    unfocusedBorderColor = Color(0xFF90A4AE),
                    focusedLabelColor = WhatsAppTealDark,
                    unfocusedLabelColor = Color(0xFF424242)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Caption Field (max 150 chars)
            OutlinedTextField(
                value = captionValue,
                onValueChange = { if (it.text.length <= 150) captionValue = it },
                textStyle = TextStyle(
                    color = Color(0xFF000000),
                    fontSize = 16.sp
                ),
                label = { Text(if (isHindi) "विवरण (अधिकतम 150 शब्द/अक्षर)" else "Caption (max 150 chars)") },
                placeholder = { Text(if (isHindi) "उदा. बाज़ार में ताज़ा फल, मोहल्ले की शाम..." else "e.g. Market street, neighborhood scene...", color = Color(0xFF9E9E9E)) },
                supportingText = {
                    Text(
                        text = "${captionValue.text.length}/150",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        fontSize = 11.sp,
                        color = if (captionValue.text.length >= 150) Color.Red else Color.Gray
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("caption_input"),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF000000),
                    unfocusedTextColor = Color(0xFF000000),
                    focusedContainerColor = Color(0xFFFFFFFF),
                    unfocusedContainerColor = Color(0xFFFFFFFF),
                    cursorColor = Color(0xFF000000),
                    focusedBorderColor = WhatsAppTealDark,
                    unfocusedBorderColor = Color(0xFF90A4AE),
                    focusedLabelColor = WhatsAppTealDark,
                    unfocusedLabelColor = Color(0xFF424242)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Location Info Pill (Ahmedabad default if GPS not available)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = WhatsAppTealDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isHindi) "स्थान (GPS जियो-टैग ऑटोमैटिक)" else "Location (GPS Geo-tag)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppTealDark
                    )
                    val coords = if (userLocation != null) {
                        "${String.format("%.4f", userLocation.latitude)}, ${String.format("%.4f", userLocation.longitude)}"
                    } else {
                        "23.0225, 72.5714 (अहमदाबाद)"
                    }
                    Text(
                        text = if (isHindi) "तस्वीर 5 किमी के आस-पास के साथियों को दिखेगी ($coords)"
                               else "Will be visible to neighbors within 5km ($coords)",
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    if (selectedImageUri == null) {
                        validationError = if (isHindi) "कृपया पहले कैमरा या गैलरी से तस्वीर चुनें!" else "Please take or select a photo first!"
                        return@Button
                    }
                    onSubmitPost(authorNameValue.text, captionValue.text, selectedImageUri!!)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_post_button"),
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhatsAppGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isHindi) "तस्वीर अपलोड हो रही है..." else "Uploading post...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "आस-पास पोस्ट करें" else "Post Nearby",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

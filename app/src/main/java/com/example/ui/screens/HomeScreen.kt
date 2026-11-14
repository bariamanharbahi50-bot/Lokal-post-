package com.example.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthManager
import com.example.data.model.Post
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.NewPostBottomSheet
import com.example.ui.components.PhotoDetailDialog
import com.example.ui.components.PostCard
import com.example.ui.theme.WhatsAppBackground
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTeal
import com.example.ui.theme.WhatsAppTealDark
import com.example.ui.viewmodel.PostViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PostViewModel,
    modifier: Modifier = Modifier,
    onLogout: (() -> Unit)? = null
) {
    val isHindi by viewModel.isHindi.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val isLocationLoading by viewModel.isLocationLoading.collectAsState()
    val isPermissionDenied by viewModel.isLocationPermissionDenied.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val posts by viewModel.filteredPosts.collectAsState()
    val notification by viewModel.notification.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeComments by viewModel.activeComments.collectAsState()
    val isLoadingComments by viewModel.isLoadingComments.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedPhotoPost by remember { mutableStateOf<Post?>(null) }
    var selectedCommentPost by remember { mutableStateOf<Post?>(null) }
    var isSearchVisible by remember { mutableStateOf(false) }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val shouldLoadMore by remember {
        androidx.compose.runtime.derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMoreStories()
        }
    }

    // Runtime Permission Request for Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        viewModel.onLocationPermissionResult(fineGranted || coarseGranted)
    }

    // Launch location permission on first composition
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Handle ViewModel snackbar notifications
    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar(it.message)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WhatsAppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // WhatsApp style circular floating camera button
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = WhatsAppGreen,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(60.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 4.dp, end = 4.dp)
                    .shadow(8.dp, CircleShape)
                    .testTag("fab_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = if (isHindi) "फोटो खींचें या अपलोड करें" else "Take or upload photo",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // WhatsApp Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WhatsAppTealDark)
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Lokal Post",
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val headerSubtitle = if (isHindi) "आस-पास की तस्वीरें (5 किमी)"
                                else "Nearby photos (5 km)"
                            Text(
                                text = headerSubtitle,
                                color = Color(0xFFDCF8C6),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Search Toggle
                            IconButton(
                                onClick = { isSearchVisible = !isSearchVisible },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Refresh Location
                            IconButton(
                                onClick = { viewModel.refreshPosts() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Language switch button
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = WhatsAppTeal,
                                modifier = Modifier
                                    .clickable { viewModel.toggleLanguage() }
                                    .testTag("language_toggle_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHindi) "हिन्दी" else "EN",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (onLogout != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = onLogout,
                                    modifier = Modifier.size(40.dp).testTag("logout_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = if (isHindi) "लॉग आउट" else "Log Out",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Search field expansion
                    AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = {
                                Text(
                                    text = if (isHindi) "तस्वीरें या साथी खोजें..." else "Search posts or users...",
                                    color = Color.LightGray,
                                    fontSize = 14.sp
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F4941),
                                unfocusedContainerColor = Color(0xFF0F4941),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = WhatsAppGreen
                            )
                        )
                    }
                }
            }

            // Permission warning only if user denied GPS
            if (isPermissionDenied) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = Color(0xFF856404)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "GPS बंद है — अहमदाबाद लोकेशन चालू है" else "GPS off — using Ahmedabad location",
                                color = Color(0xFF856404),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (isHindi) "अहमदाबाद (5 किमी) की तस्वीरें दिख रही हैं। लाइव GPS के लिए चालू करें।"
                                       else "Showing Ahmedabad (5 km) posts. Enable for live GPS.",
                                color = Color(0xFF856404),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppTealDark),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(if (isHindi) "चालू करें" else "Enable", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Main Feed
            if (posts.isEmpty()) {
                // Empty State with Call to Action
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color(0xFFE8F5E9), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = WhatsAppTealDark,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isHindi)
                                    "आपके 5 किमी के दायरे में अभी किसी ने फोटो पोस्ट नहीं की है।"
                                else
                                    "No photos posted within your 5 km radius yet.",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color(0xFF333333)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isHindi) "पहली फोटो आप डालें!" else "Be the first to share a photo!",
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                color = WhatsAppTealDark,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { showBottomSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "फोटो पोस्ट करें" else "Post Photo",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            isHindi = isHindi,
                            onLikeClick = { viewModel.toggleLike(post) },
                            onCommentClick = {
                                selectedCommentPost = post
                                viewModel.loadComments(post.id)
                            },
                            onPhotoClick = { selectedPhotoPost = post },
                            onDeleteClick = if (post.isUserPost) {
                                { viewModel.deletePost(post.id) }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheet for Adding New Post
    if (showBottomSheet) {
        NewPostBottomSheet(
            sheetState = sheetState,
            isHindi = isHindi,
            userLocation = userLocation,
            isUploading = isUploading,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showBottomSheet = false
                }
            },
            onSubmitPost = { authorName, caption, uri ->
                val profile = AuthManager(context).getUserProfile()
                viewModel.uploadPost(
                    authorName = authorName,
                    authorProfilePhotoBase64 = profile.profilePhotoBase64,
                    caption = caption,
                    imageUri = uri
                )
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showBottomSheet = false
                }
            }
        )
    }

    // Full Screen Photo Viewer Dialog
    selectedPhotoPost?.let { post ->
        PhotoDetailDialog(
            post = post,
            isHindi = isHindi,
            onDismiss = { selectedPhotoPost = null }
        )
    }

    // Comments Bottom Sheet
    selectedCommentPost?.let { post ->
        CommentsBottomSheet(
            sheetState = commentSheetState,
            post = post,
            comments = activeComments,
            isLoading = isLoadingComments,
            isHindi = isHindi,
            onDismiss = {
                scope.launch { commentSheetState.hide() }.invokeOnCompletion {
                    selectedCommentPost = null
                }
            },
            onAddComment = { commentText ->
                val profile = AuthManager(context).getUserProfile()
                viewModel.addComment(
                    postId = post.id,
                    authorName = profile.fullName,
                    authorProfilePhotoBase64 = profile.profilePhotoBase64,
                    commentText = commentText
                )
            }
        )
    }
}

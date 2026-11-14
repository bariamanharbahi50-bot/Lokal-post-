package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.location.LocationHelper
import com.example.data.location.UserLocation
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.repository.PostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiNotification(
    val message: String,
    val isError: Boolean = false
)

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)

    private val _isHindi = MutableStateFlow(true)
    val isHindi: StateFlow<Boolean> = _isHindi.asStateFlow()

    private val _userLocation = MutableStateFlow<UserLocation?>(null)
    val userLocation: StateFlow<UserLocation?> = _userLocation.asStateFlow()

    // Strictly fixed to 5.0 km
    val selectedRadiusKm: Double = 5.0

    private val _isLocationLoading = MutableStateFlow(true)
    val isLocationLoading: StateFlow<Boolean> = _isLocationLoading.asStateFlow()

    private val _isLocationPermissionDenied = MutableStateFlow(false)
    val isLocationPermissionDenied: StateFlow<Boolean> = _isLocationPermissionDenied.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeComments = MutableStateFlow<List<PostComment>>(emptyList())
    val activeComments: StateFlow<List<PostComment>> = _activeComments.asStateFlow()

    private val _isLoadingComments = MutableStateFlow(false)
    val isLoadingComments: StateFlow<Boolean> = _isLoadingComments.asStateFlow()

    val filteredPosts: StateFlow<List<Post>> = combine(
        repository.storiesFlow,
        _searchQuery
    ) { posts, query ->
        posts.filter { post ->
            val matchesRadius = (post.distanceKm ?: 0.0) <= 5.0
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                post.caption.contains(query, ignoreCase = true) ||
                post.authorName.contains(query, ignoreCase = true)
            }
            matchesRadius && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var locationJob: Job? = null

    init {
        // 1. Immediately provide default location (Ahmedabad) so user is never stuck loading
        val initialLoc = UserLocation(LocationHelper.DEFAULT_LAT, LocationHelper.DEFAULT_LON, isSimulated = false)
        _userLocation.value = initialLoc
        _isLocationLoading.value = false
        refreshPosts(initialLoc)

        // 2. Fetch real GPS / network location in background
        startLocationUpdates()

        // 3. 3-second max timeout for location verification
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000L)
            _isLocationLoading.value = false
        }
    }

    private fun startLocationUpdates() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            try {
                LocationHelper.getLocationFlow(getApplication()).collect { loc ->
                    if (loc != null) {
                        _userLocation.value = loc
                        _isLocationPermissionDenied.value = false
                        _isLocationLoading.value = false
                        refreshPosts(loc)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun onLocationPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            _isLocationPermissionDenied.value = false
            _isLocationLoading.value = false
            startLocationUpdates()
        } else {
            _isLocationPermissionDenied.value = true
            _isLocationLoading.value = false
            val fallback = UserLocation(LocationHelper.DEFAULT_LAT, LocationHelper.DEFAULT_LON, isSimulated = true)
            _userLocation.value = fallback
            refreshPosts(fallback)
        }
    }

    fun toggleLanguage() {
        _isHindi.value = !_isHindi.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshPosts(location: UserLocation? = _userLocation.value) {
        viewModelScope.launch {
            repository.loadStories(location, isRefresh = true)
        }
    }

    fun loadMoreStories() {
        if (!repository.isLoadingMore && !repository.isLastPage) {
            viewModelScope.launch {
                repository.loadStories(_userLocation.value, isRefresh = false)
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            repository.toggleLike(post.id, post.likesCount, post.isLiked)
        }
    }

    fun loadComments(postId: String) {
        viewModelScope.launch {
            _isLoadingComments.value = true
            _activeComments.value = repository.loadComments(postId)
            _isLoadingComments.value = false
        }
    }

    fun addComment(
        postId: String,
        authorName: String,
        authorProfilePhotoBase64: String = "",
        commentText: String
    ) {
        if (commentText.isBlank()) return
        viewModelScope.launch {
            val added = repository.addComment(postId, authorName, authorProfilePhotoBase64, commentText)
            _activeComments.value = _activeComments.value + added
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deleteStory(postId)
            showNotification(if (_isHindi.value) "स्टोरी हटा दी गई" else "Story removed")
        }
    }

    fun uploadPost(
        authorName: String,
        authorProfilePhotoBase64: String = "",
        caption: String,
        imageUri: Uri
    ) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val loc = _userLocation.value ?: UserLocation(LocationHelper.DEFAULT_LAT, LocationHelper.DEFAULT_LON)
                repository.createPhotoStory(
                    authorName = authorName,
                    authorProfilePhotoBase64 = authorProfilePhotoBase64,
                    caption = caption.take(150),
                    imageUri = imageUri,
                    latitude = loc.latitude,
                    longitude = loc.longitude
                )
                showNotification(if (_isHindi.value) "सफलतापूर्वक पोस्ट हो गया!" else "Successfully posted!")
            } catch (e: Exception) {
                showNotification(
                    if (_isHindi.value) "अपलोड में समस्या आई: ${e.localizedMessage}"
                    else "Upload failed: ${e.localizedMessage}",
                    isError = true
                )
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun showNotification(message: String, isError: Boolean = false) {
        _notification.value = UiNotification(message, isError)
    }

    fun clearNotification() {
        _notification.value = null
    }
}

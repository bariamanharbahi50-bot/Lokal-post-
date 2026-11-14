package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.auth.awaitTask
import com.example.data.location.LocationHelper
import com.example.data.location.UserLocation
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.util.ImageCompressor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class PostRepository(private val context: Context) {

    private val firestore get() = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null }
    private val auth get() = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }

    private fun getInitialAhmedabadPosts(loc: UserLocation? = null): List<Post> {
        val baseLat = loc?.latitude ?: LocationHelper.DEFAULT_LAT
        val baseLon = loc?.longitude ?: LocationHelper.DEFAULT_LON
        val dist1 = LocationHelper.calculateDistanceKm(baseLat, baseLon, LocationHelper.DEFAULT_LAT + 0.009, LocationHelper.DEFAULT_LON + 0.006)
        val dist2 = LocationHelper.calculateDistanceKm(baseLat, baseLon, LocationHelper.DEFAULT_LAT - 0.012, LocationHelper.DEFAULT_LON - 0.008)
        return listOf(
            Post(
                id = "lokal_post_ahmedabad_1",
                authorName = "अहमदाबाद लोकल कम्युनिटी",
                imageUrl = "android.resource://${context.packageName}/${com.localsathi.app.R.drawable.sample_bazaar}",
                caption = "अहमदाबाद बाजार में आज की ताजा रौनक! 5 किमी के दायरे में सभी साथियों का स्वागत है। 🌸🛍️",
                latitude = LocationHelper.DEFAULT_LAT + 0.009,
                longitude = LocationHelper.DEFAULT_LON + 0.006,
                distanceKm = (dist1 * 10).toInt() / 10.0,
                timestamp = System.currentTimeMillis() - 25 * 60 * 1000L,
                likesCount = 18,
                isLiked = false
            ),
            Post(
                id = "lokal_post_ahmedabad_2",
                authorName = "साबरमती फोटोग्राफी क्लब",
                imageUrl = "android.resource://${context.packageName}/${com.localsathi.app.R.drawable.sample_sunset}",
                caption = "साबरमती रिवरफ्रंट पर आज शाम का अद्भुत नज़ारा। 5 किमी के दायरे में अपनी तस्वीरें पोस्ट करें! 🌅✨",
                latitude = LocationHelper.DEFAULT_LAT - 0.012,
                longitude = LocationHelper.DEFAULT_LON - 0.008,
                distanceKm = (dist2 * 10).toInt() / 10.0,
                timestamp = System.currentTimeMillis() - 70 * 60 * 1000L,
                likesCount = 35,
                isLiked = false
            )
        )
    }

    private val _storiesFlow = MutableStateFlow<List<Post>>(getInitialAhmedabadPosts())
    val storiesFlow: Flow<List<Post>> = _storiesFlow.asStateFlow()

    private val localCommentsCache = mutableMapOf<String, MutableList<PostComment>>()
    private val userPhotoCache = mutableMapOf<String, String>()

    private suspend fun getUserProfilePhotoBase64(uid: String): String {
        if (uid.isBlank()) return ""
        userPhotoCache[uid]?.let { return it }
        return try {
            val doc = firestore?.collection("users")?.document(uid)?.get()?.awaitTask()
            val photo = doc?.getString("profilePhotoBase64") ?: ""
            userPhotoCache[uid] = photo
            photo
        } catch (_: Exception) {
            ""
        }
    }

    private var lastVisibleSnapshot: DocumentSnapshot? = null
    var isLastPage: Boolean = false
        private set
    var isLoadingMore: Boolean = false
        private set

    /**
     * Loads real photo stories from Firestore "photo_stories" collection.
     * Strictly within 5 km radius, loading 10 stories at a time with scroll pagination.
     */
    suspend fun loadStories(userLocation: UserLocation?, isRefresh: Boolean = false): List<Post> = withContext(Dispatchers.IO) {
        if (isRefresh) {
            lastVisibleSnapshot = null
            isLastPage = false
        }

        if (isLastPage && !isRefresh) {
            return@withContext _storiesFlow.value
        }

        isLoadingMore = true
        try {
            val activeLoc = userLocation ?: UserLocation(LocationHelper.DEFAULT_LAT, LocationHelper.DEFAULT_LON)
            val currentUid = auth?.currentUser?.uid ?: ""
            val db = firestore ?: return@withContext getInitialAhmedabadPosts(activeLoc)

            val targetBatchSize = 10
            val validStories = mutableListOf<Post>()
            var reachedEnd = false

            while (validStories.size < targetBatchSize && !reachedEnd) {
                var query: Query = db.collection("photo_stories")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(20)

                if (lastVisibleSnapshot != null) {
                    query = query.startAfter(lastVisibleSnapshot!!)
                }

                val querySnapshot = query.get().awaitTask()
                if (querySnapshot.isEmpty) {
                    reachedEnd = true
                    break
                }

                lastVisibleSnapshot = querySnapshot.documents.lastOrNull()
                if (querySnapshot.size() < 20) {
                    reachedEnd = true
                }

                for (doc in querySnapshot.documents) {
                    val lat = doc.getDouble("latitude") ?: continue
                    val lon = doc.getDouble("longitude") ?: continue
                    val dist = LocationHelper.calculateDistanceKm(
                        activeLoc.latitude,
                        activeLoc.longitude,
                        lat,
                        lon
                    )

                    // Strictly 5 km radius limit
                    if (dist <= 5.0) {
                        val base64 = doc.getString("imageBase64") ?: ""
                        val authorUid = doc.getString("authorUid") ?: ""
                        val likedBy = (doc.get("likedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        val isLiked = currentUid.isNotBlank() && likedBy.contains(currentUid)
                        var authorPhoto = doc.getString("authorProfilePhotoBase64") ?: ""
                        if (authorPhoto.isBlank() && authorUid.isNotBlank()) {
                            authorPhoto = getUserProfilePhotoBase64(authorUid)
                        }

                        validStories.add(
                            Post(
                                id = doc.id,
                                authorName = doc.getString("authorName") ?: "आस-पास का साथी",
                                authorProfilePhotoBase64 = authorPhoto,
                                imageUrl = base64,
                                caption = doc.getString("caption") ?: "",
                                latitude = lat,
                                longitude = lon,
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                likesCount = doc.getLong("likesCount")?.toInt() ?: likedBy.size,
                                commentsCount = doc.getLong("commentsCount")?.toInt() ?: 0,
                                isLiked = isLiked,
                                distanceKm = dist,
                                isUserPost = (authorUid == currentUid)
                            )
                        )
                        if (validStories.size >= targetBatchSize) break
                    }
                }
            }

            isLastPage = reachedEnd

            val updatedList = if (isRefresh) {
                if (validStories.isNotEmpty()) validStories else getInitialAhmedabadPosts(activeLoc)
            } else {
                val existingIds = _storiesFlow.value.map { it.id }.toSet()
                _storiesFlow.value + validStories.filterNot { it.id in existingIds }
            }

            _storiesFlow.value = updatedList
            updatedList
        } catch (e: Exception) {
            e.printStackTrace()
            _storiesFlow.value
        } finally {
            isLoadingMore = false
        }
    }

    /**
     * Photo Compression & 500 Auto-Delete Limit:
     * 1. Compresses uploaded photo under 200 KB as Base64.
     * 2. Enforces maximum 150-character caption text.
     * 3. Before saving, if total stories reach 500, automatically deletes the oldest story first.
     * 4. Saves new photo story into Firestore photo_stories collection.
     */
    suspend fun createPhotoStory(
        authorName: String,
        authorProfilePhotoBase64: String,
        caption: String,
        imageUri: Uri,
        latitude: Double,
        longitude: Double
    ): Post = withContext(Dispatchers.IO) {
        // Compress photo under 200 KB in Base64
        val base64Image = ImageCompressor.compressImageToBase64(context, imageUri)
            ?: throw IllegalStateException("तस्वीर कंप्रेस नहीं हो सकी, कृपया दोबारा प्रयास करें")

        val safeCaption = caption.take(150)
        val currentUid = auth?.currentUser?.uid ?: ""
        val db = firestore ?: throw IllegalStateException("Firebase Firestore शुरू नहीं हो सका")

        // Auto-delete oldest story if total reaches 500
        try {
            val totalQuery = db.collection("photo_stories")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(500)
                .get()
                .awaitTask()

            if (totalQuery.size() >= 500) {
                val oldestDoc = totalQuery.documents.firstOrNull()
                oldestDoc?.reference?.delete()?.awaitTask()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Save new story to Firestore photo_stories collection
        val storyData = hashMapOf(
            "authorName" to authorName.ifBlank { "आस-पास का साथी" },
            "authorUid" to currentUid,
            "authorProfilePhotoBase64" to authorProfilePhotoBase64,
            "imageBase64" to base64Image,
            "caption" to safeCaption,
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to System.currentTimeMillis(),
            "likesCount" to 0,
            "commentsCount" to 0,
            "likedBy" to emptyList<String>()
        )

        val docRef = db.collection("photo_stories").add(storyData).awaitTask()

        val newPost = Post(
            id = docRef.id,
            authorName = authorName.ifBlank { "आस-पास का साथी" },
            authorProfilePhotoBase64 = authorProfilePhotoBase64,
            imageUrl = base64Image,
            caption = safeCaption,
            latitude = latitude,
            longitude = longitude,
            timestamp = System.currentTimeMillis(),
            likesCount = 0,
            commentsCount = 0,
            isLiked = false,
            distanceKm = 0.0,
            isUserPost = true
        )

        _storiesFlow.value = listOf(newPost) + _storiesFlow.value
        newPost
    }

    /**
     * Toggles like state optimistically in UI and persists in Firestore
     */
    suspend fun toggleLike(postId: String, currentLikes: Int, currentlyLiked: Boolean) = withContext(Dispatchers.IO) {
        // Optimistic UI update so Like responds immediately on every post
        _storiesFlow.value = _storiesFlow.value.map { post ->
            if (post.id == postId) {
                val newLiked = !currentlyLiked
                val newCount = if (newLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
                post.copy(isLiked = newLiked, likesCount = newCount)
            } else {
                post
            }
        }

        val currentUid = auth?.currentUser?.uid ?: return@withContext
        val db = firestore ?: return@withContext
        val docRef = db.collection("photo_stories").document(postId)

        try {
            if (currentlyLiked) {
                docRef.update(
                    "likedBy", FieldValue.arrayRemove(currentUid),
                    "likesCount", FieldValue.increment(-1)
                ).awaitTask()
            } else {
                docRef.update(
                    "likedBy", FieldValue.arrayUnion(currentUid),
                    "likesCount", FieldValue.increment(1)
                ).awaitTask()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadComments(postId: String): List<PostComment> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext localCommentsCache[postId].orEmpty()
        try {
            val snapshot = db.collection("photo_stories")
                .document(postId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(50)
                .get()
                .awaitTask()

            val remoteComments = snapshot.documents.map { doc ->
                val cUid = doc.getString("authorUid") ?: ""
                var cPhoto = doc.getString("authorProfilePhotoBase64") ?: ""
                if (cPhoto.isBlank() && cUid.isNotBlank()) {
                    cPhoto = getUserProfilePhotoBase64(cUid)
                }
                PostComment(
                    id = doc.id,
                    authorName = doc.getString("authorName") ?: "आस-पास का साथी",
                    text = doc.getString("text") ?: "",
                    authorProfilePhotoBase64 = cPhoto,
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                )
            }
            val merged = (remoteComments + localCommentsCache[postId].orEmpty())
                .distinctBy { it.id }
                .sortedBy { it.timestamp }
            merged
        } catch (_: Exception) {
            localCommentsCache[postId].orEmpty()
        }
    }

    suspend fun addComment(
        postId: String,
        authorName: String,
        authorProfilePhotoBase64: String,
        commentText: String
    ): PostComment = withContext(Dispatchers.IO) {
        val trimmed = commentText.trim()
        val now = System.currentTimeMillis()
        val safeAuthor = authorName.ifBlank { "आस-पास का साथी" }

        var savedId = "local_${now}"
        val db = firestore
        if (db != null) {
            try {
                val data = hashMapOf(
                    "authorName" to safeAuthor,
                    "authorUid" to (auth?.currentUser?.uid ?: ""),
                    "authorProfilePhotoBase64" to authorProfilePhotoBase64,
                    "text" to trimmed,
                    "timestamp" to now
                )
                val ref = db.collection("photo_stories")
                    .document(postId)
                    .collection("comments")
                    .add(data)
                    .awaitTask()
                savedId = ref.id
                db.collection("photo_stories")
                    .document(postId)
                    .update("commentsCount", FieldValue.increment(1))
                    .awaitTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val newComment = PostComment(
            id = savedId,
            authorName = safeAuthor,
            text = trimmed,
            authorProfilePhotoBase64 = authorProfilePhotoBase64,
            timestamp = now
        )
        val list = localCommentsCache.getOrPut(postId) { mutableListOf() }
        list.add(newComment)

        _storiesFlow.value = _storiesFlow.value.map { post ->
            if (post.id == postId) {
                post.copy(commentsCount = post.commentsCount + 1)
            } else {
                post
            }
        }

        newComment
    }

    suspend fun deleteStory(postId: String) = withContext(Dispatchers.IO) {
        try {
            val db = firestore ?: return@withContext
            db.collection("photo_stories").document(postId).delete().awaitTask()
            _storiesFlow.value = _storiesFlow.value.filterNot { it.id == postId }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

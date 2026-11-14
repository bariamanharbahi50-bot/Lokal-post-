package com.example.data.model

import android.util.Base64

data class PostComment(
    val id: String,
    val authorName: String,
    val text: String,
    val authorProfilePhotoBase64: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun formatRelativeTime(isHindi: Boolean): String {
        val now = System.currentTimeMillis()
        val diffMinutes = ((now - timestamp) / (60 * 1000)).coerceAtLeast(0)
        return when {
            diffMinutes < 1 -> if (isHindi) "अभी-अभी" else "Just now"
            diffMinutes < 60 -> if (isHindi) "${diffMinutes} मि." else "${diffMinutes}m"
            diffMinutes < 1440 -> {
                val hours = diffMinutes / 60
                if (isHindi) "${hours} घं." else "${hours}h"
            }
            else -> {
                val days = diffMinutes / 1440
                if (isHindi) "${days} दि." else "${days}d"
            }
        }
    }
}

data class Post(
    val id: String,
    val authorName: String,
    val authorProfilePhotoBase64: String = "",
    val imageUrl: String,
    val caption: String = "",
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val distanceKm: Double? = null,
    val isUserPost: Boolean = false
) {
    val imageModel: Any
        get() {
            return if (imageUrl.length > 200 &&
                !imageUrl.startsWith("http") &&
                !imageUrl.startsWith("content:") &&
                !imageUrl.startsWith("file:") &&
                !imageUrl.startsWith("android.resource:")
            ) {
                try {
                    Base64.decode(imageUrl, Base64.DEFAULT)
                } catch (_: Exception) {
                    imageUrl
                }
            } else {
                imageUrl
            }
        }

    fun formatRelativeTime(isHindi: Boolean): String {
        val now = System.currentTimeMillis()
        val diffMinutes = ((now - timestamp) / (60 * 1000)).coerceAtLeast(0)
        return when {
            diffMinutes < 1 -> if (isHindi) "अभी-अभी" else "Just now"
            diffMinutes < 60 -> if (isHindi) "${diffMinutes} मिनट पहले" else "${diffMinutes}m ago"
            diffMinutes < 1440 -> {
                val hours = diffMinutes / 60
                if (isHindi) "${hours} घंटे पहले" else "${hours}h ago"
            }
            else -> {
                val days = diffMinutes / 1440
                if (isHindi) "${days} दिन पहले" else "${days}d ago"
            }
        }
    }
}

package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Post

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val authorName: String,
    val imageUrl: String,
    val caption: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val isUserPost: Boolean = true
) {
    fun toPost(distanceKm: Double? = null): Post = Post(
        id = id,
        authorName = authorName,
        imageUrl = imageUrl,
        caption = caption,
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp,
        likesCount = likesCount,
        isLiked = isLiked,
        distanceKm = distanceKm,
        isUserPost = isUserPost
    )

    companion object {
        fun fromPost(post: Post): PostEntity = PostEntity(
            id = post.id,
            authorName = post.authorName,
            imageUrl = post.imageUrl,
            caption = post.caption,
            latitude = post.latitude,
            longitude = post.longitude,
            timestamp = post.timestamp,
            likesCount = post.likesCount,
            isLiked = post.isLiked,
            isUserPost = post.isUserPost
        )
    }
}

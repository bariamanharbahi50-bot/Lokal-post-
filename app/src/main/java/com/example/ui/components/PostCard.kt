package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Post
import com.example.util.ImageCompressor
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppTealDark

@Composable
fun PostCard(
    post: Post,
    isHindi: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Card Header (WhatsApp Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle: decoded Base64 profile photo or fallback initial letter
                val avatarBitmap = remember(post.authorProfilePhotoBase64) {
                    ImageCompressor.decodeBase64ToBitmap(post.authorProfilePhotoBase64)?.asImageBitmap()
                }
                val initial = post.authorName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(WhatsAppTealDark),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap,
                            contentDescription = post.authorName,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Author and Distance info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.isUserPost) {
                            Surface(
                                color = WhatsAppGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "आप" else "You",
                                    color = WhatsAppTealDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val distanceText = post.distanceKm?.let { dist ->
                            if (isHindi) "📍 आपसे लगभग ${String.format("%.2f", dist)} किमी दूर"
                            else "📍 ~${String.format("%.2f", dist)} km away"
                        } ?: if (isHindi) "📍 आस-पास" else "📍 Nearby"

                        Text(
                            text = distanceText,
                            style = MaterialTheme.typography.bodySmall,
                            color = WhatsAppTealDark,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = " • ${post.formatRelativeTime(isHindi)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Map pin quick action
                IconButton(
                    onClick = {
                        openLocationInMaps(context, post.latitude, post.longitude, post.authorName)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = if (isHindi) "नक्शे पर देखें" else "View on Map",
                        tint = WhatsAppTealDark,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete option if it's user's post
                if (post.isUserPost && onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_post_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (isHindi) "हटाएं" else "Delete",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Post Image (Clickable for full screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .background(Color(0xFFE5E7EB))
                    .clickable { onPhotoClick() }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(post.imageModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = post.caption.ifEmpty { "Community post image" },
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }

            // Username attribution directly below photo
            Text(
                text = "By: ${post.authorName}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = WhatsAppTealDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = if (post.caption.isBlank()) 8.dp else 0.dp)
            )

            // Post Caption (if present)
            if (post.caption.isNotBlank()) {
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    lineHeight = 20.sp
                )
            }

            HorizontalDivider(
                color = Color(0xFFEEEEEE),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // Bottom Actions Bar (Like, Comment, Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Like Button
                val heartColor by animateColorAsState(
                    targetValue = if (post.isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "heart_color"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLikeClick() }
                        .padding(vertical = 10.dp)
                        .testTag("like_post_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = heartColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (post.likesCount > 0) "Like (${post.likesCount})" else "Like",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (post.isLiked) FontWeight.Bold else FontWeight.Medium,
                        color = heartColor,
                        fontSize = 13.sp
                    )
                }

                // 2. Comment Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCommentClick() }
                        .padding(vertical = 10.dp)
                        .testTag("comment_post_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Comment",
                        tint = WhatsAppTealDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (post.commentsCount > 0) "Comment (${post.commentsCount})" else "Comment",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = WhatsAppTealDark,
                        fontSize = 13.sp
                    )
                }

                // 3. Share Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { sharePost(context, post, isHindi) }
                        .padding(vertical = 10.dp)
                        .testTag("share_post_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = WhatsAppTealDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = WhatsAppTealDark,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private fun openLocationInMaps(context: Context, lat: Double, lon: Double, label: String) {
    try {
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($label)")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
        val webIntent = Intent(Intent.ACTION_VIEW, webUri)
        context.startActivity(webIntent)
    }
}

private fun sharePost(context: Context, post: Post, isHindi: Boolean) {
    val shareText = buildString {
        append(if (isHindi) "लोकल पोस्ट (Lokal Post) से साझा किया गया:\n" else "Shared from Lokal Post:\n")
        if (post.caption.isNotBlank()) {
            append("\"${post.caption}\"\n")
        }
        post.distanceKm?.let { dist ->
            append(if (isHindi) "📍 लोकेशन: लगभग ${String.format("%.2f", dist)} किमी दूर\n" else "📍 Distance: ~${String.format("%.2f", dist)} km away\n")
        }
        append("📍 Google Maps: https://maps.google.com/?q=${post.latitude},${post.longitude}")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, if (isHindi) "पोस्ट शेयर करें" else "Share Post"))
}

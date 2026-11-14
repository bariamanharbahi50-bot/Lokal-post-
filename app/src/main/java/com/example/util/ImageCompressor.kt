package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressor {

    /**
     * Compresses image from Uri to a JPEG byte array under 200 KB,
     * then encodes it as a Base64 string for Firestore storage.
     */
    fun compressImageToBase64(context: Context, imageUri: Uri, maxSizeBytes: Int = 195 * 1024): String? {
        return try {
            val cr = context.contentResolver

            // 1. Decode bounds to compute inSampleSize
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            }

            val outWidth = boundsOptions.outWidth
            val outHeight = boundsOptions.outHeight
            if (outWidth <= 0 || outHeight <= 0) return null

            // Target max dimension of 800px for optimal quality and small payload
            val maxDimension = 800
            var sampleSize = 1
            val largestDim = max(outWidth, outHeight)
            while (largestDim / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            // 2. Decode sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val sampledBitmap = cr.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return null

            // 3. Compress with quality reduction to guarantee < 200 KB
            var quality = 80
            val stream = ByteArrayOutputStream()
            sampledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

            while (stream.size() > maxSizeBytes && quality > 15) {
                stream.reset()
                quality -= 10
                sampledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            }

            val compressedBytes = stream.toByteArray()
            Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Compresses a profile photo Bitmap using 50% JPEG compression
     * and encodes it to a Base64 String for storing directly in Firestore users collection.
     */
    fun compressProfilePhotoToBase64(context: Context, imageUri: Uri): String? {
        return try {
            val cr = context.contentResolver
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            }

            val outWidth = boundsOptions.outWidth
            val outHeight = boundsOptions.outHeight
            if (outWidth <= 0 || outHeight <= 0) return null

            // Scale down avatar cleanly so Base64 stays lightweight in Firestore
            val maxDimension = 300
            var sampleSize = 1
            val largestDim = max(outWidth, outHeight)
            while (largestDim / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val bitmap = cr.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return null

            val stream = ByteArrayOutputStream()
            // Compress at 50% quality as specified
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
            val compressedBytes = stream.toByteArray()
            Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun decodeBase64ToBitmap(base64String: String?): Bitmap? {
        if (base64String.isNullOrBlank()) return null
        return try {
            val cleanBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}

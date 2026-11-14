package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.location.LocationHelper
import com.localsathi.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Lokal Post", appName)
  }

  @Test
  fun `verify distance calculation within 5km`() {
    // Connaught Place to India Gate (~2.3 km)
    val lat1 = 28.6304
    val lon1 = 77.2177
    val lat2 = 28.6129
    val lon2 = 77.2295

    val dist = LocationHelper.calculateDistanceKm(lat1, lon1, lat2, lon2)
    assertTrue("Distance should be around 2.3km but was $dist", dist in 2.0..2.6)
    assertTrue("Should be within 5km radius", dist <= 5.0)
  }

  @Test
  fun `verify auth manager session persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = com.example.data.auth.AuthManager(context)

    authManager.logout()
    assertTrue(!authManager.isLoggedIn())
    assertTrue(!authManager.areTermsAccepted())

    authManager.setLoggedIn(true)
    authManager.setTermsAccepted(true)
    assertTrue(authManager.isLoggedIn())
    assertTrue(authManager.areTermsAccepted())

    authManager.logout()
    assertTrue(!authManager.isLoggedIn())
    assertTrue(!authManager.areTermsAccepted())
  }

  @Test
  fun `verify ahmedabad default coordinates`() {
    assertEquals(23.0225, LocationHelper.DEFAULT_LAT, 0.0001)
    assertEquals(72.5714, LocationHelper.DEFAULT_LON, 0.0001)
  }

  @Test
  fun `verify base64 decode handles blank and valid data`() {
    org.junit.Assert.assertNull(com.example.util.ImageCompressor.decodeBase64ToBitmap(null))
    org.junit.Assert.assertNull(com.example.util.ImageCompressor.decodeBase64ToBitmap(""))
    org.junit.Assert.assertNull(com.example.util.ImageCompressor.decodeBase64ToBitmap("   "))

    // Create a 2x2 bitmap, compress at 50% JPEG quality, encode to Base64, and decode back
    val bitmap = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888)
    val stream = java.io.ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, stream)
    val base64 = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)

    val decoded = com.example.util.ImageCompressor.decodeBase64ToBitmap(base64)
    org.junit.Assert.assertNotNull(decoded)
    assertEquals(2, decoded?.width)
    assertEquals(2, decoded?.height)

    val decodedWithPrefix = com.example.util.ImageCompressor.decodeBase64ToBitmap("data:image/jpeg;base64,$base64")
    org.junit.Assert.assertNotNull(decodedWithPrefix)
  }

  @Test
  fun `verify user profile and post models store base64 photo`() {
    val profile = com.example.data.auth.UserProfile(
      fullName = "Rohan",
      email = "rohan@gmail.com",
      phone = "9876543210",
      profilePhotoBase64 = "abc123base64"
    )
    assertEquals("abc123base64", profile.profilePhotoBase64)

    val comment = com.example.data.model.PostComment(
      id = "c1",
      authorName = "Rohan",
      authorProfilePhotoBase64 = "abc123base64",
      text = "Nice!"
    )
    assertEquals("abc123base64", comment.authorProfilePhotoBase64)
  }
}


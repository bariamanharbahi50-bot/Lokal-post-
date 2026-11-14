package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class UserProfile(
    val fullName: String,
    val email: String,
    val phone: String,
    val profilePhotoBase64: String = ""
)

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { ex -> cont.resumeWithException(ex) }
}

class AuthManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lokal_post_auth_prefs", Context.MODE_PRIVATE)

    private val auth: FirebaseAuth? get() = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
    private val firestore: FirebaseFirestore? get() = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null }

    companion object {
        const val KEY_IS_LOGGED_IN = "isLoggedIn"
        const val KEY_TERMS_ACCEPTED = "termsAccepted"
        private const val KEY_FULL_NAME = "key_full_name"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_PHONE = "key_phone"
        private const val KEY_PROFILE_PHOTO_BASE64 = "key_profile_photo_base64"
    }

    /**
     * Checks if the user is currently authenticated with Firebase or saved in local preferences.
     * Firebase automatically keeps the session active across launches.
     */
    fun isLoggedIn(): Boolean {
        val hasFirebaseUser = try {
            auth?.currentUser != null
        } catch (_: Exception) {
            false
        }
        val hasPref = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        return hasFirebaseUser || hasPref
    }

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
    }

    fun areTermsAccepted(): Boolean {
        return prefs.getBoolean(KEY_TERMS_ACCEPTED, false)
    }

    fun setTermsAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_TERMS_ACCEPTED, accepted).apply()
    }

    /**
     * Real Firebase Email/Password Auth & Firestore users collection.
     * Stores compressed profilePhotoBase64 directly inside Firestore users document.
     */
    suspend fun signUpOrLogin(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        profilePhotoBase64: String = ""
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedName = fullName.trim()
        val trimmedPhone = phone.trim()

        val firebaseAuth = auth ?: return@withContext Pair(false, "Firebase शुरू नहीं हो सका")

        try {
            val authResult = try {
                firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).awaitTask()
            } catch (_: FirebaseAuthUserCollisionException) {
                firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password).awaitTask()
            } catch (ex: Exception) {
                if (ex.message?.contains("already in use", ignoreCase = true) == true) {
                    firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password).awaitTask()
                } else {
                    throw ex
                }
            }

            val uid = authResult.user?.uid ?: firebaseAuth.currentUser?.uid ?: ""

            // If user didn't pick a new photo during login, try restoring existing profilePhotoBase64
            var resolvedPhotoBase64 = profilePhotoBase64
            if (resolvedPhotoBase64.isBlank() && uid.isNotBlank()) {
                try {
                    val existingDoc = firestore?.collection("users")?.document(uid)?.get()?.awaitTask()
                    resolvedPhotoBase64 = existingDoc?.getString("profilePhotoBase64") ?: ""
                } catch (_: Exception) { }
            }

            // Save user profile (including profilePhotoBase64) in Firestore "users" collection
            val userProfileData = hashMapOf<String, Any>(
                "uid" to uid,
                "fullName" to trimmedName.ifBlank { trimmedEmail.substringBefore("@") },
                "email" to trimmedEmail,
                "phone" to trimmedPhone,
                "updatedAt" to System.currentTimeMillis()
            )
            if (resolvedPhotoBase64.isNotBlank()) {
                userProfileData["profilePhotoBase64"] = resolvedPhotoBase64
            } else {
                userProfileData["profilePhotoBase64"] = ""
            }

            try {
                firestore?.collection("users")?.document(uid)?.set(userProfileData, SetOptions.merge())?.awaitTask()
            } catch (firestoreEx: Exception) {
                firestoreEx.printStackTrace()
            }

            // Cache in local preferences for instant offline access and persistent session
            prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_FULL_NAME, trimmedName.ifBlank { trimmedEmail.substringBefore("@") })
                .putString(KEY_EMAIL, trimmedEmail)
                .putString(KEY_PHONE, trimmedPhone)
                .putString(KEY_PROFILE_PHOTO_BASE64, resolvedPhotoBase64)
                .apply()

            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "प्रमाणीकरण विफल रहा")
        }
    }

    fun getUserProfile(): UserProfile {
        val currentEmail = auth?.currentUser?.email ?: prefs.getString(KEY_EMAIL, "") ?: ""
        val currentName = prefs.getString(KEY_FULL_NAME, "")?.takeIf { it.isNotBlank() }
            ?: currentEmail.substringBefore("@").ifBlank { "आस-पास का साथी" }
        val currentPhone = prefs.getString(KEY_PHONE, "") ?: ""
        val currentPhoto = prefs.getString(KEY_PROFILE_PHOTO_BASE64, "") ?: ""
        return UserProfile(
            fullName = currentName,
            email = currentEmail,
            phone = currentPhone,
            profilePhotoBase64 = currentPhoto
        )
    }

    fun logout() {
        try {
            auth?.signOut()
        } catch (_: Exception) { }
        prefs.edit().clear().apply()
    }
}

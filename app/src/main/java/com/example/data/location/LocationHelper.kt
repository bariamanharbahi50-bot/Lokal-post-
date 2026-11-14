package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sqrt

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val isSimulated: Boolean = false
)

object LocationHelper {

    // Default fallback coordinates (Ahmedabad, Gujarat)
    const val DEFAULT_LAT = 23.0225
    const val DEFAULT_LON = 72.5714

    /**
     * Exact Haversine distance formula matching the user's web snippet
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p = 0.017453292519943295 // Math.PI / 180
        val a = 0.5 - cos((lat2 - lat1) * p) / 2 +
                cos(lat1 * p) * cos(lat2 * p) * (1 - cos((lon2 - lon1) * p)) / 2
        return 12742.0 * asin(sqrt(a))
    }

    @SuppressLint("MissingPermission")
    fun getLocationFlow(context: Context): Flow<UserLocation?> = callbackFlow {
        val fusedClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        // Try getting last known location first
        fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                trySend(UserLocation(loc.latitude, loc.longitude, loc.accuracy))
            }
        }.addOnFailureListener {
            // Check fallback LocationManager
            tryFallbackLocation(context)?.let { trySend(it) }
        }

        // Request an immediate fresh high-accuracy GPS fix
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        trySend(UserLocation(loc.latitude, loc.longitude, loc.accuracy))
                    }
                }
        } catch (_: Exception) { }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setMinUpdateIntervalMillis(5000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    trySend(UserLocation(loc.latitude, loc.longitude, loc.accuracy))
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } catch (e: Exception) {
            tryFallbackLocation(context)?.let { trySend(it) }
        }

        awaitClose {
            try {
                fusedClient.removeLocationUpdates(callback)
            } catch (_: Exception) { }
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryFallbackLocation(context: Context): UserLocation? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        for (provider in providers) {
            try {
                val loc = locationManager.getLastKnownLocation(provider)
                if (loc != null) {
                    return UserLocation(loc.latitude, loc.longitude, loc.accuracy)
                }
            } catch (_: Exception) { }
        }
        return null
    }
}

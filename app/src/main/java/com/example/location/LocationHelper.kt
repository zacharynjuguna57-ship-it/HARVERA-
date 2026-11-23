package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class DetectedLocation(
    val latitude: Double,
    val longitude: Double,
    val streetAddress: String,
    val city: String
)

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): Result<DetectedLocation> {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
            val cts = CancellationTokenSource()
            val location = suspendCancellableCoroutine<Location?> { cont ->
                cont.invokeOnCancellation { cts.cancel() }
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            cont.resume(loc)
                        } else {
                            client.lastLocation
                                .addOnSuccessListener { last -> cont.resume(last) }
                                .addOnFailureListener { cont.resume(null) }
                        }
                    }
                    .addOnFailureListener {
                        client.lastLocation
                            .addOnSuccessListener { last -> cont.resume(last) }
                            .addOnFailureListener { cont.resume(null) }
                    }
            } ?: return Result.failure(IllegalStateException("Unable to acquire GPS fix. Check device location services or enter an address manually."))

            val (street, city) = reverseGeocode(context.applicationContext, location.latitude, location.longitude)
            Result.success(
                DetectedLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    streetAddress = street,
                    city = city
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) {
                return@withContext formatCoordFallback(latitude, longitude)
            }
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine<List<Address>?> { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { list ->
                        cont.resume(list)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)
            }
            val first = addresses?.firstOrNull()
            if (first != null) {
                val line = first.getAddressLine(0)?.takeIf { it.isNotBlank() }
                val street = listOfNotNull(first.subThoroughfare, first.thoroughfare)
                    .joinToString(" ")
                    .ifBlank { line?.substringBefore(",") ?: "Current GPS Point" }
                val city = first.locality
                    ?: first.subAdminArea
                    ?: first.adminArea
                    ?: "Nearby Area"
                street.take(180) to city.take(75)
            } else {
                formatCoordFallback(latitude, longitude)
            }
        } catch (_: Exception) {
            formatCoordFallback(latitude, longitude)
        }
    }

    private fun formatCoordFallback(latitude: Double, longitude: Double): Pair<String, String> {
        val latStr = String.format(Locale.US, "%.4f", latitude)
        val lngStr = String.format(Locale.US, "%.4f", longitude)
        return "GPS ($latStr, $lngStr)" to "Current Location"
    }

    fun distanceKmBetween(
        lat1: Double?,
        lon1: Double?,
        lat2: Double?,
        lon2: Double?
    ): Double? {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    fun formatDistance(distanceKm: Double?): String? {
        if (distanceKm == null) return null
        return if (distanceKm < 1.0) {
            "${(distanceKm * 1000).toInt().coerceAtLeast(50)} m away"
        } else {
            String.format(Locale.US, "%.1f km away", distanceKm)
        }
    }
}

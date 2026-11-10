package com.tonyguerra.radardetector.data.location

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.tonyguerra.radardetector.data.model.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/**
 * Obtém a localização do aparelho usando só o [LocationManager] do sistema, sem depender do
 * Google Play Services.
 *
 * Quem chama precisa garantir antes que a permissão foi concedida (veja [LocationPermissions]).
 */
class LocationProvider(context: Context) {

    private val appContext = context.applicationContext
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * Pede uma leitura atual ao GPS (ou à rede, se o GPS estiver desligado) e espera até
     * [FRESH_LOCATION_TIMEOUT]. Se não vier nada, usa a última posição conhecida de qualquer
     * provedor.
     *
     * @return a posição atual, ou `null` se o aparelho não souber onde está.
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    suspend fun currentLocation(): GeoPoint? {
        val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { locationManager.isProviderEnabled(it) }

        val fresh = provider?.let {
            withTimeoutOrNull(FRESH_LOCATION_TIMEOUT) { requestCurrent(it) }
        }
        val location = fresh ?: lastKnown()
        return location?.let { GeoPoint(it.latitude, it.longitude) }
    }

    /** Adapta o callback de [LocationManagerCompat.getCurrentLocation] para coroutine. */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private suspend fun requestCurrent(provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                signal,
                ContextCompat.getMainExecutor(appContext),
            ) { location -> if (cont.isActive) cont.resume(location) }
        }

    /** A leitura mais recente entre todos os provedores ativos. */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun lastKnown(): Location? =
        locationManager.getProviders(true)
            .mapNotNull { locationManager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }

    private companion object {
        val FRESH_LOCATION_TIMEOUT = 10.seconds
    }
}

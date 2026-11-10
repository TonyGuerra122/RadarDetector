package com.tonyguerra.radardetector.data.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Coordenada geográfica em graus decimais (WGS 84).
 *
 * O app usa este tipo próprio em vez do `GeoPoint` do osmdroid para que as camadas de dados e
 * de estado não dependam da biblioteca de mapa. A conversão acontece só em `ui.map`.
 */
data class GeoPoint(val latitude: Double, val longitude: Double) {

    /** Distância em linha reta até [other], em metros, calculada pela fórmula de haversine. */
    fun distanceTo(other: GeoPoint): Double {
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_METERS * atan2(sqrt(h), sqrt(1 - h))
    }

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}

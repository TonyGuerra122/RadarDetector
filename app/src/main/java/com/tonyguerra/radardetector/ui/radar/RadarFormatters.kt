package com.tonyguerra.radardetector.ui.radar

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tonyguerra.radardetector.R
import com.tonyguerra.radardetector.data.enum.RadarType

/** Nome do tipo de radar para exibir na tela. */
@StringRes
internal fun RadarType.labelRes(): Int = when (this) {
    RadarType.FIXED -> R.string.radar_type_fixed
    RadarType.TRAFFIC_LIGHT -> R.string.radar_type_traffic_light
    RadarType.AVERAGE_SPEED -> R.string.radar_type_average_speed
}

/**
 * Distância legível: `"350 m"` abaixo de 1 km e `"1,2 km"` a partir disso. O separador decimal
 * segue o idioma do aparelho.
 */
@Composable
internal fun formatDistance(meters: Double): String =
    if (meters < 1000) {
        stringResource(R.string.distance_meters, meters.toInt())
    } else {
        stringResource(R.string.distance_kilometers, meters / 1000)
    }

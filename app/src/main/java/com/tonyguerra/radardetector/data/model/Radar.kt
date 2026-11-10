package com.tonyguerra.radardetector.data.model

import com.tonyguerra.radardetector.data.enum.RadarType

/**
 * Um radar de trânsito cadastrado no OpenStreetMap.
 *
 * @property id identificador do nó no OpenStreetMap (único e estável).
 * @property location posição do radar.
 * @property speedLimitKmh limite de velocidade em km/h, ou `null` se não estiver cadastrado.
 * @property type tipo de fiscalização.
 * @property description nome ou descrição livre do cadastro, se houver.
 * @property distanceMeters distância até o ponto usado na busca, em metros.
 */
data class Radar(
    val id: Long,
    val location: GeoPoint,
    val speedLimitKmh: Int?,
    val type: RadarType,
    val description: String?,
    val distanceMeters: Double,
)
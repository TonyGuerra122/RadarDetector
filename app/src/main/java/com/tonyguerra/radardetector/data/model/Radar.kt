package com.tonyguerra.radardetector.data.model

import com.tonyguerra.radardetector.data.enum.RadarSource
import com.tonyguerra.radardetector.data.enum.RadarType

/**
 * Um radar de trânsito devolvido pela API.
 *
 * @property id identificador único no app, no formato `"<source>:<id>"` (ex.: `"local:781"`). O
 *   `id` da API sozinho não basta: o mesmo número pode existir no Overpass e no cadastro local.
 * @property location posição do radar.
 * @property speedLimitKmh limite de velocidade em km/h, ou `null` se não vier na resposta.
 * @property type tipo de fiscalização.
 * @property road rodovia ou via (ex.: `"BR-381"`), quando a API informar.
 * @property direction sentido da fiscalização (ex.: `"Decrescente"`), quando a API informar.
 * @property source de onde a API tirou o radar.
 * @property distanceMeters distância até o ponto usado na busca, em metros.
 */
data class Radar(
    val id: String,
    val location: GeoPoint,
    val speedLimitKmh: Int?,
    val type: RadarType,
    val road: String?,
    val direction: String?,
    val source: RadarSource,
    val distanceMeters: Double,
)

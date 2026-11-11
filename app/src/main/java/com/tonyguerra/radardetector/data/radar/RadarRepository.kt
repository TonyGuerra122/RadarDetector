package com.tonyguerra.radardetector.data.radar

import com.tonyguerra.radardetector.data.model.GeoPoint
import com.tonyguerra.radardetector.data.model.Radar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Busca radares de trânsito na API do projeto, que junta o OpenStreetMap (Overpass) com um
 * cadastro local e remove os repetidos.
 */
class RadarRepository(
    private val client: RadarApiClient = RadarApiClient(),
) {

    /**
     * Lista os radares num raio de [radiusMeters] em volta de [center], do mais próximo ao mais
     * distante.
     *
     * @throws java.io.IOException se a API não responder ou devolver uma resposta inválida.
     */
    suspend fun findNearby(center: GeoPoint, radiusMeters: Int): List<Radar> {
        val json = client.fetchNearby(center, radiusMeters)
        return withContext(Dispatchers.Default) { RadarResponseParser.parse(json, center) }
    }
}

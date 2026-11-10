package com.tonyguerra.radardetector.data.radar

import com.tonyguerra.radardetector.data.enum.RadarType
import com.tonyguerra.radardetector.data.model.GeoPoint
import com.tonyguerra.radardetector.data.model.Radar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Busca radares de trânsito no OpenStreetMap (nós com a tag `highway=speed_camera`).
 *
 * Os dados vêm da comunidade do OSM, então a cobertura varia por região: um raio sem resultados
 * pode só significar que ninguém cadastrou os radares dali.
 */
class RadarRepository(
    private val client: OverpassClient = OverpassClient(),
) {

    /**
     * Lista os radares num raio de [radiusMeters] em volta de [center], do mais próximo ao mais
     * distante.
     *
     * @throws java.io.IOException se nenhum servidor Overpass responder.
     */
    suspend fun findNearby(center: GeoPoint, radiusMeters: Int): List<Radar> {
        val json = client.query(buildQuery(center, radiusMeters))
        return withContext(Dispatchers.Default) { parse(json, center) }
    }

    /**
     * Monta a consulta em Overpass QL. Ela filtra só por `highway=speed_camera` porque filtros
     * por regex em outras tags deixam a consulta muito mais lenta nos servidores públicos.
     */
    private fun buildQuery(center: GeoPoint, radiusMeters: Int): String = """
        [out:json][timeout:40];
        node["highway"="speed_camera"](around:$radiusMeters,${center.latitude},${center.longitude});
        out body;
    """.trimIndent()

    /** Converte a resposta JSON do Overpass em [Radar]s ordenados pela distância até [center]. */
    private fun parse(json: String, center: GeoPoint): List<Radar> {
        val elements = JSONObject(json).optJSONArray("elements") ?: return emptyList()
        return (0 until elements.length())
            .map { elements.getJSONObject(it) }
            .distinctBy { it.getLong("id") }
            .map { element ->
                val tags = element.optJSONObject("tags")
                val location = GeoPoint(element.getDouble("lat"), element.getDouble("lon"))
                Radar(
                    id = element.getLong("id"),
                    location = location,
                    speedLimitKmh = tags?.optString("maxspeed")?.let(::parseSpeedLimit),
                    type = parseType(tags?.optString("enforcement")),
                    description = tags?.firstNonBlank("name", "description"),
                    distanceMeters = center.distanceTo(location),
                )
            }
            .sortedBy { it.distanceMeters }
    }

    /** `"60"` → 60 e `"60 mph"` → 60 (fica só o número). Valores sem número viram `null`. */
    private fun parseSpeedLimit(maxspeed: String): Int? =
        maxspeed.takeWhile { it.isDigit() }.toIntOrNull()

    private fun parseType(enforcement: String?): RadarType = when (enforcement) {
        "traffic_signals" -> RadarType.TRAFFIC_LIGHT
        "average_speed" -> RadarType.AVERAGE_SPEED
        else -> RadarType.FIXED
    }

    private fun JSONObject.firstNonBlank(vararg keys: String): String? =
        keys.asSequence().map { optString(it) }.firstOrNull { it.isNotBlank() }
}

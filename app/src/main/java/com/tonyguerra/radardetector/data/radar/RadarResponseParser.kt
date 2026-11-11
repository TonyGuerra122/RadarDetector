package com.tonyguerra.radardetector.data.radar

import com.tonyguerra.radardetector.data.enum.RadarSource
import com.tonyguerra.radardetector.data.enum.RadarType
import com.tonyguerra.radardetector.data.model.GeoPoint
import com.tonyguerra.radardetector.data.model.Radar
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * Converte a resposta da API de radares em [Radar]s.
 *
 * Formato esperado:
 * ```json
 * {
 *   "data": [
 *     {"type": "node", "id": 8390726476, "lat": -22.80, "lon": -46.26,
 *      "tags": {"highway": "speed_camera", "maxspeed": "80", "direction": ""}, "source": "overpass"},
 *     {"type": "Controlador", "id": 781, "lat": -22.80, "lon": -46.26,
 *      "tags": {"highway": "BR-381", "maxspeed": "80", "direction": "Decrescente"}, "source": "local"}
 *   ],
 *   "overpass_count": 1, "local_count": 1, "duplicates_removed": 0
 * }
 * ```
 *
 * Regras:
 * - sem o campo `data`, a resposta inteira é inválida (lança [IOException]);
 * - itens sem `id`, `lat` ou `lon` válidos são ignorados, sem derrubar a lista;
 * - `tags.highway` igual a `speed_camera` é só a marcação do OSM; qualquer outro valor é tratado
 *   como o nome da via (ex.: `"BR-381"`);
 * - os contadores (`overpass_count` etc.) não são usados pelo app.
 */
object RadarResponseParser {

    private const val OSM_SPEED_CAMERA = "speed_camera"

    /** @return radares válidos, sem repetidos, do mais próximo ao mais distante de [center]. */
    fun parse(json: String, center: GeoPoint): List<Radar> {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw IOException("Resposta da API não é um JSON válido", e)
        }
        val data = root.optJSONArray("data") ?: throw IOException("Resposta da API sem o campo \"data\"")

        return (0 until data.length())
            .mapNotNull { index -> data.optJSONObject(index)?.let { parseItem(it, center) } }
            .distinctBy { it.id }
            .sortedBy { it.distanceMeters }
    }

    private fun parseItem(item: JSONObject, center: GeoPoint): Radar? {
        val rawId = item.text("id") ?: return null
        val lat = item.optDouble("lat")
        val lon = item.optDouble("lon")
        if (lat.isNaN() || lon.isNaN() || lat !in -90.0..90.0 || lon !in -180.0..180.0) return null

        val sourceValue = item.text("source")
        val tags = item.optJSONObject("tags")
        val highway = tags?.text("highway")
        val location = GeoPoint(lat, lon)

        return Radar(
            id = "${sourceValue ?: RadarSource.UNKNOWN.apiValue}:$rawId",
            location = location,
            speedLimitKmh = tags?.text("maxspeed")?.let(::parseSpeedLimit),
            type = parseType(tags?.text("enforcement")),
            road = highway?.takeUnless { it.equals(OSM_SPEED_CAMERA, ignoreCase = true) },
            direction = tags?.text("direction"),
            source = RadarSource.fromApi(sourceValue),
            distanceMeters = center.distanceTo(location),
        )
    }

    /** `"80"` → 80 e `"60 mph"` → 60 (fica só o número). Valores sem número viram `null`. */
    private fun parseSpeedLimit(maxspeed: String): Int? =
        maxspeed.takeWhile { it.isDigit() }.toIntOrNull()?.takeIf { it > 0 }

    private fun parseType(enforcement: String?): RadarType = when (enforcement) {
        "traffic_signals" -> RadarType.TRAFFIC_LIGHT
        "average_speed" -> RadarType.AVERAGE_SPEED
        else -> RadarType.FIXED
    }

    /**
     * Valor do campo como texto, ou `null` se estiver ausente, `null` no JSON ou vazio.
     * (O `optString` do org.json devolveria o texto `"null"` para um `null` do JSON.)
     */
    private fun JSONObject.text(key: String): String? {
        if (isNull(key)) return null
        return opt(key)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }
}

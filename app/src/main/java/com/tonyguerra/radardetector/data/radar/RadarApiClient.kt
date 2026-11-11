package com.tonyguerra.radardetector.data.radar

import com.tonyguerra.radardetector.BuildConfig
import com.tonyguerra.radardetector.data.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente HTTP da API de radares: `GET {baseUrl}/{lat}/{lon}/{radius}`.
 *
 * O endereço vem de `BuildConfig.RADAR_API_URL` (configurado no `app/build.gradle.kts`).
 * No emulador, o padrão `http://10.0.2.2:8080` aponta para o `localhost:8080` do seu PC.
 */
class RadarApiClient(
    private val baseUrl: String = BuildConfig.RADAR_API_URL,
) {

    /**
     * Pede os radares num raio de [radiusMeters] em volta de [center] e devolve o JSON bruto.
     *
     * @throws IOException em erro de rede ou se a API não responder com 2xx.
     */
    suspend fun fetchNearby(center: GeoPoint, radiusMeters: Int): String = withContext(Dispatchers.IO) {
        // Double.toString sempre usa ponto como separador decimal, independente do idioma.
        val url = "${baseUrl.trimEnd('/')}/${center.latitude}/${center.longitude}/$radiusMeters"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            // A API consulta o Overpass por trás, que pode demorar.
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("$url respondeu ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 45_000
    }
}

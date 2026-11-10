package com.tonyguerra.radardetector.data.radar

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cliente HTTP mínimo para a API Overpass (consultas ao banco de dados do OpenStreetMap).
 *
 * Os servidores públicos não exigem chave de API, mas às vezes ficam sobrecarregados e devolvem
 * timeout ou 504. Por isso o cliente tenta cada servidor de [endpoints], em ordem, até um responder.
 *
 * Usa só [HttpURLConnection] e `org.json` do próprio Android, sem dependências extras.
 */
class OverpassClient(
    private val endpoints: List<String> = DEFAULT_ENDPOINTS,
) {

    /**
     * Executa uma consulta em Overpass QL e devolve o corpo da resposta.
     *
     * @throws IOException se nenhum servidor responder com sucesso (o erro é o do último servidor).
     */
    suspend fun query(overpassQl: String): String = withContext(Dispatchers.IO) {
        var lastError: IOException? = null
        for (endpoint in endpoints) {
            try {
                return@withContext post(endpoint, overpassQl)
            } catch (e: IOException) {
                Log.w(TAG, "Falha ao consultar $endpoint", e)
                lastError = e
            }
        }
        throw lastError ?: IOException("Nenhum servidor Overpass configurado")
    }

    private fun post(endpoint: String, overpassQl: String): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            // Sem isso o HttpURLConnection manda um Accept genérico, que o Overpass pode recusar (406).
            setRequestProperty("Accept", "application/json")
            // Os servidores públicos pedem um User-Agent que identifique o app.
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            connection.outputStream.use {
                it.write("data=${URLEncoder.encode(overpassQl, "UTF-8")}".toByteArray())
            }
            if (connection.responseCode !in 200..299) {
                throw IOException("$endpoint respondeu ${connection.responseCode}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val TAG = "OverpassClient"
        private const val USER_AGENT = "RadarDetector-Android/1.0"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 45_000

        /** Servidores Overpass públicos, em ordem de preferência. */
        val DEFAULT_ENDPOINTS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.private.coffee/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
        )
    }
}

package com.tonyguerra.radardetector.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Consulta o servidor de arquivos por uma versão nova e baixa o APK.
 *
 * Segurança:
 * - só aceita endereços `https://`;
 * - valida o `latest.json` inteiro ([UpdateManifestParser]) antes de baixar qualquer coisa;
 * - confere o SHA-256 do APK com o valor do `latest.json` antes de liberar a instalação;
 * - o próprio Android recusa a atualização se o APK não estiver assinado com a mesma chave do
 *   app instalado. Ou seja, um APK adulterado no servidor não consegue substituir o app.
 *
 * @param manifestUrl endereço HTTPS do `latest.json`.
 * @param packageName pacote do app instalado. O `latest.json` precisa ser do mesmo pacote.
 * @param cacheDir pasta de cache do app. O APK fica em `cacheDir/updates/`, a pasta liberada no
 *   FileProvider (`res/xml/update_paths.xml`).
 */
class UpdateRepository(
    private val manifestUrl: String,
    private val packageName: String,
    private val cacheDir: File,
) {

    /**
     * Lê e valida o `latest.json` do servidor.
     *
     * @throws InvalidUpdateManifestException se o arquivo não passar na validação.
     * @throws IOException em erro de rede.
     */
    suspend fun fetchLatest(): UpdateInfo = withContext(Dispatchers.IO) {
        requireHttps(manifestUrl)
        UpdateManifestParser.parse(
            json = httpGet(manifestUrl),
            manifestUrl = manifestUrl,
            expectedPackageName = packageName,
        )
    }

    /**
     * Baixa o APK de [info] e confere o SHA-256 durante o download.
     *
     * @param onProgress fração baixada, de 0 a 1, ou `null` se o servidor não informar o tamanho.
     * @return o arquivo pronto para instalar.
     * @throws IOException em erro de rede ou se o hash não conferir (o arquivo é apagado).
     */
    suspend fun download(info: UpdateInfo, onProgress: (Float?) -> Unit): File =
        withContext(Dispatchers.IO) {
            // Apaga downloads antigos para não acumular APKs no cache.
            val dir = File(cacheDir, UPDATES_DIR).apply {
                deleteRecursively()
                mkdirs()
            }
            val target = File(dir, APK_FILE_NAME)
            val digest = MessageDigest.getInstance("SHA-256")

            val connection = open(info.apkUrl)
            try {
                val total = connection.contentLengthLong
                connection.inputStream.use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var downloaded = 0L
                        while (true) {
                            ensureActive() // Para se o usuário cancelar ou a tela for fechada.
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloaded += read
                            onProgress(if (total > 0) downloaded.toFloat() / total else null)
                        }
                    }
                }
            } catch (e: Exception) {
                target.delete()
                throw e
            } finally {
                connection.disconnect()
            }

            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(info.sha256.trim(), ignoreCase = true)) {
                target.delete()
                throw IOException("SHA-256 do APK não confere (esperado ${info.sha256}, recebido $actual)")
            }
            target
        }

    private fun httpGet(url: String): String {
        val connection = open(url)
        try {
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** Abre a conexão e falha logo se a resposta não for 2xx. */
    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            // Evita receber um latest.json velho guardado em cache por algum proxy.
            useCaches = false
            setRequestProperty("Cache-Control", "no-cache")
            if (responseCode !in 200..299) {
                disconnect()
                throw IOException("$url respondeu $responseCode")
            }
        }

    private fun requireHttps(url: String) {
        if (!url.startsWith("https://")) throw IOException("O atualizador só aceita HTTPS: $url")
    }

    companion object {
        /** Precisa ser igual ao `path` em `res/xml/update_paths.xml`. */
        const val UPDATES_DIR = "updates"
        private const val APK_FILE_NAME = "update.apk"
        private const val BUFFER_SIZE = 64 * 1024
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
    }
}

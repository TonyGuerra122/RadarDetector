package com.tonyguerra.radardetector.data.update

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL

/** O `latest.json` existe, mas não passou na validação. Nada é baixado nesse caso. */
class InvalidUpdateManifestException(message: String, cause: Throwable? = null) :
    IOException(message, cause)

/**
 * Lê e valida o `latest.json` publicado pelo deploy, **antes** de qualquer download.
 *
 * Formato esperado:
 * ```json
 * {
 *   "packageName": "com.tonyguerra.radardetector",
 *   "versionCode": 12,
 *   "versionName": "1.2.0",
 *   "apkFile": "RadarDetector-latest.apk",
 *   "sha256": "6fab…b827"
 * }
 * ```
 *
 * Regras (qualquer uma falhando lança [InvalidUpdateManifestException]):
 * - precisa ser um objeto JSON válido;
 * - `packageName` igual ao pacote do app (impede usar o manifesto de outro app);
 * - `versionCode` inteiro e positivo (não aceita texto, como `"12"`, nem número quebrado);
 * - `versionName` preenchido;
 * - `apkFile` só um nome de arquivo `.apk`, sem pastas nem `..`;
 * - `sha256` com exatamente 64 caracteres hexadecimais;
 * - o endereço final do APK precisa ser HTTPS.
 */
object UpdateManifestParser {

    private val APK_FILE_PATTERN = Regex("^[A-Za-z0-9._-]+\\.apk$")
    private val SHA256_PATTERN = Regex("^[0-9a-fA-F]{64}$")

    /**
     * @param json conteúdo do `latest.json`.
     * @param manifestUrl endereço de onde o `latest.json` veio. O `apkFile` é resolvido em
     *   relação a ele (mesma pasta).
     * @param expectedPackageName pacote do app instalado.
     */
    fun parse(json: String, manifestUrl: String, expectedPackageName: String): UpdateInfo {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw InvalidUpdateManifestException("latest.json não é um JSON válido", e)
        }

        val packageName = root.requireString("packageName")
        if (packageName != expectedPackageName) {
            throw InvalidUpdateManifestException(
                "latest.json é do pacote $packageName, mas o app é $expectedPackageName",
            )
        }

        val versionCode = when (val raw = root.opt("versionCode")) {
            is Int -> raw.toLong()
            is Long -> raw
            else -> throw InvalidUpdateManifestException("versionCode precisa ser um número inteiro: $raw")
        }
        if (versionCode <= 0) throw InvalidUpdateManifestException("versionCode precisa ser positivo: $versionCode")

        val versionName = root.requireString("versionName")

        val apkFile = root.requireString("apkFile")
        if (!APK_FILE_PATTERN.matches(apkFile)) {
            throw InvalidUpdateManifestException("apkFile precisa ser só um nome de arquivo .apk: $apkFile")
        }

        val sha256 = root.requireString("sha256")
        if (!SHA256_PATTERN.matches(sha256)) {
            throw InvalidUpdateManifestException("sha256 precisa ter 64 caracteres hexadecimais")
        }

        // O APK sempre se chama RadarDetector-latest.apk, então o ?v= faz cada versão ter um
        // endereço próprio e impede que um cache entregue o APK antigo.
        val apkUrl = try {
            URL(URL(manifestUrl), apkFile).toString() + "?v=$versionCode"
        } catch (e: java.net.MalformedURLException) {
            throw InvalidUpdateManifestException("Endereço do APK inválido", e)
        }
        if (!apkUrl.startsWith("https://")) {
            throw InvalidUpdateManifestException("O APK precisa estar em HTTPS: $apkUrl")
        }

        return UpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            sha256 = sha256.lowercase(),
        )
    }

    /** Campo de texto obrigatório e não vazio. */
    private fun JSONObject.requireString(key: String): String {
        val value = opt(key)
        if (value !is String || value.isBlank()) {
            throw InvalidUpdateManifestException("Campo \"$key\" ausente ou vazio no latest.json")
        }
        return value.trim()
    }
}

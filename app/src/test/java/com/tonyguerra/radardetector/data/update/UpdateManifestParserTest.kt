package com.tonyguerra.radardetector.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UpdateManifestParserTest {

    private val manifestUrl = "https://arquivos.exemplo.com/radardetector/latest.json"
    private val packageName = "com.tonyguerra.radardetector"
    private val sha = "6fabcbe1dfb23dc604fc64c1a0be43e484b2e525d518d1204ed258286671b827"

    private fun manifest(
        packageName: String = this.packageName,
        versionCode: String = "12",
        versionName: String = "\"1.2.0\"",
        apkFile: String = "\"RadarDetector-latest.apk\"",
        sha256: String = "\"$sha\"",
    ) = """
        {
          "packageName": "$packageName",
          "versionCode": $versionCode,
          "versionName": $versionName,
          "apkFile": $apkFile,
          "sha256": $sha256
        }
    """.trimIndent()

    private fun parse(json: String) = UpdateManifestParser.parse(json, manifestUrl, packageName)

    private fun assertInvalid(json: String) {
        assertThrows(InvalidUpdateManifestException::class.java) { parse(json) }
    }

    @Test
    fun `manifesto valido resolve o APK na mesma pasta com o versionCode na URL`() {
        val info = parse(manifest())

        assertEquals(12L, info.versionCode)
        assertEquals("1.2.0", info.versionName)
        assertEquals("https://arquivos.exemplo.com/radardetector/RadarDetector-latest.apk?v=12", info.apkUrl)
        assertEquals(sha, info.sha256)
    }

    @Test
    fun `sha256 em maiusculas e aceito e normalizado`() {
        val info = parse(manifest(sha256 = "\"${sha.uppercase()}\""))
        assertEquals(sha, info.sha256)
    }

    @Test
    fun `json quebrado e rejeitado`() = assertInvalid("{ isso nao e json")

    @Test
    fun `manifesto de outro app e rejeitado`() = assertInvalid(manifest(packageName = "com.outro.app"))

    @Test
    fun `versionCode como texto e rejeitado`() = assertInvalid(manifest(versionCode = "\"12\""))

    @Test
    fun `versionCode quebrado e rejeitado`() = assertInvalid(manifest(versionCode = "12.5"))

    @Test
    fun `versionCode zero ou negativo e rejeitado`() {
        assertInvalid(manifest(versionCode = "0"))
        assertInvalid(manifest(versionCode = "-3"))
    }

    @Test
    fun `versionName vazio e rejeitado`() = assertInvalid(manifest(versionName = "\"  \""))

    @Test
    fun `apkFile com pasta ou fora do padrao e rejeitado`() {
        assertInvalid(manifest(apkFile = "\"../outro/app.apk\""))
        assertInvalid(manifest(apkFile = "\"https://malicioso.com/app.apk\""))
        assertInvalid(manifest(apkFile = "\"RadarDetector-latest.zip\""))
    }

    @Test
    fun `sha256 com tamanho errado ou caracteres invalidos e rejeitado`() {
        assertInvalid(manifest(sha256 = "\"abc123\""))
        assertInvalid(manifest(sha256 = "\"${"z".repeat(64)}\""))
    }

    @Test
    fun `campo obrigatorio ausente e rejeitado`() {
        assertInvalid("""{"packageName": "$packageName", "versionCode": 12, "versionName": "1.2.0", "apkFile": "RadarDetector-latest.apk"}""")
    }

    @Test
    fun `manifesto servido por http e rejeitado`() {
        assertThrows(InvalidUpdateManifestException::class.java) {
            UpdateManifestParser.parse(manifest(), "http://arquivos.exemplo.com/latest.json", packageName)
        }
    }
}

package com.tonyguerra.radardetector.data.update

/**
 * Versão mais recente publicada no servidor, já validada pelo [UpdateManifestParser].
 *
 * @property versionCode número interno da versão. Só há atualização se for maior que o instalado.
 * @property versionName versão mostrada ao usuário.
 * @property apkUrl endereço HTTPS completo do APK, com `?v=<versionCode>` no fim para furar cache.
 * @property sha256 hash esperado do APK, em hexadecimal minúsculo. O download é descartado se
 *   não bater.
 */
data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
)

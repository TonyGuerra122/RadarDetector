package com.tonyguerra.radardetector.data.update

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

/**
 * Entrega o APK baixado ao instalador do Android.
 *
 * O Android sempre mostra a tela de confirmação: o app não consegue se atualizar sem o usuário
 * tocar em "Atualizar". No Android 8+, o usuário também precisa permitir, uma vez, que este app
 * instale apps ("Instalar apps desconhecidos").
 */
object ApkInstaller {

    /** `true` se o usuário já permitiu que este app instale APKs. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /**
     * Abre a tela de configurações onde o usuário libera "Instalar apps desconhecidos" para este app.
     * Só existe no Android 8+, que é justamente quando [canInstall] pode devolver `false`.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun permissionSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())

    /** Abre o instalador do sistema com o [apk] (exposto pelo FileProvider declarado no manifesto). */
    fun installIntent(context: Context, apk: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

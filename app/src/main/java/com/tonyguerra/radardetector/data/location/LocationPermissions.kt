package com.tonyguerra.radardetector.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** Permissões de localização usadas pelo app e como verificar se foram concedidas. */
object LocationPermissions {

    /** Pedimos as duas. O usuário pode conceder só a aproximada (COARSE), que já basta. */
    val ALL = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    /** `true` se pelo menos uma das permissões de localização estiver concedida. */
    fun isGranted(context: Context): Boolean = ALL.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

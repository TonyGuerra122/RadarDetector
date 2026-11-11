package com.tonyguerra.radardetector.data.network

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.ContextCompat
import java.net.URI

/**
 * Permissão de rede local do Android 17+ (`ACCESS_LOCAL_NETWORK`, grupo "Dispositivos próximos").
 *
 * Apps com `targetSdk` 37 têm a rede local bloqueada por padrão: conexões para IPs privados
 * (10.x, 192.168.x, 172.16–31.x, o 10.0.2.2 do emulador…) dão **timeout** sem essa permissão.
 * A internet pública não é afetada.
 *
 * O app só pede a permissão quando o endereço da API ([apiUrl]) é de rede local. Se a API estiver
 * num domínio público, o usuário nunca vê esse pedido.
 *
 * Referência: https://developer.android.com/privacy-and-security/local-network-permission
 */
class LocalNetworkAccess(private val apiUrl: String) {

    /** `true` se, neste aparelho, a API só funciona com a permissão concedida. */
    val isPermissionRequired: Boolean
        get() = isEnforced() && isLocalNetworkUrl(apiUrl)

    /** `true` se a permissão não é necessária ou já foi concedida. */
    fun hasAccess(context: Context): Boolean =
        !isPermissionRequired ||
            ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED

    companion object {
        /** Nome literal: a constante `Manifest.permission.ACCESS_LOCAL_NETWORK` só existe no API 37+. */
        const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

        private const val ANDROID_17 = 37

        /** A restrição só existe a partir do Android 17. Antes, `INTERNET` já basta. */
        @ChecksSdkIntAtLeast(api = ANDROID_17)
        fun isEnforced(): Boolean = Build.VERSION.SDK_INT >= ANDROID_17

        /**
         * `true` se o host da [url] é de rede local: IPv4 privado (10/8, 172.16/12, 192.168/16),
         * link-local (169.254/16), IPv6 local (fc00::/7, fe80::/10) ou nome `.local` (mDNS).
         *
         * Loopback (`localhost`, 127.x) não conta: é o próprio aparelho, não a rede.
         */
        fun isLocalNetworkUrl(url: String): Boolean {
            val host = runCatching { URI(url).host }.getOrNull()
                ?.lowercase()
                ?.removePrefix("[")?.removeSuffix("]")
                ?: return false

            if (host.endsWith(".local")) return true

            val ipv4 = host.split('.').mapNotNull { it.toIntOrNull() }
            if (ipv4.size == 4 && host.count { it == '.' } == 3 && ipv4.all { it in 0..255 }) {
                val (a, b) = ipv4
                return a == 10 ||
                    (a == 172 && b in 16..31) ||
                    (a == 192 && b == 168) ||
                    (a == 169 && b == 254)
            }

            if (':' in host) {
                return host.startsWith("fc") || host.startsWith("fd") ||
                    host.startsWith("fe8") || host.startsWith("fe9") ||
                    host.startsWith("fea") || host.startsWith("feb")
            }
            return false
        }
    }
}

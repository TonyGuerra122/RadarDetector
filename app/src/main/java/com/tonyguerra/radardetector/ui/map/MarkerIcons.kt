package com.tonyguerra.radardetector.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable

/**
 * Ícones dos marcadores do mapa, desenhados em código (sem arquivos de imagem).
 * São criados uma vez por tela e reaproveitados em cada redesenho.
 */
internal class MarkerIcons(context: Context) {

    /** Radar comum: ponto verde com borda e centro pretos. */
    val radar = dot(context, sizeDp = 22, ring = MapStyle.BLACK, fill = MapStyle.GREEN, core = MapStyle.BLACK)

    /** Radar selecionado na lista: maior e com borda branca para se destacar. */
    val selectedRadar = dot(context, sizeDp = 32, ring = MapStyle.WHITE, fill = MapStyle.GREEN, core = MapStyle.BLACK)

    /** Posição do usuário: ponto branco com borda verde. */
    val user = dot(context, sizeDp = 20, ring = MapStyle.GREEN, fill = MapStyle.WHITE, core = null)

    /** Desenha círculos concêntricos: anel externo, preenchimento e, opcionalmente, um miolo. */
    private fun dot(context: Context, sizeDp: Int, ring: Int, fill: Int, core: Int?): BitmapDrawable {
        val density = context.resources.displayMetrics.density
        val size = (sizeDp * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = size / 2f

        paint.color = ring
        canvas.drawCircle(radius, radius, radius, paint)
        paint.color = fill
        canvas.drawCircle(radius, radius, radius - RING_WIDTH_DP * density, paint)
        core?.let {
            paint.color = it
            canvas.drawCircle(radius, radius, radius * CORE_RATIO, paint)
        }
        return BitmapDrawable(context.resources, bitmap)
    }

    private companion object {
        const val RING_WIDTH_DP = 3f
        const val CORE_RATIO = 0.3f
    }
}

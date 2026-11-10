package com.tonyguerra.radardetector.ui.map

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex

/**
 * Configuração visual do mapa: de onde vêm os blocos (tiles), as cores e os limites de zoom.
 *
 * Os blocos são o "World Dark Gray Base" da Esri, que é público e não precisa de chave de API.
 * Uso pessoal ou não comercial é permitido desde que o crédito [MAP_ATTRIBUTION] fique visível.
 * Antes de publicar comercialmente, revise os termos da Esri.
 */
object MapStyle {

    const val MIN_ZOOM = 4.0

    /** Zoom máximo disponível nos blocos da Esri para essa camada. */
    const val MAX_ZOOM = 16.0

    /** Crédito exigido pelos provedores dos blocos e dos dados. Precisa ficar visível na tela. */
    const val MAP_ATTRIBUTION = "© Esri · © OpenStreetMap contributors"

    /** Zoom que mostra o círculo de busca inteiro na tela para cada raio. */
    fun zoomForRadius(radiusKm: Int): Double = when {
        radiusKm <= 1 -> 15.0
        radiusKm <= 3 -> 13.5
        radiusKm <= 5 -> 12.8
        else -> 11.8
    }

    /** O padrão de URL da Esri é `z/y/x` (e não `z/x/y`), por isso a URL é montada manualmente. */
    internal val tileSource = object : OnlineTileSourceBase(
        "EsriDarkGray",
        MIN_ZOOM.toInt(),
        MAX_ZOOM.toInt(),
        256,
        "",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/"),
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String =
            baseUrl + "${MapTileIndex.getZoom(pMapTileIndex)}/" +
                "${MapTileIndex.getY(pMapTileIndex)}/${MapTileIndex.getX(pMapTileIndex)}"
    }

    /**
     * Escurece os blocos cinza e puxa para o verde, combinando com a paleta do app.
     * Cada canal é multiplicado (R 0.55, G 0.95, B 0.65) e depois diminuído em 30.
     */
    internal val greenTint = ColorMatrixColorFilter(
        ColorMatrix(
            floatArrayOf(
                0.55f, 0f, 0f, 0f, -30f,
                0f, 0.95f, 0f, 0f, -30f,
                0f, 0f, 0.65f, 0f, -30f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )

    // Cores em ARGB para a API de Views do osmdroid (que não usa o Color do Compose).
    internal const val GREEN = 0xFF00E676.toInt()
    internal const val BLACK = 0xFF000000.toInt()
    internal const val WHITE = 0xFFFFFFFF.toInt()
    internal const val LOADING_GRID = 0xFF0B0F0C.toInt()
    internal const val RADIUS_FILL = 0x1A00E676
    internal const val RADIUS_OUTLINE = 0x9900E676.toInt()
}

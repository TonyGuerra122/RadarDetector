package com.tonyguerra.radardetector.ui.map

import com.tonyguerra.radardetector.data.model.GeoPoint

/**
 * Pedido único para animar a câmera do mapa até [target] com o [zoom] indicado.
 *
 * O [id] muda a cada pedido, então dois pedidos para o mesmo ponto continuam sendo diferentes.
 * Assim, tocar duas vezes em "minha localização" recentraliza o mapa nas duas vezes, mesmo que
 * o usuário tenha arrastado o mapa entre um toque e outro.
 */
data class CameraRequest(
    val target: GeoPoint,
    val zoom: Double,
    val id: Long = System.nanoTime(),
)

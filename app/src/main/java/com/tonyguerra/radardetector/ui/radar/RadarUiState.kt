package com.tonyguerra.radardetector.ui.radar

import com.tonyguerra.radardetector.data.model.GeoPoint
import com.tonyguerra.radardetector.data.model.Radar
import com.tonyguerra.radardetector.ui.map.CameraRequest

/**
 * Tudo o que a tela de radares precisa para se desenhar. É imutável: o [RadarViewModel] gera uma
 * cópia nova a cada mudança.
 *
 * @property userLocation ponto usado na busca: a posição real do usuário ou o ponto padrão.
 * @property usingFallbackLocation `true` quando a localização não estava disponível e
 *   [userLocation] é o ponto padrão ([RadarViewModel.FALLBACK_LOCATION]).
 * @property hasLocationPermission se o usuário concedeu alguma permissão de localização.
 * @property radiusKm raio de busca escolhido. Um dos valores de [RADIUS_OPTIONS_KM].
 * @property radars resultado da última busca, do mais próximo ao mais distante.
 * @property selectedRadarId radar destacado no mapa e na lista.
 * @property isLoading se há uma busca (ou leitura de localização) em andamento.
 * @property searchFailed se a última busca falhou por erro de rede ou de servidor.
 * @property needsLocalNetworkPermission a API está na rede local e o usuário ainda não liberou
 *   "Dispositivos próximos" (Android 17+). Sem isso, a busca nem é tentada.
 * @property cameraRequest último pedido de movimento da câmera do mapa.
 */
data class RadarUiState(
    val userLocation: GeoPoint? = null,
    val usingFallbackLocation: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val radiusKm: Int = DEFAULT_RADIUS_KM,
    val radars: List<Radar> = emptyList(),
    val selectedRadarId: String? = null,
    val isLoading: Boolean = false,
    val searchFailed: Boolean = false,
    val needsLocalNetworkPermission: Boolean = false,
    val cameraRequest: CameraRequest? = null,
) {
    companion object {
        /** Raios oferecidos nos botões do topo da tela. */
        val RADIUS_OPTIONS_KM = listOf(1, 3, 5, 10)
        const val DEFAULT_RADIUS_KM = 3
    }
}

package com.tonyguerra.radardetector.ui.radar

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tonyguerra.radardetector.BuildConfig
import com.tonyguerra.radardetector.data.location.LocationPermissions
import com.tonyguerra.radardetector.data.network.LocalNetworkAccess
import com.tonyguerra.radardetector.data.location.LocationProvider
import com.tonyguerra.radardetector.data.model.GeoPoint
import com.tonyguerra.radardetector.data.model.Radar
import com.tonyguerra.radardetector.data.radar.RadarRepository
import com.tonyguerra.radardetector.ui.map.CameraRequest
import com.tonyguerra.radardetector.ui.map.MapStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado e ações da tela de radares.
 *
 * Fluxo principal: a tela pede a permissão, chama [onPermissionResult], o ViewModel lê a
 * localização e busca os radares em volta. Só uma busca roda por vez: uma nova cancela a anterior
 * (por exemplo, quando o usuário troca o raio várias vezes seguidas).
 *
 * É um [AndroidViewModel] só para ter um `Context` para o [LocationProvider] e a checagem de
 * permissão. Não guarda nenhuma referência a Activity.
 */
class RadarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RadarRepository()
    private val locationProvider = LocationProvider(application)
    private val localNetwork = LocalNetworkAccess(BuildConfig.RADAR_API_URL)

    private val _uiState = MutableStateFlow(RadarUiState())
    val uiState: StateFlow<RadarUiState> = _uiState.asStateFlow()

    /** Busca em andamento, se houver. É cancelada quando outra começa. */
    private var searchJob: Job? = null

    /**
     * `true` se a API está na rede local e falta a permissão "Dispositivos próximos" (Android 17+).
     * A tela usa isso para pedir a permissão junto com a de localização.
     */
    fun isLocalNetworkPermissionMissing(): Boolean = !localNetwork.hasAccess(getApplication())

    /** Resposta do pedido de permissão de localização. Em qualquer caso, faz a primeira busca. */
    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
        locateAndSearch()
    }

    /**
     * Lê a localização atual e refaz a busca em volta dela. Sem permissão ou sem sinal, usa
     * [FALLBACK_LOCATION] e marca [RadarUiState.usingFallbackLocation].
     */
    @SuppressLint("MissingPermission") // A permissão é verificada logo abaixo.
    fun locateAndSearch() {
        launchSearch {
            val hasPermission = LocationPermissions.isGranted(getApplication())
            val location = if (hasPermission) {
                runCatching { locationProvider.currentLocation() }.getOrNull()
            } else {
                null
            }
            val center = location ?: FALLBACK_LOCATION
            _uiState.update {
                it.copy(
                    userLocation = center,
                    usingFallbackLocation = location == null,
                    hasLocationPermission = hasPermission,
                    cameraRequest = CameraRequest(center, MapStyle.zoomForRadius(it.radiusKm)),
                )
            }
            search(center)
        }
    }

    /** Troca o raio de busca, ajusta o zoom e refaz a busca no mesmo ponto. */
    fun setRadius(km: Int) {
        if (km == _uiState.value.radiusKm) return
        _uiState.update { it.copy(radiusKm = km) }
        val center = _uiState.value.userLocation ?: return
        _uiState.update { it.copy(cameraRequest = CameraRequest(center, MapStyle.zoomForRadius(km))) }
        launchSearch { search(center) }
    }

    /** Destaca o radar e aproxima o mapa dele. */
    fun selectRadar(radar: Radar) {
        _uiState.update {
            it.copy(
                selectedRadarId = radar.id,
                cameraRequest = CameraRequest(radar.location, MapStyle.MAX_ZOOM),
            )
        }
    }

    /** Tira a seleção e volta o mapa para o ponto da busca. */
    fun centerOnUser() {
        val center = _uiState.value.userLocation ?: return
        _uiState.update {
            it.copy(
                selectedRadarId = null,
                cameraRequest = CameraRequest(center, MapStyle.zoomForRadius(it.radiusKm)),
            )
        }
    }

    /** Cancela a busca anterior e inicia [block] como a nova busca. */
    private fun launchSearch(block: suspend () -> Unit) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, searchFailed = false) }
            block()
        }
    }

    private suspend fun search(center: GeoPoint) {
        // Sem a permissão de rede local, a conexão só daria timeout depois de 10 s. Melhor avisar já.
        if (isLocalNetworkPermissionMissing()) {
            _uiState.update {
                it.copy(isLoading = false, radars = emptyList(), needsLocalNetworkPermission = true)
            }
            return
        }
        _uiState.update { it.copy(needsLocalNetworkPermission = false) }
        try {
            val radars = repository.findNearby(center, _uiState.value.radiusKm * METERS_PER_KM)
            _uiState.update { it.copy(radars = radars, isLoading = false) }
        } catch (e: CancellationException) {
            throw e // Uma busca mais nova tomou o lugar desta: não é erro.
        } catch (e: Exception) {
            Log.w(TAG, "Busca de radares falhou", e)
            _uiState.update { it.copy(isLoading = false, searchFailed = true) }
        }
    }

    companion object {
        /** Centro de São Paulo, usado quando a localização não está disponível. */
        val FALLBACK_LOCATION = GeoPoint(-23.5505, -46.6333)

        private const val TAG = "RadarViewModel"

        private const val METERS_PER_KM = 1000
    }
}

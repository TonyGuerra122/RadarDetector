package com.tonyguerra.radardetector.ui.update

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tonyguerra.radardetector.BuildConfig
import com.tonyguerra.radardetector.data.update.InvalidUpdateManifestException
import com.tonyguerra.radardetector.data.update.UpdateInfo
import com.tonyguerra.radardetector.data.update.UpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Etapas do atualizador, na ordem em que acontecem. */
sealed interface UpdateUiState {
    /** Nada a mostrar: sem atualização, verificação em andamento ou usuário escolheu "Depois". */
    data object Idle : UpdateUiState

    /** Existe uma versão mais nova no servidor. */
    data class Available(val info: UpdateInfo) : UpdateUiState

    /** Baixando o APK. [progress] vai de 0 a 1, ou é `null` se o tamanho for desconhecido. */
    data class Downloading(val info: UpdateInfo, val progress: Float?) : UpdateUiState

    /** APK baixado e verificado, esperando o usuário abrir o instalador. */
    data class ReadyToInstall(val info: UpdateInfo, val apk: File) : UpdateUiState

    /** O download falhou ou o arquivo não passou na verificação do SHA-256. */
    data class Failed(val info: UpdateInfo) : UpdateUiState
}

/**
 * Verifica uma vez, ao abrir o app, se há versão nova no servidor de arquivos.
 *
 * Fica desligado em builds de debug e quando `UPDATE_MANIFEST_URL` está vazio. No debug, o APK do
 * servidor (assinado com a chave de release) nem instalaria por cima do app de debug, que usa
 * outra chave.
 *
 * O `latest.json` é validado antes de qualquer download. Se a verificação falhar (sem internet,
 * servidor fora do ar ou `latest.json` inválido), o erro só vai para o log: não vale interromper o
 * usuário por isso.
 */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val manifestUrl = BuildConfig.UPDATE_MANIFEST_URL
    private val repository = UpdateRepository(
        manifestUrl = manifestUrl,
        packageName = application.packageName,
        cacheDir = application.cacheDir,
    )

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    init {
        if (!BuildConfig.DEBUG && manifestUrl.isNotBlank()) checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            try {
                val latest = repository.fetchLatest()
                if (latest.versionCode > BuildConfig.VERSION_CODE) {
                    _uiState.value = UpdateUiState.Available(latest)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: InvalidUpdateManifestException) {
                Log.e(TAG, "latest.json inválido, atualização ignorada: ${e.message}", e)
            } catch (e: Exception) {
                Log.w(TAG, "Não foi possível verificar atualizações", e)
            }
        }
    }

    /** Começa (ou recomeça, depois de uma falha) o download da versão nova. */
    fun download(info: UpdateInfo) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _uiState.value = UpdateUiState.Downloading(info, progress = 0f)
            try {
                val apk = repository.download(info) { progress ->
                    _uiState.value = UpdateUiState.Downloading(info, progress)
                }
                _uiState.value = UpdateUiState.ReadyToInstall(info, apk)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao baixar a atualização", e)
                _uiState.value = UpdateUiState.Failed(info)
            }
        }
    }

    /** Fecha o aviso até a próxima abertura do app. Se estiver baixando, cancela. */
    fun dismiss() {
        downloadJob?.cancel()
        _uiState.value = UpdateUiState.Idle
    }

    private companion object {
        const val TAG = "UpdateViewModel"
    }
}

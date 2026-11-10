package com.tonyguerra.radardetector.ui.update

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tonyguerra.radardetector.BuildConfig
import com.tonyguerra.radardetector.R
import com.tonyguerra.radardetector.data.update.ApkInstaller
import com.tonyguerra.radardetector.ui.theme.CarbonOutline
import com.tonyguerra.radardetector.ui.theme.NeonGreen

/**
 * Diálogos do atualizador automático. Não desenha nada enquanto não houver versão nova.
 *
 * Fica por cima de qualquer tela: basta chamar uma vez, junto do conteúdo principal.
 */
@Composable
fun UpdatePrompt(viewModel: UpdateViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    when (val current = state) {
        UpdateUiState.Idle -> Unit

        is UpdateUiState.Available -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.update_available_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.update_available_message,
                        current.info.versionName,
                        BuildConfig.VERSION_NAME,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.download(current.info) }) {
                    Text(stringResource(R.string.update_action_download))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.update_action_later)) }
            },
        )

        is UpdateUiState.Downloading -> AlertDialog(
            onDismissRequest = {},
            // Tocar fora não fecha: para sair durante o download, só pelo "Cancelar".
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text(stringResource(R.string.update_downloading_title, current.info.versionName)) },
            text = {
                Column {
                    Spacer(Modifier.height(8.dp))
                    val progress = current.progress
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            color = NeonGreen,
                            trackColor = CarbonOutline,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(
                            color = NeonGreen,
                            trackColor = CarbonOutline,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.update_action_cancel)) }
            },
        )

        is UpdateUiState.ReadyToInstall -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.update_ready_title)) },
            text = { Text(stringResource(R.string.update_ready_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Sem a permissão "Instalar apps desconhecidos", leva o usuário até ela.
                        // O diálogo continua aberto para ele tocar em "Instalar" ao voltar.
                        val intent = if (ApkInstaller.canInstall(context)) {
                            ApkInstaller.installIntent(context, current.apk)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ApkInstaller.permissionSettingsIntent(context)
                        } else {
                            null
                        }
                        intent?.let(context::startActivity)
                    },
                ) { Text(stringResource(R.string.update_action_install)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.update_action_later)) }
            },
        )

        is UpdateUiState.Failed -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.update_failed_title)) },
            text = { Text(stringResource(R.string.update_failed_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.download(current.info) }) {
                    Text(stringResource(R.string.action_retry))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.update_action_close)) }
            },
        )
    }
}

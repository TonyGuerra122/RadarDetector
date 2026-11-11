package com.tonyguerra.radardetector.ui.radar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tonyguerra.radardetector.R
import com.tonyguerra.radardetector.data.model.Radar
import com.tonyguerra.radardetector.ui.radar.RadarUiState
import com.tonyguerra.radardetector.ui.theme.Carbon
import com.tonyguerra.radardetector.ui.theme.CarbonOutline
import com.tonyguerra.radardetector.ui.theme.DeepGreen
import com.tonyguerra.radardetector.ui.theme.NeonGreen
import com.tonyguerra.radardetector.ui.theme.PureBlack
import com.tonyguerra.radardetector.ui.theme.TextPrimary
import com.tonyguerra.radardetector.ui.theme.TextSecondary

/**
 * Painel inferior com a lista de radares.
 *
 * Mostra, de cima para baixo: cabeçalho com "Atualizar", barra de progresso durante a busca,
 * aviso de permissão (se faltar) e, por fim, um destes: erro, lista vazia ou lista de radares.
 */
@Composable
internal fun RadarListPanel(
    state: RadarUiState,
    onRadarClick: (Radar) -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onRequestLocalNetworkPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Carbon,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, CarbonOutline),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .heightIn(max = 360.dp)
                .navigationBarsPadding(),
        ) {
            DragHandle(Modifier.align(Alignment.CenterHorizontally))
            PanelHeader(state, onRefresh)

            if (state.isLoading) {
                LinearProgressIndicator(
                    color = NeonGreen,
                    trackColor = CarbonOutline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }

            if (state.usingFallbackLocation && !state.hasLocationPermission) {
                PermissionBanner(stringResource(R.string.permission_rationale), onRequestPermission)
            }
            if (state.needsLocalNetworkPermission) {
                PermissionBanner(stringResource(R.string.local_network_rationale), onRequestLocalNetworkPermission)
            }

            when {
                // O aviso acima já explica o que falta; não mostra "nenhum radar" por baixo dele.
                state.needsLocalNetworkPermission -> Unit

                state.searchFailed -> MessageBox(
                    message = stringResource(R.string.error_search_failed),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = onRefresh,
                )

                state.radars.isEmpty() && !state.isLoading ->
                    MessageBox(stringResource(R.string.list_empty, state.radiusKm))

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.radars, key = { it.id }) { radar ->
                        RadarListItem(
                            radar = radar,
                            selected = radar.id == state.selectedRadarId,
                            onClick = { onRadarClick(radar) },
                        )
                    }
                }
            }
        }
    }
}

/** Alça decorativa no topo do painel. */
@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(top = 10.dp)
            .size(width = 40.dp, height = 4.dp)
            .background(CarbonOutline, CircleShape),
    )
}

@Composable
private fun PanelHeader(state: RadarUiState, onRefresh: () -> Unit) {
    Row(
        Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.list_title),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (state.isLoading) {
                    stringResource(R.string.list_subtitle_loading)
                } else {
                    pluralStringResource(
                        R.plurals.list_subtitle_count,
                        state.radars.size,
                        state.radars.size,
                        state.radiusKm,
                    )
                },
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(onClick = onRefresh, enabled = !state.isLoading) {
            Text(stringResource(R.string.action_refresh), color = NeonGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Aviso com botão "Permitir", usado para as permissão de localização e de rede local. */
@Composable
private fun PermissionBanner(message: String, onRequestPermission: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(DeepGreen, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PureBlack),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) { Text(stringResource(R.string.action_allow)) }
    }
}

/** Mensagem centralizada (erro ou lista vazia), com um botão de ação opcional. */
@Composable
private fun MessageBox(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        if (actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PureBlack),
            ) { Text(actionLabel) }
        }
    }
}

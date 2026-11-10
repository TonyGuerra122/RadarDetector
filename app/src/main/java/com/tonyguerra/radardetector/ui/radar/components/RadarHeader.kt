package com.tonyguerra.radardetector.ui.radar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tonyguerra.radardetector.R
import com.tonyguerra.radardetector.ui.theme.CarbonOutline
import com.tonyguerra.radardetector.ui.theme.NeonGreen
import com.tonyguerra.radardetector.ui.theme.PureBlack
import com.tonyguerra.radardetector.ui.theme.TextPrimary
import com.tonyguerra.radardetector.ui.theme.TextSecondary

/**
 * Topo da tela: cartão com o logo, o título e a contagem de radares, e embaixo os botões de raio.
 *
 * @param usingFallbackLocation muda o subtítulo para avisar que o ponto é o padrão.
 * @param radarCount total exibido à direita. Enquanto [isLoading] for `true`, aparece "--".
 */
@Composable
internal fun RadarHeader(
    usingFallbackLocation: Boolean,
    isLoading: Boolean,
    radarCount: Int,
    radiusKm: Int,
    radiusOptionsKm: List<Int>,
    onRadiusSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        HeaderCard(usingFallbackLocation, isLoading, radarCount)
        Spacer(Modifier.height(8.dp))
        RadiusSelector(radiusKm, radiusOptionsKm, onRadiusSelected)
    }
}

@Composable
private fun HeaderCard(usingFallbackLocation: Boolean, isLoading: Boolean, radarCount: Int) {
    Surface(
        color = PureBlack.copy(alpha = 0.88f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CarbonOutline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Símbolo do logo (antena + escudo). É decorativo: o título ao lado já diz o nome do app.
            Image(
                painter = painterResource(R.drawable.logo_emblem),
                contentDescription = null,
                modifier = Modifier.size(width = 56.dp, height = 36.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.header_title),
                    color = NeonGreen,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        if (usingFallbackLocation) R.string.header_subtitle_fallback else R.string.header_subtitle_tracking,
                    ),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isLoading) stringResource(R.string.header_count_loading) else radarCount.toString(),
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.header_count_label),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/** Linha de botões ("chips") para escolher o raio. O selecionado fica verde preenchido. */
@Composable
private fun RadiusSelector(radiusKm: Int, optionsKm: List<Int>, onRadiusSelected: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        optionsKm.forEach { km ->
            val selected = km == radiusKm
            FilterChip(
                selected = selected,
                onClick = { onRadiusSelected(km) },
                label = { Text(stringResource(R.string.radius_option, km), fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = PureBlack.copy(alpha = 0.85f),
                    labelColor = TextPrimary,
                    selectedContainerColor = NeonGreen,
                    selectedLabelColor = PureBlack,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = CarbonOutline,
                    selectedBorderColor = NeonGreen,
                ),
            )
        }
    }
}

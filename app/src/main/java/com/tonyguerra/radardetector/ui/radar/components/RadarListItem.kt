package com.tonyguerra.radardetector.ui.radar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tonyguerra.radardetector.R
import com.tonyguerra.radardetector.data.model.Radar
import com.tonyguerra.radardetector.ui.radar.formatDistance
import com.tonyguerra.radardetector.ui.radar.labelRes
import com.tonyguerra.radardetector.ui.theme.CarbonLight
import com.tonyguerra.radardetector.ui.theme.CarbonOutline
import com.tonyguerra.radardetector.ui.theme.DeepGreen
import com.tonyguerra.radardetector.ui.theme.NeonGreen
import com.tonyguerra.radardetector.ui.theme.PureBlack
import com.tonyguerra.radardetector.ui.theme.TextPrimary
import com.tonyguerra.radardetector.ui.theme.TextSecondary

/**
 * Cartão de um radar na lista: placa com o limite de velocidade, tipo e descrição, e a distância.
 * Quando [selected] for `true`, o cartão fica com fundo verde-escuro e borda neon.
 */
@Composable
internal fun RadarListItem(radar: Radar, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) DeepGreen else CarbonLight,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (selected) NeonGreen else CarbonOutline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpeedBadge(radar.speedLimitKmh)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(radar.type.labelRes()),
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = radar.description ?: stringResource(R.string.radar_no_description),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDistance(radar.distanceMeters),
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.radar_distance_suffix),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/** Placa redonda com o limite de velocidade. Mostra "--" quando o limite não está cadastrado. */
@Composable
private fun SpeedBadge(speedLimitKmh: Int?) {
    Box(
        Modifier
            .size(52.dp)
            .background(PureBlack, CircleShape)
            .border(3.dp, NeonGreen, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = speedLimitKmh?.toString() ?: stringResource(R.string.radar_speed_unknown),
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                lineHeight = 17.sp,
            )
            Text(
                text = stringResource(R.string.speed_unit),
                color = TextSecondary,
                fontSize = 8.sp,
                lineHeight = 8.sp,
            )
        }
    }
}

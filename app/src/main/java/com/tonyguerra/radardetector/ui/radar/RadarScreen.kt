package com.tonyguerra.radardetector.ui.radar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tonyguerra.radardetector.R
import com.tonyguerra.radardetector.data.location.LocationPermissions
import com.tonyguerra.radardetector.data.network.LocalNetworkAccess
import com.tonyguerra.radardetector.data.model.GeoPoint
import com.tonyguerra.radardetector.data.model.Radar
import com.tonyguerra.radardetector.data.enum.RadarSource
import com.tonyguerra.radardetector.data.enum.RadarType
import com.tonyguerra.radardetector.ui.map.MapStyle
import com.tonyguerra.radardetector.ui.map.RadarMap
import com.tonyguerra.radardetector.ui.radar.components.LocateIcon
import com.tonyguerra.radardetector.ui.radar.components.RadarHeader
import com.tonyguerra.radardetector.ui.radar.components.RadarListPanel
import com.tonyguerra.radardetector.ui.theme.NeonGreen
import com.tonyguerra.radardetector.ui.theme.PureBlack
import com.tonyguerra.radardetector.ui.theme.RadarDetectorTheme
import com.tonyguerra.radardetector.ui.theme.TextSecondary

/**
 * Tela principal: mapa em tela cheia com os radares próximos.
 *
 * Esta versão "com estado" liga o [RadarViewModel] e, na primeira abertura, pede a permissão de
 * localização e, se a API estiver na rede local, a de "Dispositivos próximos" (Android 17+).
 * Todo o desenho fica em [RadarContent], que não tem estado e pode ser usado no Preview.
 */
@Composable
fun RadarScreen(viewModel: RadarViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // O resultado pode trazer também a permissão de rede local, então a localização é conferida
    // direto no sistema em vez de olhar "qualquer permissão concedida".
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.onPermissionResult(granted = LocationPermissions.isGranted(context)) }

    val localNetworkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.locateAndSearch() }

    LaunchedEffect(Unit) {
        // Se o ViewModel já tem dados (ex.: depois de girar a tela), não pede nem busca de novo.
        if (state.userLocation != null) return@LaunchedEffect
        val missing = buildList {
            if (!LocationPermissions.isGranted(context)) addAll(LocationPermissions.ALL)
            if (viewModel.isLocalNetworkPermissionMissing()) add(LocalNetworkAccess.PERMISSION)
        }
        if (missing.isEmpty()) {
            viewModel.onPermissionResult(granted = LocationPermissions.isGranted(context))
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    RadarContent(
        state = state,
        onRadiusSelected = viewModel::setRadius,
        onRadarClick = viewModel::selectRadar,
        onRefresh = viewModel::locateAndSearch,
        onCenterOnUser = viewModel::centerOnUser,
        onRequestPermission = { permissionLauncher.launch(LocationPermissions.ALL) },
        onRequestLocalNetworkPermission = { localNetworkLauncher.launch(LocalNetworkAccess.PERMISSION) },
    )
}

/**
 * Layout da tela em camadas: o mapa ocupa tudo e, por cima, ficam o cabeçalho (topo), o rodapé
 * do mapa (crédito e botão de localização) e o painel com a lista (base).
 */
@Composable
fun RadarContent(
    state: RadarUiState,
    onRadiusSelected: (Int) -> Unit,
    onRadarClick: (Radar) -> Unit,
    onRefresh: () -> Unit,
    onCenterOnUser: () -> Unit,
    onRequestPermission: () -> Unit,
    onRequestLocalNetworkPermission: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(PureBlack),
    ) {
        RadarMap(
            userLocation = state.userLocation,
            radiusMeters = state.radiusKm * 1000,
            radars = state.radars,
            selectedRadarId = state.selectedRadarId,
            cameraRequest = state.cameraRequest,
            onRadarClick = onRadarClick,
            modifier = Modifier.fillMaxSize(),
        )

        RadarHeader(
            usingFallbackLocation = state.usingFallbackLocation,
            isLoading = state.isLoading,
            radarCount = state.radars.size,
            radiusKm = state.radiusKm,
            radiusOptionsKm = RadarUiState.RADIUS_OPTIONS_KM,
            onRadiusSelected = onRadiusSelected,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(12.dp),
        )

        Column(Modifier.align(Alignment.BottomCenter)) {
            MapFooter(onCenterOnUser)
            RadarListPanel(
                state = state,
                onRadarClick = onRadarClick,
                onRefresh = onRefresh,
                onRequestPermission = onRequestPermission,
                onRequestLocalNetworkPermission = onRequestLocalNetworkPermission,
            )
        }
    }
}

/** Crédito obrigatório do mapa à esquerda e botão "minha localização" à direita. */
@Composable
private fun MapFooter(onCenterOnUser: () -> Unit) {
    val centerDescription = stringResource(R.string.cd_center_on_user)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = MapStyle.MAP_ATTRIBUTION,
            color = TextSecondary,
            fontSize = 10.sp,
            modifier = Modifier
                .weight(1f)
                .background(PureBlack.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        SmallFloatingActionButton(
            onClick = onCenterOnUser,
            containerColor = PureBlack,
            contentColor = NeonGreen,
            modifier = Modifier
                .border(1.dp, NeonGreen, RoundedCornerShape(12.dp))
                .semantics { contentDescription = centerDescription },
        ) {
            LocateIcon(NeonGreen)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun RadarContentPreview() {
    val center = GeoPoint(-23.5505, -46.6333)
    RadarDetectorTheme {
        RadarContent(
            state = RadarUiState(
                userLocation = center,
                hasLocationPermission = true,
                radars = listOf(
                    Radar("local:781", center, 80, RadarType.FIXED, "BR-381", "Decrescente", RadarSource.LOCAL, 320.0),
                    Radar("overpass:1", center, 60, RadarType.FIXED, null, null, RadarSource.OVERPASS, 870.0),
                    Radar("local:782", center, 80, RadarType.FIXED, "BR-381", "Crescente", RadarSource.LOCAL, 2450.0),
                ),
                selectedRadarId = "local:781",
            ),
            onRadiusSelected = {},
            onRadarClick = {},
            onRefresh = {},
            onCenterOnUser = {},
            onRequestPermission = {},
            onRequestLocalNetworkPermission = {},
        )
    }
}

package com.tonyguerra.radardetector.ui.map

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.tonyguerra.radardetector.data.model.GeoPoint
import com.tonyguerra.radardetector.data.model.Radar
import com.tonyguerra.radardetector.ui.theme.Carbon
import org.osmdroid.config.Configuration
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import org.osmdroid.util.GeoPoint as OsmGeoPoint

/**
 * Mapa com a posição do usuário, o círculo do raio de busca e os radares.
 *
 * É um `MapView` do osmdroid (View clássica) dentro do Compose via [AndroidView]. O composable não
 * guarda estado próprio: tudo vem por parâmetro e as ações saem por callback.
 *
 * @param userLocation posição usada na busca. Se `null`, o círculo e o marcador do usuário não aparecem.
 * @param radiusMeters raio do círculo de busca, em metros.
 * @param radars radares para desenhar.
 * @param selectedRadarId radar destacado, ou `null`.
 * @param cameraRequest quando muda, a câmera anima até o ponto pedido.
 * @param onRadarClick chamado quando o usuário toca num marcador de radar.
 */
@Composable
fun RadarMap(
    userLocation: GeoPoint?,
    radiusMeters: Int,
    radars: List<Radar>,
    selectedRadarId: Long?,
    cameraRequest: CameraRequest?,
    onRadarClick: (Radar) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        // O osmdroid não renderiza no Preview do Android Studio. Mostra só o fundo.
        Box(modifier.background(Carbon))
        return
    }

    val context = LocalContext.current
    val mapView = remember { createMapView(context) }
    val icons = remember { MarkerIcons(context) }
    val currentOnRadarClick by rememberUpdatedState(onRadarClick)

    // Liga o MapView ao ciclo de vida do composable e libera os recursos quando ele sai da tela.
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(cameraRequest) {
        cameraRequest?.let {
            mapView.controller.animateTo(it.target.toOsm(), it.zoom, CAMERA_ANIMATION_MS)
        }
    }

    // Redesenha as camadas sempre que os dados mudam. A ordem importa: o que entra por último
    // fica por cima. Por isso o usuário é adicionado depois dos radares.
    LaunchedEffect(userLocation, radiusMeters, radars, selectedRadarId) {
        mapView.overlays.clear()
        userLocation?.let { mapView.overlays.add(radiusOverlay(mapView, it, radiusMeters)) }
        radars.forEach { radar ->
            val icon = if (radar.id == selectedRadarId) icons.selectedRadar else icons.radar
            mapView.overlays.add(
                marker(mapView, radar.location, icon) { currentOnRadarClick(radar) },
            )
        }
        userLocation?.let { mapView.overlays.add(marker(mapView, it, icons.user)) }
        mapView.invalidate()
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/** Cria e configura o `MapView`. O cache dos blocos fica no cache interno do app. */
private fun createMapView(context: Context): MapView {
    Configuration.getInstance().apply {
        userAgentValue = context.packageName
        osmdroidBasePath = File(context.cacheDir, "osmdroid")
        osmdroidTileCache = File(osmdroidBasePath, "tiles")
    }
    return MapView(context).apply {
        setTileSource(MapStyle.tileSource)
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        isTilesScaledToDpi = true
        minZoomLevel = MapStyle.MIN_ZOOM
        maxZoomLevel = MapStyle.MAX_ZOOM
        overlayManager.tilesOverlay.apply {
            setColorFilter(MapStyle.greenTint)
            loadingBackgroundColor = MapStyle.BLACK
            loadingLineColor = MapStyle.LOADING_GRID
        }
        controller.setZoom(INITIAL_ZOOM)
    }
}

/** Círculo semitransparente que mostra a área da busca. */
private fun radiusOverlay(mapView: MapView, center: GeoPoint, radiusMeters: Int) =
    Polygon(mapView).apply {
        points = Polygon.pointsAsCircle(center.toOsm(), radiusMeters.toDouble())
        fillPaint.color = MapStyle.RADIUS_FILL
        outlinePaint.color = MapStyle.RADIUS_OUTLINE
        outlinePaint.strokeWidth = 3f
        // Devolve false para deixar o toque passar para o mapa, sem abrir o balão padrão.
        setOnClickListener { _, _, _ -> false }
    }

/**
 * Marcador centralizado no ponto. O listener sempre devolve true para o osmdroid não abrir o
 * balão de informação padrão.
 */
private fun marker(
    mapView: MapView,
    position: GeoPoint,
    icon: android.graphics.drawable.Drawable,
    onClick: () -> Unit = {},
) = Marker(mapView).apply {
    this.position = position.toOsm()
    this.icon = icon
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    setOnMarkerClickListener { _, _ ->
        onClick()
        true
    }
}

private fun GeoPoint.toOsm() = OsmGeoPoint(latitude, longitude)

private const val INITIAL_ZOOM = 13.0
private const val CAMERA_ANIMATION_MS = 700L

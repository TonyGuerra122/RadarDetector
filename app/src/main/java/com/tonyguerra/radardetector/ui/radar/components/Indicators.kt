package com.tonyguerra.radardetector.ui.radar.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Ícone de mira ("minha localização") desenhado em Canvas. Assim o app não precisa da
 * dependência `material-icons-extended`, que é grande.
 */
@Composable
internal fun LocateIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val r = size.minDimension / 2
        val ringRadius = r * 0.6f
        val stroke = 2.dp.toPx()
        drawCircle(color, radius = ringRadius, style = Stroke(stroke))
        drawCircle(color, radius = r * 0.22f)
        // Quatro traços saindo do anel até a borda (cima, baixo, esquerda e direita).
        val c = center
        drawLine(color, c.copy(y = 0f), c.copy(y = c.y - ringRadius), stroke)
        drawLine(color, c.copy(y = size.height), c.copy(y = c.y + ringRadius), stroke)
        drawLine(color, c.copy(x = 0f), c.copy(x = c.x - ringRadius), stroke)
        drawLine(color, c.copy(x = size.width), c.copy(x = c.x + ringRadius), stroke)
    }
}

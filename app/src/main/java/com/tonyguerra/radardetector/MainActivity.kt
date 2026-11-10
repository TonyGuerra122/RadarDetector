package com.tonyguerra.radardetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tonyguerra.radardetector.ui.radar.RadarScreen
import com.tonyguerra.radardetector.ui.theme.RadarDetectorTheme
import com.tonyguerra.radardetector.ui.update.UpdatePrompt

/**
 * Única Activity do app. Ativa o modo de ponta a ponta (o mapa fica atrás das barras do sistema)
 * e mostra a [RadarScreen], com o aviso de atualização ([UpdatePrompt]) por cima quando houver.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RadarDetectorTheme {
                RadarScreen()
                UpdatePrompt()
            }
        }
    }
}

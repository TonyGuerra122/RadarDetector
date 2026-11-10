package com.tonyguerra.radardetector.data.enum

/** Tipo de fiscalização, derivado da tag `enforcement` do OpenStreetMap. */
enum class RadarType {
    /** Radar fixo de velocidade (`enforcement=maxspeed` ou só `highway=speed_camera`). */
    FIXED,

    /** Radar de avanço de sinal vermelho (`enforcement=traffic_signals`). */
    TRAFFIC_LIGHT,

    /** Radar de velocidade média entre dois pontos (`enforcement=average_speed`). */
    AVERAGE_SPEED,
}

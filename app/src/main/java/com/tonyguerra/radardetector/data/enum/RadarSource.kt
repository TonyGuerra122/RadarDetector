package com.tonyguerra.radardetector.data.enum

/** De onde a API tirou o radar (campo `source` da resposta). */
enum class RadarSource(val apiValue: String) {
    /** Cadastro do OpenStreetMap, consultado pela API no Overpass. */
    OVERPASS("overpass"),

    /** Cadastro próprio do servidor (por exemplo, controladores de rodovia). */
    LOCAL("local"),

    /** Valor que o app ainda não conhece. O radar aparece normalmente. */
    UNKNOWN("unknown");

    companion object {
        fun fromApi(value: String?): RadarSource =
            entries.firstOrNull { it.apiValue.equals(value?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

package com.tonyguerra.radardetector.data.radar

import com.tonyguerra.radardetector.data.enum.RadarSource
import com.tonyguerra.radardetector.data.enum.RadarType
import com.tonyguerra.radardetector.data.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RadarResponseParserTest {

    private val center = GeoPoint(-22.8086, -46.2650)

    /** Exemplo real da API. */
    private val sample = """
        {"data":[
          {"type":"node","id":8390726476,"lat":-22.8086016,"lon":-46.2650164,
           "tags":{"highway":"speed_camera","maxspeed":"80","direction":""},"source":"overpass"},
          {"type":"Controlador","id":781,"lat":-22.807197,"lon":-46.261658,
           "tags":{"highway":"BR-381","maxspeed":"80","direction":"Decrescente"},"source":"local"},
          {"type":"Controlador","id":782,"lat":-22.827892,"lon":-46.314047,
           "tags":{"highway":"BR-381","maxspeed":"80","direction":"Decrescente"},"source":"local"}
        ],"overpass_count":1,"local_count":2,"duplicates_removed":0}
    """.trimIndent()

    @Test
    fun `exemplo da API vira tres radares ordenados pela distancia`() {
        val radars = RadarResponseParser.parse(sample, center)

        assertEquals(listOf("overpass:8390726476", "local:781", "local:782"), radars.map { it.id })
        assertTrue(radars.zipWithNext().all { (a, b) -> a.distanceMeters <= b.distanceMeters })
    }

    @Test
    fun `radar do overpass nao usa speed_camera como nome da via e ignora direcao vazia`() {
        val osm = RadarResponseParser.parse(sample, center).first { it.source == RadarSource.OVERPASS }

        assertEquals(80, osm.speedLimitKmh)
        assertEquals(RadarType.FIXED, osm.type)
        assertNull(osm.road)
        assertNull(osm.direction)
    }

    @Test
    fun `radar local traz rodovia e sentido`() {
        val local = RadarResponseParser.parse(sample, center).first { it.id == "local:781" }

        assertEquals(RadarSource.LOCAL, local.source)
        assertEquals("BR-381", local.road)
        assertEquals("Decrescente", local.direction)
        assertEquals(80, local.speedLimitKmh)
    }

    @Test
    fun `mesmo id em fontes diferentes nao colide`() {
        val json = """
            {"data":[
              {"id":781,"lat":-22.80,"lon":-46.26,"tags":{},"source":"overpass"},
              {"id":781,"lat":-22.81,"lon":-46.27,"tags":{},"source":"local"}
            ]}
        """.trimIndent()

        assertEquals(setOf("overpass:781", "local:781"), RadarResponseParser.parse(json, center).map { it.id }.toSet())
    }

    @Test
    fun `item repetido na mesma fonte aparece uma vez`() {
        val json = """
            {"data":[
              {"id":781,"lat":-22.80,"lon":-46.26,"source":"local"},
              {"id":781,"lat":-22.80,"lon":-46.26,"source":"local"}
            ]}
        """.trimIndent()

        assertEquals(1, RadarResponseParser.parse(json, center).size)
    }

    @Test
    fun `itens sem coordenadas ou id validos sao ignorados sem derrubar a lista`() {
        val json = """
            {"data":[
              {"id":1,"lon":-46.26,"source":"local"},
              {"id":2,"lat":"abc","lon":-46.26,"source":"local"},
              {"id":3,"lat":120.0,"lon":-46.26,"source":"local"},
              {"lat":-22.80,"lon":-46.26,"source":"local"},
              {"id":4,"lat":-22.80,"lon":-46.26,"source":"local"}
            ]}
        """.trimIndent()

        assertEquals(listOf("local:4"), RadarResponseParser.parse(json, center).map { it.id })
    }

    @Test
    fun `campos nulos ou ausentes viram null em vez do texto null`() {
        val json = """{"data":[{"id":5,"lat":-22.80,"lon":-46.26,"tags":{"highway":null,"maxspeed":null},"source":null}]}"""
        val radar = RadarResponseParser.parse(json, center).single()

        assertNull(radar.road)
        assertNull(radar.speedLimitKmh)
        assertEquals(RadarSource.UNKNOWN, radar.source)
        assertEquals("unknown:5", radar.id)
    }

    @Test
    fun `lista vazia e valida`() {
        assertTrue(RadarResponseParser.parse("""{"data":[],"overpass_count":0}""", center).isEmpty())
    }

    @Test
    fun `resposta sem data ou que nao e JSON e rejeitada`() {
        assertThrows(IOException::class.java) { RadarResponseParser.parse("""{"erro":"x"}""", center) }
        assertThrows(IOException::class.java) { RadarResponseParser.parse("<html>502</html>", center) }
    }
}

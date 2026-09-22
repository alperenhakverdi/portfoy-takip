package com.portfoy.calc

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TazelikTest {
    private val simdi = Instant.parse("2026-09-22T12:00:00Z")

    @Test
    fun `elle fiyat tam 7 gunde eski sayilmaz, 7 gunu asinca sayilir`() {
        assertFalse(Tazelik.elleFiyatEskiMi(simdi.minusSeconds(7 * 24 * 3600), simdi))
        assertTrue(Tazelik.elleFiyatEskiMi(simdi.minusSeconds(7 * 24 * 3600 + 1), simdi))
    }

    @Test
    fun `kur hafta sonu kadar eskiyse sorun sayilmaz, iki gunu asinca belirtilir`() {
        assertFalse(Tazelik.kurEskiMi(simdi.minusSeconds(36 * 3600), simdi)) // cuma akşamı → pazar
        assertFalse(Tazelik.kurEskiMi(simdi.minusSeconds(48 * 3600), simdi))
        assertTrue(Tazelik.kurEskiMi(simdi.minusSeconds(48 * 3600 + 1), simdi))
    }

    @Test
    fun `gun farki tam gun verir, gelecekteki zamani sifir sayar`() {
        assertEquals(8L, Tazelik.gunFarki(simdi.minusSeconds(8 * 24 * 3600 + 5000), simdi))
        assertEquals(0L, Tazelik.gunFarki(simdi.plusSeconds(3600), simdi))
    }
}

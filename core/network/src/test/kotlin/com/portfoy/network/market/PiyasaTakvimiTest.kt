package com.portfoy.network.market

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PiyasaTakvimiTest {

    private val takvim = MarketCalendar()
    private val istanbul = ZoneId.of("Europe/Istanbul")

    private fun tsi(instant: Instant) = instant.atZone(istanbul).toLocalTime().toString()

    @Test
    fun `ABD seansi kis saatinde 17_30 - 00_00 TSI`() {
        // 2026-03-06 Cuma: ABD henüz kış saatinde (EST).
        val seans = takvim.session(Market.US, LocalDate.of(2026, 3, 6))!!
        assertEquals("17:30", tsi(seans.open))
        assertEquals("00:00", tsi(seans.close))
    }

    @Test
    fun `ABD seansi yaz saatinde 16_30 - 23_00 TSI`() {
        // 2026-03-09 Pazartesi: ABD yaz saatine geçmiş (8 Mart), TSİ karşılığı bir saat erken.
        val seans = takvim.session(Market.US, LocalDate.of(2026, 3, 9))!!
        assertEquals("16:30", tsi(seans.open))
        assertEquals("23:00", tsi(seans.close))
    }

    @Test
    fun `sonbahar gecisi - kasim basinda TSI karsiligi tekrar kayar`() {
        // ABD 1 Kasım 2026'da kış saatine döner.
        val oncesi = takvim.session(Market.US, LocalDate.of(2026, 10, 30))!! // Cuma, yaz saati
        val sonrasi = takvim.session(Market.US, LocalDate.of(2026, 11, 2))!! // Pazartesi, kış saati
        assertEquals("16:30", tsi(oncesi.open))
        assertEquals("17:30", tsi(sonrasi.open))
    }

    @Test
    fun `BIST seansi 10_00 - 18_00 TSI`() {
        val seans = takvim.session(Market.BIST, LocalDate.of(2026, 9, 21))!!
        assertEquals("10:00", tsi(seans.open))
        assertEquals("18:00", tsi(seans.close))
    }

    @Test
    fun `hafta sonu ve tatilde seans yok`() {
        assertNull(takvim.session(Market.BIST, LocalDate.of(2026, 9, 19))) // Cumartesi
        assertNull(takvim.session(Market.BIST, LocalDate.of(2026, 10, 29))) // Cumhuriyet Bayramı
        assertNull(takvim.session(Market.US, LocalDate.of(2026, 7, 3))) // Bağımsızlık Günü (gözlenen)
    }

    @Test
    fun `ABD ve Turkiye takvimleri birbirinden bagimsizdir`() {
        // 3 Temmuz: ABD kapalı, BIST açık. 29 Ekim: BIST kapalı, ABD açık.
        assertFalse(takvim.isTradingDay(Market.US, LocalDate.of(2026, 7, 3)))
        assertTrue(takvim.isTradingDay(Market.BIST, LocalDate.of(2026, 7, 3)))
        assertFalse(takvim.isTradingDay(Market.BIST, LocalDate.of(2026, 10, 29)))
        assertTrue(takvim.isTradingDay(Market.US, LocalDate.of(2026, 10, 29)))
    }

    @Test
    fun `seans icinde acik, disinda kapali`() {
        assertTrue(takvim.isOpen(Market.BIST, Instant.parse("2026-09-21T09:00:00Z"))) // 12:00 TSİ
        assertFalse(takvim.isOpen(Market.BIST, Instant.parse("2026-09-21T04:00:00Z"))) // 07:00 TSİ
        assertFalse(takvim.isOpen(Market.BIST, Instant.parse("2026-09-21T15:00:00Z"))) // 18:00 TSİ, kapanış
    }

    @Test
    fun `takvimin kapsadigi yil bilinir`() {
        assertTrue(takvim.coversYear(Market.US, 2026))
        assertFalse(takvim.coversYear(Market.US, 2027))
    }
}

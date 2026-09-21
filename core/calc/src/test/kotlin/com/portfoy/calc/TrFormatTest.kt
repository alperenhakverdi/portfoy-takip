package com.portfoy.calc

import com.portfoy.calc.format.TrFormat
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TrFormatTest {

    @Test
    fun `tutar binlik nokta ve ondalik virgulle yazilir`() {
        assertEquals("1.234.567,89 ₺", TrFormat.money(bd("1234567.89")))
        assertEquals("0,00 ₺", TrFormat.money(bd("0")))
    }

    @Test
    fun `tutar iki haneye yarim yukari yuvarlanir`() {
        assertEquals("0,01 ₺", TrFormat.money(bd("0.005")))
        assertEquals("10,00 ₺", TrFormat.money(bd("9.996")))
    }

    @Test
    fun `yuzde iki hane`() {
        assertEquals("%12,40", TrFormat.percent(bd("12.4")))
    }

    @Test
    fun `adet en fazla dort hane, gereksiz sifir yok`() {
        assertEquals("10,5", TrFormat.quantity(bd("10.5")))
        assertEquals("0,1235", TrFormat.quantity(bd("0.12345678")))
        assertEquals("1.000", TrFormat.quantity(bd("1000")))
    }

    @Test
    fun `yon isareti`() {
        assertEquals("▲ %12,40", TrFormat.signedPercent(bd("12.4")))
        assertEquals("▼ %3,10", TrFormat.signedPercent(bd("-3.1")))
        assertEquals("▲ 18.430,50 ₺", TrFormat.signedMoney(bd("18430.5")))
        assertEquals("▼ 250,00 ₺", TrFormat.signedMoney(bd("-250")))
    }

    @Test
    fun `yuvarlaninca sifir gorunen deger yon isareti almaz`() {
        assertEquals("%0,00", TrFormat.signedPercent(bd("0.001")))
        assertEquals("%0,00", TrFormat.signedPercent(bd("-0.001")))
        assertEquals("%0,00", TrFormat.signedPercent(bd("0")))
    }

    @Test
    fun `hesaplanamayan deger tire ile gosterilir`() {
        assertEquals("—", TrFormat.percent(null))
        assertEquals("—", TrFormat.signedPercent(null))
        assertEquals("—", TrFormat.money(null))
    }

    @Test
    fun `son guncelleme bugunse yalnizca saat, degilse tarih de yazilir`() {
        val istanbul = ZoneId.of("Europe/Istanbul")
        val zaman = Instant.parse("2026-09-21T15:45:00Z") // İstanbul 18:45
        val ayniGun = Instant.parse("2026-09-21T20:00:00Z")
        val uctenSonra = Instant.parse("2026-09-24T09:00:00Z")

        assertEquals("18:45", TrFormat.lastUpdate(zaman, ayniGun, istanbul))
        assertEquals("21.09 18:45", TrFormat.lastUpdate(zaman, uctenSonra, istanbul))
    }
}

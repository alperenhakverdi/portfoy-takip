package com.portfoy.network.market

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TazelemeTest {

    private val program = RefreshSchedule()
    private val istanbul = ZoneId.of("Europe/Istanbul")

    private fun tsi(slot: Slot) = slot.time.atZone(istanbul).toLocalTime().toString()

    // --- Adaptif aralık ---

    @Test
    fun `kucuk portfoyde gun ici 30 dakikada bir`() {
        val plan = AdaptiveInterval.choose(assetCount = 20, dailyCap = 500, sessionMinutes = 390)
        assertEquals(30, plan.intervalMinutes)
        assertEquals(20, plan.assetsPerRound)
        assertEquals(1, plan.rotationEvery)
    }

    @Test
    fun `butce daralinca aralik uzar`() {
        // 40 varlık: açılış+kapanış 80 çağrı ayrılır, kalan 120. 30 dk → 12 tur × 30 = 360 sığmaz.
        val plan = AdaptiveInterval.choose(assetCount = 40, dailyCap = 200, sessionMinutes = 390)
        // 45 dk → 8 tur × 30 = 240 sığmaz; 60 dk → 6 tur × 30 = 180 sığmaz → gün içi kapanır.
        assertNull(plan.intervalMinutes)
    }

    @Test
    fun `orta portfoyde 45 dakikaya cikar`() {
        // 30 varlık: ayrılan 60, kalan 190. 30 dk → 12×30=360 sığmaz; 45 dk → 8×30=240 sığmaz;
        // 60 dk → 6×30=180 sığar.
        val plan = AdaptiveInterval.choose(assetCount = 30, dailyCap = 250, sessionMinutes = 390)
        assertEquals(60, plan.intervalMinutes)
    }

    @Test
    fun `cok buyuk portfoyde gun ici kapanir, acilis ve kapanis korunur`() {
        val plan = AdaptiveInterval.choose(assetCount = 200, dailyCap = 500, sessionMinutes = 390)
        assertNull(plan.intervalMinutes)
        assertEquals(30, plan.assetsPerRound)
        assertEquals(7, plan.rotationEvery) // 200 / 30 → 7 turda bir
    }

    @Test
    fun `30dan fazla varlik donusumlu tazelenir, tur basina 30 cagri asilmaz`() {
        val plan = AdaptiveInterval.choose(assetCount = 75, dailyCap = 5000, sessionMinutes = 390)
        assertEquals(30, plan.assetsPerRound)
        assertEquals(3, plan.rotationEvery)
    }

    @Test
    fun `varlik yoksa gun ici yok`() {
        assertNull(AdaptiveInterval.choose(0, 500, 390).intervalMinutes)
    }

    @Test
    fun `seans tur sayisi acilis ve kapanis haric hesaplanir`() {
        assertEquals(12, AdaptiveInterval.intradayRounds(390, 30)) // ABD
        assertEquals(15, AdaptiveInterval.intradayRounds(480, 30)) // BIST
    }

    // --- Turlar ---

    @Test
    fun `ABD turlari - acilis, gun ici ve kapanis`() {
        val turlar = program.slots(RefreshGroup.US, LocalDate.of(2026, 9, 21), intradayMinutes = 30)
        assertEquals(RoundKind.OPEN, turlar.first().kind)
        assertEquals("16:32", tsi(turlar.first())) // açılış 16:30 + 2 dk (yaz saati)
        assertEquals(RoundKind.CLOSE, turlar.last().kind)
        assertEquals("23:15", tsi(turlar.last())) // kapanış 23:00 + 15 dk
        assertEquals(12, turlar.count { it.kind == RoundKind.INTRADAY })
    }

    @Test
    fun `gun ici kapaliysa yalnizca acilis ve kapanis turu vardir`() {
        val turlar = program.slots(RefreshGroup.BIST, LocalDate.of(2026, 9, 21), intradayMinutes = null)
        assertEquals(listOf(RoundKind.OPEN, RoundKind.CLOSE), turlar.map { it.kind })
    }

    @Test
    fun `BIST gun ici turlari kapanistan once biter`() {
        val turlar = program.slots(RefreshGroup.BIST, LocalDate.of(2026, 9, 21), intradayMinutes = 30)
        val sonGunIci = turlar.last { it.kind == RoundKind.INTRADAY }
        assertTrue(sonGunIci.time.isBefore(Instant.parse("2026-09-21T15:00:00Z"))) // 18:00 TSİ
        assertEquals(15, turlar.count { it.kind == RoundKind.INTRADAY })
    }

    @Test
    fun `fon gunde bir kez 21_15`() {
        val turlar = program.slots(RefreshGroup.FUND, LocalDate.of(2026, 9, 21))
        assertEquals(1, turlar.size)
        assertEquals("21:15", tsi(turlar.single()))
    }

    @Test
    fun `hafta sonu ve tatilde kripto disinda hicbir grup calismaz`() {
        val cumartesi = LocalDate.of(2026, 9, 19)
        (RefreshGroup.entries - RefreshGroup.KRIPTO).forEach { assertTrue("$it", program.slots(it, cumartesi).isEmpty()) }
        assertTrue(program.slots(RefreshGroup.BIST, LocalDate.of(2026, 10, 29)).isEmpty())
        assertTrue(program.slots(RefreshGroup.FUND, LocalDate.of(2026, 10, 29)).isEmpty())
        assertTrue(program.slots(RefreshGroup.US, LocalDate.of(2026, 7, 3)).isEmpty())
    }

    @Test
    fun `kripto hafta sonu dahil gunun her saati 30 dakikada bir`() {
        val cumartesi = LocalDate.of(2026, 9, 19)
        val turlar = program.slots(RefreshGroup.KRIPTO, cumartesi)
        assertEquals(48, turlar.size) // 00:00 - 23:30
        assertEquals("00:00", tsi(turlar.first()))
        assertEquals("23:30", tsi(turlar.last()))
        assertTrue(turlar.all { it.kind == RoundKind.INTRADAY })
    }

    @Test
    fun `kur saat basi, altin 30 dakikada bir`() {
        val kur = program.slots(RefreshGroup.FX, LocalDate.of(2026, 9, 21))
        assertEquals(11, kur.size)
        assertEquals("09:00", tsi(kur.first()))
        val altin = program.slots(RefreshGroup.GOLD, LocalDate.of(2026, 9, 21))
        assertEquals(29, altin.size) // 08:00–22:00
    }

    // --- Vadesi gelen tur ---

    @Test
    fun `hic calismamissa gecmis en yakin tur vadesi gelmis sayilir`() {
        val simdi = Instant.parse("2026-09-21T09:10:00Z") // 12:10 TSİ
        val tur = program.dueSlot(RefreshGroup.BIST, simdi, lastRun = null)
        assertNotNull(tur)
        assertEquals("12:02", tsi(tur!!)) // 10:02 açılış, 10:32, 11:02 ... en yakın geçmiş tur 12:02
    }

    @Test
    fun `son calismadan sonra yeni tur yoksa vade yok`() {
        val simdi = Instant.parse("2026-09-21T09:10:00Z") // 12:10 TSİ
        val sonCalisma = Instant.parse("2026-09-21T09:03:00Z") // 12:03 TSİ, 12:02 turundan sonra
        assertNull(program.dueSlot(RefreshGroup.BIST, simdi, sonCalisma))
    }

    @Test
    fun `kacirilan kapanis turu akşam acilinca telafi edilir`() {
        val simdi = Instant.parse("2026-09-21T20:00:00Z") // 23:00 TSİ, BIST kapanmış
        val sonCalisma = Instant.parse("2026-09-21T13:40:00Z") // 16:40 TSİ, gün içi tur
        val tur = program.dueSlot(RefreshGroup.BIST, simdi, sonCalisma)!!
        assertEquals(RoundKind.CLOSE, tur.kind)
        assertEquals("18:15", tsi(tur))
    }

    @Test
    fun `kapanis turu bir kez calistiysa tekrar vadesi gelmez`() {
        val simdi = Instant.parse("2026-09-21T20:00:00Z")
        val sonCalisma = Instant.parse("2026-09-21T15:20:00Z") // 18:20 TSİ, kapanış turundan sonra
        assertNull(program.dueSlot(RefreshGroup.BIST, simdi, sonCalisma))
    }

    @Test
    fun `seans oncesi vade yok`() {
        val simdi = Instant.parse("2026-09-21T05:00:00Z") // 08:00 TSİ, BIST açılmadan
        assertNull(program.dueSlot(RefreshGroup.BIST, simdi, lastRun = null))
    }

    @Test
    fun `sonraki tur hafta sonunu atlar`() {
        val cumaAksami = Instant.parse("2026-09-18T20:00:00Z") // Cuma 23:00 TSİ
        val sonraki = program.nextSlot(RefreshGroup.BIST, cumaAksami)!!
        assertEquals(LocalDate.of(2026, 9, 21), sonraki.time.atZone(istanbul).toLocalDate()) // Pazartesi
        assertEquals(RoundKind.OPEN, sonraki.kind)
    }
}

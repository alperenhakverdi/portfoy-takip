package com.portfoy.network

import com.portfoy.model.SourceId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ButceVeSaglikTest {

    /** Elle ilerletilebilen saat. */
    private class HareketliSaat(var simdi: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = simdi
    }

    @Test
    fun `gunluk tavan asilinca istek reddedilir`() {
        val saat = HareketliSaat(Instant.parse("2026-09-21T10:00:00Z"))
        val butce = CallBudget(mapOf(SourceId.YAHOO to 5), saat, ZoneOffset.UTC)

        assertTrue(butce.tryConsume(SourceId.YAHOO, 3))
        assertTrue(butce.tryConsume(SourceId.YAHOO, 2))
        assertFalse(butce.tryConsume(SourceId.YAHOO, 1))
        assertEquals(0, butce.remaining(SourceId.YAHOO))
    }

    @Test
    fun `parcali harcama tavani asacaksa hic dusulmez`() {
        val saat = HareketliSaat(Instant.parse("2026-09-21T10:00:00Z"))
        val butce = CallBudget(mapOf(SourceId.YAHOO to 5), saat, ZoneOffset.UTC)

        assertFalse(butce.tryConsume(SourceId.YAHOO, 6))
        assertEquals(5, butce.remaining(SourceId.YAHOO))
    }

    @Test
    fun `gun donunce sayac sifirlanir`() {
        val saat = HareketliSaat(Instant.parse("2026-09-21T10:00:00Z"))
        val butce = CallBudget(mapOf(SourceId.YAHOO to 5), saat, ZoneOffset.UTC)
        butce.tryConsume(SourceId.YAHOO, 5)

        saat.simdi = Instant.parse("2026-09-22T00:01:00Z")

        assertEquals(5, butce.remaining(SourceId.YAHOO))
    }

    @Test
    fun `tavani tanimli olmayan kaynak sinirsizdir`() {
        val butce = CallBudget(emptyMap(), Clock.systemUTC(), ZoneOffset.UTC)
        assertTrue(butce.tryConsume(SourceId.MANUEL, 1_000_000))
    }

    @Test
    fun `tur ve gunluk gecmis tavani dokumandaki degerlerdir`() {
        assertEquals(30, CallBudget.ROUND_CAP)
        assertEquals(100, CallBudget.HISTORY_DAILY_CAP)
    }

    // --- Kaynak sağlığı ---

    @Test
    fun `24 saat veri vermeyen kaynak icin bir kez uyarilir`() {
        val saglik = SourceHealth()
        val t0 = Instant.parse("2026-09-20T10:00:00Z")
        saglik.recordSuccess(SourceId.TEFAS, t0)
        saglik.recordFailure(SourceId.TEFAS, t0.plusSeconds(3600))

        assertFalse(saglik.shouldWarn(SourceId.TEFAS, t0.plus(Duration.ofHours(23))))
        assertTrue(saglik.shouldWarn(SourceId.TEFAS, t0.plus(Duration.ofHours(25))))
        assertFalse(saglik.shouldWarn(SourceId.TEFAS, t0.plus(Duration.ofHours(30)))) // tek seferlik
    }

    @Test
    fun `hata olmayan kaynak icin uyari yok`() {
        val saglik = SourceHealth()
        saglik.recordSuccess(SourceId.TEFAS, Instant.parse("2026-09-01T10:00:00Z"))
        assertFalse(saglik.shouldWarn(SourceId.TEFAS, Instant.parse("2026-09-21T10:00:00Z")))
    }

    @Test
    fun `kaynak toparlaninca sonraki kesintide yeniden uyarilabilir`() {
        val saglik = SourceHealth()
        val t0 = Instant.parse("2026-09-01T00:00:00Z")
        saglik.recordSuccess(SourceId.TEFAS, t0)
        saglik.recordFailure(SourceId.TEFAS, t0.plusSeconds(60))
        assertTrue(saglik.shouldWarn(SourceId.TEFAS, t0.plus(Duration.ofHours(30))))

        val t1 = t0.plus(Duration.ofDays(5))
        saglik.recordSuccess(SourceId.TEFAS, t1)
        saglik.recordFailure(SourceId.TEFAS, t1.plusSeconds(60))
        assertTrue(saglik.shouldWarn(SourceId.TEFAS, t1.plus(Duration.ofHours(30))))
    }
}

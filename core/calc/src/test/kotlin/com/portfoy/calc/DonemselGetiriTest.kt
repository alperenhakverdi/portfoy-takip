package com.portfoy.calc

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Basit Dietz senaryoları. Portföy 100.000 TL ile başlar, 30 günlük dönemin sonunda 210.000 TL olur
 * ve dönem içinde 100.000 TL eklenir. Toplam kazanç 10.000 TL'dir; getiri, eklenen paranın dönemde
 * ne kadar kaldığına göre değişir.
 */
class DonemselGetiriTest {

    private val baslangic = LocalDate.of(2026, 9, 1)
    private val bitis = LocalDate.of(2026, 10, 1) // 30 gün

    private fun getiri(katkiTarihi: LocalDate?) = periodReturn(
        startValue = bd("100000"),
        endValue = bd("210000"),
        contributions = listOfNotNull(katkiTarihi?.let { Contribution(it, bd("100000")) }),
        periodStart = baslangic,
        periodEnd = bitis,
    )

    @Test
    fun `katki yok - getiri dogrudan yuzde 10`() {
        val sonuc = periodReturn(bd("100000"), bd("110000"), emptyList(), baslangic, bitis)
        assertBd("10", sonuc)
    }

    @Test
    fun `katki donem basinda - para tum donem piyasada, getiri yuzde 5`() {
        assertBd("5", getiri(baslangic.plusDays(1)))
    }

    @Test
    fun `katki donem ortasinda - agirlik yarim, getiri yaklasik yuzde 6,67`() {
        assertBd("6.67", getiri(LocalDate.of(2026, 9, 17)))
    }

    @Test
    fun `katki son gun - getiri seyrelmez, yuzde 9,68 (duz formulun yuzde 5'i degil)`() {
        assertBd("9.68", getiri(bitis))
    }

    @Test
    fun `donem baslangicinda yapilan alim zaten baslangic degerindedir, katki sayilmaz`() {
        val sonuc = periodReturn(
            startValue = bd("200000"),
            endValue = bd("220000"),
            contributions = listOf(Contribution(baslangic, bd("100000"))),
            periodStart = baslangic,
            periodEnd = bitis,
        )
        assertBd("10", sonuc)
    }

    @Test
    fun `donem sonrasi katki yok sayilir`() {
        val sonuc = periodReturn(
            startValue = bd("100000"),
            endValue = bd("110000"),
            contributions = listOf(Contribution(bitis.plusDays(3), bd("50000"))),
            periodStart = baslangic,
            periodEnd = bitis,
        )
        assertBd("10", sonuc)
    }

    @Test
    fun `zarar eksi getiri verir`() {
        assertBd("-5", periodReturn(bd("100000"), bd("95000"), emptyList(), baslangic, bitis))
    }

    @Test
    fun `payda sifirsa getiri hesaplanamaz`() {
        assertNullValue(periodReturn(bd("0"), bd("0"), emptyList(), baslangic, bitis))
    }

    @Test
    fun `sifir gunluk donem hesaplanamaz`() {
        assertNullValue(periodReturn(bd("100"), bd("110"), emptyList(), baslangic, baslangic))
    }

    // --- Portföy yaşı ve geçmiş çekim penceresi ---

    @Test
    fun `portfoy yasi secilen donemden kisaysa grafik mevcut veri kadar cizilir`() {
        val bugun = LocalDate.of(2026, 9, 20)
        val pencere = chartWindow(
            requestedStart = bugun.minusYears(3),
            oldestTransaction = bugun.minusDays(60),
            today = bugun,
        )
        assertTrue(pencere.truncated)
        assertEquals(bugun.minusDays(60), pencere.start)
        assertEquals(60L, pencere.portfolioDays)
    }

    @Test
    fun `portfoy yasi yeterliyse grafik istenen donemden baslar`() {
        val bugun = LocalDate.of(2026, 9, 20)
        val pencere = chartWindow(bugun.minusMonths(1), bugun.minusYears(2), bugun)
        assertFalse(pencere.truncated)
        assertEquals(bugun.minusMonths(1), pencere.start)
    }

    @Test
    fun `alis tarihi 5 yildan eskiyse gecmis seri 5 yila sinirlanir`() {
        val bugun = LocalDate.of(2026, 9, 20)
        val aralik = historyFetchRange(LocalDate.of(2015, 3, 1), bugun)
        assertTrue(aralik.truncated)
        assertEquals(bugun.minusYears(5), aralik.start)
    }

    @Test
    fun `alis tarihi 5 yil icindeyse gecmis seri alis tarihinden baslar`() {
        val bugun = LocalDate.of(2026, 9, 20)
        val alis = LocalDate.of(2024, 5, 10)
        val aralik = historyFetchRange(alis, bugun)
        assertFalse(aralik.truncated)
        assertEquals(alis, aralik.start)
    }
}

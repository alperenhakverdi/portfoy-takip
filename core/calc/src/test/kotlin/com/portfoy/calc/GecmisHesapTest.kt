package com.portfoy.calc

import com.portfoy.model.Candle
import com.portfoy.model.Category
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GecmisHesapTest {

    private fun gun(d: Int) = LocalDate.of(2026, 9, d)

    private fun alim(varlik: Long, d: Int, adet: String, maliyet: String) =
        HistoricalPurchase(varlik, gun(d), bd(adet), bd(maliyet))

    private fun mum(d: Int, kapanis: String) = Candle(gun(d), bd(kapanis))

    private val kategoriler = mapOf(1L to Category.BIST, 2L to Category.ABD, 3L to Category.NAKIT)

    @Test
    fun `alis gunu ve sonrasi her gun adet carpi son bilinen fiyat`() {
        val seri = portfolioSeries(
            purchases = listOf(alim(1, 1, "10", "1000")),
            prices = mapOf(1L to listOf(mum(1, "100"), mum(2, "110"), mum(3, "120"))),
            categories = kategoriler,
            from = gun(1),
            to = gun(3),
        )
        assertEquals(listOf(1000, 1100, 1200), seri.map { it.valueTl.toInt() })
        assertTrue(seri.all { it.costTl.compareTo(bd("1000")) == 0 })
        assertFalse(seri.any { it.estimated })
    }

    @Test
    fun `hafta sonu ve tatil gunleri son islem gununun degeriyle duzlestirilir`() {
        // 4 Eylül 2026 Cuma kapanış 110; 5 ve 6 Eylül hafta sonu (fiyat yok); 7 Pazartesi 120.
        val seri = portfolioSeries(
            purchases = listOf(alim(1, 4, "10", "1000")),
            prices = mapOf(1L to listOf(mum(4, "110"), mum(7, "120"))),
            categories = kategoriler,
            from = gun(4),
            to = gun(7),
        )
        assertEquals(listOf(1100, 1100, 1100, 1200), seri.map { it.valueTl.toInt() })
    }

    @Test
    fun `alis tarihinden onceki gunlerde varlik yoktur, sonraki alim adedi arttirir`() {
        val seri = portfolioSeries(
            purchases = listOf(alim(1, 3, "10", "1000"), alim(1, 5, "10", "1200")),
            prices = mapOf(1L to (1..6).map { mum(it, "100") }),
            categories = kategoriler,
            from = gun(1),
            to = gun(6),
        )
        assertEquals(listOf(3, 4, 5, 6), seri.map { it.date.dayOfMonth }) // 1 ve 2'de portföy yok
        assertEquals(listOf(1000, 1000, 2000, 2000), seri.map { it.valueTl.toInt() })
        assertEquals(listOf(1000, 1000, 2200, 2200), seri.map { it.costTl.toInt() })
    }

    @Test
    fun `nakit her gun 1 TL fiyatla degerlenir ve kategori dagilimi dogru`() {
        val seri = portfolioSeries(
            purchases = listOf(alim(1, 1, "10", "1000"), alim(3, 1, "500", "500")),
            prices = mapOf(1L to listOf(mum(1, "100"), mum(2, "150"))),
            categories = kategoriler,
            from = gun(1),
            to = gun(2),
        )
        assertEquals(listOf(1500, 2000), seri.map { it.valueTl.toInt() })
        assertBd("1500", seri[1].byCategory[Category.BIST])
        assertBd("500", seri[1].byCategory[Category.NAKIT])
    }

    @Test
    fun `fiyati olmayan varlik maliyetiyle degerlenir ve tahmini isaretlenir`() {
        val seri = portfolioSeries(
            purchases = listOf(alim(1, 1, "10", "1000")),
            prices = emptyMap(),
            categories = kategoriler,
            from = gun(1),
            to = gun(3),
        )
        assertEquals(listOf(1000, 1000, 1000), seri.map { it.valueTl.toInt() })
        assertTrue(seri.all { it.estimated })
    }

    @Test
    fun `serinin ilk fiyatindan once olan gunler maliyetle degerlenir`() {
        // Fiyat serisi 3. günde başlıyor, alım 1. günde.
        val seri = portfolioSeries(
            purchases = listOf(alim(1, 1, "10", "1000")),
            prices = mapOf(1L to listOf(mum(3, "130"))),
            categories = kategoriler,
            from = gun(1),
            to = gun(3),
        )
        assertEquals(listOf(1000, 1000, 1300), seri.map { it.valueTl.toInt() })
        assertEquals(listOf(true, true, false), seri.map { it.estimated })
    }

    @Test
    fun `sirasiz fiyat ve alim listeleri de dogru sonuc verir`() {
        val seri = portfolioSeries(
            purchases = listOf(alim(1, 3, "10", "1200"), alim(1, 1, "10", "1000")),
            prices = mapOf(1L to listOf(mum(3, "120"), mum(1, "100"), mum(2, "110"))),
            categories = kategoriler,
            from = gun(1),
            to = gun(3),
        )
        assertEquals(listOf(1000, 1100, 2400), seri.map { it.valueTl.toInt() })
    }

    @Test
    fun `alim yoksa ya da aralik ters ise bos doner`() {
        assertTrue(portfolioSeries(emptyList(), emptyMap(), kategoriler, gun(1), gun(3)).isEmpty())
        assertTrue(portfolioSeries(listOf(alim(1, 1, "1", "1")), emptyMap(), kategoriler, gun(5), gun(3)).isEmpty())
    }

    @Test
    fun `kategorisi bilinmeyen varlik atlanir`() {
        val seri = portfolioSeries(
            purchases = listOf(alim(99, 1, "10", "1000")),
            prices = emptyMap(),
            categories = kategoriler,
            from = gun(1),
            to = gun(2),
        )
        assertTrue(seri.isEmpty())
    }

    // --- Getiri serisi ---

    private fun nokta(d: Int, deger: String, maliyet: String) =
        DailyPoint(gun(d), bd(deger), bd(maliyet), emptyMap(), estimated = false)

    @Test
    fun `getiri serisi ilk gun sifirdan baslar ve fiyat artisini izler`() {
        val getiri = returnSeries(
            listOf(nokta(1, "1000", "1000"), nokta(2, "1050", "1000"), nokta(3, "1100", "1000")),
            emptyList(),
        )
        assertBd("0", getiri[0].second)
        assertBd("5", getiri[1].second)
        assertBd("10", getiri[2].second)
    }

    @Test
    fun `donem icinde eklenen para getiri sayilmaz`() {
        // Gün 1: 1000 TL. Gün 2: fiyat değişmedi ama 1000 TL daha alındı (değer 2000). Getiri hâlâ 0 olmalı.
        val getiri = returnSeries(
            listOf(nokta(1, "1000", "1000"), nokta(2, "2000", "2000")),
            listOf(Contribution(gun(2), bd("1000"))),
        )
        assertBd("0", getiri[1].second)
    }

    @Test
    fun `bos seri bos getiri verir`() {
        assertTrue(returnSeries(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun `tek noktali seri sifir getiri verir`() {
        val getiri = returnSeries(listOf(nokta(1, "1000", "1000")), emptyList())
        assertEquals(1, getiri.size)
        assertBd("0", getiri.single().second)
    }
}

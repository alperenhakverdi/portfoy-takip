package com.portfoy.calc

import com.portfoy.model.Category
import com.portfoy.model.UnitType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PozisyonVeDegerlemeTest {

    @Test
    fun `tek alim - maliyet adet ve birim maliyet`() {
        val position = positionOf(listOf(alis(1, "10", "100")))
        assertBd("1000", position.totalCost)
        assertBd("10", position.quantity)
        assertBd("100", position.unitCost)
    }

    @Test
    fun `komisyonlu alim - komisyon maliyete eklenir`() {
        val position = positionOf(listOf(alis(1, "10", "100", commission = "5")))
        assertBd("1005", position.totalCost)
        assertBd("100.5", position.unitCost)
    }

    @Test
    fun `cok alim - agirlikli ortalama maliyet`() {
        val position = positionOf(
            listOf(
                alis(1, "10", "100", commission = "5"), // 1005
                alis(1, "10", "120"), // 1200
            ),
        )
        assertBd("2205", position.totalCost)
        assertBd("20", position.quantity)
        assertBd("110.25", position.unitCost)
    }

    @Test
    fun `adet sifirsa birim maliyet tanimsiz`() {
        assertNullValue(positionOf(emptyList()).unitCost)
    }

    @Test
    fun `azaltma sonrasi agirlikli ortalama maliyet degismez`() {
        // 20 adet 100'den (2000 maliyet, ortalama 100) -> 5 adet azalt (fiyati onemsiz, gerceklesen kar zarar yok)
        val position = positionOf(
            listOf(
                alis(1, "20", "100", date = LocalDate.of(2026, 1, 1)),
                azalt(1, "5", "999", date = LocalDate.of(2026, 1, 5)),
            ),
        )
        assertBd("15", position.quantity)
        assertBd("1500", position.totalCost)
        assertBd("100", position.unitCost)
    }

    @Test
    fun `azaltma elde olandan fazlaysa sifirda durur`() {
        val position = positionOf(
            listOf(
                alis(1, "10", "50", date = LocalDate.of(2026, 1, 1)),
                azalt(1, "999", date = LocalDate.of(2026, 1, 2)),
            ),
        )
        assertBd("0", position.quantity)
        assertBd("0", position.totalCost)
        assertNullValue(position.unitCost)
    }

    @Test
    fun `azaltma sonrasi yeni alim ortalamayi yeniden hesaplar`() {
        // 10 adet 100'den (1000) -> 5 azalt (kalan 5 adet, 500 maliyet) -> 5 adet 200'den al (1000 daha)
        // kalan: 10 adet, 1500 maliyet, ortalama 150.
        val position = positionOf(
            listOf(
                alis(1, "10", "100", date = LocalDate.of(2026, 1, 1)),
                azalt(1, "5", date = LocalDate.of(2026, 1, 2)),
                alis(1, "5", "200", date = LocalDate.of(2026, 1, 3)),
            ),
        )
        assertBd("10", position.quantity)
        assertBd("1500", position.totalCost)
        assertBd("150", position.unitCost)
    }

    @Test
    fun `kayitlar tarih sirasina gore islenir, ekleme sirasindan bagimsiz`() {
        // Ayni islemler ters sirada verilse de tarihe gore islenir; sonuc degismez.
        val position = positionOf(
            listOf(
                azalt(1, "5", date = LocalDate.of(2026, 1, 2)),
                alis(1, "10", "100", date = LocalDate.of(2026, 1, 1)),
            ),
        )
        assertBd("5", position.quantity)
        assertBd("500", position.totalCost)
    }

    @Test
    fun `kar zarar ve getiri yuzdesi`() {
        val value = currentValue(bd("120"), bd("10")) // 1200
        assertBd("200", profitLoss(value, bd("1000")))
        assertBd("20", returnPercent(value, bd("1000")))
    }

    @Test
    fun `zarar eksi getiri verir`() {
        assertBd("-10", returnPercent(bd("900"), bd("1000")))
    }

    @Test
    fun `maliyet sifirsa getiri hesaplanamaz`() {
        assertNullValue(returnPercent(bd("100"), bd("0")))
    }

    @Test
    fun `kur ve hisse hareketi bileske getiri verir`() {
        // 10 adet, alışta 200 USD x 40 TL = 8.000 TL / adet -> maliyet 80.000 TL.
        // Bugün: hisse 220 USD (+%10), kur 48 (+%20) -> beklenen bileşke %32, toplamda %30 değil.
        val priceTl = usdToTry(bd("220"), bd("48"))
        val abd = asset(1, "AAPL", Category.ABD)
        val summary = summarize(listOf(Holding(abd, listOf(alis(1, "10", "8000")), priceTl)))

        assertBd("105600", summary.totalValue)
        assertBd("25600", summary.profitLoss)
        assertBd("32", summary.returnPercent)
    }

    @Test
    fun `yalnizca nakit portfoy - getiri sifir ve sifira bolme yok`() {
        val nakit = asset(1, "TRY", Category.NAKIT, UnitType.TL)
        val summary = summarize(listOf(Holding(nakit, listOf(alis(1, "1000", "1")), currentPriceTl = null)))

        assertBd("1000", summary.totalValue)
        assertBd("0", summary.profitLoss)
        assertBd("0", summary.returnPercent)
        assertBd("100", summary.allocation.single().percent)
    }

    @Test
    fun `nakit fiyati verilen degerden bagimsiz olarak 1 kabul edilir`() {
        val nakit = asset(1, "TRY", Category.NAKIT, UnitType.TL)
        val summary = summarize(listOf(Holding(nakit, listOf(alis(1, "500", "1")), currentPriceTl = bd("99"))))
        assertBd("500", summary.totalValue)
    }

    @Test
    fun `bos portfoy`() {
        val summary = summarize(emptyList())
        assertTrue(summary.isEmpty)
        assertBd("0", summary.totalValue)
        assertNullValue(summary.returnPercent)
        assertTrue(summary.allocation.isEmpty())
    }

    @Test
    fun `fiyat alinamayan varlik maliyetiyle degerlenir ve isaretlenir`() {
        val bist = asset(1, "THYAO", Category.BIST)
        val summary = summarize(listOf(Holding(bist, listOf(alis(1, "10", "100")), currentPriceTl = null)))

        val result = summary.categories.single().assets.single()
        assertTrue(result.priceMissing)
        assertBd("1000", result.currentValue)
        assertBd("0", result.profitLoss)
    }

    @Test
    fun `kayitlari silinen varlik portfoyden duser`() {
        val bist = asset(1, "THYAO", Category.BIST)
        val summary = summarize(listOf(Holding(bist, emptyList(), bd("300"))))
        assertTrue(summary.isEmpty)
    }

    @Test
    fun `kategoriler ve varliklar tl degerine gore buyukten kucuge siralanir`() {
        val a = asset(1, "AAPL", Category.ABD)
        val b = asset(2, "GOOGL", Category.ABD)
        val t = asset(3, "THYAO", Category.BIST)
        val summary = summarize(
            listOf(
                Holding(a, listOf(alis(1, "1", "100")), bd("100")), // ABD 100
                Holding(b, listOf(alis(2, "1", "100")), bd("300")), // ABD 300
                Holding(t, listOf(alis(3, "1", "100")), bd("500")), // BIST 500
            ),
        )

        assertEquals(listOf(Category.BIST, Category.ABD), summary.categories.map { it.category })
        assertEquals(listOf("GOOGL", "AAPL"), summary.categories[1].assets.map { it.asset.code })
        assertBd("900", summary.totalValue)
        assertFalse(summary.isEmpty)
    }
}

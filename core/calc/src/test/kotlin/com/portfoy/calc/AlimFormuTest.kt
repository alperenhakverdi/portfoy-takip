package com.portfoy.calc

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlimFormuTest {

    private val bugun = LocalDate.of(2026, 9, 21)

    private fun dogrula(
        fiyat: String = "100",
        adet: String = "10",
        komisyon: String = "",
        tarih: LocalDate = bugun,
        nakit: Boolean = false,
    ) = dogrulaAlim(fiyat, adet, komisyon, tarih, bugun, nakit)

    // --- Sayı çözme ---

    @Test
    fun `turkce ve noktali sayilar cozulur`() {
        assertBd("1234.56", parseDecimal("1.234,56"))
        assertBd("1234.56", parseDecimal("1234,56"))
        assertBd("1234.56", parseDecimal("1234.56"))
        assertBd("0.5", parseDecimal("0,5"))
        assertBd("1234567", parseDecimal("1.234.567"), scale = 0)
    }

    @Test
    fun `nokta ve tam uc hane binlik ayiracidir, digerleri ondalik`() {
        assertBd("5000", parseDecimal("5.000"), scale = 0)
        assertBd("1234", parseDecimal("1.234"), scale = 0)
        assertBd("0.125", parseDecimal("0.125"), scale = 3)
        assertBd("1.5", parseDecimal("1.5"), scale = 1)
        assertBd("12.34", parseDecimal("12.34"))
    }

    @Test
    fun `bos ve bozuk metin cozulemez`() {
        assertNull(parseDecimal(""))
        assertNull(parseDecimal("   "))
        assertNull(parseDecimal("abc"))
        assertNull(parseDecimal("12,3,4"))
    }

    // --- Doğrulama ---

    @Test
    fun `gecerli form kabul edilir`() {
        val sonuc = dogrula(fiyat = "285,50", adet = "10", komisyon = "5")
        assertTrue(sonuc.hatalar.gecerli)
        assertBd("285.50", sonuc.degerler!!.fiyat)
        assertBd("5", sonuc.degerler!!.komisyon)
    }

    @Test
    fun `komisyon bos birakilabilir ve sifir sayilir`() {
        assertBd("0", dogrula(komisyon = "").degerler!!.komisyon)
    }

    @Test
    fun `sifir ya da negatif adet ve fiyat engellenir`() {
        assertNotNull(dogrula(adet = "0").hatalar.adet)
        assertNotNull(dogrula(adet = "-5").hatalar.adet)
        assertNotNull(dogrula(fiyat = "0").hatalar.fiyat)
        assertNotNull(dogrula(fiyat = "-1").hatalar.fiyat)
        assertNull(dogrula(adet = "0").degerler)
    }

    @Test
    fun `bos alan hata verir`() {
        assertNotNull(dogrula(adet = "").hatalar.adet)
        assertNotNull(dogrula(fiyat = "").hatalar.fiyat)
    }

    @Test
    fun `gelecek tarih engellenir, bugun ve gecmis kabul edilir`() {
        assertEquals("Tarih bugünden ileri olamaz", dogrula(tarih = bugun.plusDays(1)).hatalar.tarih)
        assertNull(dogrula(tarih = bugun).hatalar.tarih)
        assertNull(dogrula(tarih = bugun.minusYears(10)).hatalar.tarih)
    }

    @Test
    fun `asiri buyuk sayi engellenir`() {
        assertNotNull(dogrula(adet = "1000000001").hatalar.adet)
        assertNotNull(dogrula(fiyat = "99999999999999").hatalar.fiyat)
        assertNotNull(dogrula(komisyon = "99999999999999").hatalar.komisyon)
    }

    @Test
    fun `negatif komisyon engellenir`() {
        assertNotNull(dogrula(komisyon = "-1").hatalar.komisyon)
    }

    @Test
    fun `nakitte yalnizca tutar istenir, fiyat 1 kabul edilir`() {
        val sonuc = dogrula(fiyat = "", adet = "5.000", nakit = true)
        assertTrue(sonuc.hatalar.gecerli)
        assertBd("1", sonuc.degerler!!.fiyat)
        assertBd("5000", sonuc.degerler!!.adet)
    }

    @Test
    fun `nakitte adet ust siniri daha yuksek`() {
        assertTrue(dogrula(fiyat = "", adet = "5000000000", nakit = true).hatalar.gecerli)
        assertFalse(dogrula(adet = "5000000000").hatalar.gecerli)
    }

    @Test
    fun `toplam maliyet fiyat carpi adet artı komisyon`() {
        assertBd("2855", purchaseTotal(bd("285.5"), bd("10")))
        assertBd("2860", purchaseTotal(bd("285.5"), bd("10"), bd("5")))
    }
}

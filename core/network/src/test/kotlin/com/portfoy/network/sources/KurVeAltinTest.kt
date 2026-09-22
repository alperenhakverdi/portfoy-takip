package com.portfoy.network.sources

import com.portfoy.model.AssetRef
import com.portfoy.model.Candle
import com.portfoy.model.Category
import com.portfoy.model.SourceId
import com.portfoy.network.BeklenmeyenYanitException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KurVeAltinTest {

    private val altin = AssetRef("XAUGR", Category.EMTIA)
    private val gumus = AssetRef("XAGGR", Category.EMTIA)

    // --- Truncgil ---

    @Test
    fun `truncgil gram altin gumus ve dolar tek cagrida, alis satis ortalamasi`() = runTest {
        val http = SahteHttp.sabit(SahteHttp.fixture("truncgil.json"))
        val sonuc = TruncgilSource(http).getQuotes(listOf(altin, gumus, AssetRef.USDTRY)).getOrThrow()

        assertEquals(BigDecimal("6817.3050"), sonuc[0].price) // (6816.89 + 6817.72) / 2
        assertEquals(BigDecimal("103.6350"), sonuc[1].price)
        assertEquals(BigDecimal("48.8176"), sonuc[2].price) // (48.7206 + 48.9146) / 2
        assertTrue(sonuc.all { it.currency == "TRY" && it.source == SourceId.TRUNCGIL })
        assertEquals(Instant.parse("2026-09-21T21:56:02Z"), sonuc[0].timestamp) // 00:56:02 TSİ
        assertEquals("https://finans.truncgil.com/v4/today.json", http.istekler.single().url)
        // "Change" alanı günlük değişim yüzdesini doğrudan verir, hesaplamaya gerek yok.
        assertEquals(BigDecimal("0.05"), sonuc[0].changePercent)
    }

    @Test
    fun `truncgil ons alani sifir donuyor, kullanilmaz`() = runTest {
        val sonuc = TruncgilSource(SahteHttp.sabit(SahteHttp.fixture("truncgil.json"))).getQuotes(listOf(AssetRef("ONS", Category.EMTIA)))
        assertTrue(sonuc.exceptionOrNull() is UnsupportedOperationException)
    }

    @Test
    fun `truncgil fiyat sifir gelirse gecersiz sayilir`() = runTest {
        val govde = """{"Update_Date":"2026-09-22 00:56:02","GRA":{"Buying":0,"Selling":0}}"""
        assertTrue(TruncgilSource(SahteHttp.sabit(govde)).getQuotes(listOf(altin)).exceptionOrNull() is BeklenmeyenYanitException)
    }

    @Test
    fun `truncgil gecmis vermez`() = runTest {
        assertTrue(TruncgilSource(SahteHttp.sabit("")).getHistory(altin, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)).exceptionOrNull() is UnsupportedOperationException)
    }

    // --- TCMB günlük kur ---

    @Test
    fun `tcmb gunluk kur dosyasindan USD ortalamasi`() = runTest {
        val http = SahteHttp.sabit(SahteHttp.fixture("tcmb_today.xml"))
        val sonuc = TcmbGunlukSource(http).getQuotes(listOf(AssetRef.USDTRY)).getOrThrow().single()

        assertEquals(BigDecimal("48.7570"), sonuc.price) // (48.7131 + 48.8009) / 2
        assertEquals(SourceId.TCMB_DAILY, sonuc.source)
        assertEquals("https://www.tcmb.gov.tr/kurlar/today.xml", http.istekler.single().url)
    }

    @Test
    fun `tcmb dosyada USD yoksa basarisiz`() = runTest {
        val sonuc = TcmbGunlukSource(SahteHttp.sabit("<Tarih_Date></Tarih_Date>")).getQuotes(listOf(AssetRef.USDTRY))
        assertTrue(sonuc.exceptionOrNull() is BeklenmeyenYanitException)
    }

    @Test
    fun `tcmb yalnizca USD TRY verir`() = runTest {
        assertTrue(TcmbGunlukSource(SahteHttp.sabit("")).getQuotes(listOf(altin)).exceptionOrNull() is UnsupportedOperationException)
    }

    // --- EVDS ---

    @Test
    fun `evds gunluk kur gecmisi, anahtar basliktadir ve bos gunler atlanir`() = runTest {
        val http = SahteHttp.sabit(SahteHttp.fixture("evds_usd.json"))
        val mumlar = EvdsSource(http, "EVDS-ANAHTARI")
            .getHistory(AssetRef.USDTRY, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 21)).getOrThrow()

        assertEquals(LocalDate.of(2026, 9, 10), mumlar.first().date)
        assertEquals(BigDecimal("48.39050000"), mumlar.first().close)
        assertTrue(mumlar.size < 12) // hafta sonu ve tatil günleri null geliyor
        assertTrue(mumlar.all { it.date.dayOfWeek.value <= 5 })
        assertEquals(mumlar.map { it.date }.sorted(), mumlar.map { it.date })

        val istek = http.istekler.single()
        assertEquals("EVDS-ANAHTARI", istek.basliklar["key"])
        assertEquals(
            "https://evds3.tcmb.gov.tr/igmevdsms-dis/series=TP.DK.USD.A.YTL&startDate=10-09-2026&endDate=21-09-2026&type=json",
            istek.url, // parametreler '?' olmadan yola eklenir
        )
        assertTrue(!istek.url.contains("EVDS-ANAHTARI")) // anahtar adreste değil
    }

    @Test
    fun `evds hatali anahtarda duz metin doner ve hata olarak yorumlanir`() = runTest {
        val sonuc = EvdsSource(SahteHttp.sabit(SahteHttp.fixture("evds_hata.json")), "bozuk")
            .getHistory(AssetRef.USDTRY, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 21))
        val hata = sonuc.exceptionOrNull()
        assertTrue(hata is BeklenmeyenYanitException)
        assertTrue(hata!!.message!!.contains("EVDS"))
    }

    @Test
    fun `evds yalnizca USD TRY gecmisi verir, anlik fiyat vermez`() = runTest {
        val kaynak = EvdsSource(SahteHttp.sabit(""), "K")
        assertTrue(kaynak.getHistory(altin, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)).exceptionOrNull() is UnsupportedOperationException)
        assertTrue(kaynak.getQuotes(listOf(AssetRef.USDTRY)).exceptionOrNull() is UnsupportedOperationException)
    }

    // --- Ons → gram çevrimi ---

    private fun mum(gun: Int, deger: String) = Candle(LocalDate.of(2026, 9, gun), BigDecimal(deger))

    @Test
    fun `gram serisi ons carpi kur bolu 31,1035`() {
        val seri = gramSerisi(listOf(mum(21, "3110.35")), listOf(mum(21, "40")))
        assertEquals(0, BigDecimal("4000").compareTo(seri.single().close)) // 3110.35 × 40 ÷ 31.1035
    }

    @Test
    fun `kuru olmayan gun icin en son bilinen kur kullanilir`() {
        val ons = listOf(mum(14, "3110.35"), mum(15, "3110.35"), mum(16, "3110.35")) // Pzt, Sal, Çar
        val kur = listOf(mum(11, "30"), mum(14, "40")) // Cuma ve Pazartesi; Salı ve Çarşamba yok
        val seri = gramSerisi(ons, kur)

        assertEquals(3, seri.size)
        assertEquals(0, BigDecimal("4000").compareTo(seri[0].close)) // Pzt: aynı gün kuru (40)
        assertEquals(0, BigDecimal("4000").compareTo(seri[1].close)) // Sal: son bilinen 40
        assertEquals(0, BigDecimal("4000").compareTo(seri[2].close))
    }

    @Test
    fun `ilk kurdan once olan ons gunleri atlanir`() {
        val seri = gramSerisi(listOf(mum(10, "3000"), mum(15, "3000")), listOf(mum(12, "40")))
        assertEquals(listOf(LocalDate.of(2026, 9, 15)), seri.map { it.date })
    }

    @Test
    fun `gram serisi sirasiz girdiyi de siralar`() {
        val seri = gramSerisi(listOf(mum(16, "3110.35"), mum(14, "3110.35")), listOf(mum(15, "40"), mum(13, "35")))
        assertEquals(listOf(14, 16), seri.map { it.date.dayOfMonth })
        assertEquals(0, BigDecimal("3500").compareTo(seri[0].close)) // 14'ünde son kur 13'ündeki 35
        assertEquals(0, BigDecimal("4000").compareTo(seri[1].close)) // 16'sında 15'indeki 40
    }

    @Test
    fun `ons kaynagi yahoo vadelisi ve kurdan gram TL fiyati uretir`() = runTest {
        val http = SahteHttp { url, _ ->
            when {
                url.contains("GC%3DF") -> SahteHttp.fixture("yahoo_gold.json")
                url.contains("USDTRY%3DX") -> SahteHttp.fixture("yahoo_usdtry.json")
                else -> error("beklenmeyen istek: $url")
            }
        }
        val sonuc = OnsEmtiaSource(YahooSource(http)).getQuotes(listOf(altin)).getOrThrow().single()

        val ons = BigDecimal(SahteHttp.yahooFiyati(SahteHttp.fixture("yahoo_gold.json")))
        val kur = BigDecimal(SahteHttp.yahooFiyati(SahteHttp.fixture("yahoo_usdtry.json")))
        val beklenen = ons.multiply(kur).divide(GRAM_PER_ONS, java.math.MathContext.DECIMAL64)
        assertEquals(0, beklenen.compareTo(sonuc.price))
        assertEquals("TRY", sonuc.currency)
        assertEquals("XAUGR", sonuc.code)
    }

    @Test
    fun `ons kaynagi kaynagin bilmedigi emtiayi reddeder`() = runTest {
        val sonuc = OnsEmtiaSource(YahooSource(SahteHttp.sabit(""))).getQuotes(listOf(AssetRef("ZIYNET", Category.EMTIA)))
        assertNull(sonuc.getOrNull())
        assertTrue(sonuc.isFailure)
    }
}

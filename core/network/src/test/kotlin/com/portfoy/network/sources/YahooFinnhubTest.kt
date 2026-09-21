package com.portfoy.network.sources

import com.portfoy.model.AssetRef
import com.portfoy.model.Category
import com.portfoy.model.SourceId
import com.portfoy.network.BeklenmeyenYanitException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YahooFinnhubTest {

    private val thyao = AssetRef("THYAO", Category.BIST)
    private val aapl = AssetRef("AAPL", Category.ABD)

    // --- Yahoo ---

    @Test
    fun `yahoo BIST fiyati THYAO IS sembolu ve tarayici kimligiyle istenir`() = runTest {
        val http = SahteHttp.sabit(SahteHttp.fixture("yahoo_thyao_quote.json"))
        val sonuc = YahooSource(http).getQuotes(listOf(thyao)).getOrThrow().single()

        assertEquals(0, BigDecimal(SahteHttp.yahooFiyati(SahteHttp.fixture("yahoo_thyao_quote.json"))).compareTo(sonuc.price))
        assertEquals("TRY", sonuc.currency)
        assertEquals(SourceId.YAHOO, sonuc.source)
        assertTrue(sonuc.timestamp.isAfter(Instant.parse("2020-01-01T00:00:00Z")))
        val istek = http.istekler.single()
        assertTrue(istek.url, istek.url.contains("/v8/finance/chart/THYAO.IS?"))
        assertTrue(istek.basliklar["User-Agent"]!!.startsWith("Mozilla/5.0"))
    }

    @Test
    fun `yahoo USD TRY sembolu ve ABD nokta tire donusumu`() = runTest {
        val http = SahteHttp.sabit(SahteHttp.fixture("yahoo_usdtry.json"))
        YahooSource(http).getQuotes(listOf(AssetRef.USDTRY, AssetRef("BRK.B", Category.ABD)))

        assertTrue(http.istekler[0].url.contains("/chart/USDTRY%3DX?"))
        assertTrue(http.istekler[1].url.contains("/chart/BRK-B?"))
    }

    @Test
    fun `yahoo gecmis seri tarih sirali, bos kapanislar atlanir`() = runTest {
        val http = SahteHttp.sabit(SahteHttp.fixture("yahoo_aapl.json"))
        val mumlar = YahooSource(http).getHistory(aapl, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 21)).getOrThrow()

        assertTrue(mumlar.size in 15..25)
        assertEquals(mumlar.map { it.date }.sorted(), mumlar.map { it.date })
        assertEquals(mumlar.size, mumlar.map { it.date }.distinct().size)
        assertTrue(mumlar.all { it.close.signum() > 0 })
        val url = http.istekler.single().url
        assertTrue(url, url.contains("period1=") && url.contains("period2=") && url.contains("interval=1d"))
    }

    @Test
    fun `yahoo gecmis aralik bitis gununu kapsar`() = runTest {
        val http = SahteHttp.sabit(SahteHttp.fixture("yahoo_aapl.json"))
        YahooSource(http).getHistory(aapl, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2))
        val url = http.istekler.single().url
        // period2 = 3 Eylül 00:00 UTC (bitiş günü dahil olsun diye ertesi gün)
        assertTrue(url, url.contains("period2=${LocalDate.of(2026, 9, 3).atStartOfDay(java.time.ZoneOffset.UTC).toEpochSecond()}"))
    }

    @Test
    fun `yahoo fon ve nakit gibi desteklemedigi varliklar hata verir`() = runTest {
        val sonuc = YahooSource(SahteHttp.sabit("{}")).getQuotes(listOf(AssetRef("AAL", Category.FON)))
        assertTrue(sonuc.exceptionOrNull() is UnsupportedOperationException)
    }

    @Test
    fun `yahoo hata govdesi ve bozuk yanit basarisiz sonuc doner`() = runTest {
        val hata = """{"chart":{"result":null,"error":{"code":"Not Found","description":"No data found"}}}"""
        assertTrue(YahooSource(SahteHttp.sabit(hata)).getQuotes(listOf(thyao)).exceptionOrNull() is BeklenmeyenYanitException)
        val bozuk = YahooSource(SahteHttp.sabit("<html>engellendi</html>")).getQuotes(listOf(thyao)).exceptionOrNull()
        assertTrue("$bozuk", bozuk is BeklenmeyenYanitException)
    }

    @Test
    fun `yahoo her sembol icin ayri istek, maliyet varlik sayisi kadar`() {
        assertEquals(3, YahooSource(SahteHttp.sabit("")).requestCost(listOf(thyao, aapl, thyao)))
    }

    // --- Finnhub ---

    @Test
    fun `finnhub anlik fiyat ve zaman damgasi`() = runTest {
        val http = SahteHttp.sabit(SahteHttp.fixture("finnhub_quote.json"))
        val sonuc = FinnhubSource(http, apiKey = "ANAHTAR").getQuotes(listOf(aapl)).getOrThrow().single()

        assertEquals(BigDecimal("338.98"), sonuc.price)
        assertEquals("USD", sonuc.currency)
        assertEquals(Instant.ofEpochSecond(1790020800), sonuc.timestamp)
        assertEquals("https://finnhub.io/api/v1/quote?symbol=AAPL&token=ANAHTAR", http.istekler.single().url)
    }

    @Test
    fun `finnhub bilinmeyen sembolde sifir fiyat hata sayilir`() = runTest {
        val sonuc = FinnhubSource(SahteHttp.sabit(SahteHttp.fixture("finnhub_quote_bilinmeyen.json")), "K")
            .getQuotes(listOf(AssetRef("ZZZZNOPE", Category.ABD)))
        assertTrue(sonuc.exceptionOrNull() is BeklenmeyenYanitException)
    }

    @Test
    fun `finnhub toplu istegi boler ve gercek istek sayisini bildirir`() = runTest {
        val http = SahteHttp.sabit(SahteHttp.fixture("finnhub_quote.json"))
        val kaynak = FinnhubSource(http, "K")
        val varliklar = listOf(aapl, AssetRef("NVDA", Category.ABD), AssetRef("MSFT", Category.ABD))

        assertEquals(3, kaynak.requestCost(varliklar))
        assertEquals(3, kaynak.getQuotes(varliklar).getOrThrow().size)
        assertEquals(3, http.istekler.size)
    }

    @Test
    fun `finnhub gecmis veri ucretsiz anahtarda desteklenmez`() = runTest {
        val sonuc = FinnhubSource(SahteHttp.sabit(""), "K").getHistory(aapl, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1))
        assertTrue(sonuc.exceptionOrNull() is UnsupportedOperationException)
    }

    @Test
    fun `finnhub sembol listesi hisse ETF ve ADR alir, tercihli ve bozuk kayitlari atar`() = runTest {
        val liste = """[
          {"currency":"USD","description":"APPLE INC","displaySymbol":"AAPL","figi":"BBG000B9XRY4","isin":"","mic":"XNAS","symbol":"AAPL","symbol2":"","type":"Common Stock"},
          {"currency":"USD","description":"VANGUARD S&P 500 ETF","displaySymbol":"VOO","figi":"X","isin":"","mic":"ARCX","symbol":"VOO","symbol2":"","type":"ETP"},
          {"currency":"USD","description":"BIR TERCIHLI HISSE","displaySymbol":"XYZ-P","figi":"X","isin":"","mic":"XNYS","symbol":"XYZ-P","symbol2":"","type":"Preferred Stock"},
          {"currency":"USD","description":"","displaySymbol":"NODESC","figi":"X","isin":"","mic":"XNYS","symbol":"NODESC","symbol2":"","type":"Common Stock"},
          {"currency":"USD","description":"APPLE INC","displaySymbol":"AAPL","figi":"Y","isin":"","mic":"XNAS","symbol":"AAPL","symbol2":"","type":"Common Stock"}
        ]"""
        val sonuc = FinnhubSource(SahteHttp.sabit(liste), "K").listSymbols().getOrThrow()

        assertEquals(listOf("AAPL", "VOO", "NODESC"), sonuc.map { it.code })
        assertEquals("APPLE INC", sonuc[0].name)
        assertEquals("NODESC", sonuc[2].name) // açıklaması olmayan, kodla adlandırılır
        assertTrue(sonuc.all { it.category == Category.ABD && it.currency == "USD" })
        assertFalse(sonuc.any { it.code.contains("-P") })
    }
}

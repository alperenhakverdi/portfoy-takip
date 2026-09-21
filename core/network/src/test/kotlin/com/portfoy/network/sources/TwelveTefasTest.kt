package com.portfoy.network.sources

import com.portfoy.model.AssetRef
import com.portfoy.model.Category
import com.portfoy.model.SourceId
import com.portfoy.network.BeklenmeyenYanitException
import com.portfoy.network.IstekAraligi
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TwelveTefasTest {

    private val aralik = IstekAraligi(Duration.ZERO)

    // --- Twelve Data ---

    @Test
    fun `twelve data ABD gecmis serisi tarih sirali gelir`() = runTest {
        val http = SahteHttp.sabit(SahteHttp.fixture("td_timeseries_aapl.json"))
        val mumlar = TwelveDataSource(http, "ANAHTAR", aralik = aralik)
            .getHistory(AssetRef("AAPL", Category.ABD), LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 21)).getOrThrow()

        assertEquals(6, mumlar.size)
        assertEquals(mumlar.map { it.date }.sorted(), mumlar.map { it.date })
        assertEquals(LocalDate.of(2026, 9, 21), mumlar.last().date)
        assertEquals(BigDecimal("338.89001"), mumlar.last().close)
        assertEquals(LocalDate.of(2026, 9, 14), mumlar.first().date)
        assertEquals(BigDecimal("333.079987"), mumlar.first().close)
        val url = http.istekler.single().url
        assertTrue(url, url.contains("symbol=AAPL") && url.contains("start_date=2026-09-14") &&
            url.contains("end_date=2026-09-21") && url.contains("apikey=ANAHTAR") && url.contains("interval=1day"))
    }

    @Test
    fun `twelve data spot altin ons gecmisi ham sembolle sorulur`() = runTest {
        val http = SahteHttp.sabit(SahteHttp.fixture("td_timeseries_xau.json"))
        val mumlar = TwelveDataSource(http, "K", aralik = aralik)
            .getHistory(AssetRef("XAU/USD", null), LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 22)).getOrThrow()

        assertEquals(6, mumlar.size)
        assertTrue(mumlar.all { it.close > BigDecimal(1000) }) // ons fiyatı
        assertTrue(http.istekler.single().url.contains("symbol=XAU/USD"))
    }

    @Test
    fun `twelve data anlik fiyat`() = runTest {
        val sonuc = TwelveDataSource(SahteHttp.sabit(SahteHttp.fixture("td_price_aapl.json")), "K", aralik = aralik)
            .getQuotes(listOf(AssetRef("AAPL", Category.ABD))).getOrThrow().single()
        assertEquals(BigDecimal("338.89001"), sonuc.price)
        assertEquals(SourceId.TWELVE_DATA, sonuc.source)
    }

    @Test
    fun `twelve data HTTP 200 donse de status error basarisizlik sayilir`() = runTest {
        val govde = """{"code":429,"message":"You have run out of API credits for the current minute.","status":"error"}"""
        val sonuc = TwelveDataSource(SahteHttp.sabit(govde), "K", aralik = aralik)
            .getHistory(AssetRef("AAPL", Category.ABD), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2))
        val hata = sonuc.exceptionOrNull()
        assertTrue(hata is BeklenmeyenYanitException)
        assertTrue(hata!!.message!!.contains("429"))
    }

    @Test
    fun `twelve data BIST ve fon varliklarini desteklemez`() = runTest {
        val sonuc = TwelveDataSource(SahteHttp.sabit("{}"), "K", aralik = aralik)
            .getHistory(AssetRef("THYAO", Category.BIST), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2))
        assertTrue(sonuc.exceptionOrNull() is UnsupportedOperationException)
    }

    // --- TEFAS ---

    private fun tefas(
        bugun: LocalDate = LocalDate.of(2026, 9, 18),
        yanit: (String?) -> String = { SahteHttp.fixture("tefas_gun.json") },
    ): Pair<TefasSource, SahteHttp> {
        val http = SahteHttp { _, govde -> yanit(govde) }
        return TefasSource(http, bugun = { bugun }, aralik = aralik) to http
    }

    @Test
    fun `tefas guncel fiyat tum fonlar listesinden secilir, gun sonu 21_00 zamaniyla`() = runTest {
        val (kaynak, http) = tefas()
        val sonuc = kaynak.getQuotes(listOf(AssetRef("AAL", Category.FON, "YAT"))).getOrThrow().single()

        assertEquals("AAL", sonuc.code)
        assertEquals(BigDecimal("3.58631"), sonuc.price)
        assertEquals("TRY", sonuc.currency)
        assertEquals(Instant.parse("2026-09-18T18:00:00Z"), sonuc.timestamp) // 21:00 TSİ
        val istek = http.istekler.single()
        assertEquals("https://www.tefas.gov.tr/api/funds/fonGnlBlgSiraliGetir", istek.url)
        assertEquals("https://www.tefas.gov.tr", istek.basliklar["Origin"])
        assertEquals("https://www.tefas.gov.tr/tr/fon-verileri", istek.basliklar["Referer"])
        assertTrue(istek.govde!!.contains("\"fonKodu\":null") && istek.govde!!.contains("\"basTarih\":\"20260918\"") &&
            istek.govde!!.contains("\"fonTipi\":\"YAT\""))
    }

    @Test
    fun `tefas listede olmayan fon atlanir`() = runTest {
        val (kaynak, _) = tefas()
        val sonuc = kaynak.getQuotes(listOf(AssetRef("AAL", Category.FON, "YAT"), AssetRef("YOKFON", Category.FON, "YAT"))).getOrThrow()
        assertEquals(listOf("AAL"), sonuc.map { it.code })
    }

    @Test
    fun `tefas hafta sonu cuma gununun fiyatini alir`() = runTest {
        val (kaynak, http) = tefas(bugun = LocalDate.of(2026, 9, 19)) // Cumartesi
        kaynak.getQuotes(listOf(AssetRef("AAL", Category.FON, "YAT"))).getOrThrow()

        assertEquals(1, http.istekler.size)
        assertTrue(http.istekler.single().govde!!.contains("\"basTarih\":\"20260918\""))
    }

    @Test
    fun `tefas veri donmeyen tatil gununde bir onceki is gunune bakar`() = runTest {
        val bos = """{"errorCode":null,"errorMessage":null,"resultList":[],"toplamSayi":null}"""
        val (kaynak, http) = tefas(bugun = LocalDate.of(2026, 9, 18)) { govde ->
            if (govde!!.contains("\"basTarih\":\"20260918\"")) bos else SahteHttp.fixture("tefas_gun.json")
        }
        kaynak.getQuotes(listOf(AssetRef("AAL", Category.FON, "YAT"))).getOrThrow()

        assertEquals(2, http.istekler.size)
        assertTrue(http.istekler[1].govde!!.contains("\"basTarih\":\"20260917\""))
    }

    @Test
    fun `tefas farkli fon turleri ayri istektir`() = runTest {
        val (kaynak, http) = tefas()
        val varliklar = listOf(AssetRef("AAL", Category.FON, "YAT"), AssetRef("ABC", Category.FON, "BYF"))

        assertEquals(2, kaynak.requestCost(varliklar))
        kaynak.getQuotes(varliklar)
        assertEquals(2, http.istekler.size)
        assertTrue(http.istekler[1].govde!!.contains("\"fonTipi\":\"BYF\""))
    }

    @Test
    fun `tefas tek fon gecmisi fon kodu ve 21 gunluk aralikla tek istek`() = runTest {
        val (kaynak, http) = tefas { SahteHttp.fixture("tefas_aal_gecmis.json") }
        val mumlar = kaynak.getHistory(AssetRef("AAL", Category.FON, "YAT"), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 21)).getOrThrow()

        assertEquals(1, http.istekler.size)
        val govde = http.istekler.single().govde!!
        assertTrue(govde.contains("\"fonKodu\":\"AAL\"") && govde.contains("\"basTarih\":\"20260901\"") && govde.contains("\"bitTarih\":\"20260921\""))
        assertTrue(mumlar.size in 10..25)
        assertEquals(mumlar.map { it.date }.sorted(), mumlar.map { it.date })
        assertTrue(mumlar.all { it.close.signum() > 0 })
    }

    @Test
    fun `tefas uzun gecmis 28 gunluk parcalara bolunur`() = runTest {
        val (kaynak, http) = tefas { SahteHttp.fixture("tefas_aal_gecmis.json") }
        kaynak.getHistory(AssetRef("AAL", Category.FON, "YAT"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1))

        val aralıklar = http.istekler.map { i ->
            Regex("\"basTarih\":\"(\\d+)\".*\"bitTarih\":\"(\\d+)\"").find(i.govde!!)!!.groupValues.let { it[1] to it[2] }
        }
        assertEquals(
            listOf("20260101" to "20260128", "20260129" to "20260225", "20260226" to "20260301"),
            aralıklar,
        )
    }

    @Test
    fun `tefas bir ay siniri hatasi tekrar denenmeden basarisiz olur`() = runTest {
        val govde = """{"errorCode":null,"errorMessage":"Geçersiz veri: Tarih aralığı 1 ayı aşamaz","resultList":null}"""
        val (kaynak, _) = tefas { govde }
        val sonuc = kaynak.getHistory(AssetRef("AAL", Category.FON, "YAT"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10))
        assertTrue(sonuc.exceptionOrNull() is BeklenmeyenYanitException)
    }

    @Test
    fun `tefas sembol listesi yatirim ve borsa fonlarini kodu adi ve turuyle verir`() = runTest {
        val (kaynak, http) = tefas()
        val liste = kaynak.listSymbols().getOrThrow()

        assertEquals(2, http.istekler.size) // YAT ve BYF
        val aal = liste.first { it.code == "AAL" }
        assertEquals("ATA PORTFÖY PARA PİYASASI (TL) FONU", aal.name)
        assertEquals(Category.FON, aal.category)
        assertEquals("YAT", aal.fundKind)
        assertEquals(liste.map { it.code }.distinct(), liste.map { it.code }) // iki türden gelen aynı kod tekrarlanmaz
    }

    @Test
    fun `tefas gunlerce veri vermezse basarisiz olur`() = runTest {
        val bos = """{"errorCode":null,"errorMessage":null,"resultList":[]}"""
        val (kaynak, _) = tefas { bos }
        assertTrue(kaynak.getQuotes(listOf(AssetRef("AAL", Category.FON, "YAT"))).exceptionOrNull() is BeklenmeyenYanitException)
    }
}

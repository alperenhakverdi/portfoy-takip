package com.portfoy.network.sources

import com.portfoy.model.AssetRef
import com.portfoy.model.Category
import com.portfoy.network.GecmisAnahtari
import com.portfoy.network.GecmisKaynagi
import com.portfoy.network.GunlukSayac
import com.portfoy.network.HistoryRouter
import com.portfoy.network.RouteKey
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Döviz kategorisi: portföydeki USD/EUR, çevrim kuruyla aynı kaynaklardan beslenir. Kaynaklar para birimine göre
 * parametriktir, yani yeni bir kur eklemek için kod değil yalnız varlık kaydı gerekir.
 */
class DovizTest {

    private val dolar = AssetRef("USDTRY", Category.DOVIZ)
    private val euro = AssetRef("EURTRY", Category.DOVIZ)

    @Test
    fun `kur varligi para birimine cozulur, digerleri cozulmez`() {
        assertEquals("USD", AssetRef.USDTRY.fxCurrency)
        assertEquals("EUR", AssetRef.EURTRY.fxCurrency)
        assertEquals("USD", dolar.fxCurrency)
        assertEquals("EUR", euro.fxCurrency)
        assertNull(AssetRef("GC=F", null).fxCurrency) // ham sembol, kur değil
        assertNull(AssetRef("AAPL", Category.ABD).fxCurrency)
        assertNull(AssetRef("XAUGR", Category.EMTIA).fxCurrency)
    }

    @Test
    fun `doviz varligi kur rotasina gider`() {
        assertEquals(RouteKey.FX, RouteKey.of(dolar))
        assertEquals(RouteKey.FX, RouteKey.of(euro))
    }

    @Test
    fun `truncgil euroyu da ayni cagridan verir`() = runTest {
        val sonuc = TruncgilSource(SahteHttp.sabit(SahteHttp.fixture("truncgil.json")))
            .getQuotes(listOf(dolar, euro)).getOrThrow()

        assertEquals(BigDecimal("48.8176"), sonuc[0].price)
        assertEquals(BigDecimal("56.0058"), sonuc[1].price) // (55.9801 + 56.0315) / 2
    }

    @Test
    fun `tcmb gunluk dosyasindan euro okunur`() = runTest {
        val sonuc = TcmbGunlukSource(SahteHttp.sabit(SahteHttp.fixture("tcmb_today.xml")))
            .getQuotes(listOf(euro)).getOrThrow().single()

        assertEquals(BigDecimal("56.0790"), sonuc.price) // (56.0284 + 56.1296) / 2
    }

    @Test
    fun `evds euro serisini kendi seri adiyla ister`() = runTest {
        val http = SahteHttp.sabit(SahteHttp.fixture("evds_eur.json"))
        val mumlar = EvdsSource(http, "anahtar")
            .getHistory(euro, LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 18)).getOrThrow()

        assertTrue(http.istekler.single().url.contains("series=TP.DK.EUR.A.YTL"))
        assertEquals(3, mumlar.size) // null gelen gün atlanır
        assertEquals(BigDecimal("56.02840000"), mumlar.first().close)
    }

    @Test
    fun `yahoo doviz sembolu kur ciftidir`() = runTest {
        val http = SahteHttp.sabit(SahteHttp.fixture("yahoo_usdtry.json"))
        YahooSource(http).getHistory(euro, LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 18))

        assertTrue(http.istekler.single().url.contains("EURTRY%3DX"))
    }

    @Test
    fun `doviz gecmisi kur zincirinden gelir`() = runTest {
        val evds = EvdsSource(SahteHttp.sabit(SahteHttp.fixture("evds_eur.json")), "anahtar")
        val router = HistoryRouter(
            mapOf(GecmisAnahtari.KUR to listOf(GecmisKaynagi(evds))),
            GunlukSayac(10, Clock.fixed(Instant.parse("2026-09-22T09:00:00Z"), ZoneId.of("UTC")), ZoneId.of("UTC")),
        )

        val mumlar = router.getHistory(euro, LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 18)).getOrThrow()
        assertEquals(3, mumlar.size)
    }
}

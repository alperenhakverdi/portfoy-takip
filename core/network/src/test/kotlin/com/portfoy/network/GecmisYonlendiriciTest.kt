package com.portfoy.network

import com.portfoy.model.AssetRef
import com.portfoy.model.Candle
import com.portfoy.model.Category
import com.portfoy.model.Quote
import com.portfoy.model.SourceId
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GecmisYonlendiriciTest {

    private val saat: Clock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC)
    private val bas = LocalDate.of(2026, 9, 1)
    private val bit = LocalDate.of(2026, 9, 30)

    /** Geçmiş çağrılarını kaydeden, verilen sonucu döndüren sahte kaynak. */
    private class Kayitli(
        override val id: SourceId,
        private val sonuc: () -> Result<List<Candle>>,
    ) : PriceSource {
        val cagrilar = mutableListOf<AssetRef>()
        override suspend fun getQuotes(assets: List<AssetRef>): Result<List<Quote>> = Result.failure(UnsupportedOperationException())
        override suspend fun getHistory(asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>> {
            cagrilar += asset
            return sonuc()
        }
    }

    private fun mumlar(vararg deger: Pair<Int, String>) =
        deger.map { (g, k) -> Candle(LocalDate.of(2026, 9, g), BigDecimal(k)) }

    private fun kaynak(id: SourceId, vararg deger: Pair<Int, String>) = Kayitli(id) { Result.success(mumlar(*deger)) }

    private fun bozuk(id: SourceId) = Kayitli(id) { Result.failure(java.io.IOException("bozuk")) }

    private fun yonlendirici(zincirler: Map<GecmisAnahtari, List<GecmisKaynagi>>, cap: Int = 100) =
        HistoryRouter(zincirler, GunlukSayac(cap, saat, ZoneOffset.UTC))

    @Test
    fun `ABD birincil calisirsa yedege dokunulmaz`() = runTest {
        val twelve = kaynak(SourceId.TWELVE_DATA, 1 to "100")
        val yahoo = kaynak(SourceId.YAHOO, 1 to "999")
        val r = yonlendirici(mapOf(GecmisAnahtari.ABD to listOf(GecmisKaynagi(twelve), GecmisKaynagi(yahoo))))

        val sonuc = r.getHistory(AssetRef("AAPL", Category.ABD), bas, bit).getOrThrow()

        assertEquals(BigDecimal("100"), sonuc.single().close)
        assertTrue(yahoo.cagrilar.isEmpty())
    }

    @Test
    fun `birincil hatali ya da bos donerse siradaki kaynak denenir`() = runTest {
        val bosKaynak = Kayitli(SourceId.TWELVE_DATA) { Result.success(emptyList()) }
        val yahoo = kaynak(SourceId.YAHOO, 1 to "50")
        val r = yonlendirici(mapOf(GecmisAnahtari.ABD to listOf(GecmisKaynagi(bosKaynak), GecmisKaynagi(yahoo))))
        assertEquals(BigDecimal("50"), r.getHistory(AssetRef("AAPL", Category.ABD), bas, bit).getOrThrow().single().close)

        val r2 = yonlendirici(mapOf(GecmisAnahtari.ABD to listOf(GecmisKaynagi(bozuk(SourceId.TWELVE_DATA)), GecmisKaynagi(yahoo))))
        assertEquals(BigDecimal("50"), r2.getHistory(AssetRef("AAPL", Category.ABD), bas, bit).getOrThrow().single().close)
    }

    @Test
    fun `tum zincir basarisizsa hata doner`() = runTest {
        val r = yonlendirici(mapOf(GecmisAnahtari.BIST to listOf(GecmisKaynagi(bozuk(SourceId.YAHOO)))))
        val hata = r.getHistory(AssetRef("THYAO", Category.BIST), bas, bit).exceptionOrNull()
        assertTrue(hata is AllSourcesFailedException)
    }

    @Test
    fun `zincirdeki kaynaga kendi sembolu sorulur`() = runTest {
        val yahoo = kaynak(SourceId.YAHOO, 1 to "4400")
        val r = yonlendirici(mapOf(GecmisAnahtari.ONS_ALTIN to listOf(GecmisKaynagi(yahoo, AssetRef("GC=F", null))),
            GecmisAnahtari.KUR to listOf(GecmisKaynagi(kaynak(SourceId.TCMB_EVDS, 1 to "40")))))

        r.getHistory(AssetRef("XAUGR", Category.EMTIA), bas, bit)

        assertEquals(listOf(AssetRef("GC=F", null)), yahoo.cagrilar)
    }

    @Test
    fun `gram altin ons ve kur serisinden turetilir`() = runTest {
        val ons = kaynak(SourceId.TWELVE_DATA, 1 to "3110.35", 2 to "6220.70")
        val kur = kaynak(SourceId.TCMB_EVDS, 1 to "40", 2 to "50")
        val r = yonlendirici(
            mapOf(
                GecmisAnahtari.ONS_ALTIN to listOf(GecmisKaynagi(ons, AssetRef("XAU/USD", null))),
                GecmisAnahtari.KUR to listOf(GecmisKaynagi(kur)),
            ),
        )

        val sonuc = r.getHistory(AssetRef("XAUGR", Category.EMTIA), bas, bit).getOrThrow()

        assertEquals(2, sonuc.size)
        assertEquals(0, BigDecimal("4000").compareTo(sonuc[0].close)) // 3110,35 × 40 ÷ 31,1035
        assertEquals(0, BigDecimal("10000").compareTo(sonuc[1].close)) // 6220,70 × 50 ÷ 31,1035
        assertEquals(AssetRef.USDTRY, kur.cagrilar.single())
    }

    @Test
    fun `gumus icin gumus ons zinciri kullanilir`() = runTest {
        val altinOns = kaynak(SourceId.TWELVE_DATA, 1 to "1")
        val gumusOns = kaynak(SourceId.YAHOO, 1 to "31.1035")
        val r = yonlendirici(
            mapOf(
                GecmisAnahtari.ONS_ALTIN to listOf(GecmisKaynagi(altinOns)),
                GecmisAnahtari.ONS_GUMUS to listOf(GecmisKaynagi(gumusOns, AssetRef("SI=F", null))),
                GecmisAnahtari.KUR to listOf(GecmisKaynagi(kaynak(SourceId.TCMB_EVDS, 1 to "40"))),
            ),
        )

        val sonuc = r.getHistory(AssetRef("XAGGR", Category.EMTIA), bas, bit).getOrThrow()

        assertEquals(0, BigDecimal("40").compareTo(sonuc.single().close))
        assertTrue(altinOns.cagrilar.isEmpty())
    }

    @Test
    fun `kur serisi alinamazsa emtia gecmisi de basarisiz olur`() = runTest {
        val r = yonlendirici(
            mapOf(
                GecmisAnahtari.ONS_ALTIN to listOf(GecmisKaynagi(kaynak(SourceId.TWELVE_DATA, 1 to "3110"))),
                GecmisAnahtari.KUR to listOf(GecmisKaynagi(bozuk(SourceId.TCMB_EVDS))),
            ),
        )
        assertTrue(r.getHistory(AssetRef("XAUGR", Category.EMTIA), bas, bit).isFailure)
    }

    @Test
    fun `gunluk butce dolunca istek atilmaz ve butce asimi doner`() = runTest {
        val yahoo = kaynak(SourceId.YAHOO, 1 to "1")
        val r = yonlendirici(mapOf(GecmisAnahtari.BIST to listOf(GecmisKaynagi(yahoo))), cap = 2)
        val thyao = AssetRef("THYAO", Category.BIST)

        assertTrue(r.getHistory(thyao, bas, bit).isSuccess)
        assertTrue(r.getHistory(thyao, bas, bit).isSuccess)
        val ucuncu = r.getHistory(thyao, bas, bit).exceptionOrNull()

        assertTrue(ucuncu is BudgetExceededException)
        assertEquals(2, yahoo.cagrilar.size)
    }

    @Test
    fun `sayac gun donunce sifirlanir`() {
        class HareketliSaat(var simdi: Instant) : Clock() {
            override fun getZone() = ZoneOffset.UTC
            override fun withZone(zone: java.time.ZoneId?) = this
            override fun instant() = simdi
        }
        val s = HareketliSaat(Instant.parse("2026-09-21T10:00:00Z"))
        val sayac = GunlukSayac(1, s, ZoneOffset.UTC)
        assertTrue(sayac.tuket())
        assertTrue(!sayac.tuket())
        s.simdi = Instant.parse("2026-09-22T00:01:00Z")
        assertTrue(sayac.tuket())
    }

    @Test
    fun `nakit icin gecmis seri yoktur`() = runTest {
        val r = yonlendirici(emptyMap())
        assertTrue(r.getHistory(AssetRef("TRY", Category.NAKIT), bas, bit).exceptionOrNull() is UnsupportedOperationException)
    }
}

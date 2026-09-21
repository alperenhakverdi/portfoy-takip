package com.portfoy.network

import com.portfoy.model.AssetRef
import com.portfoy.model.Category
import com.portfoy.model.SourceId
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceRouterTest {

    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC)
    private val abd = listOf(AssetRef("AAPL", Category.ABD), AssetRef("NVDA", Category.ABD))

    private fun router(
        primary: FakePriceSource,
        fallback: FakePriceSource? = null,
        caps: Map<SourceId, Int> = emptyMap(),
        health: SourceHealth = SourceHealth(),
    ) = SourceRouter(
        routes = mapOf(RouteKey.US to Route(primary, fallback)),
        budget = CallBudget(caps, clock, ZoneId.of("UTC")),
        health = health,
        clock = clock,
        retryDelayMillis = 0,
    )

    @Test
    fun `birincil calisirsa yedege dokunulmaz`() = runTest {
        val birincil = FakePriceSource(SourceId.FINNHUB, clock)
        val yedek = FakePriceSource(SourceId.TWELVE_DATA, clock)

        val sonuc = router(birincil, yedek).quotes(RouteKey.US, abd)

        val basari = sonuc as RouteOutcome.Success
        assertEquals(SourceId.FINNHUB, basari.source)
        assertFalse(basari.usedFallback)
        assertEquals(2, basari.quotes.size)
        assertEquals(0, yedek.calls)
    }

    @Test
    fun `birincil arka arkaya 3 kez basarisiz olursa yedege gecilir`() = runTest {
        val birincil = FakePriceSource(SourceId.FINNHUB, clock, failWhen = { true })
        val yedek = FakePriceSource(SourceId.TWELVE_DATA, clock)

        val sonuc = router(birincil, yedek).quotes(RouteKey.US, abd)

        val basari = sonuc as RouteOutcome.Success
        assertEquals(3, birincil.calls)
        assertEquals(SourceId.TWELVE_DATA, basari.source)
        assertTrue(basari.usedFallback)
    }

    @Test
    fun `ikinci denemede basarili olursa yedege gecilmez`() = runTest {
        val birincil = FakePriceSource(SourceId.FINNHUB, clock, failWhen = { it == 1 })
        val yedek = FakePriceSource(SourceId.TWELVE_DATA, clock)

        val sonuc = router(birincil, yedek).quotes(RouteKey.US, abd)

        assertFalse((sonuc as RouteOutcome.Success).usedFallback)
        assertEquals(2, birincil.calls)
        assertEquals(0, yedek.calls)
    }

    @Test
    fun `iki kaynak da basarisizsa sonuc basarisiz doner`() = runTest {
        val birincil = FakePriceSource(SourceId.FINNHUB, clock, failWhen = { true })
        val yedek = FakePriceSource(SourceId.TWELVE_DATA, clock, failWhen = { true })

        val sonuc = router(birincil, yedek).quotes(RouteKey.US, abd)

        val hata = (sonuc as RouteOutcome.Failed).error
        assertTrue(hata is AllSourcesFailedException)
    }

    @Test
    fun `yedek tanimli degilse birincil basarisizligi hata olur`() = runTest {
        val birincil = FakePriceSource(SourceId.FINNHUB, clock, failWhen = { true })
        assertTrue(router(birincil).quotes(RouteKey.US, abd) is RouteOutcome.Failed)
    }

    @Test
    fun `birincilin gunluk butcesi dolunca istek atilmadan yedege gecilir`() = runTest {
        val birincil = FakePriceSource(SourceId.FINNHUB, clock)
        val yedek = FakePriceSource(SourceId.TWELVE_DATA, clock)

        val sonuc = router(birincil, yedek, caps = mapOf(SourceId.FINNHUB to 0)).quotes(RouteKey.US, abd)

        assertEquals(0, birincil.calls)
        assertEquals(SourceId.TWELVE_DATA, (sonuc as RouteOutcome.Success).source)
    }

    @Test
    fun `butce doldugunda ve yedek yoksa hata degil butce asimi doner`() = runTest {
        val birincil = FakePriceSource(SourceId.FINNHUB, clock)
        val sonuc = router(birincil, caps = mapOf(SourceId.FINNHUB to 0)).quotes(RouteKey.US, abd)

        assertTrue((sonuc as RouteOutcome.Failed).error is BudgetExceededException)
        assertEquals(0, birincil.calls)
    }

    @Test
    fun `toplu istegi bolen kaynak gercek istek sayisini butceye yansitir`() = runTest {
        // Finnhub tek sembol alır: 2 varlık = 2 gerçek istek.
        val finnhub = FakePriceSource(SourceId.FINNHUB, clock, costPerRequest = { it.size })
        val butce = mapOf(SourceId.FINNHUB to 3)
        val r = router(finnhub, caps = butce)

        assertTrue(r.quotes(RouteKey.US, abd) is RouteOutcome.Success) // 2 harcandı, 1 kaldı
        assertTrue((r.quotes(RouteKey.US, abd) as RouteOutcome.Failed).error is BudgetExceededException)
    }

    @Test
    fun `butce her denemede duser, basarisiz denemeler de sayilir`() = runTest {
        val birincil = FakePriceSource(SourceId.FINNHUB, clock, failWhen = { true })
        val butce = CallBudget(mapOf(SourceId.FINNHUB to 10), clock, ZoneId.of("UTC"))
        val r = SourceRouter(
            mapOf(RouteKey.US to Route(birincil)), butce, SourceHealth(), clock, retryDelayMillis = 0,
        )
        r.quotes(RouteKey.US, abd)
        assertEquals(7, butce.remaining(SourceId.FINNHUB))
    }

    @Test
    fun `nakit icin rota yoktur`() {
        assertEquals(null, RouteKey.of(AssetRef("TRY", Category.NAKIT)))
        assertEquals(RouteKey.FX, RouteKey.of(AssetRef.USDTRY))
        assertEquals(RouteKey.FUND, RouteKey.of(AssetRef("AAL", Category.FON, "YAT")))
    }

    @Test
    fun `bos varlik listesi istek atmaz`() = runTest {
        val birincil = FakePriceSource(SourceId.FINNHUB, clock)
        val sonuc = router(birincil).quotes(RouteKey.US, emptyList())
        assertTrue((sonuc as RouteOutcome.Success).quotes.isEmpty())
        assertEquals(0, birincil.calls)
    }
}

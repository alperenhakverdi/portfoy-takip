package com.portfoy.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.portfoy.calc.normalizeForSearch
import com.portfoy.data.db.AssetEntity
import com.portfoy.data.db.FX_USDTRY_ID
import com.portfoy.data.db.PortfoyDatabase
import com.portfoy.model.Category
import com.portfoy.model.SourceId
import com.portfoy.model.UnitType
import com.portfoy.network.CallBudget
import com.portfoy.network.FakePriceSource
import com.portfoy.network.Route
import com.portfoy.network.RouteKey
import com.portfoy.network.SourceHealth
import com.portfoy.network.SourceRouter
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FiyatDeposuTest {

    private class HareketliSaat(var simdi: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = simdi
    }

    private lateinit var db: PortfoyDatabase
    private val saat = HareketliSaat(Instant.parse("2026-09-21T10:00:00Z"))

    @Before
    fun kur() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PortfoyDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun kapat() = db.close()

    private fun depo(
        abd: FakePriceSource,
        kur: FakePriceSource = FakePriceSource(SourceId.TCMB_HOURLY, saat),
        bist: FakePriceSource = FakePriceSource(SourceId.YAHOO, saat),
    ): PriceRepository {
        val router = SourceRouter(
            routes = mapOf(
                RouteKey.US to Route(abd),
                RouteKey.FX to Route(kur),
                RouteKey.BIST to Route(bist),
            ),
            budget = CallBudget(emptyMap(), saat, ZoneOffset.UTC),
            health = SourceHealth(),
            clock = saat,
            retryDelayMillis = 0,
        )
        return PriceRepository(router, db.priceQuoteDao(), saat)
    }

    private suspend fun ekle(code: String, category: Category): AssetEntity {
        val varlik = AssetEntity(
            code = code,
            name = code,
            category = category,
            currency = if (category == Category.ABD) "USD" else "TRY",
            unitType = if (category == Category.NAKIT) UnitType.TL else UnitType.ADET,
            searchText = normalizeForSearch(code),
        )
        return varlik.copy(id = db.assetDao().insert(varlik))
    }

    @Test
    fun `ABD fiyati guncel kurla TL ye cevrilir`() = runBlocking {
        val aapl = ekle("AAPL", Category.ABD)
        val abd = FakePriceSource(SourceId.FINNHUB, saat)

        val rapor = depo(abd).refresh(listOf(aapl))

        val kayit = db.priceQuoteDao().latestFor(aapl.id)!!
        assertEquals("USD", kayit.currency)
        assertEquals(0, (kayit.price * BigDecimal("48.80")).compareTo(kayit.priceTl)) // kur 48,80
        assertEquals(1, rapor.updated)
        assertFalse(rapor.hasProblems)
        assertEquals(BigDecimal("48.80"), db.priceQuoteDao().latestFor(FX_USDTRY_ID)!!.priceTl)
    }

    @Test
    fun `TL fiyat kurla carpilmaz`() = runBlocking {
        val thyao = ekle("THYAO", Category.BIST)
        depo(FakePriceSource(SourceId.FINNHUB, saat)).refresh(listOf(thyao))

        val kayit = db.priceQuoteDao().latestFor(thyao.id)!!
        assertEquals(kayit.price, kayit.priceTl)
    }

    @Test
    fun `15 dakika icinde ikinci istek atilmaz`() = runBlocking {
        val thyao = ekle("THYAO", Category.BIST)
        val bist = FakePriceSource(SourceId.YAHOO, saat)
        val depo = depo(FakePriceSource(SourceId.FINNHUB, saat), bist = bist)

        depo.refresh(listOf(thyao))
        saat.simdi = saat.simdi.plus(Duration.ofMinutes(10))
        val ikinci = depo.refresh(listOf(thyao))

        assertEquals(1, bist.calls)
        assertEquals(1, ikinci.skippedFresh)
        assertEquals(0, ikinci.updated)
    }

    @Test
    fun `15 dakika gecince yeniden cekilir`() = runBlocking {
        val thyao = ekle("THYAO", Category.BIST)
        val bist = FakePriceSource(SourceId.YAHOO, saat)
        val depo = depo(FakePriceSource(SourceId.FINNHUB, saat), bist = bist)

        depo.refresh(listOf(thyao))
        saat.simdi = saat.simdi.plus(Duration.ofMinutes(16))
        depo.refresh(listOf(thyao))

        assertEquals(2, bist.calls)
    }

    @Test
    fun `elle yenilemede sure bir dakikadir`() = runBlocking {
        val thyao = ekle("THYAO", Category.BIST)
        val bist = FakePriceSource(SourceId.YAHOO, saat)
        val depo = depo(FakePriceSource(SourceId.FINNHUB, saat), bist = bist)

        depo.refresh(listOf(thyao))
        saat.simdi = saat.simdi.plus(Duration.ofMinutes(2))
        depo.refresh(listOf(thyao), PriceRepository.MANUAL_MIN_AGE)

        assertEquals(2, bist.calls)
    }

    @Test
    fun `nakit ve elle fiyat girilen varliklar tazelenmez`() = runBlocking {
        val nakit = ekle("TRY", Category.NAKIT)
        val elle = ekle("YENIHISSE", Category.BIST)
        val bist = FakePriceSource(SourceId.YAHOO, saat)
        val depo = depo(FakePriceSource(SourceId.FINNHUB, saat), bist = bist)
        depo.setManualPrice(elle.id, BigDecimal("12.50"))
        saat.simdi = saat.simdi.plus(Duration.ofHours(3)) // süre dolsa bile

        val rapor = depo.refresh(listOf(nakit, elle))

        assertEquals(0, bist.calls)
        assertEquals(0, rapor.updated)
        assertEquals(SourceId.MANUEL.name, db.priceQuoteDao().latestFor(elle.id)!!.source)
        assertNull(db.priceQuoteDao().latestFor(nakit.id))
    }

    @Test
    fun `kaynak basarisiz olursa son bilinen fiyat korunur ve rapor bunu bildirir`() = runBlocking {
        val thyao = ekle("THYAO", Category.BIST)
        val bist = FakePriceSource(SourceId.YAHOO, saat, failWhen = { it > 1 })
        val depo = depo(FakePriceSource(SourceId.FINNHUB, saat), bist = bist)

        depo.refresh(listOf(thyao))
        val ilkFiyat = db.priceQuoteDao().latestFor(thyao.id)!!.priceTl
        saat.simdi = saat.simdi.plus(Duration.ofMinutes(20))
        val rapor = depo.refresh(listOf(thyao))

        assertEquals(setOf(RouteKey.BIST), rapor.failedGroups)
        assertTrue(rapor.hasProblems)
        assertEquals(ilkFiyat, db.priceQuoteDao().latestFor(thyao.id)!!.priceTl)
    }

    @Test
    fun `kur hic yoksa ABD fiyati yazilmaz ve rapor bildirir`() = runBlocking {
        val aapl = ekle("AAPL", Category.ABD)
        val bozukKur = FakePriceSource(SourceId.TCMB_HOURLY, saat, failWhen = { true })

        val rapor = depo(FakePriceSource(SourceId.FINNHUB, saat), kur = bozukKur).refresh(listOf(aapl))

        assertTrue(rapor.fxMissing)
        assertNull(db.priceQuoteDao().latestFor(aapl.id))
    }

    @Test
    fun `kur alinamazsa son bilinen kurla cevrilir`() = runBlocking {
        val aapl = ekle("AAPL", Category.ABD)
        val kur = FakePriceSource(SourceId.TCMB_HOURLY, saat, failWhen = { it > 1 })
        val depo = depo(FakePriceSource(SourceId.FINNHUB, saat), kur = kur)

        depo.refresh(listOf(aapl)) // ilk tur: kur 48,80
        saat.simdi = saat.simdi.plus(Duration.ofMinutes(20))
        val rapor = depo.refresh(listOf(aapl)) // kur çekilemez, eski kurla devam

        assertEquals(1, rapor.updated)
        assertTrue(RouteKey.FX in rapor.failedGroups)
        val kayit = db.priceQuoteDao().latestFor(aapl.id)!!
        assertEquals(0, (kayit.price * BigDecimal("48.80")).compareTo(kayit.priceTl))
    }

    @Test
    fun `elle girilen fiyatlar seri olusturur, en yenisi gecerlidir`() = runBlocking {
        val elle = ekle("YENIHISSE", Category.BIST)
        val depo = depo(FakePriceSource(SourceId.FINNHUB, saat))

        depo.setManualPrice(elle.id, BigDecimal("10"))
        saat.simdi = saat.simdi.plus(Duration.ofDays(1))
        depo.setManualPrice(elle.id, BigDecimal("11"))

        assertEquals(BigDecimal("11"), db.priceQuoteDao().latestFor(elle.id)!!.priceTl)
    }
}

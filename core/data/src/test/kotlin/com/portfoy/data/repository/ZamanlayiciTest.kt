package com.portfoy.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.portfoy.calc.normalizeForSearch
import com.portfoy.data.db.AssetEntity
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
import com.portfoy.network.market.RefreshGroup
import com.portfoy.network.market.RefreshSchedule
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.flowOf
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
class ZamanlayiciTest {

    private class HareketliSaat(var simdi: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = simdi
    }

    private lateinit var db: PortfoyDatabase
    private val saat = HareketliSaat(Instant.parse("2026-09-21T07:10:00Z")) // Pazartesi 10:10 TSİ

    private val yahoo = FakePriceSource(SourceId.YAHOO, saat)
    private val tefas = FakePriceSource(SourceId.TEFAS, saat)
    private val truncgil = FakePriceSource(SourceId.TRUNCGIL, saat)
    private val kur = FakePriceSource(SourceId.TRUNCGIL, saat)
    private val finnhub = FakePriceSource(SourceId.FINNHUB, saat)

    @Before
    fun kur() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PortfoyDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun kapat() = db.close()

    private suspend fun varlik(kod: String, kategori: Category, tur: String? = null): AssetEntity {
        val v = AssetEntity(
            code = kod, name = kod, category = kategori, currency = if (kategori == Category.ABD) "USD" else "TRY",
            unitType = UnitType.ADET, fundKind = tur, searchText = normalizeForSearch(kod),
        )
        return v.copy(id = db.assetDao().insert(v))
    }

    private fun zamanlayici(vararg v: AssetEntity, son: SonCalismaDeposu = BellekSonCalisma()): Pair<TazelemeZamanlayici, SonCalismaDeposu> {
        val router = SourceRouter(
            routes = mapOf(
                RouteKey.US to Route(finnhub), RouteKey.BIST to Route(yahoo), RouteKey.FUND to Route(tefas),
                RouteKey.COMMODITY to Route(truncgil), RouteKey.FX to Route(kur),
            ),
            budget = CallBudget(emptyMap(), saat, ZoneOffset.UTC), health = SourceHealth(), clock = saat, retryDelayMillis = 0,
        )
        val fiyat = PriceRepository(router, db.priceQuoteDao(), saat)
        return TazelemeZamanlayici(fiyat, flowOf(v.toList()), RefreshSchedule(), son, saat) to son
    }

    @Test
    fun `buyuk portfoyde gun ici turlar kapanir, kucukte acik kalir`() {
        // BIST günlük tavan 300: her varlık açılış + kapanışta 2 çağrı → 150 varlıkta gün içine bütçe kalmaz.
        assertTrue(TazelemeZamanlayici.gunIciKapali(Category.BIST, 150))
        assertFalse(TazelemeZamanlayici.gunIciKapali(Category.BIST, 10))
        // ABD tavanı 500.
        assertTrue(TazelemeZamanlayici.gunIciKapali(Category.ABD, 250))
        assertFalse(TazelemeZamanlayici.gunIciKapali(Category.ABD, 20))
    }

    @Test
    fun `piyasasi olmayan ya da bos kategoride gun ici kapali bilgisi cikmaz`() {
        assertFalse(TazelemeZamanlayici.gunIciKapali(Category.FON, 1000))
        assertFalse(TazelemeZamanlayici.gunIciKapali(Category.EMTIA, 1000))
        assertFalse(TazelemeZamanlayici.gunIciKapali(Category.BIST, 0))
    }

    @Test
    fun `BIST acilis turu seans acildiktan sonra calisir`() = runBlocking {
        val thyao = varlik("THYAO", Category.BIST)
        val (z, _) = zamanlayici(thyao)

        val calisan = z.calistir() // 10:10 TSİ: 10:02 açılış turunun vadesi geldi

        assertTrue(RefreshGroup.BIST in calisan)
        assertEquals(1, yahoo.calls)
    }

    @Test
    fun `ayni tur ikinci kez calismaz`() = runBlocking {
        val (z, _) = zamanlayici(varlik("THYAO", Category.BIST))
        z.calistir()
        val ikinci = z.calistir()

        assertTrue(RefreshGroup.BIST !in ikinci)
        assertEquals(1, yahoo.calls)
    }

    @Test
    fun `sonraki gun ici tur 30 dakika sonra calisir`() = runBlocking {
        val (z, _) = zamanlayici(varlik("THYAO", Category.BIST))
        z.calistir() // 10:10
        saat.simdi = Instant.parse("2026-09-21T07:40:00Z") // 10:40 TSİ, 10:32 turu geldi
        val calisan = z.calistir()

        assertTrue(RefreshGroup.BIST in calisan)
    }

    @Test
    fun `seans oncesi ve hafta sonu hicbir sey calismaz`() = runBlocking {
        val (z, _) = zamanlayici(varlik("THYAO", Category.BIST), varlik("AAL", Category.FON, "YAT"))

        saat.simdi = Instant.parse("2026-09-21T05:00:00Z") // 08:00 TSİ, BIST açılmadan
        assertTrue(RefreshGroup.BIST !in z.calistir())

        saat.simdi = Instant.parse("2026-09-19T12:00:00Z") // Cumartesi
        assertTrue(z.calistir().isEmpty())
        assertEquals(0, yahoo.calls)
    }

    @Test
    fun `kacirilan kapanis turu aksam telafi edilir`() = runBlocking {
        val (z, _) = zamanlayici(varlik("THYAO", Category.BIST))
        z.calistir() // sabah turu
        saat.simdi = Instant.parse("2026-09-21T20:00:00Z") // 23:00 TSİ, BIST kapanmış; son çalışma sabah
        val calisan = z.calistir()

        assertTrue(RefreshGroup.BIST in calisan) // 18:15 kapanış turu telafi
    }

    @Test
    fun `portfoyde olmayan kategori hic cagri yapmaz, kur ise her zaman calisir`() = runBlocking {
        val (z, _) = zamanlayici(varlik("THYAO", Category.BIST))
        saat.simdi = Instant.parse("2026-09-21T09:30:00Z") // 12:30 TSİ

        val calisan = z.calistir()

        assertTrue(RefreshGroup.US !in calisan && RefreshGroup.FUND !in calisan && RefreshGroup.GOLD !in calisan)
        assertTrue(RefreshGroup.FX in calisan)
        assertEquals(0, finnhub.calls + tefas.calls + truncgil.calls)
        assertEquals(1, kur.calls)
    }

    @Test
    fun `fon gunde bir kez 21_15 sonrasinda calisir`() = runBlocking {
        val (z, _) = zamanlayici(varlik("AAL", Category.FON, "YAT"))

        saat.simdi = Instant.parse("2026-09-21T17:00:00Z") // 20:00 TSİ: henüz yok
        assertTrue(RefreshGroup.FUND !in z.calistir())

        saat.simdi = Instant.parse("2026-09-21T18:30:00Z") // 21:30 TSİ
        assertTrue(RefreshGroup.FUND in z.calistir())
        assertTrue(RefreshGroup.FUND !in z.calistir()) // aynı gün tekrar yok
        assertEquals(1, tefas.calls)
    }

    @Test
    fun `altin ve gumus 30 dakikada bir tazelenir`() = runBlocking {
        val (z, _) = zamanlayici(varlik("XAUGR", Category.EMTIA))
        saat.simdi = Instant.parse("2026-09-21T09:05:00Z") // 12:05 TSİ
        assertTrue(RefreshGroup.GOLD in z.calistir())
        assertEquals(1, truncgil.calls)
    }

    @Test
    fun `gun ici plan portfoy buyudukce uzar ve sonunda kapanir`() = runBlocking {
        val (z, _) = zamanlayici()
        assertEquals(30, z.plan(RefreshGroup.BIST, 10)!!.intervalMinutes)
        assertNull(z.plan(RefreshGroup.BIST, 300)!!.intervalMinutes) // açılış+kapanış bütçeyi bitirir
        assertNull(z.plan(RefreshGroup.FUND, 10)) // piyasası olmayan grupta plan yok
    }

    @Test
    fun `gun ici kapaliysa yalnizca acilis ve kapanis turu calisir`() = runBlocking {
        // 300 BIST varlığı: gün içi kapanır. 10:40'ta (gün içi vadesi olurdu) tur çalışmamalı.
        val varliklar = (1..300).map { varlik("H$it", Category.BIST) }.toTypedArray()
        val (z, _) = zamanlayici(*varliklar)
        z.calistir() // 10:10: açılış turu
        val once = yahoo.calls

        saat.simdi = Instant.parse("2026-09-21T07:40:00Z") // 10:40 TSİ
        assertTrue(RefreshGroup.BIST !in z.calistir())
        assertEquals(once, yahoo.calls)
    }

    @Test
    fun `30dan fazla varlik tur basina en fazla 30 cagriyla parcalanir`() = runBlocking {
        val varliklar = (1..70).map { varlik("H$it", Category.BIST) }.toTypedArray()
        val (z, _) = zamanlayici(*varliklar)

        z.calistir() // açılış turu tüm varlıkları kapsar: 70 → 30 + 30 + 10

        assertEquals(3, yahoo.calls)
    }
}

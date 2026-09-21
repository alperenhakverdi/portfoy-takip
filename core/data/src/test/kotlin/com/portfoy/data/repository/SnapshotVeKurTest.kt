package com.portfoy.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.portfoy.calc.normalizeForSearch
import com.portfoy.data.db.AssetEntity
import com.portfoy.data.db.FX_USDTRY_ID
import com.portfoy.data.db.PortfoyDatabase
import com.portfoy.data.db.PriceHistoryEntity
import com.portfoy.data.db.PriceQuoteEntity
import com.portfoy.data.db.TransactionEntity
import com.portfoy.model.Category
import com.portfoy.model.SourceId
import com.portfoy.model.TransactionType
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
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SnapshotVeKurTest {

    private lateinit var db: PortfoyDatabase
    private val saat: Clock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC)

    @Before
    fun kur() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PortfoyDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun kapat() = db.close()

    private fun gun(d: Int) = LocalDate.of(2026, 9, d)

    private suspend fun thyao(): AssetEntity {
        val v = AssetEntity(
            code = "THYAO", name = "THY", category = Category.BIST, currency = "TRY",
            unitType = UnitType.ADET, searchText = normalizeForSearch("THYAO THY"),
        )
        return v.copy(id = db.assetDao().insert(v))
    }

    private suspend fun alim(v: AssetEntity, d: Int) = db.transactionDao().insert(
        TransactionEntity(
            assetId = v.id, type = TransactionType.ALIS, quantity = BigDecimal("10"), unitPriceTl = BigDecimal("100"),
            commissionTl = BigDecimal.ZERO, tradeDate = gun(d), note = null, createdAt = Instant.EPOCH,
        ),
    )

    private suspend fun kapanis(id: Long, vararg noktalar: Pair<Int, String>) = db.priceHistoryDao().upsertAll(
        noktalar.map { (d, k) -> PriceHistoryEntity(id, gun(d), BigDecimal(k), BigDecimal(k), "TEST") },
    )

    private fun snapshot() = SnapshotDeposu(
        GrafikDeposu(db.transactionDao(), db.assetDao(), db.priceHistoryDao(), db.priceQuoteDao()),
        db.portfolioSnapshotDao(),
    )

    // --- Günlük kayıt ---

    @Test
    fun `bugunden onceki gunler yazilir, bugun yazilmaz`() = runBlocking {
        val v = thyao()
        alim(v, 1)
        kapanis(v.id, *(1..10).map { it to "${100 + it}" }.toTypedArray())

        val yazilan = snapshot().eksikGunleriYaz(gun(10))

        assertEquals(9, yazilan) // 1..9
        val kayitlar = db.portfolioSnapshotDao().range(gun(1), gun(30))
        assertEquals(gun(9), kayitlar.last().date)
        assertEquals(BigDecimal("1010"), kayitlar.first().totalValueTl) // 10 × 101
        assertEquals(BigDecimal("1000"), kayitlar.first().totalCostTl)
        assertTrue(kayitlar.first().categoryDistribution.contains("\"BIST\":\"1010\""))
    }

    @Test
    fun `ikinci calistirmada tekrar yazilmaz, yeni gun eklenince yalnizca o gun yazilir`() = runBlocking {
        val v = thyao()
        alim(v, 1)
        kapanis(v.id, *(1..11).map { it to "${100 + it}" }.toTypedArray())
        val s = snapshot()

        assertEquals(9, s.eksikGunleriYaz(gun(10)))
        assertEquals(0, s.eksikGunleriYaz(gun(10)))
        assertEquals(1, s.eksikGunleriYaz(gun(11))) // yalnızca 10'u
    }

    @Test
    fun `uzun sure acilmadiysa eksik gunler geriye donuk tamamlanir`() = runBlocking {
        val v = thyao()
        alim(v, 1)
        kapanis(v.id, *(1..20).map { it to "${100 + it}" }.toTypedArray())
        val s = snapshot()

        s.eksikGunleriYaz(gun(5)) // 1..4 yazıldı
        val sonra = s.eksikGunleriYaz(gun(20)) // 5..19

        assertEquals(15, sonra)
        assertEquals(19, db.portfolioSnapshotDao().range(gun(1), gun(30)).size)
    }

    @Test
    fun `fiyat gecmisi olmayan gunler yazilmaz`() = runBlocking {
        val v = thyao()
        alim(v, 1) // geçmiş seri yok: maliyetle değerlenir
        assertEquals(0, snapshot().eksikGunleriYaz(gun(10)))
        assertNull(db.portfolioSnapshotDao().latest())
    }

    @Test
    fun `islem yoksa kayit yazilmaz`() = runBlocking {
        assertEquals(0, snapshot().eksikGunleriYaz(gun(10)))
    }

    // --- Alış tarihindeki kur ---

    private fun depo() = PortfolioRepository(db.assetDao(), db.transactionDao(), db.priceQuoteDao(), saat, db.priceHistoryDao())

    @Test
    fun `alis tarihindeki kur o gune kadarki son kurdur`() = runBlocking {
        kapanis(FX_USDTRY_ID, 14 to "40", 16 to "50")

        assertEquals(BigDecimal("40"), depo().usdTryOn(gun(14)))
        assertEquals(BigDecimal("40"), depo().usdTryOn(gun(15))) // 15'inde kur yok, 14'ü
        assertEquals(BigDecimal("50"), depo().usdTryOn(gun(20)))
    }

    @Test
    fun `seri baslamadan onceki tarihte canli kura duser, hic kur yoksa null`() = runBlocking {
        assertNull(depo().usdTryOn(gun(10)))

        db.priceQuoteDao().insert(
            PriceQuoteEntity(assetId = FX_USDTRY_ID, price = BigDecimal("48.8"), currency = "TRY", priceTl = BigDecimal("48.8"),
                timestamp = Instant.parse("2026-09-21T09:00:00Z"), source = "TEST"),
        )
        kapanis(FX_USDTRY_ID, 14 to "40")
        assertEquals(BigDecimal("48.8"), depo().usdTryOn(gun(10))) // 14'ünden önce: canlı kur
    }

    // --- Kur tazeleme ---

    private fun fiyatDeposu(kur: FakePriceSource): PriceRepository {
        val router = SourceRouter(
            routes = mapOf(RouteKey.FX to Route(kur)),
            budget = CallBudget(emptyMap(), saat, ZoneOffset.UTC),
            health = SourceHealth(),
            clock = saat,
            retryDelayMillis = 0,
        )
        return PriceRepository(router, db.priceQuoteDao(), saat)
    }

    @Test
    fun `kur tazeleme yalnizca USD TRY yi yazar ve tazeyse tekrar cekmez`() = runBlocking {
        val kaynak = FakePriceSource(SourceId.TRUNCGIL, saat)
        val depo = fiyatDeposu(kaynak)

        val ilk = depo.refreshFx(Duration.ofMinutes(15))
        val ikinci = depo.refreshFx(Duration.ofMinutes(15))

        assertEquals(1, ilk.updated)
        assertEquals(0, ikinci.updated)
        assertEquals(1, kaynak.calls)
        assertEquals(BigDecimal("48.80"), db.priceQuoteDao().latestFor(FX_USDTRY_ID)!!.priceTl)
    }

    @Test
    fun `kur kaynagi basarisizsa rapor kur grubunu bildirir`() = runBlocking {
        val depo = fiyatDeposu(FakePriceSource(SourceId.TRUNCGIL, saat, failWhen = { true }))
        val rapor = depo.refreshFx(Duration.ofMinutes(15))
        assertEquals(setOf(RouteKey.FX), rapor.failedGroups)
    }
}

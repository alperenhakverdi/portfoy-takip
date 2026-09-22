package com.portfoy.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.portfoy.calc.normalizeForSearch
import com.portfoy.data.db.AssetEntity
import com.portfoy.data.db.PortfoyDatabase
import com.portfoy.data.db.PriceHistoryEntity
import com.portfoy.data.db.PriceQuoteEntity
import com.portfoy.data.db.TransactionEntity
import com.portfoy.model.Category
import com.portfoy.model.TransactionType
import com.portfoy.model.UnitType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Portföyden çıkan varlığın serisi 30 gün sonra temizlenir (doküman 11.3/3, karar 32). "30 gün önce
 * çıkarıldı" ayrı bir zaman damgası olmadan, serinin doğal olarak eskimesinden anlaşılır.
 */
@RunWith(RobolectricTestRunner::class)
class TerkedilenSeriTemizleyiciTest {

    private lateinit var db: PortfoyDatabase
    private lateinit var temizleyici: TerkedilenSeriTemizleyici

    @Before
    fun kur() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PortfoyDatabase::class.java).allowMainThreadQueries().build()
        temizleyici = TerkedilenSeriTemizleyici(db.transactionDao(), db.priceHistoryDao(), db.priceQuoteDao())
    }

    @After
    fun kapat() = db.close()

    private suspend fun varlik(kod: String): Long = db.assetDao().insert(
        AssetEntity(
            code = kod, name = kod, category = Category.BIST, currency = "TRY",
            unitType = UnitType.ADET, searchText = normalizeForSearch(kod),
        ),
    )

    private suspend fun gun(v: Long, tarih: LocalDate) = db.priceHistoryDao().upsertAll(
        listOf(PriceHistoryEntity(v, tarih, BigDecimal("100"), BigDecimal("100"), "TEST")),
    )

    private suspend fun kur(v: Long) = db.priceQuoteDao().insert(
        PriceQuoteEntity(
            assetId = v, price = BigDecimal("100"), currency = "TRY", priceTl = BigDecimal("100"),
            timestamp = Instant.parse("2026-09-01T10:00:00Z"), source = "TEST",
        ),
    )

    private suspend fun alim(v: Long) = db.transactionDao().insert(
        TransactionEntity(
            assetId = v, type = TransactionType.ALIS, quantity = BigDecimal.ONE, unitPriceTl = BigDecimal("100"),
            commissionTl = BigDecimal.ZERO, tradeDate = LocalDate.of(2026, 8, 1), note = null, createdAt = Instant.EPOCH,
        ),
    )

    @Test
    fun `30 gunden eski, portfoyde olmayan seri temizlenir`() = runBlocking {
        val v = varlik("ESKI")
        gun(v, LocalDate.of(2026, 8, 1)) // son gün 31 gün önce (bugün 1 Eylül varsayımıyla)
        kur(v)
        // hiç alım yok: portföyde değil

        val silinen = temizleyici.temizle(LocalDate.of(2026, 9, 1))

        assertEquals(1, silinen)
        assertNull(db.priceHistoryDao().lastDate(v))
        assertNull(db.priceQuoteDao().latestFor(v))
    }

    @Test
    fun `portfoyde tutulan varlik eski olsa da silinmez`() = runBlocking {
        val v = varlik("TUTULAN")
        gun(v, LocalDate.of(2026, 8, 1))
        kur(v)
        alim(v) // portföyde

        val silinen = temizleyici.temizle(LocalDate.of(2026, 9, 1))

        assertEquals(0, silinen)
        assertNotNull(db.priceHistoryDao().lastDate(v))
    }

    @Test
    fun `30 gunden taze seri silinmez, grace suresi tanir`() = runBlocking {
        val v = varlik("YENI")
        gun(v, LocalDate.of(2026, 8, 15)) // yalnızca 17 gün önce

        val silinen = temizleyici.temizle(LocalDate.of(2026, 9, 1))

        assertEquals(0, silinen)
        assertNotNull(db.priceHistoryDao().lastDate(v))
    }

    @Test
    fun `hic gecmisi olmayan varlik dokunulmadan kalir`() = runBlocking {
        varlik("BOS") // hiç history yok, listeye hiç girmez
        val silinen = temizleyici.temizle(LocalDate.of(2026, 9, 1))
        assertEquals(0, silinen)
    }
}

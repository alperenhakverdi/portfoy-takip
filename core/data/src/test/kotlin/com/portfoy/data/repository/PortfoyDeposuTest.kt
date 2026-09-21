package com.portfoy.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.portfoy.calc.normalizeForSearch
import com.portfoy.calc.summarize
import com.portfoy.data.db.AssetEntity
import com.portfoy.data.db.PortfoyDatabase
import com.portfoy.data.db.PriceQuoteEntity
import com.portfoy.data.db.VarsayilanVarliklar
import com.portfoy.model.Category
import com.portfoy.model.UnitType
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PortfoyDeposuTest {

    private lateinit var db: PortfoyDatabase
    private lateinit var depo: PortfolioRepository
    private val saat = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC)

    @Before
    fun kur() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PortfoyDatabase::class.java).allowMainThreadQueries().build()
        depo = PortfolioRepository(db.assetDao(), db.transactionDao(), db.priceQuoteDao(), saat)
    }

    @After
    fun kapat() = db.close()

    private suspend fun varlik(code: String, name: String, category: Category): Long =
        db.assetDao().insert(
            AssetEntity(
                code = code, name = name, category = category, currency = "TRY",
                unitType = UnitType.ADET, searchText = normalizeForSearch("$code $name"),
            ),
        )

    private suspend fun fiyat(assetId: Long, tl: String, zaman: String = "2026-09-21T09:00:00Z", kaynak: String = "YAHOO") =
        db.priceQuoteDao().insert(
            PriceQuoteEntity(
                assetId = assetId, price = BigDecimal(tl), currency = "TRY", priceTl = BigDecimal(tl),
                timestamp = Instant.parse(zaman), source = kaynak,
            ),
        )

    @Test
    fun `islem kaydi olmayan varlik portfoyde gorunmez`() = runBlocking {
        varlik("THYAO", "Türk Hava Yolları", Category.BIST)
        assertTrue(depo.observePortfolio().first().holdings.isEmpty())
    }

    @Test
    fun `alim eklenince portfoy ozeti dogru hesaplanir`() = runBlocking {
        val id = varlik("THYAO", "Türk Hava Yolları", Category.BIST)
        fiyat(id, "300")
        depo.addPurchase(id, BigDecimal("10"), BigDecimal("250"), BigDecimal("5"), LocalDate.of(2026, 8, 1), null)

        val veri = depo.observePortfolio().first()
        val ozet = summarize(veri.holdings)

        assertEquals(0, BigDecimal("3000").compareTo(ozet.totalValue))
        assertEquals(0, BigDecimal("2505").compareTo(ozet.totalCost))
        assertEquals(Instant.parse("2026-09-21T09:00:00Z"), veri.lastUpdate)
    }

    @Test
    fun `fiyati olmayan varlik fiyat alinamadi olarak isaretlenir`() = runBlocking {
        val id = varlik("THYAO", "Türk Hava Yolları", Category.BIST)
        depo.addPurchase(id, BigDecimal("10"), BigDecimal("250"), BigDecimal.ZERO, LocalDate.of(2026, 8, 1), null)

        val ozet = summarize(depo.observePortfolio().first().holdings)
        assertTrue(ozet.categories.single().assets.single().priceMissing)
    }

    @Test
    fun `silme geri alinabilir`() = runBlocking {
        val id = varlik("THYAO", "Türk Hava Yolları", Category.BIST)
        val kayit = depo.addPurchase(id, BigDecimal("10"), BigDecimal("250"), BigDecimal.ZERO, LocalDate.of(2026, 8, 1), "not")

        val silinen = depo.deletePurchase(kayit)!!
        assertTrue(depo.observePortfolio().first().holdings.isEmpty()) // tüm kayıtlar silinince varlık düşer

        depo.restorePurchase(silinen)
        val geri = depo.observePortfolio().first().holdings.single().transactions.single()
        assertEquals(kayit, geri.id)
        assertEquals("not", geri.note)
    }

    @Test
    fun `olmayan kaydi silmek null doner`() = runBlocking {
        assertNull(depo.deletePurchase(12345))
    }

    @Test
    fun `alim duzenlenebilir`() = runBlocking {
        val id = varlik("THYAO", "Türk Hava Yolları", Category.BIST)
        val kayit = depo.addPurchase(id, BigDecimal("10"), BigDecimal("250"), BigDecimal.ZERO, LocalDate.of(2026, 8, 1), null)

        val mevcut = depo.observePortfolio().first().holdings.single().transactions.single()
        depo.updatePurchase(mevcut.copy(quantity = BigDecimal("20"), tradeDate = LocalDate.of(2026, 7, 1)))

        val guncel = depo.observePortfolio().first().holdings.single().transactions.single()
        assertEquals(kayit, guncel.id)
        assertEquals(0, BigDecimal("20").compareTo(guncel.quantity))
        assertEquals(LocalDate.of(2026, 7, 1), depo.oldestPurchaseDate())
    }

    @Test
    fun `bos not kaydedilmez`() = runBlocking {
        val id = varlik("THYAO", "Türk Hava Yolları", Category.BIST)
        depo.addPurchase(id, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, LocalDate.of(2026, 8, 1), "   ")
        assertNull(depo.observePortfolio().first().holdings.single().transactions.single().note)
    }

    @Test
    fun `arama iki karakterden once sonuc vermez, sonra onbellekteki fiyati getirir`() = runBlocking {
        val id = varlik("THYAO", "Türk Hava Yolları", Category.BIST)
        fiyat(id, "300")

        assertTrue(depo.search("t").isEmpty())
        val sonuc = depo.search("turk")
        assertEquals("THYAO", sonuc.single().asset.code)
        assertEquals(0, BigDecimal("300").compareTo(sonuc.single().lastPriceTl))
    }

    @Test
    fun `kategoriyle arama baska kategorileri getirmez`() = runBlocking {
        varlik("THYAO", "Türk Hava Yolları", Category.BIST)
        varlik("TUR", "Turkey ETF", Category.ABD)

        assertEquals(2, depo.search("tur").size)
        assertEquals("THYAO", depo.search("tur", category = Category.BIST).single().asset.code)
        assertEquals("TUR", depo.search("tur", category = Category.ABD).single().asset.code)
    }

    @Test
    fun `kategori listesi aramasiz gelir, sayisi bilinir`() = runBlocking {
        varlik("EREGL", "Ereğli Demir Çelik", Category.BIST)
        varlik("THYAO", "Türk Hava Yolları", Category.BIST)
        varlik("AAPL", "Apple", Category.ABD)

        assertEquals(listOf("EREGL", "THYAO"), depo.kategoriListesi(Category.BIST).map { it.asset.code })
        assertEquals(2, depo.kategoriSayisi(Category.BIST))
        assertEquals(1, depo.kategoriListesi(Category.BIST, limit = 1).size)
    }

    @Test
    fun `fiyati olmayan arama sonucu fiyatsiz gelir`() = runBlocking {
        varlik("THYAO", "Türk Hava Yolları", Category.BIST)
        assertNull(depo.search("thyao").single().lastPriceTl)
    }

    @Test
    fun `elle eklenen varlik MANUEL fiyatla portfoye girebilir`() = runBlocking {
        val id = depo.addManualAsset("yenihisse", "Yeni Halka Arz", Category.BIST, BigDecimal("12.50"))
        depo.addPurchase(id, BigDecimal("100"), BigDecimal("10"), BigDecimal.ZERO, LocalDate.of(2026, 9, 1), null)

        val veri = depo.observePortfolio().first()
        assertEquals("YENIHISSE", veri.holdings.single().asset.code)
        assertEquals(setOf(id), veri.manualPriceAssetIds)
        assertEquals(0, BigDecimal("1250").compareTo(summarize(veri.holdings).totalValue))
    }

    @Test
    fun `ayni kodla elle ekleme yeni kayit acmaz`() = runBlocking {
        val ilk = depo.addManualAsset("ABC", "Bir", Category.BIST, BigDecimal.ONE)
        val ikinci = depo.addManualAsset("abc", "Bir", Category.BIST, BigDecimal.TEN)
        assertEquals(ilk, ikinci)
    }

    @Test
    fun `nakit varligina arama yapmadan erisilir`() = runBlocking {
        VarsayilanVarliklar.ekle(db.assetDao())
        assertNotNull(depo.cashAsset())
        assertEquals(Category.NAKIT, depo.cashAsset()!!.category)
    }

    @Test
    fun `son eklenen varliklar en yeniden eskiye siralanir`() = runBlocking {
        val a = varlik("AAA", "Bir", Category.BIST)
        val b = varlik("BBB", "İki", Category.BIST)
        val kaydet = { id: Long -> runBlocking { depo.addPurchase(id, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, LocalDate.of(2026, 8, 1), null) } }
        kaydet(a)
        // createdAt sabit saatle aynı; sıralama kararlı olsun diye ikinciyi sonraya bırak
        kaydet(b)

        val son = depo.observeRecentAssets().first().map { it.code }
        assertEquals(setOf("AAA", "BBB"), son.toSet())
    }
}

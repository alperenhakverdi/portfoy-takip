package com.portfoy.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.portfoy.calc.normalizeForSearch
import com.portfoy.data.db.PortfoyDatabase
import com.portfoy.model.AssetInfo
import com.portfoy.model.Category
import com.portfoy.model.UnitType
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KatalogDeposuTest {

    private lateinit var db: PortfoyDatabase
    private lateinit var katalog: KatalogDeposu

    @Before
    fun kur() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PortfoyDatabase::class.java).allowMainThreadQueries().build()
        katalog = KatalogDeposu(db)
    }

    @After
    fun kapat() = db.close()

    private fun abd(kod: String, ad: String) = AssetInfo(kod, ad, Category.ABD, "USD", "XNAS")

    private fun fon(kod: String, ad: String) = AssetInfo(kod, ad, Category.FON, "TRY", "TEFAS", "YAT")

    @Test
    fun `ilk yukleme bos kategoriye toplu ekler`() = runBlocking {
        val sonuc = katalog.ilkYukleme(Category.ABD, listOf(abd("AAPL", "APPLE INC"), abd("NVDA", "NVIDIA CORP")))

        assertEquals(2, sonuc.eklenen)
        assertEquals(2, db.assetDao().countByCategory(Category.ABD))
    }

    @Test
    fun `ilk yukleme dolu kategoriye dokunmaz`() = runBlocking {
        katalog.ilkYukleme(Category.ABD, listOf(abd("AAPL", "APPLE INC")))
        val ikinci = katalog.ilkYukleme(Category.ABD, listOf(abd("MSFT", "MICROSOFT")))

        assertEquals(0, ikinci.eklenen)
        assertEquals(1, db.assetDao().countByCategory(Category.ABD))
    }

    @Test
    fun `buyuk liste parcalar halinde eklenir`() = runBlocking {
        val liste = (1..5_500).map { abd("S$it", "SIRKET $it") }
        assertEquals(5_500, katalog.guncelle(liste).eklenen)
        assertEquals(5_500, db.assetDao().countByCategory(Category.ABD))
    }

    @Test
    fun `tazeleme yeni enstruman ekler, degisen adi gunceller, digerlerine dokunmaz`() = runBlocking {
        katalog.guncelle(listOf(abd("AAPL", "APPLE INC"), abd("FB", "FACEBOOK")))
        val sonuc = katalog.guncelle(listOf(abd("AAPL", "APPLE INC"), abd("FB", "META PLATFORMS"), abd("NVDA", "NVIDIA CORP")))

        assertEquals(1, sonuc.eklenen)
        assertEquals(1, sonuc.guncellenen)
        assertEquals("META PLATFORMS", db.assetDao().getByCode("FB", Category.ABD)!!.name)
    }

    @Test
    fun `tazeleme varlik kimligini ve portfoy kayitlarini korur`() = runBlocking {
        katalog.guncelle(listOf(abd("FB", "FACEBOOK")))
        val kimlik = db.assetDao().getByCode("FB", Category.ABD)!!.id
        val depo = PortfolioRepository(db.assetDao(), db.transactionDao(), db.priceQuoteDao(), java.time.Clock.systemUTC())
        depo.addPurchase(kimlik, BigDecimal.TEN, BigDecimal("100"), BigDecimal.ZERO, LocalDate.of(2026, 9, 1), null)

        katalog.guncelle(listOf(abd("FB", "META PLATFORMS")))

        assertEquals(kimlik, db.assetDao().getByCode("FB", Category.ABD)!!.id)
        assertEquals(1, depo.observePortfolio().first().holdings.single().transactions.size)
    }

    @Test
    fun `guncellenen ad aramada Turkce karakter duyarsiz bulunur`() = runBlocking {
        katalog.guncelle(listOf(fon("AAL", "ESKİ AD")))
        katalog.guncelle(listOf(fon("AAL", "ATA PORTFÖY PARA PİYASASI FONU")))

        val sonuc = db.assetDao().search(normalizeForSearch("piyasasi"), 10)
        assertEquals(listOf("AAL"), sonuc.map { it.code })
    }

    @Test
    fun `fon PAY, ABD ADET birimiyle eklenir ve fon turu saklanir`() = runBlocking {
        katalog.guncelle(listOf(fon("AAL", "ATA FON"), abd("AAPL", "APPLE")))

        val aal = db.assetDao().getByCode("AAL", Category.FON)!!
        assertEquals(UnitType.PAY, aal.unitType)
        assertEquals("YAT", aal.fundKind)
        assertEquals(UnitType.ADET, db.assetDao().getByCode("AAPL", Category.ABD)!!.unitType)
        assertNotNull(db.assetDao().getByCode("AAPL", Category.ABD)!!.exchange)
    }

    @Test
    fun `ayni kodlu tekrarlar tek kayit olur`() = runBlocking {
        val sonuc = katalog.guncelle(listOf(abd("AAPL", "APPLE INC"), abd("AAPL", "APPLE INC (TEKRAR)")))
        assertEquals(1, sonuc.eklenen)
        assertTrue(db.assetDao().countByCategory(Category.ABD) == 1)
    }
}

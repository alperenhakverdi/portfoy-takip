package com.portfoy.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.portfoy.calc.normalizeForSearch
import com.portfoy.calc.positionOf
import com.portfoy.model.Category
import com.portfoy.model.TransactionType
import com.portfoy.model.UnitType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VeritabaniTest {

    private lateinit var db: PortfoyDatabase

    @Before
    fun kur() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PortfoyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun kapat() = db.close()

    private fun varlik(code: String, name: String, category: Category = Category.BIST) = AssetEntity(
        code = code,
        name = name,
        category = category,
        currency = "TRY",
        unitType = UnitType.ADET,
        searchText = normalizeForSearch("$code $name"),
    )

    private fun islem(assetId: Long, adet: String, fiyat: String, tarih: LocalDate = LocalDate.of(2026, 9, 1)) =
        TransactionEntity(
            assetId = assetId,
            type = TransactionType.ALIS,
            quantity = BigDecimal(adet),
            unitPriceTl = BigDecimal(fiyat),
            commissionTl = BigDecimal.ZERO,
            tradeDate = tarih,
            note = null,
            createdAt = Instant.parse("2026-09-01T10:00:00Z"),
        )

    @Test
    fun `varsayilan varliklar bir kez eklenir, tekrar cagirinca cogalmaz`() = runBlocking {
        VarsayilanVarliklar.ekle(db.assetDao())
        VarsayilanVarliklar.ekle(db.assetDao())

        assertEquals(7, db.assetDao().count())
        assertNotNull(db.assetDao().getByCode(VarsayilanVarliklar.NAKIT_KODU, Category.NAKIT))
        assertNotNull(db.assetDao().getByCode(VarsayilanVarliklar.ALTIN_KODU, Category.EMTIA))
        assertNotNull(db.assetDao().getByCode(VarsayilanVarliklar.DOLAR_KODU, Category.DOVIZ))
        assertNotNull(db.assetDao().getByCode(VarsayilanVarliklar.EURO_KODU, Category.DOVIZ))
        assertNotNull(db.assetDao().getByCode(VarsayilanVarliklar.BITCOIN_KODU, Category.KRIPTO))
        assertNotNull(db.assetDao().getByCode(VarsayilanVarliklar.ETHEREUM_KODU, Category.KRIPTO))
    }

    @Test
    fun `parasal alanlar tam hassasiyetle saklanir`() = runBlocking {
        val id = db.assetDao().insert(varlik("THYAO", "Türk Hava Yolları"))
        val hassas = "123456789.123456789012345"
        val kayitId = db.transactionDao().insert(islem(id, "0.1234", hassas))

        val okunan = db.transactionDao().getById(kayitId)!!
        assertEquals(BigDecimal(hassas), okunan.unitPriceTl)
        assertEquals(BigDecimal("0.1234"), okunan.quantity)
    }

    @Test
    fun `kayitlardan agirlikli ortalama maliyet hesaplanir`() = runBlocking {
        val id = db.assetDao().insert(varlik("THYAO", "Türk Hava Yolları"))
        db.transactionDao().insert(islem(id, "10", "100"))
        db.transactionDao().insert(islem(id, "10", "120"))

        val kayitlar = db.transactionDao().observeForAsset(id).first().map { it.toModel() }
        val pozisyon = positionOf(kayitlar)
        assertEquals(0, BigDecimal("2200").compareTo(pozisyon.totalCost))
        assertEquals(0, BigDecimal("110").compareTo(pozisyon.unitCost))
    }

    @Test
    fun `varlik silinince kayitlari da silinir`() = runBlocking {
        val id = db.assetDao().insert(varlik("THYAO", "Türk Hava Yolları"))
        db.transactionDao().insert(islem(id, "10", "100"))
        db.openHelper.writableDatabase.execSQL("DELETE FROM asset WHERE id = $id")

        assertTrue(db.transactionDao().observeAll().first().isEmpty())
    }

    @Test
    fun `olmayan varliga islem eklenemez`() = runBlocking {
        try {
            db.transactionDao().insert(islem(assetId = 999, adet = "1", fiyat = "1"))
            fail("Yabancı anahtar hatası bekleniyordu")
        } catch (_: SQLiteConstraintException) {
            // beklenen
        }
    }

    @Test
    fun `silinen islem eski kimligiyle geri yuklenebilir`() = runBlocking {
        val id = db.assetDao().insert(varlik("THYAO", "Türk Hava Yolları"))
        val kayitId = db.transactionDao().insert(islem(id, "10", "100"))
        val silinen = db.transactionDao().getById(kayitId)!!

        db.transactionDao().delete(kayitId)
        assertNull(db.transactionDao().getById(kayitId))

        db.transactionDao().insert(silinen)
        assertEquals(silinen, db.transactionDao().getById(kayitId))
    }

    @Test
    fun `portfoydeki varliklar islem kaydi olanlardir`() = runBlocking {
        val a = db.assetDao().insert(varlik("THYAO", "Türk Hava Yolları"))
        db.assetDao().insert(varlik("ASELS", "Aselsan"))
        db.transactionDao().insert(islem(a, "5", "300"))

        assertEquals(listOf("THYAO"), db.assetDao().observeHeldAssets().first().map { it.code })
    }

    @Test
    fun `en eski islem tarihi bulunur`() = runBlocking {
        val id = db.assetDao().insert(varlik("THYAO", "Türk Hava Yolları"))
        db.transactionDao().insert(islem(id, "1", "1", LocalDate.of(2026, 5, 3)))
        db.transactionDao().insert(islem(id, "1", "1", LocalDate.of(2026, 8, 9)))

        assertEquals(LocalDate.of(2026, 5, 3), db.transactionDao().oldestTradeDate())
    }

    // --- Arama ---

    @Test
    fun `turkce karakter duyarsiz arama - turk Türk sonucunu getirir`() = runBlocking {
        db.assetDao().insertAll(
            listOf(varlik("THYAO", "Türk Hava Yolları"), varlik("ASELS", "Aselsan")),
        )
        val sonuc = db.assetDao().search(normalizeForSearch("turk"), 20)
        assertEquals(listOf("THYAO"), sonuc.map { it.code })
    }

    @Test
    fun `arama hem koda hem isme gore calisir`() = runBlocking {
        db.assetDao().insertAll(
            listOf(varlik("NVDA", "NVIDIA Corp", Category.ABD), varlik("AAPL", "Apple Inc", Category.ABD)),
        )
        assertEquals(listOf("NVDA"), db.assetDao().search("nvda", 20).map { it.code })
        assertEquals(listOf("NVDA"), db.assetDao().search("nvidia", 20).map { it.code })
    }

    @Test
    fun `kodu tam eslesen once, sonra kodla baslayan, sonra digerleri gelir`() = runBlocking {
        db.assetDao().insertAll(
            listOf(
                varlik("ASELSAN2", "Başka bir şirket"),
                varlik("XYZ", "Aselsan iştiraki"),
                varlik("ASELS", "Aselsan Elektronik"),
            ),
        )
        assertEquals(
            listOf("ASELS", "ASELSAN2", "XYZ"),
            db.assetDao().search("asels", 20).map { it.code },
        )
    }

    @Test
    fun `adinda kelime baslangici eslesen, metnin ortasinda eslesenden once gelir`() = runBlocking {
        db.assetDao().insertAll(
            listOf(
                varlik("GRTRK", "GRAINTURK TARIM A.Ş."),
                varlik("THYAO", "TÜRK HAVA YOLLARI A.O."),
                varlik("ALBRK", "ALBARAKA TÜRK KATILIM BANKASI"),
            ),
        )
        assertEquals(
            listOf("ALBRK", "THYAO", "GRTRK"),
            db.assetDao().search(normalizeForSearch("turk"), 20).map { it.code },
        )
    }

    @Test
    fun `ayni kod farkli kategoride ayri kayit olabilir, ayni kod ve kategori olamaz`() = runBlocking {
        val ilk = db.assetDao().insert(varlik("ABC", "Bir", Category.BIST))
        val ayni = db.assetDao().insert(varlik("ABC", "Yinelenen", Category.BIST))
        val farkli = db.assetDao().insert(varlik("ABC", "Başka", Category.ABD))

        assertTrue(ilk > 0)
        assertEquals(-1L, ayni) // IGNORE: eklenmedi
        assertTrue(farkli > 0)
    }

    // --- Fiyatlar ---

    @Test
    fun `en yeni fiyat gecerlidir, elle girilen fiyatlar seri olusturur`() = runBlocking {
        val id = db.assetDao().insert(varlik("KOD", "Manuel varlık"))
        fun fiyat(saat: String, tutar: String) = PriceQuoteEntity(
            assetId = id,
            price = BigDecimal(tutar),
            currency = "TRY",
            priceTl = BigDecimal(tutar),
            timestamp = Instant.parse("2026-09-01T${saat}:00Z"),
            source = "MANUEL",
        )
        db.priceQuoteDao().insert(fiyat("10:00", "100"))
        db.priceQuoteDao().insert(fiyat("12:00", "110"))
        db.priceQuoteDao().insert(fiyat("11:00", "105"))

        assertEquals(BigDecimal("110"), db.priceQuoteDao().latestFor(id)!!.priceTl)
        val enYeniler = db.priceQuoteDao().observeLatest().first()
        assertEquals(1, enYeniler.size)
        assertEquals(BigDecimal("110"), enYeniler.single().priceTl)
    }

    @Test
    fun `gecmis seri ust uste yazilir ve aralik sorgulanir`() = runBlocking {
        fun satir(gun: Int, kapanis: String) = PriceHistoryEntity(
            assetId = FX_USDTRY_ID,
            date = LocalDate.of(2026, 9, gun),
            close = BigDecimal(kapanis),
            closeTl = BigDecimal(kapanis),
            source = "EVDS",
        )
        db.priceHistoryDao().upsertAll(listOf(satir(1, "48.0"), satir(2, "48.1"), satir(3, "48.2")))
        db.priceHistoryDao().upsertAll(listOf(satir(2, "49.0"))) // düzeltme

        val aralik = db.priceHistoryDao().range(FX_USDTRY_ID, LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 3))
        assertEquals(listOf(BigDecimal("49.0"), BigDecimal("48.2")), aralik.map { it.close })
        assertEquals(LocalDate.of(2026, 9, 3), db.priceHistoryDao().lastDate(FX_USDTRY_ID))
        assertEquals(LocalDate.of(2026, 9, 1), db.priceHistoryDao().firstDate(FX_USDTRY_ID))
    }

    @Test
    fun `saklama siniri eski gecmisi siler`() = runBlocking {
        fun satir(yil: Int) = PriceHistoryEntity(
            FX_USDTRY_ID, LocalDate.of(yil, 1, 1), BigDecimal.ONE, BigDecimal.ONE, "EVDS",
        )
        db.priceHistoryDao().upsertAll(listOf(satir(2019), satir(2025)))
        db.priceHistoryDao().deleteOlderThan(LocalDate.of(2021, 1, 1))

        assertEquals(
            listOf(LocalDate.of(2025, 1, 1)),
            db.priceHistoryDao().range(FX_USDTRY_ID, LocalDate.of(2000, 1, 1), LocalDate.of(2030, 1, 1)).map { it.date },
        )
    }

    @Test
    fun `gunluk kayit ayni gune ikinci kez yazilinca guncellenir`() = runBlocking {
        fun kayit(deger: String) = PortfolioSnapshotEntity(
            date = LocalDate.of(2026, 9, 20),
            totalValueTl = BigDecimal(deger),
            totalCostTl = BigDecimal("100"),
            categoryDistribution = """{"ABD":"$deger"}""",
        )
        db.portfolioSnapshotDao().upsert(kayit("150"))
        db.portfolioSnapshotDao().upsert(kayit("160"))

        val liste = db.portfolioSnapshotDao().range(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        assertEquals(1, liste.size)
        assertEquals(BigDecimal("160"), liste.single().totalValueTl)
        assertEquals(liste.single(), db.portfolioSnapshotDao().latest())
    }
}

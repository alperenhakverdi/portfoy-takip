package com.portfoy.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.portfoy.calc.Donem
import com.portfoy.calc.normalizeForSearch
import com.portfoy.data.db.AssetEntity
import com.portfoy.data.db.FX_USDTRY_ID
import com.portfoy.data.db.PortfoyDatabase
import com.portfoy.data.db.PriceQuoteEntity
import com.portfoy.data.db.TransactionEntity
import com.portfoy.model.AssetRef
import com.portfoy.model.Candle
import com.portfoy.model.Category
import com.portfoy.model.Quote
import com.portfoy.model.SourceId
import com.portfoy.model.TransactionType
import com.portfoy.model.UnitType
import com.portfoy.network.GecmisAnahtari
import com.portfoy.network.GecmisKaynagi
import com.portfoy.network.GunlukSayac
import com.portfoy.network.HistoryRouter
import com.portfoy.network.PriceSource
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GecmisVeGrafikTest {

    /** Verilen serileri döndüren, yapılan istekleri saklayan sahte geçmiş kaynağı. */
    private class SahteSeri(override val id: SourceId, private val seri: () -> List<Candle>) : PriceSource {
        data class Cagri(val varlik: AssetRef, val from: LocalDate, val to: LocalDate)

        val cagrilar = mutableListOf<Cagri>()
        var hata: Throwable? = null

        override suspend fun getQuotes(assets: List<AssetRef>): Result<List<Quote>> = Result.failure(UnsupportedOperationException())

        override suspend fun getHistory(asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>> {
            cagrilar += Cagri(asset, from, to)
            hata?.let { return Result.failure(it) }
            return Result.success(seri().filter { it.date in from..to })
        }
    }

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

    private fun gun(d: Int) = LocalDate.of(2026, 9, d)

    private fun mum(d: Int, k: String) = Candle(gun(d), BigDecimal(k))

    private fun depo(zincirler: Map<GecmisAnahtari, List<PriceSource>>, cap: Int = 100) = HistoryRepository(
        HistoryRouter(zincirler.mapValues { (_, l) -> l.map { GecmisKaynagi(it) } }, GunlukSayac(cap, saat, ZoneOffset.UTC)),
        db.priceHistoryDao(),
        saat,
        ZoneOffset.UTC,
    )

    private suspend fun varlik(kod: String, kategori: Category, tur: String? = null): AssetEntity {
        val v = AssetEntity(
            code = kod, name = kod, category = kategori, currency = if (kategori == Category.ABD) "USD" else "TRY",
            unitType = if (kategori == Category.NAKIT) UnitType.TL else UnitType.ADET, fundKind = tur, searchText = normalizeForSearch(kod),
        )
        return v.copy(id = db.assetDao().insert(v))
    }

    private suspend fun alim(v: AssetEntity, d: Int, adet: String, fiyat: String) = db.transactionDao().insert(
        TransactionEntity(
            assetId = v.id, type = TransactionType.ALIS, quantity = BigDecimal(adet), unitPriceTl = BigDecimal(fiyat),
            commissionTl = BigDecimal.ZERO, tradeDate = gun(d), note = null, createdAt = Instant.EPOCH,
        ),
    )

    private suspend fun gecmis(id: Long) = db.priceHistoryDao().range(id, gun(1).minusYears(10), gun(30))

    // --- Geçmiş çekimi ---

    @Test
    fun `BIST gecmisi alis tarihinden bugune tek istekle cekilir ve TL olarak saklanir`() = runBlocking {
        val yahoo = SahteSeri(SourceId.YAHOO) { (1..21).map { mum(it, "${100 + it}") } }
        val thyao = varlik("THYAO", Category.BIST)

        val sonuc = depo(mapOf(GecmisAnahtari.BIST to listOf(yahoo))).ensure(thyao, gun(1))

        assertTrue(sonuc.basarili)
        assertEquals(21, sonuc.eklenen)
        assertEquals(listOf(SahteSeri.Cagri(AssetRef("THYAO", Category.BIST, null), gun(1), gun(21))), yahoo.cagrilar)
        assertEquals(21, gecmis(thyao.id).size)
        assertEquals(BigDecimal("101"), gecmis(thyao.id).first().closeTl)
    }

    @Test
    fun `ABD fiyati o gunun kuruyla TL ye cevrilir, kuru olmayan gun onceki kuru kullanir`() = runBlocking {
        val twelve = SahteSeri(SourceId.TWELVE_DATA) { listOf(mum(14, "100"), mum(15, "100"), mum(16, "100")) }
        // 15'inde kur yok (Türkiye tatili), ABD açık.
        val evds = SahteSeri(SourceId.TCMB_EVDS) { listOf(mum(14, "40"), mum(16, "50")) }
        val aapl = varlik("AAPL", Category.ABD)

        val sonuc = depo(mapOf(GecmisAnahtari.ABD to listOf(twelve), GecmisAnahtari.KUR to listOf(evds))).ensure(aapl, gun(14))

        assertTrue(sonuc.basarili)
        val satirlar = gecmis(aapl.id)
        assertEquals(listOf(4000, 4000, 5000), satirlar.map { it.closeTl.setScale(0, RoundingMode.HALF_UP).toInt() })
        assertEquals(BigDecimal("100"), satirlar.first().close) // USD fiyatı da saklanır
        assertEquals(2, gecmis(FX_USDTRY_ID).size) // kur serisi tek sefer, ortak
    }

    @Test
    fun `ikinci cagri yalnizca eksik son gunleri ister`() = runBlocking {
        val yahoo = SahteSeri(SourceId.YAHOO) { (1..30).map { mum(it, "100") } }
        val thyao = varlik("THYAO", Category.BIST)
        val d = depo(mapOf(GecmisAnahtari.BIST to listOf(yahoo)))

        d.ensure(thyao, gun(1))
        saat.simdi = Instant.parse("2026-09-25T10:00:00Z")
        d.ensure(thyao, gun(1))

        assertEquals(2, yahoo.cagrilar.size)
        assertEquals(gun(22), yahoo.cagrilar[1].from) // son saklanan 21 idi
        assertEquals(gun(25), yahoo.cagrilar[1].to)
    }

    @Test
    fun `veri guncelse yeniden cekim yapilmaz`() = runBlocking {
        val yahoo = SahteSeri(SourceId.YAHOO) { (1..21).map { mum(it, "100") } }
        val thyao = varlik("THYAO", Category.BIST)
        val d = depo(mapOf(GecmisAnahtari.BIST to listOf(yahoo)))

        d.ensure(thyao, gun(1))
        d.ensure(thyao, gun(1))
        d.ensure(thyao, gun(1))

        assertEquals(1, yahoo.cagrilar.size)
    }

    @Test
    fun `alis tarihi geriye cekilirse yalnizca eksik baslangic cekilir`() = runBlocking {
        val yahoo = SahteSeri(SourceId.YAHOO) { (1..21).map { mum(it, "100") } }
        val thyao = varlik("THYAO", Category.BIST)
        val d = depo(mapOf(GecmisAnahtari.BIST to listOf(yahoo)))

        d.ensure(thyao, gun(10)) // 10..21
        d.ensure(thyao, gun(1)) // eksik: 1..9

        assertEquals(gun(1), yahoo.cagrilar[1].from)
        assertEquals(gun(9), yahoo.cagrilar[1].to)
        assertEquals(21, gecmis(thyao.id).size)
    }

    @Test
    fun `alis tarihi 5 yildan eskiyse cekim 5 yila sinirlanir`() = runBlocking {
        val yahoo = SahteSeri(SourceId.YAHOO) { emptyList() }
        val thyao = varlik("THYAO", Category.BIST)

        depo(mapOf(GecmisAnahtari.BIST to listOf(yahoo))).ensure(thyao, LocalDate.of(2015, 3, 1))

        assertEquals(LocalDate.of(2021, 9, 21), yahoo.cagrilar.single().from)
    }

    @Test
    fun `kaynak basarisizsa hata doner ve hicbir sey yazilmaz`() = runBlocking {
        val yahoo = SahteSeri(SourceId.YAHOO) { emptyList() }.also { it.hata = java.io.IOException("ağ yok") }
        val thyao = varlik("THYAO", Category.BIST)

        val sonuc = depo(mapOf(GecmisAnahtari.BIST to listOf(yahoo))).ensure(thyao, gun(1))

        assertFalse(sonuc.basarili)
        assertNotNull(sonuc.hata)
        assertTrue(gecmis(thyao.id).isEmpty())
    }

    @Test
    fun `kur serisi alinamazsa ABD gecmisi de yazilmaz`() = runBlocking {
        val twelve = SahteSeri(SourceId.TWELVE_DATA) { listOf(mum(14, "100")) }
        val evds = SahteSeri(SourceId.TCMB_EVDS) { emptyList() }.also { it.hata = java.io.IOException("kur yok") }
        val aapl = varlik("AAPL", Category.ABD)

        val sonuc = depo(mapOf(GecmisAnahtari.ABD to listOf(twelve), GecmisAnahtari.KUR to listOf(evds))).ensure(aapl, gun(14))

        assertFalse(sonuc.basarili)
        assertTrue(twelve.cagrilar.isEmpty())
    }

    @Test
    fun `nakit icin gecmis cekilmez`() = runBlocking {
        val yahoo = SahteSeri(SourceId.YAHOO) { emptyList() }
        val nakit = varlik("TRY", Category.NAKIT)
        assertTrue(depo(mapOf(GecmisAnahtari.BIST to listOf(yahoo))).ensure(nakit, gun(1)).basarili)
        assertTrue(yahoo.cagrilar.isEmpty())
    }

    // --- Grafik hesabı ---

    private fun grafik() = GrafikDeposu(db.transactionDao(), db.assetDao(), db.priceHistoryDao(), db.priceQuoteDao())

    private suspend fun seriYaz(v: AssetEntity, vararg noktalar: Pair<Int, String>) = db.priceHistoryDao().upsertAll(
        noktalar.map { (d, k) ->
            com.portfoy.data.db.PriceHistoryEntity(v.id, gun(d), BigDecimal(k), BigDecimal(k), "TEST")
        },
    )

    private suspend fun canli(v: AssetEntity, tl: String, zaman: String = "2026-09-10T10:00:00Z") = db.priceQuoteDao().insert(
        PriceQuoteEntity(assetId = v.id, price = BigDecimal(tl), currency = "TRY", priceTl = BigDecimal(tl), timestamp = Instant.parse(zaman), source = "TEST"),
    )

    @Test
    fun `islem yoksa grafik verisi yoktur`() = runBlocking {
        assertNull(grafik().hesapla(Donem.BIR_AY, gun(10)))
        assertTrue(grafik().varlikGetirileri(Donem.BIR_AY, gun(10)).isEmpty())
    }

    @Test
    fun `deger serisi alis gunu 1000 den bugunku canli fiyata 1100 e gider`() = runBlocking {
        val v = varlik("THYAO", Category.BIST)
        alim(v, 1, "10", "100")
        seriYaz(v, 1 to "100", 2 to "101", 3 to "102", 4 to "103", 7 to "106", 8 to "107", 9 to "108", 10 to "109")
        canli(v, "110")

        val veri = grafik().hesapla(Donem.BIR_AY, gun(10))!!

        assertEquals(1000, veri.noktalar.first().valueTl.toInt())
        assertEquals(1100, veri.noktalar.last().valueTl.toInt()) // son nokta canlı fiyat
        assertEquals(gun(10), veri.noktalar.last().date)
        assertEquals(10, veri.noktalar.size) // 1..10, hafta sonu (5 ve 6) düzleştirilmiş
        assertEquals(1030, veri.noktalar[4].valueTl.toInt()) // 5. gün: 4'ünün kapanışı (103) ile
        assertBd("100", veri.toplamTl)
        assertBd("10", veri.toplamYuzde!!)
        assertFalse(veri.tahmini)
    }

    @Test
    fun `portfoy donemden gencse pencere kisalir ve gun sayisi bildirilir`() = runBlocking {
        val v = varlik("THYAO", Category.BIST)
        alim(v, 1, "10", "100")
        seriYaz(v, 1 to "100", 10 to "110")

        val veri = grafik().hesapla(Donem.BIR_AY, gun(10))!!

        assertTrue(veri.pencere.truncated)
        assertEquals(9L, veri.pencere.portfolioDays)
        assertEquals(gun(1), veri.pencere.start)
    }

    @Test
    fun `donem icinde eklenen para getiri sayilmaz (Dietz)`() = runBlocking {
        val v = varlik("THYAO", Category.BIST)
        alim(v, 1, "10", "100") // 1000
        alim(v, 5, "10", "105") // 1050, dönem içinde eklenen para
        seriYaz(v, 1 to "100", 2 to "101", 3 to "102", 4 to "103", 5 to "105", 7 to "106", 8 to "107", 9 to "108", 10 to "109")
        canli(v, "110")

        val veri = grafik().hesapla(Donem.BIR_AY, gun(10))!!

        // Bitiş 20 × 110 = 2200; kazanç 2200 − 1000 − 1050 = 150; payda 1000 + 1050 × 6/9 = 1700 → %8,82
        assertBd("150", veri.toplamTl)
        assertBd("8.82", veri.toplamYuzde!!)
    }

    @Test
    fun `secilen donem baslangici portfoyden sonraysa deger o gunden baslar`() = runBlocking {
        val v = varlik("THYAO", Category.BIST)
        alim(v, 1, "10", "100")
        seriYaz(v, 1 to "100", 3 to "102", 4 to "103", 7 to "106", 8 to "107", 9 to "108", 10 to "109")
        canli(v, "110")

        val veri = grafik().hesapla(Donem.BIR_HAFTA, gun(10))!! // dönem başı 3 Eylül

        assertEquals(gun(3), veri.noktalar.first().date)
        assertFalse(veri.pencere.truncated)
        assertBd("7.84", veri.toplamYuzde!!) // 1020 → 1100
    }

    @Test
    fun `fiyat gecmisi olmayan varlik maliyetle degerlenir ve tahmini isaretlenir`() = runBlocking {
        val v = varlik("YENI", Category.BIST)
        alim(v, 1, "10", "100")

        val veri = grafik().hesapla(Donem.BIR_AY, gun(10))!!

        assertTrue(veri.tahmini)
        assertTrue(veri.noktalar.all { it.valueTl.toInt() == 1000 })
    }

    @Test
    fun `canli fiyati olan ama gecmis serisi henuz dolmamis yeni varlikta donem getirisi aninda hesaplanir (M31)`() = runBlocking {
        // Varlık bugün alındı, fiyatı canlı çekildi (price_quote) ama geçmiş seri (price_history)
        // henüz ağdan dolmadı — tam olarak M31'de düzeltilen senaryo.
        val v = varlik("BTC", Category.KRIPTO)
        alim(v, 10, "1", "100")
        canli(v, "110", "2026-09-10T10:00:00Z")

        val veri = grafik().hesapla(Donem.BIR_AY, gun(10))!!

        assertNotNull("geçmiş seri boş olsa da getiri hesaplanabilmeli", veri.toplamYuzde)
        assertEquals(0, BigDecimal("10.00").compareTo(veri.toplamYuzde!!.setScale(2, RoundingMode.HALF_UP)))
        assertEquals(0, BigDecimal("10.00").compareTo(veri.toplamTl.setScale(2, RoundingMode.HALF_UP)))
    }

    @Test
    fun `elle girilen fiyatlar seri olusturur`() = runBlocking {
        val v = varlik("YENI", Category.BIST)
        alim(v, 1, "10", "100")
        canli(v, "100", "2026-09-01T10:00:00Z")
        canli(v, "120", "2026-09-05T10:00:00Z")

        val veri = grafik().hesapla(Donem.BIR_AY, gun(10))!!

        assertEquals(1000, veri.noktalar[0].valueTl.toInt()) // 1 Eylül
        assertEquals(1200, veri.noktalar[5].valueTl.toInt()) // 6 Eylül: 5'inde girilen 120
        assertEquals(1200, veri.noktalar.last().valueTl.toInt())
    }

    @Test
    fun `varlik bazli getiri portfoye sonradan giren varligi giris tarihiyle gosterir`() = runBlocking {
        val a = varlik("AAA", Category.BIST)
        val b = varlik("BBB", Category.BIST)
        alim(a, 1, "10", "100")
        alim(b, 6, "10", "200")
        seriYaz(a, 1 to "100", 3 to "102", 4 to "103", 7 to "106", 10 to "109")
        seriYaz(b, 7 to "200", 8 to "210", 10 to "220")
        canli(a, "110")
        canli(b, "220")

        val sonuc = grafik().varlikGetirileri(Donem.BIR_HAFTA, gun(10)).associateBy { it.varlik.code } // dönem başı 3 Eylül

        assertBd("7.84", sonuc.getValue("AAA").yuzde!!)
        assertNull(sonuc.getValue("AAA").girisTarihi)
        assertEquals(gun(6), sonuc.getValue("BBB").girisTarihi) // dönem başında portföyde değildi
        assertTrue(sonuc.getValue("BBB").yuzde!!.signum() != 0)
    }

    @Test
    fun `nakit her donemde sifir getiri verir`() = runBlocking {
        val n = varlik("TRY", Category.NAKIT)
        alim(n, 1, "1000", "1")

        val sonuc = grafik().varlikGetirileri(Donem.BIR_AY, gun(10)).single()

        assertBd("0", sonuc.yuzde!!)
        assertBd("0", sonuc.tl)
    }

    @Test
    fun `kategori getirisi varliklarin ortalamasi degil, birlikte degerlenmesidir`() = runBlocking {
        val a = varlik("AAA", Category.BIST)
        val b = varlik("BBB", Category.BIST)
        val n = varlik("TRY", Category.NAKIT)
        alim(a, 1, "10", "100") // 1000 → 1100
        alim(b, 1, "1", "1000") // 1000 → 1000 (değişmedi)
        alim(n, 1, "500", "1")
        seriYaz(a, 1 to "100", 10 to "110")
        seriYaz(b, 1 to "1000", 10 to "1000")
        canli(a, "110")
        canli(b, "1000")

        val gruplar = grafik().kategoriGetirileri(Donem.BIR_AY, gun(10)).associateBy { it.kategori }

        // BIST: 2000 → 2100, yani %5. Varlık yüzdelerinin ortalaması (%10 ve %0) alınsaydı da %5 çıkardı ama
        // ağırlıklar eşit olmasaydı ayrışırdı; burada toplam TL kazanç tek ölçüttür.
        assertBd("100", gruplar.getValue(Category.BIST).tl)
        assertBd("5", gruplar.getValue(Category.BIST).yuzde!!)
        assertEquals(listOf("AAA", "BBB"), gruplar.getValue(Category.BIST).varliklar.map { it.varlik.code }.sorted())
        assertBd("0", gruplar.getValue(Category.NAKIT).yuzde!!)
        // Varlık listesi kategori kırılımının düzleştirilmiş hâlidir.
        assertEquals(3, grafik().varlikGetirileri(Donem.BIR_AY, gun(10)).size)
    }

    @Test
    fun `portfoydeki dolar kur serisini kopyalar, ikinci kez cekilmez`() = runBlocking {
        val evds = SahteSeri(SourceId.TCMB_EVDS) { (1..10).map { mum(it, "${40 + it}") } }
        val usd = varlik("USDTRY", Category.DOVIZ)

        val sonuc = depo(mapOf(GecmisAnahtari.KUR to listOf(evds))).ensure(usd, gun(1))

        assertTrue(sonuc.basarili)
        assertEquals(1, evds.cagrilar.size) // yalnızca ortak kur serisi çekildi
        assertEquals(gecmis(FX_USDTRY_ID).map { it.closeTl }, gecmis(usd.id).map { it.closeTl })
        assertEquals(BigDecimal("41"), gecmis(usd.id).first().closeTl)
    }

    @Test
    fun `euro kendi serisiyle cekilir`() = runBlocking {
        val evds = SahteSeri(SourceId.TCMB_EVDS) { (1..10).map { mum(it, "${55 + it}") } }
        val eur = varlik("EURTRY", Category.DOVIZ)

        depo(mapOf(GecmisAnahtari.KUR to listOf(evds))).ensure(eur, gun(1))

        assertEquals(AssetRef("EURTRY", Category.DOVIZ, null), evds.cagrilar.single().varlik)
        assertEquals(BigDecimal("56"), gecmis(eur.id).first().closeTl)
    }

    private fun assertBd(beklenen: String, gercek: BigDecimal) =
        assertEquals(BigDecimal(beklenen).setScale(2), gercek.setScale(2, RoundingMode.HALF_UP))
}

package com.portfoy.data.repository

import com.portfoy.calc.ChartWindow
import com.portfoy.calc.Contribution
import com.portfoy.calc.DailyPoint
import com.portfoy.calc.Donem
import com.portfoy.calc.HistoricalPurchase
import com.portfoy.calc.chartWindow
import com.portfoy.calc.periodReturn
import com.portfoy.calc.portfolioSeries
import com.portfoy.calc.returnSeries
import com.portfoy.data.db.AssetDao
import com.portfoy.data.db.PriceHistoryDao
import com.portfoy.data.db.PriceQuoteDao
import com.portfoy.data.db.TransactionDao
import com.portfoy.data.db.TransactionEntity
import com.portfoy.data.db.toModel
import com.portfoy.model.Asset
import com.portfoy.model.Candle
import com.portfoy.model.Category
import java.math.BigDecimal
import java.time.LocalDate

/** Portföy grafiklerinin verisi: TL değer serisi ve getiri (yüzde) serisi. */
data class GrafikVerisi(
    val donem: Donem,
    val pencere: ChartWindow,
    /** Her gün için portföy değeri (TL), maliyeti ve kategori dağılımı. */
    val noktalar: List<DailyPoint>,
    /** Dönem başından her güne birikimli getiri yüzdesi (basit Dietz). */
    val getiri: List<Pair<LocalDate, BigDecimal>>,
    /** Dönem başındaki değer ile bugünkü değer arasındaki TL kâr/zarar (dönem içi eklenen para düşülmüş). */
    val toplamTl: BigDecimal,
    val toplamYuzde: BigDecimal?,
    /** Bazı günler fiyat bilinmeyip maliyetle değerlendi (geçmiş seri henüz çekilmedi ya da elle fiyatlı varlık). */
    val tahmini: Boolean,
) {
    val bos: Boolean get() = noktalar.isEmpty()
}

/** Bir varlığın seçilen dönemdeki getirisi. */
data class VarlikDonemGetirisi(
    val varlik: Asset,
    val yuzde: BigDecimal?,
    val tl: BigDecimal,
    /** Varlık dönem başında portföyde değildiyse giriş tarihi; getiriye yalnızca portföyde bulunduğu süre kadar katılır. */
    val girisTarihi: LocalDate?,
)

/** Bir kategorinin seçilen dönemdeki getirisi ve altındaki varlıklar (Performans listesi kategori kırılımlıdır). */
data class KategoriDonemGetirisi(
    val kategori: Category,
    val yuzde: BigDecimal?,
    val tl: BigDecimal,
    val varliklar: List<VarlikDonemGetirisi>,
)

/**
 * Grafikleri saklanan geçmiş serilerden hesaplar ("geriye dönük hesaplama"). Grafiğin son noktası, Portföy ekranındaki
 * güncel değerle tutarlı olsun diye son bilinen canlı fiyatı kullanır.
 */
class GrafikDeposu(
    private val transactionDao: TransactionDao,
    private val assetDao: AssetDao,
    private val historyDao: PriceHistoryDao,
    private val quoteDao: PriceQuoteDao,
) {
    /** Portföyün TL değer ve getiri serisi. İşlem yoksa `null`. */
    suspend fun hesapla(donem: Donem, bugun: LocalDate): GrafikVerisi? {
        val islemler = transactionDao.getAll()
        if (islemler.isEmpty()) return null
        val yukleme = yukle(islemler, bugun)
        val enEski = islemler.minOf { it.tradeDate }
        val pencere = chartWindow(donem.baslangic(bugun, enEski), enEski, bugun)

        val noktalar = portfolioSeries(yukleme.alimlar, yukleme.fiyatlar, yukleme.kategoriler, pencere.start, bugun)
        val katkilar = katkilar(yukleme.alimlar, noktalar.firstOrNull()?.date ?: pencere.start)
        val getiri = returnSeries(noktalar, katkilar)
        return GrafikVerisi(
            donem = donem,
            pencere = pencere,
            noktalar = noktalar,
            getiri = getiri,
            toplamTl = kazanc(noktalar, katkilar),
            toplamYuzde = yuzde(noktalar, katkilar),
            tahmini = noktalar.any { it.estimated },
        )
    }

    /** Portföydeki her varlığın seçilen dönemdeki getirisi (yüzde ve TL). */
    suspend fun varlikGetirileri(donem: Donem, bugun: LocalDate): List<VarlikDonemGetirisi> =
        kategoriGetirileri(donem, bugun).flatMap { it.varliklar }

    /**
     * Kategori kırılımı: her kategorinin dönem getirisi ve altındaki varlıklar. Kategori yüzdesi varlık
     * yüzdelerinin ortalaması değildir; kategorinin varlıkları birlikte değerlenip aynı basit Dietz
     * formülüyle hesaplanır, böylece portföy toplamıyla tutarlı kalır.
     */
    suspend fun kategoriGetirileri(donem: Donem, bugun: LocalDate): List<KategoriDonemGetirisi> {
        val islemler = transactionDao.getAll()
        if (islemler.isEmpty()) return emptyList()
        val yukleme = yukle(islemler, bugun)
        val enEski = islemler.minOf { it.tradeDate }
        val baslangic = donem.baslangic(bugun, enEski)

        return yukleme.varliklar.groupBy { it.category }.map { (kategori, varliklar) ->
            val satirlar = varliklar.map { varlikGetirisi(it, yukleme, baslangic, bugun) }
            if (kategori == Category.NAKIT) {
                return@map KategoriDonemGetirisi(kategori, BigDecimal.ZERO, BigDecimal.ZERO, satirlar)
            }
            val kimlikler = varliklar.map { it.id }.toSet()
            val alimlar = yukleme.alimlar.filter { it.assetId in kimlikler }
            val noktalar = portfolioSeries(
                alimlar,
                yukleme.fiyatlar.filterKeys { it in kimlikler },
                yukleme.kategoriler,
                baslangic,
                bugun,
            )
            val katkilar = katkilar(alimlar, noktalar.firstOrNull()?.date ?: baslangic)
            KategoriDonemGetirisi(kategori, yuzde(noktalar, katkilar), kazanc(noktalar, katkilar), satirlar)
        }
    }

    private fun varlikGetirisi(
        varlik: Asset,
        yukleme: Yukleme,
        baslangic: LocalDate,
        bugun: LocalDate,
    ): VarlikDonemGetirisi {
        val alimlar = yukleme.alimlar.filter { it.assetId == varlik.id }
        if (varlik.category == Category.NAKIT) {
            return VarlikDonemGetirisi(varlik, BigDecimal.ZERO, BigDecimal.ZERO, giris(alimlar, baslangic))
        }
        val noktalar = portfolioSeries(
            alimlar,
            yukleme.fiyatlar.filterKeys { it == varlik.id },
            yukleme.kategoriler,
            baslangic,
            bugun,
        )
        val katkilar = katkilar(alimlar, noktalar.firstOrNull()?.date ?: baslangic)
        return VarlikDonemGetirisi(varlik, yuzde(noktalar, katkilar), kazanc(noktalar, katkilar), giris(alimlar, baslangic))
    }

    private fun giris(alimlar: List<HistoricalPurchase>, donemBasi: LocalDate): LocalDate? =
        alimlar.minOfOrNull { it.date }?.takeIf { it.isAfter(donemBasi) }

    /** Dönem içinde (başlangıç gününden sonra) yapılan alımlar: eklenen para getiri sayılmaz. */
    private fun katkilar(alimlar: List<HistoricalPurchase>, donemBasi: LocalDate) =
        alimlar.filter { it.date.isAfter(donemBasi) }.map { Contribution(it.date, it.costTl) }

    private fun kazanc(noktalar: List<DailyPoint>, katkilar: List<Contribution>): BigDecimal {
        if (noktalar.size < 2) return BigDecimal.ZERO
        val eklenen = katkilar.fold(BigDecimal.ZERO) { a, k -> a + k.amount }
        return noktalar.last().valueTl - noktalar.first().valueTl - eklenen
    }

    private fun yuzde(noktalar: List<DailyPoint>, katkilar: List<Contribution>): BigDecimal? {
        if (noktalar.size < 2) return null
        return periodReturn(noktalar.first().valueTl, noktalar.last().valueTl, katkilar, noktalar.first().date, noktalar.last().date)
    }

    private class Yukleme(
        val varliklar: List<Asset>,
        val alimlar: List<HistoricalPurchase>,
        val fiyatlar: Map<Long, List<Candle>>,
        val kategoriler: Map<Long, Category>,
    )

    private suspend fun yukle(islemler: List<TransactionEntity>, bugun: LocalDate): Yukleme {
        val varliklar = islemler.map { it.assetId }.distinct().mapNotNull { assetDao.getById(it)?.toModel() }
        val alimlar = islemler.map {
            HistoricalPurchase(it.assetId, it.tradeDate, it.quantity, it.unitPriceTl * it.quantity + it.commissionTl)
        }
        val enEski = islemler.minOf { it.tradeDate }

        val fiyatlar = varliklar.associate { varlik ->
            val seri = historyDao.range(varlik.id, enEski.minusDays(GERI_BAKIS), bugun).map { Candle(it.date, it.closeTl) }.toMutableList()

            // Geçmiş serisi olmayan (elle fiyatlı) varlıklarda elle girilen fiyatlar seri oluşturur: günün son kaydı.
            if (seri.isEmpty()) {
                quoteDao.forAsset(varlik.id)
                    .groupBy { it.timestamp.atZone(java.time.ZoneOffset.UTC).toLocalDate() }
                    .forEach { (gun, l) -> seri += Candle(gun, l.last().priceTl) }
            }
            // Bugünün noktası canlı fiyattır: grafiğin sonu Portföy ekranındaki güncel değerle aynı olur.
            quoteDao.latestFor(varlik.id)?.let { canli ->
                seri.removeAll { it.date == bugun }
                seri += Candle(bugun, canli.priceTl)
            }
            varlik.id to seri.toList()
        }
        return Yukleme(varliklar, alimlar, fiyatlar, varliklar.associate { it.id to it.category })
    }

    private companion object {
        /** İlk alıştan önceki hafta sonu için fiyat aranırken geriye bakılan gün sayısı. */
        const val GERI_BAKIS = 14L
    }
}

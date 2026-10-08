package com.portfoy.data.repository

import com.portfoy.calc.historyFetchRange
import com.portfoy.data.db.AssetEntity
import com.portfoy.data.db.FX_USDTRY_ID
import com.portfoy.data.db.PriceHistoryDao
import com.portfoy.data.db.PriceHistoryEntity
import com.portfoy.data.db.VarsayilanVarliklar
import com.portfoy.model.AssetRef
import com.portfoy.model.Candle
import com.portfoy.model.Category
import com.portfoy.network.HistoryRouter
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** Bir varlığın geçmiş seri çekiminin özeti. [hata] doluysa seri eksik kalmıştır; sonra yeniden denenir. */
data class GecmisSonucu(val eklenen: Int, val hata: Throwable? = null) {
    val basarili: Boolean get() = hata == null
}

/**
 * Geçmiş kapanış serilerini kaynaklardan çeker ve `price_history` tablosuna yazar.
 *
 * - Çekim **alış tarihinden** bugüne, en fazla 5 yıl geriye yapılır (doküman karar 3 ve 20).
 * - Zaten saklanan günler tekrar çekilmez: yalnızca eksik başlangıç ve son günler istenir. Bu yüzden her açılışta
 *   yeniden çekim olmaz (doküman 9.3/2).
 * - ABD ve Kripto fiyatları (ikisi de USD) **o günün kuruyla** TL'ye çevrilir; bugünkü kurla çarpmak TL'nin değer kaybı yüzünden geçmiş değerleri
 *   olduğundan yüksek gösterirdi. USD/TRY serisi tek sefer çekilir ([FX_USDTRY_ID]) ve tüm varlıklarca paylaşılır;
 *   portföye döviz olarak USD eklenirse o seri kopyalanır, yeniden çekilmez.
 * - Hafta sonu ve tatil günleri için kayıt yoktur; grafik hesabı son işlem gününün değeriyle düzleştirir.
 */
class HistoryRepository(
    private val router: HistoryRouter,
    private val historyDao: PriceHistoryDao,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    private fun bugun(): LocalDate = clock.instant().atZone(zone).toLocalDate()

    /** [asset] için, ilk alış tarihinden itibaren eksik olan geçmiş günleri tamamlar. */
    suspend fun ensure(asset: AssetEntity, firstBuy: LocalDate): GecmisSonucu {
        if (asset.category == Category.NAKIT) return GecmisSonucu(0)
        val bugun = bugun()
        val aralik = historyFetchRange(firstBuy, bugun)

        // Portföydeki USD, çevrim için zaten çekilen USD/TRY serisiyle birebir aynıdır: ağdan ikinci kez istenmez.
        if (asset.category == Category.DOVIZ && asset.code == VarsayilanVarliklar.DOLAR_KODU) {
            ensureFx(aralik.start, bugun).let { if (!it.basarili) return it }
            val satirlar = historyDao.range(FX_USDTRY_ID, aralik.start, bugun).map { it.copy(assetId = asset.id) }
            historyDao.upsertAll(satirlar)
            return GecmisSonucu(satirlar.size)
        }

        var kur: List<Candle> = emptyList()
        if (asset.category == Category.ABD || asset.category == Category.KRIPTO) {
            ensureFx(aralik.start, bugun).let { if (!it.basarili) return it }
            kur = historyDao.range(FX_USDTRY_ID, aralik.start.minusDays(KUR_GERI_BAKIS), bugun).map { Candle(it.date, it.close) }
        }

        val ref = AssetRef(asset.code, asset.category, asset.fundKind)
        return doldur(asset.id, ref, aralik.start, bugun) { mum ->
            when (asset.category) {
                Category.ABD, Category.KRIPTO -> sonBilinenKur(kur, mum.date)?.let { mum.close * it }
                else -> mum.close
            }
        }
    }

    /** USD/TRY günlük serisini tamamlar. */
    suspend fun ensureFx(from: LocalDate, to: LocalDate): GecmisSonucu =
        doldur(FX_USDTRY_ID, AssetRef.USDTRY, from, to) { it.close }

    private suspend fun doldur(
        id: Long,
        ref: AssetRef,
        baslangic: LocalDate,
        bitis: LocalDate,
        tlDegeri: (Candle) -> BigDecimal?,
    ): GecmisSonucu {
        var eklenen = 0
        for ((a, b) in eksikAraliklar(id, baslangic, bitis)) {
            val mumlar = router.getHistory(ref, a, b).getOrElse { return GecmisSonucu(eklenen, it) }
            val satirlar = mumlar.mapNotNull { m ->
                tlDegeri(m)?.let { tl -> PriceHistoryEntity(id, m.date, m.close, tl, KAYNAK) }
            }
            historyDao.upsertAll(satirlar)
            eklenen += satirlar.size
        }
        return GecmisSonucu(eklenen)
    }

    /** Saklanan seride eksik kalan başlangıç ve son günler. Hiç veri yoksa tüm aralık. */
    private suspend fun eksikAraliklar(id: Long, baslangic: LocalDate, bitis: LocalDate): List<Pair<LocalDate, LocalDate>> {
        val ilk = historyDao.firstDate(id)
        val son = historyDao.lastDate(id)
        if (ilk == null || son == null) return listOf(baslangic to bitis)

        val eksik = mutableListOf<Pair<LocalDate, LocalDate>>()
        // Başlangıç bir hafta sonuna denk gelebilir; küçük farklar eksik sayılmaz.
        if (ilk.isAfter(baslangic.plusDays(BASLANGIC_TOLERANSI))) eksik += baslangic to ilk.minusDays(1)
        if (son.isBefore(bitis.minusDays(1))) eksik += son.plusDays(1) to bitis
        return eksik
    }

    private fun sonBilinenKur(kur: List<Candle>, gun: LocalDate): BigDecimal? =
        kur.lastOrNull { !it.date.isAfter(gun) }?.close

    private companion object {
        const val KAYNAK = "GECMIS"
        const val BASLANGIC_TOLERANSI = 7L
        /** İlk mumdan önceki hafta sonu için kur aranırken geriye bakılan gün sayısı. */
        const val KUR_GERI_BAKIS = 10L
    }
}

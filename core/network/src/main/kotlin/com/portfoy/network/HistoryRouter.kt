package com.portfoy.network

import com.portfoy.model.AssetRef
import com.portfoy.model.Candle
import com.portfoy.model.Category
import com.portfoy.network.sources.gramSerisi
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** Günlük geçmiş seri çağrı sayacı: günde en fazla [cap] çağrı (doküman 9.3/3). Gün dönünce sıfırlanır. */
class GunlukSayac(private val cap: Int, private val clock: Clock, private val zone: ZoneId) {
    private var gun: LocalDate = bugun()
    private var kullanilan = 0

    private fun bugun() = clock.instant().atZone(zone).toLocalDate()

    @Synchronized
    fun tuket(): Boolean {
        val simdi = bugun()
        if (simdi != gun) {
            gun = simdi
            kullanilan = 0
        }
        if (kullanilan >= cap) return false
        kullanilan++
        return true
    }
}

/** Zincirdeki bir kaynak. [varlik] doluysa çağrıdaki varlık yerine bu sembol sorulur (ör. Yahoo'da `GC=F`). */
data class GecmisKaynagi(val kaynak: PriceSource, val varlik: AssetRef? = null)

enum class GecmisAnahtari { KUR, ABD, BIST, FON, ONS_ALTIN, ONS_GUMUS }

/**
 * Geçmiş kapanış serilerini kaynaklardan alır. Her tür için bir kaynak zinciri vardır; ilki boş ya da hatalı dönerse
 * sıradaki denenir (doküman 9.1):
 *
 * | Tür | Zincir |
 * |---|---|
 * | USD/TRY | EVDS → Yahoo |
 * | ABD | Twelve Data → Yahoo |
 * | BIST | Yahoo |
 * | Fon | TEFAS |
 * | Gram altın/gümüş | ons (Twelve Data → Yahoo) × USD/TRY geçmişi ÷ 31,1035 (Truncgil geçmiş vermez) |
 *
 * Kaynak çağrıları günlük geçmiş bütçesinden düşülür; bütçe yetmezse [BudgetExceededException] döner ve iş ertesi güne kalır.
 */
class HistoryRouter(
    private val zincirler: Map<GecmisAnahtari, List<GecmisKaynagi>>,
    private val sayac: GunlukSayac,
) {
    suspend fun getHistory(asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>> = when {
        asset == AssetRef.USDTRY -> zincir(GecmisAnahtari.KUR, asset, from, to)
        asset.category == Category.ABD -> zincir(GecmisAnahtari.ABD, asset, from, to)
        asset.category == Category.BIST -> zincir(GecmisAnahtari.BIST, asset, from, to)
        asset.category == Category.FON -> zincir(GecmisAnahtari.FON, asset, from, to)
        asset.category == Category.EMTIA -> emtia(asset, from, to)
        else -> Result.failure(UnsupportedOperationException("Geçmiş seri bu varlık için yok: ${asset.code}"))
    }

    /** Gram altın/gümüş: ons serisi ile USD/TRY serisinden türetilir. */
    private suspend fun emtia(asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>> {
        val anahtar = when (asset.code) {
            "XAUGR" -> GecmisAnahtari.ONS_ALTIN
            "XAGGR" -> GecmisAnahtari.ONS_GUMUS
            else -> return Result.failure(UnsupportedOperationException("Bilinmeyen emtia: ${asset.code}"))
        }
        val ons = zincir(anahtar, asset, from.minusDays(7), to).getOrElse { return Result.failure(it) }
        val kur = getHistory(AssetRef.USDTRY, from.minusDays(7), to).getOrElse { return Result.failure(it) }
        return Result.success(gramSerisi(ons, kur).filter { !it.date.isBefore(from) })
    }

    private suspend fun zincir(anahtar: GecmisAnahtari, asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>> {
        var sonHata: Throwable? = null
        for (halka in zincirler[anahtar].orEmpty()) {
            if (!sayac.tuket()) return Result.failure(BudgetExceededException("Günlük geçmiş seri bütçesi doldu"))
            val sonuc = halka.kaynak.getHistory(halka.varlik ?: asset, from, to)
            val mumlar = sonuc.getOrNull()
            if (mumlar != null && mumlar.isNotEmpty()) return Result.success(mumlar)
            sonHata = sonuc.exceptionOrNull() ?: BeklenmeyenYanitException("${halka.kaynak.id} boş seri döndürdü: ${asset.code}")
        }
        return Result.failure(AllSourcesFailedException("$anahtar için geçmiş seri alınamadı: ${asset.code}", sonHata))
    }
}

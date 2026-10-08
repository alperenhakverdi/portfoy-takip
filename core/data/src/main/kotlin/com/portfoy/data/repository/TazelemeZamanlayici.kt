package com.portfoy.data.repository

import com.portfoy.data.db.AssetEntity
import com.portfoy.model.Category
import com.portfoy.network.CallBudget
import com.portfoy.network.SourceRouter
import com.portfoy.network.market.AdaptiveInterval
import com.portfoy.network.market.Market
import com.portfoy.network.market.RefreshGroup
import com.portfoy.network.market.RefreshSchedule
import com.portfoy.network.market.RoundKind
import com.portfoy.network.market.Slot
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Her grubun son çalışma zamanı. Uygulamada kalıcıdır (SharedPreferences), testlerde bellekte tutulur. */
interface SonCalismaDeposu {
    fun al(grup: RefreshGroup): Instant?
    fun koy(grup: RefreshGroup, zaman: Instant)
}

class BellekSonCalisma : SonCalismaDeposu {
    private val harita = mutableMapOf<RefreshGroup, Instant>()
    override fun al(grup: RefreshGroup) = harita[grup]
    override fun koy(grup: RefreshGroup, zaman: Instant) { harita[grup] = zaman }
}

/**
 * Fiyat tazelemeyi piyasa olaylarına göre yürütür. Arka plan işi her 15 dakikada bir [calistir]'ı çağırır; hangi grubun
 * vadesinin geldiğine [RefreshSchedule] karar verir:
 *
 * - **ABD, BIST:** açılış + 2 dk, gün içi (adaptif aralık), kapanış + 15 dk. Kapanış turu kaçırılsa bile telafi edilir.
 * - **Fon (TEFAS):** günde bir kez, 21:15.
 * - **Altın/gümüş:** 08:00–22:00 arası 30 dakikada bir, hafta içi.
 * - **USD/TRY:** 09:00–19:00 arası saat başı, hafta içi.
 *
 * Gün içi aralık portföy büyüklüğüne göre kendiliğinden uzar (30 → 45 → 60 dk); bütçe yetmezse gün içi turlar kapanır ve
 * yalnızca açılış/kapanış turları yapılır. Portföy 30 varlığı aşarsa gün içi turlarda varlıklar dönüşümlü tazelenir; tur
 * başına çağrı 30'u geçmez (doküman 9.3/1). Açılış ve kapanış turları tüm varlıkları kapsar.
 */
class TazelemeZamanlayici(
    private val fiyatDeposu: PriceRepository,
    private val varliklar: Flow<List<AssetEntity>>,
    private val program: RefreshSchedule,
    private val sonCalisma: SonCalismaDeposu,
    private val clock: Clock,
) {
    /** Vadesi gelen grupları çalıştırır ve çalışanları döndürür. */
    suspend fun calistir(): List<RefreshGroup> {
        val simdi = clock.instant()
        val tum = varliklar.first()
        val calisan = mutableListOf<RefreshGroup>()

        for (grup in RefreshGroup.entries) {
            val grubunku = tum.filter { kategori(grup) == it.category }
            if (grup != RefreshGroup.FX && grubunku.isEmpty()) continue

            val plan = plan(grup, grubunku.size)
            val slot = program.dueSlot(grup, simdi, sonCalisma.al(grup), plan?.intervalMinutes) ?: continue

            tur(grup, slot, grubunku, plan?.rotationEvery ?: 1)
            sonCalisma.koy(grup, simdi)
            calisan += grup
        }
        return calisan
    }

    /** Gün içi plan; piyasası olmayan gruplarda `null` (aralık sabittir). */
    fun plan(grup: RefreshGroup, varlikSayisi: Int): com.portfoy.network.market.IntradayPlan? = gunIciPlan(grup, varlikSayisi)

    private suspend fun tur(grup: RefreshGroup, slot: Slot, grubunku: List<AssetEntity>, rotasyon: Int) {
        if (grup == RefreshGroup.FX) {
            // Çevrim kuru her turda tazelenir; portföyde döviz varlığı varsa aynı turda o da tazelenir.
            fiyatDeposu.refreshFx(PriceRepository.AUTO_MIN_AGE)
            if (grubunku.isNotEmpty()) fiyatDeposu.refresh(grubunku, PriceRepository.AUTO_MIN_AGE)
            return
        }
        val secilen = if (slot.kind == RoundKind.INTRADAY && grubunku.size > CallBudget.ROUND_CAP) {
            // Dönüşümlü: her turda sıradaki dilim tazelenir.
            val dilimler = grubunku.chunked(CallBudget.ROUND_CAP)
            val sira = ((slot.time.epochSecond / 60) % rotasyon.coerceAtLeast(1)).toInt() % dilimler.size
            dilimler[sira]
        } else {
            grubunku
        }
        // Tur başına en fazla 30 çağrı: büyük listeler ardışık parçalara bölünür.
        secilen.chunked(CallBudget.ROUND_CAP).forEach { fiyatDeposu.refresh(it, PriceRepository.AUTO_MIN_AGE) }
    }

    private fun kategori(grup: RefreshGroup): Category? = when (grup) {
        RefreshGroup.US -> Category.ABD
        RefreshGroup.BIST -> Category.BIST
        RefreshGroup.FUND -> Category.FON
        RefreshGroup.GOLD -> Category.EMTIA
        RefreshGroup.FX -> Category.DOVIZ
        RefreshGroup.KRIPTO -> Category.KRIPTO
    }

    companion object {
        /**
         * Gün içi plan; piyasası olmayan gruplarda `null`. Ekran da aynı kuralla "gün içi güncelleme kapalı" bilgisini gösterir,
         * bu yüzden kural tek yerde durur.
         */
        fun gunIciPlan(grup: RefreshGroup, varlikSayisi: Int): com.portfoy.network.market.IntradayPlan? {
            val (tavan, seans) = when (grup) {
                RefreshGroup.US -> 500 to seansDakikasi(Market.US)
                RefreshGroup.BIST -> 300 to seansDakikasi(Market.BIST)
                else -> return null
            }
            return AdaptiveInterval.choose(varlikSayisi, tavan, seans)
        }

        /** Bu kategoride, verilen varlık sayısıyla gün içi turlar kapalı mı? Piyasası olmayan kategoride `false`. */
        fun gunIciKapali(kategori: Category, varlikSayisi: Int): Boolean {
            val grup = when (kategori) {
                Category.ABD -> RefreshGroup.US
                Category.BIST -> RefreshGroup.BIST
                else -> return false
            }
            return gunIciPlan(grup, varlikSayisi)?.let { !it.enabled && varlikSayisi > 0 } == true
        }

        private fun seansDakikasi(market: Market): Int =
            Duration.between(market.open, market.close).toMinutes().toInt()
    }
}

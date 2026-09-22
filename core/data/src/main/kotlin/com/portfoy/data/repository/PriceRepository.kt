package com.portfoy.data.repository

import com.portfoy.data.db.AssetEntity
import com.portfoy.data.db.FX_USDTRY_ID
import com.portfoy.data.db.PriceQuoteDao
import com.portfoy.data.db.PriceQuoteEntity
import com.portfoy.model.AssetRef
import com.portfoy.model.Category
import com.portfoy.model.Quote
import com.portfoy.model.SourceId
import com.portfoy.network.RouteKey
import com.portfoy.network.RouteOutcome
import com.portfoy.network.SourceRouter
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Bir tazeleme turunun özeti. Kullanıcıya hata olarak gösterilmez; yalnızca "güncel değil" işareti içindir. */
data class RefreshReport(
    val updated: Int = 0,
    /** Yakın zamanda çekildiği için önbellekten okunan varlık sayısı. */
    val skippedFresh: Int = 0,
    /** Kaynaklardan fiyat alınamayan gruplar; bu gruplar son bilinen fiyatla gösterilir. */
    val failedGroups: Set<RouteKey> = emptySet(),
    /** Kaynak günlük bütçesi dolduğu için bu turda atlanan gruplar. */
    val budgetLimitedGroups: Set<RouteKey> = emptySet(),
    /** ABD varlıkları var ama hiç kur bilgisi yok; TL'ye çevrilemedi. */
    val fxMissing: Boolean = false,
) {
    val hasProblems: Boolean get() = failedGroups.isNotEmpty() || fxMissing
}

/**
 * Fiyatları kaynaklardan çeker ve `price_quote` tablosuna yazar.
 *
 * - Aynı varlık için [autoMinAge] içinde ikinci istek atılmaz; önbellekten okunur (doküman 9.3/5).
 *   Elle yenilemede (pull-to-refresh) bu süre [manualMinAge]'dir.
 * - ABD fiyatları güncel USD/TRY ile TL'ye çevrilir. `priceTl` çevrim anındaki kurla saklanır.
 * - Elle fiyat girilen (`MANUEL`) varlıklar ve nakit tazelemeye girmez.
 * - Kaynak başarısız olursa hiçbir şey silinmez; son bilinen fiyat geçerli kalır.
 */
class PriceRepository(
    private val router: SourceRouter,
    private val quoteDao: PriceQuoteDao,
    private val clock: Clock,
) {
    /** Her varlığın en yeni fiyatı, varlık kimliğine göre. */
    fun observeLatestPrices(): Flow<Map<Long, PriceQuoteEntity>> =
        quoteDao.observeLatest().map { list -> list.associateBy { it.assetId } }

    suspend fun refresh(assets: List<AssetEntity>, minAge: Duration = AUTO_MIN_AGE): RefreshReport {
        val now = clock.instant()
        var skipped = 0

        val due = assets.filter { asset ->
            if (asset.category == Category.NAKIT) return@filter false
            val latest = quoteDao.latestFor(asset.id)
            when {
                latest?.source == SourceId.MANUEL.name -> false // elle girilen fiyat otomatik güncellenmez
                latest != null && Duration.between(latest.timestamp, now) < minAge -> {
                    skipped++
                    false
                }
                else -> true
            }
        }

        val failed = mutableSetOf<RouteKey>()
        val limited = mutableSetOf<RouteKey>()
        var updated = 0

        // Kur önce: ABD fiyatlarının TL karşılığı buna bağlıdır.
        if (due.any { it.category == Category.ABD }) {
            val fxLatest = quoteDao.latestFor(FX_USDTRY_ID)
            if (fxLatest == null || Duration.between(fxLatest.timestamp, now) >= minAge) {
                when (val outcome = router.quotes(RouteKey.FX, listOf(AssetRef.USDTRY))) {
                    is RouteOutcome.Success -> outcome.quotes.forEach { store(FX_USDTRY_ID, it, BigDecimal.ONE) }
                    is RouteOutcome.Failed -> record(outcome, RouteKey.FX, failed, limited)
                }
            }
        }
        val usdTry = quoteDao.latestFor(FX_USDTRY_ID)?.priceTl
        var fxMissing = false

        for ((key, group) in due.groupBy { RouteKey.of(it.toRef()) }) {
            if (key == null) continue
            if (key == RouteKey.US && usdTry == null) {
                fxMissing = true
                continue
            }
            val byCode = group.associateBy { it.code }
            when (val outcome = router.quotes(key, group.map { it.toRef() })) {
                is RouteOutcome.Success -> outcome.quotes.forEach { quote ->
                    val asset = byCode[quote.code] ?: return@forEach
                    val rate = if (quote.currency == "USD") usdTry!! else BigDecimal.ONE
                    store(asset.id, quote, rate)
                    updated++
                }
                is RouteOutcome.Failed -> record(outcome, key, failed, limited)
            }
        }

        return RefreshReport(updated, skipped, failed, limited, fxMissing)
    }

    /**
     * Yalnızca USD/TRY kurunu tazeler (kur grubunun kendi zamanlaması vardır: saat başı). Kur, ABD varlıklarının TL
     * karşılığı için gerekir; portföyde ABD varlığı olmasa da geçmiş grafik için güncel tutulur.
     */
    suspend fun refreshFx(minAge: Duration = AUTO_MIN_AGE): RefreshReport {
        val failed = mutableSetOf<RouteKey>()
        val limited = mutableSetOf<RouteKey>()
        val son = quoteDao.latestFor(FX_USDTRY_ID)
        var guncellenen = 0
        if (son == null || Duration.between(son.timestamp, clock.instant()) >= minAge) {
            when (val sonuc = router.quotes(RouteKey.FX, listOf(AssetRef.USDTRY))) {
                is RouteOutcome.Success -> {
                    sonuc.quotes.forEach { store(FX_USDTRY_ID, it, BigDecimal.ONE) }
                    guncellenen = sonuc.quotes.size
                }
                is RouteOutcome.Failed -> record(sonuc, RouteKey.FX, failed, limited)
            }
        }
        return RefreshReport(updated = guncellenen, failedGroups = failed, budgetLimitedGroups = limited)
    }

    /** Kullanıcının elle girdiği fiyat. Her giriş yeni satırdır; zamanla bir seri oluşturur. */
    suspend fun setManualPrice(assetId: Long, priceTl: BigDecimal) {
        quoteDao.insert(
            PriceQuoteEntity(
                assetId = assetId,
                price = priceTl,
                currency = "TRY",
                priceTl = priceTl,
                timestamp = clock.instant(),
                source = SourceId.MANUEL.name,
            ),
        )
    }

    private suspend fun store(assetId: Long, quote: Quote, rateToTl: BigDecimal) {
        quoteDao.insert(
            PriceQuoteEntity(
                assetId = assetId,
                price = quote.price,
                currency = quote.currency,
                priceTl = quote.price * rateToTl,
                timestamp = quote.timestamp,
                source = quote.source.name,
                changePercent = quote.changePercent,
            ),
        )
    }

    private fun record(
        outcome: RouteOutcome.Failed,
        key: RouteKey,
        failed: MutableSet<RouteKey>,
        limited: MutableSet<RouteKey>,
    ) {
        if (outcome.error is com.portfoy.network.BudgetExceededException) limited += key else failed += key
    }

    companion object {
        val AUTO_MIN_AGE: Duration = Duration.ofMinutes(15)
        val MANUAL_MIN_AGE: Duration = Duration.ofMinutes(1)
    }
}

private fun AssetEntity.toRef() = AssetRef(code, category, fundKind)

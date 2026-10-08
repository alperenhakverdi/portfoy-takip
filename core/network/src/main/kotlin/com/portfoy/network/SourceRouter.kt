package com.portfoy.network

import com.portfoy.model.AssetRef
import com.portfoy.model.Category
import com.portfoy.model.Quote
import com.portfoy.model.SourceId
import java.time.Clock
import kotlinx.coroutines.delay

/** Hangi kaynak çiftinin hangi varlık türünü çekeceği. */
enum class RouteKey {
    US, BIST, FUND, COMMODITY, FX, CRYPTO;

    companion object {
        fun of(asset: AssetRef): RouteKey? = when (asset.category) {
            Category.ABD -> US
            Category.BIST -> BIST
            Category.FON -> FUND
            Category.EMTIA -> COMMODITY
            Category.DOVIZ -> FX // döviz varlığı, çevrim kuruyla aynı kaynaklardan gelir
            Category.NAKIT -> null // nakit için kaynak yok, fiyat sabit 1,00 ₺
            Category.KRIPTO -> CRYPTO
            null -> FX
        }
    }
}

/** Bir rota için birincil ve yedek kaynak. */
data class Route(val primary: PriceSource, val fallback: PriceSource? = null)

sealed interface RouteOutcome {
    data class Success(val quotes: List<Quote>, val source: SourceId, val usedFallback: Boolean) : RouteOutcome
    data class Failed(val error: Throwable) : RouteOutcome
}

/**
 * Fiyat isteklerini kaynaklara dağıtır:
 * - Birincil kaynak arka arkaya [retries] kez başarısız olursa yedeğe geçilir (doküman 9.1/2).
 * - Her istek önce günlük çağrı bütçesinden düşülür; bütçe yetmezse istek atılmaz.
 * - Başarı ve başarısızlıklar [SourceHealth]'e işlenir.
 */
class SourceRouter(
    private val routes: Map<RouteKey, Route>,
    private val budget: CallBudget,
    private val health: SourceHealth,
    private val clock: Clock,
    private val retries: Int = 3,
    private val retryDelayMillis: Long = 500,
    /** Kaynak hataları için isteğe bağlı günlük; kullanıcıya gösterilmez, tanı içindir. */
    private val hataGunlugu: ((String) -> Unit)? = null,
) {
    suspend fun quotes(key: RouteKey, assets: List<AssetRef>): RouteOutcome {
        val route = routes[key] ?: return RouteOutcome.Failed(IllegalStateException("$key için kaynak tanımlı değil"))
        if (assets.isEmpty()) return RouteOutcome.Success(emptyList(), route.primary.id, usedFallback = false)

        var lastError: Throwable? = null

        attempt(route.primary, assets)?.let { result ->
            result.onSuccess { return RouteOutcome.Success(it, route.primary.id, usedFallback = false) }
            lastError = result.exceptionOrNull()
        }

        val fallback = route.fallback
        if (fallback != null) {
            attempt(fallback, assets)?.let { result ->
                result.onSuccess { return RouteOutcome.Success(it, fallback.id, usedFallback = true) }
                lastError = result.exceptionOrNull()
            }
        }

        val error = lastError
        return RouteOutcome.Failed(
            if (error is BudgetExceededException) error
            else AllSourcesFailedException("$key için hiçbir kaynaktan fiyat alınamadı", error),
        )
    }

    /**
     * Kaynağı en fazla [retries] kez dener. Bütçe yetmiyorsa istek atmadan [BudgetExceededException]
     * ile döner; hiç denenmediyse (`null`) çağıran yedeğe geçer.
     */
    private suspend fun attempt(source: PriceSource, assets: List<AssetRef>): Result<List<Quote>>? {
        var last: Result<List<Quote>>? = null
        repeat(retries) { index ->
            val cost = source.requestCost(assets)
            if (!budget.tryConsume(source.id, cost)) {
                return Result.failure(BudgetExceededException("${source.id} günlük çağrı bütçesi doldu"))
            }
            val result = runCatching { source.getQuotes(assets).getOrThrow() }
            val now = clock.instant()
            if (result.isSuccess) {
                health.recordSuccess(source.id, now)
                return result
            }
            health.recordFailure(source.id, now)
            hataGunlugu?.invoke("${source.id} başarısız (deneme ${index + 1}/$retries, ${assets.size} varlık): ${result.exceptionOrNull()}")
            last = result
            if (index < retries - 1 && retryDelayMillis > 0) delay(retryDelayMillis)
        }
        return last
    }
}

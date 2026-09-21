package com.portfoy.network

import com.portfoy.model.AssetRef
import com.portfoy.model.Candle
import com.portfoy.model.Category
import com.portfoy.model.Quote
import com.portfoy.model.SourceId
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.sin

/**
 * Ağa çıkmayan, deterministik sahte kaynak. Wireframe ekranları ve testler bununla çalışır.
 * [failWhen], kaçıncı çağrıda hata döneceğini belirler (1'den başlar); testlerde yedeğe geçişi denemek içindir.
 */
class FakePriceSource(
    override val id: SourceId = SourceId.FAKE,
    private val clock: Clock = Clock.systemUTC(),
    private val failWhen: (callNumber: Int) -> Boolean = { false },
    private val costPerRequest: (assets: List<AssetRef>) -> Int = { 1 },
) : PriceSource {

    /** Bu kaynağa yapılan çağrı sayısı. */
    var calls: Int = 0
        private set

    override fun requestCost(assets: List<AssetRef>): Int = costPerRequest(assets)

    override suspend fun getQuotes(assets: List<AssetRef>): Result<List<Quote>> {
        calls++
        if (failWhen(calls)) return Result.failure(IOException("Sahte kaynak hatası (çağrı $calls)"))
        val now = clock.instant()
        return Result.success(
            assets.map { asset ->
                Quote(
                    code = asset.code,
                    price = basePrice(asset),
                    currency = if (asset.category == Category.ABD) "USD" else "TRY",
                    timestamp = now,
                    source = id,
                )
            },
        )
    }

    override suspend fun getHistory(asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>> {
        calls++
        if (failWhen(calls)) return Result.failure(IOException("Sahte kaynak hatası (çağrı $calls)"))
        val base = basePrice(asset)
        val days = ChronoUnit.DAYS.between(from, to).toInt()
        return Result.success(
            (0..days).map { offset ->
                val wave = 1.0 + 0.05 * sin(offset / 10.0)
                Candle(from.plusDays(offset.toLong()), base.multiply(BigDecimal(wave)).setScale(4, RoundingMode.HALF_UP))
            },
        )
    }

    private fun basePrice(asset: AssetRef): BigDecimal {
        if (asset == AssetRef.USDTRY) return BigDecimal("48.80")
        val seed = abs(asset.code.hashCode() % 900) + 10
        return BigDecimal(seed).setScale(2)
    }
}

package com.portfoy.network.sources

import com.portfoy.model.AssetRef
import com.portfoy.model.Candle
import com.portfoy.model.Category
import com.portfoy.model.Quote
import com.portfoy.model.SourceId
import com.portfoy.network.PriceSource
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

/** 1 troy ons = 31,1035 gram. */
val GRAM_PER_ONS: BigDecimal = BigDecimal("31.1035")

/**
 * Ons fiyat serisinden gram TL serisi türetir: `ons (USD) × USD/TRY ÷ 31,1035`.
 * Kur günlük seridir (hafta sonu ve tatilde değer yoktur); ons gününe kadar en son bilinen kur kullanılır.
 * Kuru hiç olmayan günler atlanır.
 */
fun gramSerisi(ons: List<Candle>, kur: List<Candle>): List<Candle> {
    val kurlar = kur.sortedBy { it.date }
    var i = 0
    var sonKur: BigDecimal? = null
    return ons.sortedBy { it.date }.mapNotNull { mum ->
        while (i < kurlar.size && !kurlar[i].date.isAfter(mum.date)) {
            sonKur = kurlar[i].close
            i++
        }
        sonKur?.let { Candle(mum.date, mum.close.multiply(it).divide(GRAM_PER_ONS, MathContext.DECIMAL64)) }
    }
}

/**
 * Gram altın/gümüş için yedek kaynak: Yahoo'dan ons vadeli fiyatı (`GC=F`, `SI=F`) ve USD/TRY alınır, gram TL'ye çevrilir.
 * Vadeli işlem fiyatı olduğu için spot fiyattan küçük fark olabilir; kabul edilir (doküman 9.1 yedek formülü).
 * Ana kaynak Truncgil'dir, o doğrudan gram TL fiyatı verir.
 */
class OnsEmtiaSource(private val yahoo: YahooSource) : PriceSource {
    override val id = SourceId.YAHOO

    override fun requestCost(assets: List<AssetRef>): Int = assets.size + 1 // ons fiyatları + kur

    override suspend fun getQuotes(assets: List<AssetRef>): Result<List<Quote>> = runCatching {
        val kur = yahoo.getQuotes(listOf(AssetRef.USDTRY)).getOrThrow().single()
        assets.map { asset ->
            val ons = yahoo.getQuotes(listOf(onsSembolu(asset))).getOrThrow().single()
            Quote(
                code = asset.code,
                price = ons.price.multiply(kur.price).divide(GRAM_PER_ONS, MathContext.DECIMAL64),
                currency = "TRY",
                timestamp = minOf(ons.timestamp, kur.timestamp),
                source = id,
            )
        }
    }

    override suspend fun getHistory(asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>> = runCatching {
        val ons = yahoo.getHistory(onsSembolu(asset), from.minusDays(7), to).getOrThrow()
        val kur = yahoo.getHistory(AssetRef.USDTRY, from.minusDays(7), to).getOrThrow()
        gramSerisi(ons, kur).filter { !it.date.isBefore(from) }
    }

    private fun onsSembolu(asset: AssetRef): AssetRef = when {
        asset.category == Category.EMTIA && asset.code == "XAUGR" -> AssetRef("GC=F", null)
        asset.category == Category.EMTIA && asset.code == "XAGGR" -> AssetRef("SI=F", null)
        else -> throw UnsupportedOperationException("Ons kaynağı bu varlığı desteklemiyor: ${asset.code}")
    }
}

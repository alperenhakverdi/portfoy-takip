package com.portfoy.network.sources

import com.portfoy.model.AssetRef
import com.portfoy.model.Candle
import com.portfoy.model.Category
import com.portfoy.model.Quote
import com.portfoy.model.SourceId
import com.portfoy.network.BeklenmeyenYanitException
import com.portfoy.network.Http
import com.portfoy.network.HttpIstemci
import com.portfoy.network.PriceSource
import com.portfoy.network.jsonCoz
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Yahoo Finance `chart` ucu: BIST (`THYAO.IS`), ABD, USD/TRY (`USDTRY=X`) ve ons altın/gümüş (`GC=F`, `SI=F`).
 *
 * Anahtar gerektirmez ama resmî bir API değildir: kullanım şartlarını ihlal edebilir ve sık bozulur
 * (doküman 9.4/4). Bu yüzden ana kaynak yerine yedek ve BIST için kullanılır, isteği seyrek tutulur.
 * Yahoo toplu sorgu sunmaz; her sembol ayrı istektir.
 */
class YahooSource(
    private val http: HttpIstemci,
    private val baseUrl: String = "https://query1.finance.yahoo.com",
) : PriceSource {
    override val id = SourceId.YAHOO

    override fun requestCost(assets: List<AssetRef>): Int = assets.size

    override suspend fun getQuotes(assets: List<AssetRef>): Result<List<Quote>> = runCatching {
        assets.map { asset ->
            val sembol = sembol(asset) ?: throw UnsupportedOperationException("Yahoo bu varlığı desteklemiyor: ${asset.code}")
            val meta = sonuc(sembol, "range=1d&interval=1d")["meta"]!!.jsonObject
            val fiyat = meta["regularMarketPrice"]?.jsonPrimitive?.doubleOrNull
                ?: throw BeklenmeyenYanitException("Yahoo fiyatı yok: $sembol")
            val oncekiKapanis = meta["chartPreviousClose"]?.jsonPrimitive?.doubleOrNull
            Quote(
                code = asset.code,
                price = BigDecimal.valueOf(fiyat),
                currency = meta["currency"]?.jsonPrimitive?.contentOrNull ?: "USD",
                timestamp = meta["regularMarketTime"]?.jsonPrimitive?.longOrNull?.let(Instant::ofEpochSecond) ?: Instant.now(),
                source = id,
                changePercent = gunlukDegisim(fiyat, oncekiKapanis),
            )
        }
    }

    override suspend fun getHistory(asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>> = runCatching {
        val sembol = sembol(asset) ?: throw UnsupportedOperationException("Yahoo bu varlığı desteklemiyor: ${asset.code}")
        val baslangic = from.atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        val bitis = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        candles(sonuc(sembol, "period1=$baslangic&period2=$bitis&interval=1d"))
    }

    /** Sembol eşlemesi. Ons fiyatı için doğrudan sembol kullanılır (kod `GC=F`, `SI=F`). */
    private fun sembol(asset: AssetRef): String? = when {
        asset.fxCurrency != null -> "${asset.fxCurrency}TRY=X"
        asset.category == Category.BIST -> "${asset.code}.IS"
        asset.category == Category.ABD -> asset.code.replace('.', '-') // BRK.B → BRK-B
        asset.category == Category.KRIPTO -> "${asset.code}-USD" // BTC → BTC-USD
        asset.category == null -> asset.code // ham sembol: GC=F, SI=F
        else -> null
    }

    private suspend fun sonuc(sembol: String, sorgu: String): JsonObject {
        val govde = http.get(
            "$baseUrl/v8/finance/chart/${java.net.URLEncoder.encode(sembol, "UTF-8")}?$sorgu",
            mapOf("User-Agent" to Http.TARAYICI),
        )
        val chart = jsonCoz(govde).jsonObject["chart"]?.jsonObject
            ?: throw BeklenmeyenYanitException("Yahoo yanıtında chart yok")
        val hata = chart["error"]
        if (hata != null && hata !is kotlinx.serialization.json.JsonNull) {
            throw BeklenmeyenYanitException("Yahoo hata döndürdü: $hata")
        }
        return chart["result"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw BeklenmeyenYanitException("Yahoo sonuç boş: $sembol")
    }

    private fun candles(sonuc: JsonObject): List<Candle> {
        val zamanlar = sonuc["timestamp"]?.jsonArray ?: return emptyList()
        val kapanislar = sonuc["indicators"]?.jsonObject?.get("quote")?.jsonArray?.firstOrNull()?.jsonObject?.get("close")?.jsonArray
            ?: throw BeklenmeyenYanitException("Yahoo kapanış serisi yok")
        val ofset = sonuc["meta"]?.jsonObject?.get("gmtoffset")?.jsonPrimitive?.longOrNull ?: 0L
        return zamanlar.indices.mapNotNull { i ->
            val kapanis = (kapanislar.getOrNull(i) as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
            val zaman = zamanlar[i].jsonPrimitive.longOrNull ?: return@mapNotNull null
            Candle(Instant.ofEpochSecond(zaman + ofset).atZone(ZoneOffset.UTC).toLocalDate(), BigDecimal.valueOf(kapanis))
        }.distinctBy { it.date }
    }
}

private fun JsonArray.getOrNull(i: Int) = if (i in indices) this[i] else null

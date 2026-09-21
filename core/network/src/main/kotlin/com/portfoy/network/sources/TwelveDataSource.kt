package com.portfoy.network.sources

import com.portfoy.model.AssetRef
import com.portfoy.model.Candle
import com.portfoy.model.Category
import com.portfoy.model.Quote
import com.portfoy.model.SourceId
import com.portfoy.network.BeklenmeyenYanitException
import com.portfoy.network.Http
import com.portfoy.network.HttpIstemci
import com.portfoy.network.IstekAraligi
import com.portfoy.network.PriceSource
import com.portfoy.network.jsonCoz
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Twelve Data: ABD geçmiş seri (`/time_series`) ve spot altın/gümüş ons geçmişi (`XAU/USD`, `XAG/USD`).
 *
 * Ücretsiz katman günde 800 kredi, dakikada 8 kredi verir; istekler bu yüzden en az 8 saniye arayla atılır.
 * Yanıt hata olsa da HTTP 200 dönebilir (`"status":"error"`), gövde kontrol edilir.
 * Anlık fiyat için de kullanılabilir (`/price`); ABD anlık fiyatında Finnhub'ın yedeğidir.
 */
class TwelveDataSource(
    private val http: HttpIstemci,
    private val apiKey: String,
    private val baseUrl: String = "https://api.twelvedata.com",
    private val aralik: IstekAraligi = IstekAraligi(Duration.ofSeconds(8)),
) : PriceSource {
    override val id = SourceId.TWELVE_DATA

    override fun requestCost(assets: List<AssetRef>): Int = assets.size

    override suspend fun getQuotes(assets: List<AssetRef>): Result<List<Quote>> = runCatching {
        assets.map { asset ->
            aralik.bekle()
            val o = govde("$baseUrl/price?symbol=${sembol(asset)}&apikey=$apiKey")
            val fiyat = o["price"]?.jsonPrimitive?.contentOrNull?.toBigDecimalOrNull()
                ?: throw BeklenmeyenYanitException("Twelve Data fiyatı yok: ${asset.code}")
            Quote(asset.code, fiyat, "USD", Instant.now(), id)
        }
    }

    override suspend fun getHistory(asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>> = runCatching {
        aralik.bekle()
        val o = govde(
            "$baseUrl/time_series?symbol=${sembol(asset)}&interval=1day&start_date=$from&end_date=$to&outputsize=5000&order=asc&apikey=$apiKey",
        )
        val degerler = o["values"]?.jsonArray ?: throw BeklenmeyenYanitException("Twelve Data seri yok: ${asset.code}")
        degerler.mapNotNull { e ->
            val v = e.jsonObject
            val tarih = v["datetime"]?.jsonPrimitive?.contentOrNull?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val kapanis = v["close"]?.jsonPrimitive?.contentOrNull?.toBigDecimalOrNull()
            if (tarih == null || kapanis == null) null else Candle(tarih, kapanis)
        }.sortedBy { it.date }
    }

    /** ABD hisseleri koduyla, ham semboller (`XAU/USD`) olduğu gibi sorulur. */
    private fun sembol(asset: AssetRef): String = when (asset.category) {
        Category.ABD, null -> asset.code
        else -> throw UnsupportedOperationException("Twelve Data bu varlığı desteklemiyor: ${asset.code}")
    }

    private suspend fun govde(url: String): kotlinx.serialization.json.JsonObject {
        val o = jsonCoz(http.get(url)).jsonObject
        if (o["status"]?.jsonPrimitive?.contentOrNull == "error") {
            throw BeklenmeyenYanitException("Twelve Data hatası ${o["code"]}: ${o["message"]?.jsonPrimitive?.contentOrNull?.take(120)}")
        }
        return o
    }
}

private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()

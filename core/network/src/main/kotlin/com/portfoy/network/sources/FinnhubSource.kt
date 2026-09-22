package com.portfoy.network.sources

import com.portfoy.model.AssetInfo
import com.portfoy.model.AssetRef
import com.portfoy.model.Candle
import com.portfoy.model.Category
import com.portfoy.model.Quote
import com.portfoy.model.SourceId
import com.portfoy.network.BeklenmeyenYanitException
import com.portfoy.network.Http
import com.portfoy.network.HttpIstemci
import com.portfoy.network.PriceSource
import com.portfoy.network.SymbolCatalogSource
import com.portfoy.network.jsonCoz
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * Finnhub: ABD anlık fiyat (`/quote`) ve ABD sembol listesi (`/stock/symbol`).
 *
 * Ücretsiz anahtar dakikada 60 çağrıya izin verir. Geçmiş mum ucu ücretsiz anahtarda 403 döner (doküman 9.4/3),
 * bu yüzden [getHistory] desteklenmez; geçmiş seri Twelve Data'dan alınır.
 * `/quote` tek sembol alır: toplu istek adaptör içinde sıralı çağrıya bölünür ve gerçek istek sayısı bildirilir.
 */
class FinnhubSource(
    private val http: HttpIstemci,
    private val apiKey: String,
    private val baseUrl: String = "https://finnhub.io/api/v1",
) : PriceSource, SymbolCatalogSource {
    override val id = SourceId.FINNHUB

    override fun requestCost(assets: List<AssetRef>): Int = assets.size

    override suspend fun getQuotes(assets: List<AssetRef>): Result<List<Quote>> = runCatching {
        assets.map { asset ->
            val govde = http.get("$baseUrl/quote?symbol=${asset.code}&token=$apiKey")
            val o = jsonCoz(govde).jsonObject
            val fiyat = o["c"]?.jsonPrimitive?.doubleOrNull ?: throw BeklenmeyenYanitException("Finnhub fiyatı yok: ${asset.code}")
            // Bilinmeyen sembolde Finnhub hata değil c=0 döndürür.
            if (fiyat <= 0.0) throw BeklenmeyenYanitException("Finnhub fiyat vermedi (bilinmeyen sembol?): ${asset.code}")
            // "dp" günlük değişim yüzdesini doğrudan verir; yoksa önceki kapanıştan (pc) hesaplanır.
            val degisim = o["dp"]?.jsonPrimitive?.doubleOrNull?.let { BigDecimal.valueOf(it) }
                ?: gunlukDegisim(fiyat, o["pc"]?.jsonPrimitive?.doubleOrNull)
            Quote(
                code = asset.code,
                price = BigDecimal.valueOf(fiyat),
                currency = "USD",
                timestamp = o["t"]?.jsonPrimitive?.longOrNull?.takeIf { it > 0 }?.let(Instant::ofEpochSecond) ?: Instant.now(),
                source = id,
                changePercent = degisim,
            )
        }
    }

    override suspend fun getHistory(asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>> =
        Result.failure(UnsupportedOperationException("Finnhub ücretsiz anahtarı geçmiş veri vermez"))

    /**
     * ABD sembol listesi. Uç, listeyi bir dosya adresine yönlendirir (OkHttp takip eder); dosya büyüktür
     * (~30 bin kayıt) ve indirmesi uzun sürer, bu yüzden ayda bir arka planda çekilir.
     */
    override suspend fun listSymbols(): Result<List<AssetInfo>> = runCatching {
        val govde = http.get("$baseUrl/stock/symbol?exchange=US&token=$apiKey")
        jsonCoz(govde).jsonArray.mapNotNull { e ->
            val o = e.jsonObject
            val tur = o["type"]?.jsonPrimitive?.contentOrNull
            val sembol = o["symbol"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            if (tur !in KABUL_EDILEN_TURLER || sembol.isBlank() || sembol.length > 12) return@mapNotNull null
            AssetInfo(
                code = sembol,
                name = o["description"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: sembol,
                category = Category.ABD,
                currency = "USD",
                exchange = o["mic"]?.jsonPrimitive?.contentOrNull,
            )
        }.distinctBy { it.code }
    }

    private companion object {
        val KABUL_EDILEN_TURLER = setOf("Common Stock", "ETP", "ADR", "REIT")
    }
}

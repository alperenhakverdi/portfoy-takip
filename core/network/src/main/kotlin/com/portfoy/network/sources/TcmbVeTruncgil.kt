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
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val ISTANBUL: ZoneId = ZoneId.of("Europe/Istanbul")

private fun ortalama(alis: Double, satis: Double): BigDecimal =
    BigDecimal.valueOf((alis + satis) / 2.0).setScale(4, RoundingMode.HALF_UP)

/**
 * Truncgil Finans ücretsiz API'si (`/v4/today.json`): gram altın, gram gümüş ve USD/TRY **anlık**, tek çağrıda.
 * Geçmiş vermez. Alış ve satış ortalaması alınır. `ONS` alanı sıfır döndürüyor, kullanılmaz.
 */
class TruncgilSource(
    private val http: HttpIstemci,
    private val baseUrl: String = "https://finans.truncgil.com",
) : PriceSource {
    override val id = SourceId.TRUNCGIL

    override suspend fun getQuotes(assets: List<AssetRef>): Result<List<Quote>> = runCatching {
        val kok = jsonCoz(http.get("$baseUrl/v4/today.json")).jsonObject
        val zaman = kok["Update_Date"]?.jsonPrimitive?.contentOrNull
            ?.let { runCatching { LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(ISTANBUL).toInstant() }.getOrNull() }
            ?: Instant.now()

        assets.map { asset ->
            val anahtar = anahtar(asset) ?: throw UnsupportedOperationException("Truncgil bu varlığı desteklemiyor: ${asset.code}")
            val o = kok[anahtar]?.jsonObject ?: throw BeklenmeyenYanitException("Truncgil'de $anahtar yok")
            val alis = o["Buying"]?.jsonPrimitive?.doubleOrNull
            val satis = o["Selling"]?.jsonPrimitive?.doubleOrNull
            if (alis == null || satis == null || alis <= 0 || satis <= 0) throw BeklenmeyenYanitException("Truncgil $anahtar fiyatı geçersiz")
            Quote(asset.code, ortalama(alis, satis), "TRY", zaman, id)
        }
    }

    override suspend fun getHistory(asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>> =
        Result.failure(UnsupportedOperationException("Truncgil geçmiş veri vermez"))

    private fun anahtar(asset: AssetRef): String? = when {
        asset == AssetRef.USDTRY -> "USD"
        asset.category == Category.EMTIA && asset.code == "XAUGR" -> "GRA"
        asset.category == Category.EMTIA && asset.code == "XAGGR" -> "GUMUS"
        else -> null
    }
}

/**
 * TCMB günlük kur dosyası (`kurlar/today.xml`): resmî USD/TRY, anahtarsız. Günde bir kez yayınlanır;
 * gün içi çevrim için Truncgil ana kaynaktır, bu dosya yedektir.
 */
class TcmbGunlukSource(
    private val http: HttpIstemci,
    private val baseUrl: String = "https://www.tcmb.gov.tr",
) : PriceSource {
    override val id = SourceId.TCMB_DAILY

    override suspend fun getQuotes(assets: List<AssetRef>): Result<List<Quote>> = runCatching {
        assets.map { asset ->
            if (asset != AssetRef.USDTRY) throw UnsupportedOperationException("TCMB günlük kur yalnızca USD/TRY verir")
            val xml = http.get("$baseUrl/kurlar/today.xml")
            val blok = Regex("""<Currency [^>]*Kod="USD"[^>]*>(.*?)</Currency>""", RegexOption.DOT_MATCHES_ALL).find(xml)?.groupValues?.get(1)
                ?: throw BeklenmeyenYanitException("TCMB dosyasında USD yok")
            fun deger(etiket: String) = Regex("<$etiket>([^<]+)</$etiket>").find(blok)?.groupValues?.get(1)?.toDoubleOrNull()
            val alis = deger("ForexBuying")
            val satis = deger("ForexSelling")
            if (alis == null || satis == null) throw BeklenmeyenYanitException("TCMB USD alış/satış okunamadı")
            Quote(asset.code, ortalama(alis, satis), "TRY", Instant.now(), id)
        }
    }

    override suspend fun getHistory(asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>> =
        Result.failure(UnsupportedOperationException("Geçmiş kur için EVDS kullanılır"))
}

/**
 * TCMB EVDS3: günlük resmî USD/TRY geçmişi (`TP.DK.USD.A.YTL`). Ücretsiz anahtar gerekir ve **HTTP başlığında**
 * (`key`) gönderilir, adreste değil. Parametreler `?` olmadan doğrudan yola eklenir; tarih `GG-AA-YYYY`.
 * Hafta sonu ve tatil günlerinde değer `null` gelir, atlanır. Eski `evds2` adresleri kapandı (doküman 9.4/2).
 */
class EvdsSource(
    private val http: HttpIstemci,
    private val apiKey: String,
    private val baseUrl: String = "https://evds3.tcmb.gov.tr",
) : PriceSource {
    override val id = SourceId.TCMB_EVDS

    override suspend fun getQuotes(assets: List<AssetRef>): Result<List<Quote>> =
        Result.failure(UnsupportedOperationException("EVDS günlük seri verir, anlık fiyat için Truncgil kullanılır"))

    override suspend fun getHistory(asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>> = runCatching {
        if (asset != AssetRef.USDTRY) throw UnsupportedOperationException("EVDS bu varlığı desteklemiyor: ${asset.code}")
        val bicim = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val govde = http.get(
            "$baseUrl/igmevdsms-dis/series=TP.DK.USD.A.YTL&startDate=${from.format(bicim)}&endDate=${to.format(bicim)}&type=json",
            mapOf("key" to apiKey),
        )
        val o = try {
            jsonCoz(govde).jsonObject
        } catch (e: BeklenmeyenYanitException) {
            // Hatalı anahtarda düz metin ("Invalid API Key") döner.
            throw BeklenmeyenYanitException("EVDS hata döndürdü: ${govde.take(80)}", e)
        }
        val items = o["items"]?.jsonArray ?: throw BeklenmeyenYanitException("EVDS yanıtında items yok")
        items.mapNotNull { e ->
            val satir = e.jsonObject
            val tarih = satir["Tarih"]?.jsonPrimitive?.contentOrNull?.let { runCatching { LocalDate.parse(it, bicim) }.getOrNull() }
            val deger = satir["TP_DK_USD_A_YTL"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.contentOrNull?.let { runCatching { BigDecimal(it) }.getOrNull() }
            if (tarih == null || deger == null) null else Candle(tarih, deger)
        }.sortedBy { it.date }
    }
}

package com.portfoy.network

import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Sunucu başarısız (4xx/5xx) yanıt verdi. */
class HttpException(val code: Int, url: String, val govde: String = "") :
    IOException("HTTP $code: ${url.substringBefore('?').take(120)}")

/** Yanıt beklenen biçimde değil (kaynağın arayüzü değişmiş olabilir). */
class BeklenmeyenYanitException(mesaj: String, neden: Throwable? = null) : IOException(mesaj, neden)

/**
 * Tüm kaynak adaptörlerinin kullandığı ince HTTP katmanı. Ağ çağrıları IO dizisinde çalışır ve iptal edilebilir.
 */
/** Adaptörlerin bağlandığı HTTP arayüzü; testlerde gerçek ağ yerine kayıtlı yanıtlar döndüren bir sahte ile değiştirilir. */
interface HttpIstemci {
    suspend fun get(url: String, basliklar: Map<String, String> = emptyMap()): String

    suspend fun postJson(url: String, govde: String, basliklar: Map<String, String> = emptyMap()): String
}

class Http(private val client: OkHttpClient = varsayilanIstemci()) : HttpIstemci {

    override suspend fun get(url: String, basliklar: Map<String, String>): String =
        calistir(Request.Builder().url(url).apply { basliklar.forEach { (k, v) -> header(k, v) } }.build())

    override suspend fun postJson(url: String, govde: String, basliklar: Map<String, String>): String =
        calistir(
            Request.Builder().url(url)
                .apply { basliklar.forEach { (k, v) -> header(k, v) } }
                .post(govde.toRequestBody("application/json".toMediaType()))
                .build(),
        )

    private suspend fun calistir(istek: Request): String = withContext(Dispatchers.IO) {
        client.newCall(istek).execute().use { yanit ->
            val metin = yanit.body.string()
            if (!yanit.isSuccessful) throw HttpException(yanit.code, istek.url.toString(), metin.take(300))
            metin
        }
    }

    companion object {
        /** Resmî olmayan uçlar (Yahoo) tarayıcı kimliği olmadan reddedebilir. */
        const val TARAYICI = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"

        fun varsayilanIstemci(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build()
    }
}

/** Katı ayrıştırma: JSON olmayan yanıt (engelleme sayfası, düz metin hata) net bir hata verir. */
internal val jsonAyrac = Json { ignoreUnknownKeys = true }

/**
 * Yanıtı ayrıştırır. Kaynakların hepsi JSON nesnesi ya da dizisi döndürür; kütüphane, `<html>...` ya da
 * `Invalid API Key` gibi JSON olmayan metni de "değişmez değer" saydığı için bu açıkça reddedilir.
 */
internal fun jsonCoz(metin: String): JsonElement {
    val eleman = try {
        jsonAyrac.parseToJsonElement(metin)
    } catch (e: Exception) {
        throw BeklenmeyenYanitException("Yanıt JSON değil: ${metin.take(80)}", e)
    }
    if (eleman !is kotlinx.serialization.json.JsonObject && eleman !is kotlinx.serialization.json.JsonArray) {
        throw BeklenmeyenYanitException("Yanıt JSON nesnesi ya da dizisi değil: ${metin.take(80)}")
    }
    return eleman
}

/**
 * İstekler arasında en az [aralik] bekleten sayaç. Ücretsiz katmanların dakikalık limitleri için
 * (Twelve Data dakikada 8, TEFAS dakikada 6).
 */
class IstekAraligi(private val aralik: Duration) {
    private val kilit = Mutex()
    private var son: Long = 0

    suspend fun bekle() = kilit.withLock {
        val gecen = System.nanoTime() - son
        val gerekli = aralik.toNanos() - gecen
        if (son != 0L && gerekli > 0) delay(TimeUnit.NANOSECONDS.toMillis(gerekli) + 1)
        son = System.nanoTime()
    }
}

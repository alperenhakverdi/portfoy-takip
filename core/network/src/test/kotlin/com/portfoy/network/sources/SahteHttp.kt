package com.portfoy.network.sources

import com.portfoy.network.HttpIstemci

/** Gerçek ağa çıkmayan HTTP: kayıtlı yanıtlar döndürür ve yapılan istekleri saklar. */
class SahteHttp(private val yanit: (url: String, govde: String?) -> String) : HttpIstemci {

    data class Istek(val yontem: String, val url: String, val basliklar: Map<String, String>, val govde: String?)

    val istekler = mutableListOf<Istek>()

    override suspend fun get(url: String, basliklar: Map<String, String>): String {
        istekler += Istek("GET", url, basliklar, null)
        return yanit(url, null)
    }

    override suspend fun postJson(url: String, govde: String, basliklar: Map<String, String>): String {
        istekler += Istek("POST", url, basliklar, govde)
        return yanit(url, govde)
    }

    companion object {
        /** `src/test/resources/fixtures` altındaki, gerçek API yanıtlarından üretilmiş dosya. */
        fun fixture(ad: String): String =
            SahteHttp::class.java.getResource("/fixtures/$ad")?.readText() ?: error("Fixture yok: $ad")

        /** Her isteğe aynı yanıtı verir. */
        fun sabit(metin: String) = SahteHttp { _, _ -> metin }

        /** Fixture'daki ilk `regularMarketPrice` değeri (Yahoo `meta` bloğu). */
        fun yahooFiyati(fixture: String): String =
            Regex("\"regularMarketPrice\":([0-9.]+)").find(fixture)?.groupValues?.get(1) ?: error("fiyat yok")
    }
}

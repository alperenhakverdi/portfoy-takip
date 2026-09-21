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
import com.portfoy.network.IstekAraligi
import com.portfoy.network.PriceSource
import com.portfoy.network.SymbolCatalogSource
import com.portfoy.network.jsonCoz
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** TEFAS'ın bir fon için bir günlük kaydı. */
data class TefasSatiri(val kod: String, val ad: String, val tarih: LocalDate, val fiyat: BigDecimal)

/**
 * TEFAS'ın kendi ucu: `POST /api/funds/fonGnlBlgSiraliGetir`. Anahtar gerektirmez.
 *
 * - `fonKodu` boşsa o günün **tüm** fonları tek çağrıda gelir (~2000 kayıt): hem güncel fiyat hem sembol listesi.
 * - `fonKodu` verilirse fonun geçmişi gelir. Sunucu tek istekte en fazla 1 ay ve en fazla 5 yıl geriye izin verir;
 *   bu yüzden geçmiş 28 günlük parçalarla çekilir. Oran sınırı dakikada ~6 istek olduğu için istekler arası 10 sn beklenir.
 * - Başlıklar (Origin, Referer, tarayıcı kimliği) olmadan uç 404 döner.
 * - Site Nisan 2026'da baştan yazıldı ve bot koruması eklendi (doküman 9.4/1); uç değişirse yalnızca bu sınıf etkilenir.
 */
class TefasSource(
    private val http: HttpIstemci,
    private val baseUrl: String = "https://www.tefas.gov.tr",
    private val bugun: () -> LocalDate = { LocalDate.now(ISTANBUL) },
    private val aralik: IstekAraligi = IstekAraligi(Duration.ofSeconds(10)),
) : PriceSource, SymbolCatalogSource {
    override val id = SourceId.TEFAS

    override fun requestCost(assets: List<AssetRef>): Int = assets.mapNotNull { it.fundKind ?: VARSAYILAN_TUR }.distinct().size

    override suspend fun getQuotes(assets: List<AssetRef>): Result<List<Quote>> = runCatching {
        val istenen = assets.associateBy { it.code }
        val sonuc = mutableListOf<Quote>()
        for (tur in assets.map { it.fundKind ?: VARSAYILAN_TUR }.distinct()) {
            val satirlar = sonIsGunuSatirlari(tur)
            satirlar.filter { it.kod in istenen }.forEach { s ->
                sonuc += Quote(s.kod, s.fiyat, "TRY", s.tarih.atTime(LocalTime.of(21, 0)).atZone(ISTANBUL).toInstant(), id)
            }
        }
        sonuc
    }

    override suspend fun getHistory(asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>> = runCatching {
        val tur = asset.fundKind ?: VARSAYILAN_TUR
        val sonuc = mutableListOf<Candle>()
        var parcaBasi = from
        while (!parcaBasi.isAfter(to)) {
            val parcaSonu = minOf(parcaBasi.plusDays(PARCA_GUN - 1), to)
            istek(tur, asset.code, parcaBasi, parcaSonu).forEach { sonuc += Candle(it.tarih, it.fiyat) }
            parcaBasi = parcaSonu.plusDays(1)
        }
        sonuc.distinctBy { it.date }.sortedBy { it.date }
    }

    /** Fon listesi: fonun kodu, adı ve türüyle. Fiyat çekimiyle aynı çağrıdan gelir. */
    override suspend fun listSymbols(): Result<List<AssetInfo>> = runCatching {
        TURLER.flatMap { tur ->
            sonIsGunuSatirlari(tur).map { AssetInfo(it.kod, it.ad, Category.FON, "TRY", "TEFAS", tur) }
        }.distinctBy { it.code }
    }

    /** Bugünden geriye, veri dönen ilk iş gününü bulur (hafta sonu ve tatilde fiyat açıklanmaz). */
    private suspend fun sonIsGunuSatirlari(tur: String): List<TefasSatiri> {
        var gun = bugun()
        repeat(GERI_BAKIS_GUN) {
            if (gun.dayOfWeek != DayOfWeek.SATURDAY && gun.dayOfWeek != DayOfWeek.SUNDAY) {
                val satirlar = istek(tur, null, gun, gun)
                if (satirlar.isNotEmpty()) return satirlar
            }
            gun = gun.minusDays(1)
        }
        throw BeklenmeyenYanitException("TEFAS son $GERI_BAKIS_GUN günde veri vermedi ($tur)")
    }

    private suspend fun istek(tur: String, kod: String?, bas: LocalDate, bit: LocalDate): List<TefasSatiri> {
        aralik.bekle()
        val kodJson = if (kod == null) "null" else "\"$kod\""
        val govde = """{"fonTipi":"$tur","fonKodu":$kodJson,"aramaMetni":null,"fonTurKod":null,"fonGrubu":null,"sfonTurKod":null,""" +
            """"fonTurAciklama":null,"kurucuKod":null,"basTarih":"${bas.format(GUN)}","bitTarih":"${bit.format(GUN)}",""" +
            """"basSira":1,"bitSira":100000,"dil":"TR","sFonTurKod":"","fonKod":"","fonGrup":"","fonUnvanTip":""}"""
        val yanit = jsonCoz(
            http.postJson(
                "$baseUrl/api/funds/fonGnlBlgSiraliGetir",
                govde,
                mapOf(
                    "Origin" to baseUrl,
                    "Referer" to "$baseUrl/tr/fon-verileri",
                    "User-Agent" to Http.TARAYICI,
                    "Accept" to "*/*",
                ),
            ),
        ).jsonObject

        val hata = yanit["errorMessage"]?.jsonPrimitive?.contentOrNull
        val liste = yanit["resultList"]
        if (liste == null || liste is kotlinx.serialization.json.JsonNull) {
            // Sunucu, aralıkta veri yoksa da hata mesajı döndürür; gerçek bir sınır ihlali ise tekrar denenmez.
            if (hata != null && hata.contains("aşamaz", ignoreCase = true) || hata?.contains("eski olamaz", ignoreCase = true) == true) {
                throw BeklenmeyenYanitException("TEFAS istek sınırı: $hata")
            }
            return emptyList()
        }
        return liste.jsonArray.mapNotNull { e ->
            val o = e.jsonObject
            val fiyat = o["fiyat"]?.jsonPrimitive?.contentOrNull?.let { runCatching { BigDecimal(it) }.getOrNull() }
            val tarih = o["tarih"]?.jsonPrimitive?.contentOrNull?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
            val kodu = o["fonKodu"]?.jsonPrimitive?.contentOrNull
            if (fiyat == null || tarih == null || kodu == null) null
            else TefasSatiri(kodu, o["fonUnvan"]?.jsonPrimitive?.contentOrNull ?: kodu, tarih, fiyat)
        }
    }

    companion object {
        private val ISTANBUL: ZoneId = ZoneId.of("Europe/Istanbul")
        private val GUN: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        const val VARSAYILAN_TUR = "YAT"

        /** Arama listesine alınan fon türleri: yatırım fonları ve borsa yatırım fonları. */
        val TURLER = listOf("YAT", "BYF")

        /** Tek istekte en fazla 1 ay istenebilir; güvenli parça uzunluğu. */
        const val PARCA_GUN = 28L

        private const val GERI_BAKIS_GUN = 7
    }
}

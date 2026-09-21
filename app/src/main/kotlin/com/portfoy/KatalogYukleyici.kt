package com.portfoy

import android.content.Context
import com.portfoy.calc.normalizeForSearch
import com.portfoy.data.db.AssetDao
import com.portfoy.data.db.AssetEntity
import com.portfoy.model.Category
import com.portfoy.model.UnitType
import org.json.JSONArray

/**
 * Arama listesini (katalog) veritabanına yükler. Liste pakete gömülüdür; ilk açılışta indirme yapılmaz,
 * böylece internetsiz ilk açılışta da arama çalışır.
 *
 * BIST listesi gerçektir (`assets/bist_symbols.json`). ABD ve fon listeleri wireframe için küçük örneklerdir;
 * gerçek listeler (Finnhub, TEFAS) M5'te aynı tabloya yüklenir, aynı kod ve kategori zaten varsa atlanır.
 */
object KatalogYukleyici {

    suspend fun yukle(context: Context, dao: AssetDao) {
        if (dao.countByCategory(Category.BIST) == 0) dao.insertAll(bist(context))
        if (dao.countByCategory(Category.ABD) == 0) dao.insertAll(ornekAbd())
        if (dao.countByCategory(Category.FON) == 0) dao.insertAll(ornekFon())
    }

    private fun bist(context: Context): List<AssetEntity> {
        val json = context.assets.open("bist_symbols.json").bufferedReader().use { it.readText() }
        val dizi = JSONArray(json)
        return (0 until dizi.length()).map { i ->
            val o = dizi.getJSONObject(i)
            varlik(o.getString("code"), o.getString("name"), Category.BIST, "TRY", UnitType.ADET, "BIST")
        }
    }

    private fun ornekAbd() = listOf(
        "AAPL" to "Apple Inc", "MSFT" to "Microsoft Corp", "GOOGL" to "Alphabet Inc Class A",
        "AMZN" to "Amazon.com Inc", "NVDA" to "NVIDIA Corp", "META" to "Meta Platforms Inc",
        "TSLA" to "Tesla Inc", "VOO" to "Vanguard S&P 500 ETF", "QQQ" to "Invesco QQQ Trust",
        "SPY" to "SPDR S&P 500 ETF Trust", "NFLX" to "Netflix Inc", "AMD" to "Advanced Micro Devices Inc",
    ).map { (kod, ad) -> varlik(kod, ad, Category.ABD, "USD", UnitType.ADET, "NASDAQ/NYSE") }

    private fun ornekFon() = listOf(
        "AFA" to "Ak Portföy Amerikan Hisse Senedi Fonu", "TTE" to "Türkiye İş Bankası Teknoloji Fonu",
        "IPJ" to "İş Portföy Elektrikli Araçlar Karma Fon", "AAL" to "Ata Portföy Para Piyasası (TL) Fonu",
    ).map { (kod, ad) -> varlik(kod, ad, Category.FON, "TRY", UnitType.PAY, "TEFAS").copy(fundKind = "YAT") }

    private fun varlik(code: String, name: String, category: Category, currency: String, unit: UnitType, exchange: String) =
        AssetEntity(
            code = code,
            name = name,
            category = category,
            currency = currency,
            unitType = unit,
            exchange = exchange,
            searchText = normalizeForSearch("$code $name"),
        )
}

package com.portfoy

import android.content.Context
import com.portfoy.data.repository.KatalogDeposu
import com.portfoy.model.AssetInfo
import com.portfoy.model.Category
import org.json.JSONArray

/**
 * Arama listesini (katalog) pakete gömülü dosyalardan veritabanına yükler. İlk açılışta indirme yapılmaz, böylece
 * internetsiz ilk açılışta da arama çalışır. Kategori doluysa dokunulmaz; güncel liste aylık tazelemeyle gelir.
 *
 * - `bist_symbols.json`: Borsa İstanbul (elle hazırlanmış, ücretsiz toplu liste yok; yılda bir gözden geçirilir).
 * - `us_symbols.json`: ABD hisse, ETF, ADR ve REIT'leri (Finnhub'tan üretildi), her kayıt `[kod, ad, borsa]`.
 * - `fon_symbols.json`: TEFAS yatırım ve borsa yatırım fonları (TEFAS'tan üretildi), her kayıt `[kod, ad, tür]`.
 */
object KatalogYukleyici {

    suspend fun yukle(context: Context, katalog: KatalogDeposu) {
        suspend fun olc(ad: String, kategori: Category, oku: () -> List<AssetInfo>) {
            val t0 = System.currentTimeMillis()
            val liste = oku()
            val t1 = System.currentTimeMillis()
            val sonuc = katalog.ilkYukleme(kategori, liste)
            android.util.Log.i("Portfoy", "Katalog $ad: ${liste.size} kayıt, okuma ${t1 - t0} ms, yazma ${System.currentTimeMillis() - t1} ms (eklenen ${sonuc.eklenen})")
        }
        olc("BIST", Category.BIST) { bist(context) }
        olc("ABD", Category.ABD) { abd(context) }
        olc("FON", Category.FON) { fon(context) }
    }

    private fun oku(context: Context, dosya: String): JSONArray =
        JSONArray(context.assets.open(dosya).bufferedReader().use { it.readText() })

    private fun bist(context: Context): List<AssetInfo> {
        val dizi = oku(context, "bist_symbols.json")
        return (0 until dizi.length()).map { i ->
            val o = dizi.getJSONObject(i)
            AssetInfo(o.getString("code"), o.getString("name"), Category.BIST, "TRY", "BIST")
        }
    }

    private fun abd(context: Context): List<AssetInfo> {
        val dizi = oku(context, "us_symbols.json")
        return (0 until dizi.length()).map { i ->
            val s = dizi.getJSONArray(i)
            AssetInfo(s.getString(0), s.getString(1), Category.ABD, "USD", s.optString(2).ifBlank { null })
        }
    }

    private fun fon(context: Context): List<AssetInfo> {
        val dizi = oku(context, "fon_symbols.json")
        return (0 until dizi.length()).map { i ->
            val s = dizi.getJSONArray(i)
            AssetInfo(s.getString(0), s.getString(1), Category.FON, "TRY", "TEFAS", s.getString(2))
        }
    }
}

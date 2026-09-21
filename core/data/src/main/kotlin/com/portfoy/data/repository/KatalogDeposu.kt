package com.portfoy.data.repository

import androidx.room.withTransaction
import com.portfoy.calc.normalizeForSearch
import com.portfoy.data.db.PortfoyDatabase
import com.portfoy.model.AssetInfo
import com.portfoy.model.Category
import com.portfoy.model.UnitType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/** Katalog yüklemesinin özeti. */
data class KatalogSonucu(val eklenen: Int, val guncellenen: Int)

/**
 * Arama listesini (katalog) veritabanına yazar. Katalog kaynağı gömülü dosyalar ve aylık tazeleme (Finnhub, TEFAS)dır.
 *
 * Tazelemede mevcut varlıkların **kimliği korunur**: ad değiştiyse güncellenir, yeni olan eklenir, hiçbir kayıt
 * silinmez. Silip yeniden eklemek işlem kayıtlarını (yabancı anahtar, `CASCADE`) yok ederdi.
 *
 * Yeni kayıtlar Room'un `@Insert` metoduyla değil, hazır bir SQL ifadesiyle eklenir: Room'un liste eklemesi emülatörde
 * satır başına ~2,3 ms sürdü (27 bin kayıt için 72 sn). Ayrıca yazma kısa işlemlere bölünür; tek uzun işlem,
 * Portföy ekranının okumalarını yazma bitene kadar beklettiği için ilk açılışta ekran "Yükleniyor…"da kalıyordu.
 */
class KatalogDeposu(private val db: PortfoyDatabase) {

    /** Kategori boşsa hızlı toplu ekleme; doluysa hiçbir şey yapmaz. Gömülü listelerin ilk yüklemesi içindir. */
    suspend fun ilkYukleme(category: Category, bilgiler: List<AssetInfo>): KatalogSonucu {
        if (db.assetDao().countByCategory(category) > 0) return KatalogSonucu(0, 0)
        return guncelle(bilgiler)
    }

    /** Var olanları güncelleyip yenileri ekler. Kesilirse yeniden çalıştırmak güvenlidir (tekrarlar atlanır). */
    suspend fun guncelle(bilgiler: List<AssetInfo>): KatalogSonucu {
        val dao = db.assetDao()
        var eklenen = 0
        var guncellenen = 0
        for ((kategori, grup) in bilgiler.groupBy { it.category }) {
            val mevcut = dao.codeNames(kategori).associate { it.code to it.name }

            val yeniler = grup.filter { it.code !in mevcut }.distinctBy { it.code }
            topluEkle(yeniler)
            eklenen += yeniler.size

            val degisenler = grup.filter { mevcut[it.code] != null && mevcut[it.code] != it.name }
            degisenler.chunked(PARCA).forEach { parca ->
                db.withTransaction {
                    parca.forEach { b ->
                        dao.updateInfo(b.code, kategori, b.name, normalizeForSearch("${b.code} ${b.name}"), b.exchange, b.fundKind)
                    }
                }
                yield()
            }
            guncellenen += degisenler.size
        }
        return KatalogSonucu(eklenen, guncellenen)
    }

    /**
     * Hazır ifadeyle, [PARCA] kayıtlık kısa işlemler halinde ekler; aralarda bekleyen okumalara yol verilir.
     * Aynı (kod, kategori) zaten varsa atlanır.
     */
    private suspend fun topluEkle(bilgiler: List<AssetInfo>) {
        if (bilgiler.isEmpty()) return
        val sql = db.openHelper.writableDatabase
        for (parca in bilgiler.chunked(PARCA)) {
            withContext(Dispatchers.IO) {
                sql.beginTransaction()
                try {
                    val ifade = sql.compileStatement(
                        "INSERT OR IGNORE INTO asset (code, name, category, currency, unitType, exchange, active, fundKind, searchText) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?)",
                    )
                    for (b in parca) {
                        ifade.clearBindings()
                        ifade.bindString(1, b.code)
                        ifade.bindString(2, b.name)
                        ifade.bindString(3, b.category.name)
                        ifade.bindString(4, b.currency)
                        ifade.bindString(5, birimi(b.category).name)
                        b.exchange?.let { ifade.bindString(6, it) }
                        b.fundKind?.let { ifade.bindString(7, it) }
                        ifade.bindString(8, normalizeForSearch("${b.code} ${b.name}"))
                        ifade.executeInsert()
                    }
                    sql.setTransactionSuccessful()
                } finally {
                    sql.endTransaction()
                }
            }
            yield()
        }
        // Ham SQL Room'un kendi işlem hattından geçmediği için gözlemcilere değişiklik ayrıca bildirilir.
        db.invalidationTracker.refreshVersionsAsync()
    }

    private fun birimi(category: Category): UnitType = when (category) {
        Category.FON -> UnitType.PAY
        Category.EMTIA -> UnitType.GRAM
        Category.DOVIZ -> UnitType.BIRIM
        Category.NAKIT -> UnitType.TL
        else -> UnitType.ADET
    }

    private companion object {
        /** Bir işlemde eklenen kayıt sayısı: yazma hızı ile ekranın bekleme süresi arasında denge. */
        const val PARCA = 500
    }
}

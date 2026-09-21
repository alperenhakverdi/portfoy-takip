package com.portfoy.data.repository

import com.portfoy.calc.Donem
import com.portfoy.data.db.PortfolioSnapshotDao
import com.portfoy.data.db.PortfolioSnapshotEntity
import com.portfoy.model.Category
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Günlük portföy kaydı (snapshot): her gün için toplam değer, maliyet ve kategori dağılımı. Hem doğrulama hem hızlı
 * çizim içindir (doküman 11.2/2). Günde bir kayıttır ve hacmi düşüktür, sınırsız saklanır.
 *
 * Kayıt **kapsadığı güne** yazılır, tetiklenme anına değil: uygulama günlerce açılmadıysa eksik günler geriye dönük
 * tamamlanır ("uzun süre açılmadı" durumu). Fiyat geçmişi henüz çekilmemiş (maliyetle değerlenmiş) günler yazılmaz;
 * yanlış bir değer saklamak yerine geçmiş tamamlanınca yazılır.
 */
class SnapshotDeposu(
    private val grafik: GrafikDeposu,
    private val snapshotDao: PortfolioSnapshotDao,
) {
    /** [bugun]'den önceki, henüz kaydı olmayan günleri yazar. Yazılan gün sayısını döndürür. */
    suspend fun eksikGunleriYaz(bugun: LocalDate): Int {
        val veri = grafik.hesapla(Donem.TUMU, bugun) ?: return 0
        val sonKayit = snapshotDao.latest()?.date

        val yazilacak = veri.noktalar.filter { !it.estimated && it.date.isBefore(bugun) && (sonKayit == null || it.date.isAfter(sonKayit)) }
        yazilacak.forEach { n ->
            snapshotDao.upsert(PortfolioSnapshotEntity(n.date, n.valueTl, n.costTl, dagilimJson(n.byCategory)))
        }
        return yazilacak.size
    }

    /** Kategori → TL değeri, örn. `{"ABD":"1500.25","BIST":"800"}`. */
    private fun dagilimJson(kategoriler: Map<Category, BigDecimal>): String =
        kategoriler.entries.sortedBy { it.key.ordinal }
            .joinToString(prefix = "{", postfix = "}", separator = ",") { (k, v) -> "\"${k.name}\":\"${v.toPlainString()}\"" }
}

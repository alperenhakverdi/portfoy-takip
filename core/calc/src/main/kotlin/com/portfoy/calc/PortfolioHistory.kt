package com.portfoy.calc

import com.portfoy.model.Category
import java.math.BigDecimal
import java.time.LocalDate

/** Geçmiş hesaplamaya giren bir alım: hangi varlık, ne zaman, kaç adet, toplam maliyeti (komisyon dahil). */
data class HistoricalPurchase(
    val assetId: Long,
    val date: LocalDate,
    val quantity: BigDecimal,
    val costTl: BigDecimal,
)

/** Geçmiş hesabın bir günü. */
data class DailyPoint(
    val date: LocalDate,
    val valueTl: BigDecimal,
    /** O güne kadar ödenen toplam maliyet. */
    val costTl: BigDecimal,
    val byCategory: Map<Category, BigDecimal>,
    /** O gün fiyatı bilinmeyip maliyetle değerlenen varlık var mı. */
    val estimated: Boolean,
)

/**
 * Portföyün geçmiş günlerdeki değerini işlem kayıtları ve varlıkların geçmiş fiyat serilerinden hesaplar
 * ("geriye dönük hesaplama": uygulama kurulmadan önceki dönem için de grafik çizilebilir).
 *
 * - Bir gün için varlığın adedi, o güne (dahil) kadar yapılan alımların toplamıdır.
 * - Fiyat, o güne kadarki **son bilinen** kapanıştır: hafta sonu ve tatil günleri son işlem gününün değeriyle
 *   düzleştirilir. Nakit TL'nin fiyatı her zaman 1'dir.
 * - Varlığın hiç fiyatı yoksa (seri henüz çekilmedi, elle girilen fiyat) birim maliyetiyle değerlenir ve gün
 *   [DailyPoint.estimated] olarak işaretlenir; grafikte o dönem düz görünür, uydurma bir hareket üretilmez.
 * - Alış tarihinden önceki günlerde varlık yoktur, o varlık o güne katkı vermez.
 *
 * @param prices varlık kimliği → TL kapanış serisi (tarihe göre sıralı olması gerekmez)
 */
fun portfolioSeries(
    purchases: List<HistoricalPurchase>,
    prices: Map<Long, List<com.portfoy.model.Candle>>,
    categories: Map<Long, Category>,
    from: LocalDate,
    to: LocalDate,
): List<DailyPoint> {
    if (to.isBefore(from) || purchases.isEmpty()) return emptyList()

    val alimlar = purchases.groupBy { it.assetId }.mapValues { (_, l) -> l.sortedBy { it.date } }
    val seriler = prices.mapValues { (_, l) -> l.sortedBy { it.date } }
    val imlecler = mutableMapOf<Long, Int>() // her varlık için seride ilerleyen konum

    val sonuc = mutableListOf<DailyPoint>()
    var gun = from
    while (!gun.isAfter(to)) {
        var toplamDeger = BigDecimal.ZERO
        var toplamMaliyet = BigDecimal.ZERO
        val kategoriler = mutableMapOf<Category, BigDecimal>()
        var tahmini = false

        for ((varlik, liste) in alimlar) {
            val elde = liste.filter { !it.date.isAfter(gun) }
            if (elde.isEmpty()) continue
            val adet = elde.fold(BigDecimal.ZERO) { a, p -> a + p.quantity }
            val maliyet = elde.fold(BigDecimal.ZERO) { a, p -> a + p.costTl }
            val kategori = categories[varlik] ?: continue

            val fiyat: BigDecimal? = when (kategori) {
                Category.NAKIT -> BigDecimal.ONE
                else -> sonBilinenFiyat(seriler[varlik], imlecler, varlik, gun)
            }
            val deger = if (fiyat != null) fiyat * adet else {
                tahmini = true
                maliyet // fiyat yok: maliyetiyle değerlenir
            }
            toplamDeger += deger
            toplamMaliyet += maliyet
            kategoriler.merge(kategori, deger, BigDecimal::add)
        }
        if (toplamMaliyet.signum() > 0) sonuc += DailyPoint(gun, toplamDeger, toplamMaliyet, kategoriler, tahmini)
        gun = gun.plusDays(1)
    }
    return sonuc
}

private fun sonBilinenFiyat(
    seri: List<com.portfoy.model.Candle>?,
    imlecler: MutableMap<Long, Int>,
    varlik: Long,
    gun: LocalDate,
): BigDecimal? {
    if (seri.isNullOrEmpty()) return null
    var i = imlecler[varlik] ?: 0
    while (i < seri.size && !seri[i].date.isAfter(gun)) i++
    imlecler[varlik] = i
    return if (i == 0) null else seri[i - 1].close
}

/**
 * Getiri grafiği için, dönem başından her güne kadar birikimli getiri yüzdesi (basit Dietz). Dönem içinde eklenen
 * para getiri sayılmaz ve dönemde kaldığı gün oranınca paydaya girer ([periodReturn]).
 *
 * @param points [portfolioSeries] çıktısı; ilk nokta dönem başı değerdir
 * @param contributions dönem içindeki alımlar (dönem başlangıç gününde ya da öncesinde olanlar başlangıç değerindedir)
 */
fun returnSeries(points: List<DailyPoint>, contributions: List<Contribution>): List<Pair<LocalDate, BigDecimal>> {
    if (points.isEmpty()) return emptyList()
    val baslangic = points.first()
    return points.map { nokta ->
        val yuzde = periodReturn(baslangic.valueTl, nokta.valueTl, contributions, baslangic.date, nokta.date)
        nokta.date to (yuzde ?: BigDecimal.ZERO)
    }
}

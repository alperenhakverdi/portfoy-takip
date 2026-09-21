package com.portfoy.calc

import com.portfoy.model.Category
import java.math.BigDecimal
import java.math.RoundingMode

/** Dağılım grafiğindeki bir dilim. [percent] 2 haneye yuvarlanmıştır. */
data class AllocationSlice(val category: Category, val value: BigDecimal, val percent: BigDecimal)

/**
 * Kategori yüzdelerini güncel TL değerlerinden hesaplar.
 *
 * - Değeri sıfır olan kategori dilim üretmez ("%0" satırı yazılmaz).
 * - Yüzdeler 2 haneye yuvarlanır; toplam yuvarlama yüzünden 100'ü aşarsa fark en büyük dilimden
 *   düşülür. Toplam 100'ün altında kalırsa aynı dilime eklenir; böylece toplam her zaman %100'dür.
 * - Dilimler değere göre büyükten küçüğe sıralıdır.
 */
fun allocate(categoryValues: Map<Category, BigDecimal>): List<AllocationSlice> {
    val positive = categoryValues.filterValues { it.signum() > 0 }
    if (positive.isEmpty()) return emptyList()

    val total = positive.values.fold(BigDecimal.ZERO, BigDecimal::add)
    val sorted = positive.entries.sortedWith(
        compareByDescending<Map.Entry<Category, BigDecimal>> { it.value }.thenBy { it.key.ordinal },
    )

    val slices = sorted.map { (category, value) ->
        val percent = value.dividedBy(total).multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP)
        AllocationSlice(category, value, percent)
    }.toMutableList()

    val sum = slices.fold(BigDecimal.ZERO) { acc, s -> acc + s.percent }
    val difference = HUNDRED.setScale(2) - sum
    if (difference.signum() != 0) {
        val largest = slices[0]
        slices[0] = largest.copy(percent = largest.percent + difference)
    }
    return slices
}

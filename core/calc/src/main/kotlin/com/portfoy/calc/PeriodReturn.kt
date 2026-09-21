package com.portfoy.calc

import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Dönem içinde portföye eklenen para (alımın toplam maliyeti, komisyon dahil). */
data class Contribution(val date: LocalDate, val amount: BigDecimal)

/**
 * Dönemsel getiri, basit Dietz yöntemiyle. Dönem içinde eklenen para getiri sayılmaz ve dönemde
 * kaldığı gün oranınca paydaya girer:
 *
 * ```
 *              V_bitiş − V_başlangıç − ΣC
 * Getiri % = ───────────────────────────────────────── × 100
 *             V_başlangıç + Σ(C_i × kalan_gün_i / dönem_gün)
 * ```
 *
 * Son gün yapılan büyük bir alım, düz formülün aksine getiriyi seyreltmez. Dönem başlangıç tarihinde
 * ya da öncesinde yapılan alımlar zaten başlangıç değerindedir ve katkı sayılmaz.
 * Bir katkı yapıldığı gün de piyasada sayılır: son gün → 1/dönem_gün, dönemin ilk günü → 1.
 *
 * Payda sıfır ya da negatifse (dönem başı değer yok ve katkı yok) `null` döner.
 */
fun periodReturn(
    startValue: BigDecimal,
    endValue: BigDecimal,
    contributions: List<Contribution>,
    periodStart: LocalDate,
    periodEnd: LocalDate,
): BigDecimal? {
    val periodDays = ChronoUnit.DAYS.between(periodStart, periodEnd)
    if (periodDays <= 0) return null

    val inPeriod = contributions.filter { it.date.isAfter(periodStart) && !it.date.isAfter(periodEnd) }
    val totalContribution = inPeriod.fold(BigDecimal.ZERO) { acc, c -> acc + c.amount }
    val weightedContribution = inPeriod.fold(BigDecimal.ZERO) { acc, c ->
        val remainingDays = ChronoUnit.DAYS.between(c.date, periodEnd) + 1
        acc + (c.amount * BigDecimal(remainingDays)).dividedBy(BigDecimal(periodDays))
    }

    val denominator = startValue + weightedContribution
    if (denominator.signum() <= 0) return null
    return (endValue - startValue - totalContribution).dividedBy(denominator) * HUNDRED
}

package com.portfoy.calc

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Grafiğin gerçekte çizeceği aralık. [truncated] ise ekranda bir not gösterilir. */
data class ChartWindow(
    val start: LocalDate,
    val end: LocalDate,
    val truncated: Boolean,
    /** "portföy geçmişi X gün" notundaki X. */
    val portfolioDays: Long,
)

/**
 * Seçilen dönem portföy yaşından uzunsa grafik yalnızca mevcut veri kadar çizilir.
 * Alış tarihinden önceki dönem çizilmez.
 */
fun chartWindow(requestedStart: LocalDate, oldestTransaction: LocalDate, today: LocalDate): ChartWindow {
    val truncated = oldestTransaction.isAfter(requestedStart)
    val start = if (truncated) oldestTransaction else requestedStart
    return ChartWindow(start, today, truncated, ChronoUnit.DAYS.between(oldestTransaction, today))
}

/** Geçmiş fiyat serisi en fazla bu kadar yıl geriye çekilir ve saklanır. */
const val HISTORY_YEARS = 5L

/** Geçmiş seri çekiminin aralığı. [truncated] ise alış tarihi 5 yıldan eskidir ve ekranda not gösterilir. */
data class HistoryFetchRange(val start: LocalDate, val end: LocalDate, val truncated: Boolean)

/** Geçmiş seri çekiminin başlangıcı: alış tarihi, ama en fazla [HISTORY_YEARS] yıl geriye. */
fun historyFetchRange(buyDate: LocalDate, today: LocalDate): HistoryFetchRange {
    val limit = today.minusYears(HISTORY_YEARS)
    val truncated = buyDate.isBefore(limit)
    return HistoryFetchRange(if (truncated) limit else buyDate, today, truncated)
}

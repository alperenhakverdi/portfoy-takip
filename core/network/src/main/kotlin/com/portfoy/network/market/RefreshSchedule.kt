package com.portfoy.network.market

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Tazeleme grupları: her birinin kendi ritmi ve takvimi vardır. */
enum class RefreshGroup { US, BIST, FUND, GOLD, FX }

enum class RoundKind { OPEN, INTRADAY, CLOSE, DAILY }

data class Slot(val time: Instant, val kind: RoundKind)

/**
 * Tazeleme turlarının ne zaman yapılacağı. Sabit "15 dakikada bir" yerine piyasa olaylarına bağlıdır:
 *
 * | Grup | Turlar |
 * |---|---|
 * | ABD, BIST | açılış + 2 dk, gün içi (adaptif aralık), kapanış + 15 dk |
 * | Fon (TEFAS) | günde 1 kez, 21:15 (fiyat 21:00'de açıklanır) |
 * | Altın/gümüş | 08:00–22:00 arası 30 dk, hafta içi |
 * | USD/TRY | 09:00–19:00 arası saat başı, hafta içi (TCMB saat başı yayınlar) |
 *
 * Kapanış (ve fon) turu **kaçırılırsa telafi edilir**: uygulama akşam açılsa bile o günün kapanış turu
 * yapılır. Gün içi turlar telafi edilmez; birden fazla tur kaçmışsa yalnızca sonuncusu çalışır.
 */
class RefreshSchedule(private val calendar: MarketCalendar = MarketCalendar()) {

    private val istanbul = ZoneId.of("Europe/Istanbul")

    fun slots(group: RefreshGroup, date: LocalDate, intradayMinutes: Int? = 30): List<Slot> = when (group) {
        RefreshGroup.US -> marketSlots(Market.US, date, intradayMinutes)
        RefreshGroup.BIST -> marketSlots(Market.BIST, date, intradayMinutes)
        RefreshGroup.FUND ->
            if (calendar.isTradingDay(Market.BIST, date)) listOf(Slot(at(date, LocalTime.of(21, 15)), RoundKind.DAILY))
            else emptyList()
        RefreshGroup.GOLD ->
            if (MarketCalendar.isWeekend(date)) emptyList()
            else generateSequence(LocalTime.of(8, 0)) { it.plusMinutes(30) }
                .takeWhile { !it.isAfter(LocalTime.of(22, 0)) }
                .map { Slot(at(date, it), RoundKind.INTRADAY) }.toList()
        RefreshGroup.FX ->
            if (!calendar.isTradingDay(Market.BIST, date)) emptyList()
            else (9..19).map { Slot(at(date, LocalTime.of(it, 0)), RoundKind.INTRADAY) }
    }

    /**
     * [now] itibarıyla çalışması gereken tur, yoksa `null`. [lastRun] grubun son başarılı turudur.
     * Gün, grubun kendi zaman diliminde belirlenir.
     */
    fun dueSlot(group: RefreshGroup, now: Instant, lastRun: Instant?, intradayMinutes: Int? = 30): Slot? {
        val date = now.atZone(zoneOf(group)).toLocalDate()
        return slots(group, date, intradayMinutes)
            .filter { !it.time.isAfter(now) && (lastRun == null || lastRun.isBefore(it.time)) }
            .maxByOrNull { it.time }
    }

    /** Sonraki planlı tur (bugünden başlayarak en fazla 10 gün ileriye bakar). */
    fun nextSlot(group: RefreshGroup, now: Instant, intradayMinutes: Int? = 30): Slot? {
        var date = now.atZone(zoneOf(group)).toLocalDate()
        repeat(10) {
            slots(group, date, intradayMinutes).firstOrNull { it.time.isAfter(now) }?.let { return it }
            date = date.plusDays(1)
        }
        return null
    }

    private fun marketSlots(market: Market, date: LocalDate, intradayMinutes: Int?): List<Slot> {
        val session = calendar.session(market, date) ?: return emptyList()
        val first = session.open.plus(Duration.ofMinutes(AdaptiveInterval.OPEN_OFFSET_MINUTES.toLong()))
        val result = mutableListOf(Slot(first, RoundKind.OPEN))
        if (intradayMinutes != null) {
            var t = first.plus(Duration.ofMinutes(intradayMinutes.toLong()))
            while (t.isBefore(session.close)) {
                result += Slot(t, RoundKind.INTRADAY)
                t = t.plus(Duration.ofMinutes(intradayMinutes.toLong()))
            }
        }
        result += Slot(session.close.plus(Duration.ofMinutes(15)), RoundKind.CLOSE)
        return result
    }

    private fun zoneOf(group: RefreshGroup): ZoneId = when (group) {
        RefreshGroup.US -> Market.US.zone
        else -> istanbul
    }

    private fun at(date: LocalDate, time: LocalTime): Instant = ZonedDateTime.of(date, time, istanbul).toInstant()
}

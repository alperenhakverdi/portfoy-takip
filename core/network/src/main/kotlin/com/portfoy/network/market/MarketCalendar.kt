package com.portfoy.network.market

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Piyasa seansları kendi zaman diliminde tanımlanır; TSİ karşılığı çalışma anında hesaplanır.
 * ABD yaz saati uyguladığı, Türkiye ise kalıcı UTC+3 kullandığı için TSİ saati hiçbir yere sabit yazılmaz.
 */
enum class Market(val zone: ZoneId, val open: LocalTime, val close: LocalTime) {
    US(ZoneId.of("America/New_York"), LocalTime.of(9, 30), LocalTime.of(16, 0)),
    BIST(ZoneId.of("Europe/Istanbul"), LocalTime.of(10, 0), LocalTime.of(18, 0)),
}

/** Bir işlem gününün açılış ve kapanış anları. */
data class Session(val open: Instant, val close: Instant)

/**
 * İki ayrı tatil takvimi: ABD ve Türkiye farklı günlerde kapalıdır (4 Temmuz'da ABD kapalı BIST açık,
 * 29 Ekim'de tersi). TEFAS Türkiye takvimini kullanır. Altın/gümüş ve kur için yalnızca hafta sonu
 * kuralı uygulanır.
 *
 * Yarım günler (arife, Şükran Günü sonrası) desteklenmez: o günlerde kapanış turu geç kalır ama
 * kapanış fiyatı yine doğru gelir.
 */
class MarketCalendar(private val holidays: Map<Market, Set<LocalDate>> = Holidays.ALL) {

    fun isTradingDay(market: Market, date: LocalDate): Boolean =
        !isWeekend(date) && date !in holidays[market].orEmpty()

    fun session(market: Market, date: LocalDate): Session? {
        if (!isTradingDay(market, date)) return null
        return Session(
            open = ZonedDateTime.of(date, market.open, market.zone).toInstant(),
            close = ZonedDateTime.of(date, market.close, market.zone).toInstant(),
        )
    }

    fun isOpen(market: Market, now: Instant): Boolean {
        val date = now.atZone(market.zone).toLocalDate()
        val session = session(market, date) ?: return false
        return !now.isBefore(session.open) && now.isBefore(session.close)
    }

    /** Takvimin bu yılı kapsayıp kapsamadığı; kapsamıyorsa yıllık güncelleme gerekir. */
    fun coversYear(market: Market, year: Int): Boolean =
        holidays[market].orEmpty().any { it.year == year }

    companion object {
        fun isWeekend(date: LocalDate): Boolean =
            date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    }
}

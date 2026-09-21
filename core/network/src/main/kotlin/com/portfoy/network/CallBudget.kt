package com.portfoy.network

import com.portfoy.model.SourceId
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * Kaynak başına günlük çağrı bütçesi. Ücretsiz katman limitlerinin altında kalan kendi tavanlarımız
 * burada uygulanır; gün dönünce sayaçlar sıfırlanır.
 */
class CallBudget(
    private val dailyCaps: Map<SourceId, Int>,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    private var day: LocalDate = today()
    private val used = mutableMapOf<SourceId, Int>()

    private fun today(): LocalDate = clock.instant().atZone(zone).toLocalDate()

    private fun rollover() {
        val now = today()
        if (now != day) {
            day = now
            used.clear()
        }
    }

    /** Tavanı tanımlı olmayan kaynak sınırsız sayılır (ör. gömülü ya da elle giriş). */
    @Synchronized
    fun remaining(source: SourceId): Int {
        rollover()
        val cap = dailyCaps[source] ?: return Int.MAX_VALUE
        return (cap - (used[source] ?: 0)).coerceAtLeast(0)
    }

    @Synchronized
    fun tryConsume(source: SourceId, cost: Int): Boolean {
        rollover()
        val cap = dailyCaps[source] ?: return true
        val current = used[source] ?: 0
        if (current + cost > cap) return false
        used[source] = current + cost
        return true
    }

    companion object {
        /** Bir tazeleme turunda toplam çağrı sayısı bunu aşamaz (doküman 9.3/1). */
        const val ROUND_CAP = 30

        /** Günlük geçmiş seri çağrı tavanı (doküman 9.3/3). */
        const val HISTORY_DAILY_CAP = 100

        /** Sağlayıcı limitlerinin altında kalan varsayılan günlük tavanlar. */
        val DEFAULT_DAILY_CAPS: Map<SourceId, Int> = mapOf(
            SourceId.FINNHUB to 500,
            SourceId.YAHOO to 300,
            SourceId.TWELVE_DATA to 200,
            SourceId.TCMB_EVDS to 50,
            SourceId.TCMB_HOURLY to 50,
            SourceId.TEFAS to 5,
            SourceId.TRUNCGIL to 50,
        )
    }
}

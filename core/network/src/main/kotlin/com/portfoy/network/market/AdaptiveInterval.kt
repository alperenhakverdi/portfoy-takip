package com.portfoy.network.market

import com.portfoy.network.CallBudget

/**
 * Gün içi tazeleme planı. [intervalMinutes] `null` ise gün içi turlar kapalıdır; yalnızca açılış ve
 * kapanış turları yapılır.
 */
data class IntradayPlan(
    val intervalMinutes: Int?,
    /** Bir turda çekilecek varlık sayısı; portföy büyükse varlıklar dönüşümlü tazelenir. */
    val assetsPerRound: Int,
    /** Bir varlığın gün içinde kaç turda bir tazelendiği (1 = her turda). */
    val rotationEvery: Int,
) {
    val enabled: Boolean get() = intervalMinutes != null
}

/**
 * Kapanış > açılış > gün içi önceliğiyle gün içi aralığı seçer.
 *
 * Açılış ve kapanış turları **tüm** varlıkları kapsar ve bütçeden önce ayrılır; gün içi turlar kalan
 * bütçeye sığdığı en sık aralıkta (30 → 45 → 60 dk) yapılır. Hiçbiri sığmazsa gün içi kapatılır.
 * Aynı varlık için 15 dakikanın altına inilmez (doküman 9.3/5).
 */
object AdaptiveInterval {
    private val candidates = listOf(30, 45, 60)

    /** İlk gün içi tur açılıştan bu kadar sonra başlar; açılış turu open+2 dk'da yapılır. */
    const val OPEN_OFFSET_MINUTES = 2

    fun choose(
        assetCount: Int,
        dailyCap: Int,
        sessionMinutes: Int,
        roundCap: Int = CallBudget.ROUND_CAP,
    ): IntradayPlan {
        if (assetCount <= 0) return IntradayPlan(null, 0, 1)

        val perRound = minOf(assetCount, roundCap)
        val rotation = (assetCount + roundCap - 1) / roundCap
        val reserved = 2 * assetCount // açılış + kapanış turları tüm varlıkları çeker
        val remaining = dailyCap - reserved
        if (remaining <= 0) return IntradayPlan(null, perRound, rotation)

        for (interval in candidates) {
            val rounds = intradayRounds(sessionMinutes, interval)
            if (rounds * perRound <= remaining) return IntradayPlan(interval, perRound, rotation)
        }
        return IntradayPlan(null, perRound, rotation)
    }

    /** Açılış turundan sonra kapanışa kadar kaç gün içi tur yapılabileceği. */
    fun intradayRounds(sessionMinutes: Int, interval: Int): Int =
        ((sessionMinutes - OPEN_OFFSET_MINUTES - 1) / interval).coerceAtLeast(0)
}

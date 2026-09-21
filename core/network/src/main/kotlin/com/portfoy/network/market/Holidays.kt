package com.portfoy.network.market

import java.time.LocalDate

/**
 * Gömülü resmî tatil takvimleri. Yılda bir güncellenir; bir yıl eksikse [MarketCalendar.coversYear]
 * `false` döner ve o yılın tatilleri bilinmez (yalnızca hafta sonları kapalı sayılır).
 *
 * 2026 tarihleri elle girilmiştir; dini bayram tarihleri Diyanet takviminden yıl başında
 * doğrulanmalıdır.
 */
object Holidays {

    /** NYSE / Nasdaq tam gün kapalı günler. */
    val US_2026: Set<LocalDate> = setOf(
        LocalDate.of(2026, 1, 1), // Yılbaşı
        LocalDate.of(2026, 1, 19), // Martin Luther King Günü
        LocalDate.of(2026, 2, 16), // Başkanlar Günü
        LocalDate.of(2026, 4, 3), // Kutsal Cuma
        LocalDate.of(2026, 5, 25), // Anma Günü
        LocalDate.of(2026, 6, 19), // Juneteenth
        LocalDate.of(2026, 7, 3), // Bağımsızlık Günü (4 Temmuz Cumartesi, Cuma gözlenir)
        LocalDate.of(2026, 9, 7), // İşçi Bayramı
        LocalDate.of(2026, 11, 26), // Şükran Günü
        LocalDate.of(2026, 12, 25), // Noel
    )

    /** Borsa İstanbul ve TEFAS tam gün kapalı günler (hafta içine denk gelenler). */
    val TR_2026: Set<LocalDate> = setOf(
        LocalDate.of(2026, 1, 1), // Yılbaşı
        LocalDate.of(2026, 3, 20), // Ramazan Bayramı 1. gün
        LocalDate.of(2026, 4, 23), // Ulusal Egemenlik ve Çocuk Bayramı
        LocalDate.of(2026, 5, 1), // Emek ve Dayanışma Günü
        LocalDate.of(2026, 5, 19), // Atatürk'ü Anma, Gençlik ve Spor Bayramı
        LocalDate.of(2026, 5, 27), // Kurban Bayramı 1. gün
        LocalDate.of(2026, 5, 28), // Kurban Bayramı 2. gün
        LocalDate.of(2026, 5, 29), // Kurban Bayramı 3. gün
        LocalDate.of(2026, 7, 15), // Demokrasi ve Millî Birlik Günü
        LocalDate.of(2026, 10, 29), // Cumhuriyet Bayramı
    )

    val ALL: Map<Market, Set<LocalDate>> = mapOf(
        Market.US to US_2026,
        Market.BIST to TR_2026,
    )
}

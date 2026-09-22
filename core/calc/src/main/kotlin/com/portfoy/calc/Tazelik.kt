package com.portfoy.calc

import java.time.Duration
import java.time.Instant

/**
 * Verinin ne kadar eskidiğine dair eşikler (doküman 14, karar 9). Ekranlar bu eşiklerle "güncel değil" notu gösterir;
 * eşikleri tek yerde tutmak, ekranların birbirinden farklı bir "eski" tanımı kullanmasını engeller.
 */
object Tazelik {
    /** Elle girilen fiyat bu süreden eskiyse "fiyat güncel değil" uyarısı çıkar (otomatik güncellenmediği için). */
    val ELLE_FIYAT_ESKI: Duration = Duration.ofDays(7)

    /**
     * Kur bu süreden eskiyse ABD varlıklarının TL değeri eski kurla hesaplanıyor demektir ve belirtilir.
     * İki gün: hafta sonu ve resmî tatilde kur kendiliğinden bir iki gün eski kalabilir, bu bir sorun sayılmaz.
     */
    val KUR_ESKI: Duration = Duration.ofDays(2)

    fun elleFiyatEskiMi(zaman: Instant, simdi: Instant): Boolean = Duration.between(zaman, simdi) > ELLE_FIYAT_ESKI

    fun kurEskiMi(zaman: Instant, simdi: Instant): Boolean = Duration.between(zaman, simdi) > KUR_ESKI

    /** "8 gün önce" gibi gösterim için tam gün sayısı; gelecekteki zaman 0 sayılır. */
    fun gunFarki(zaman: Instant, simdi: Instant): Long = Duration.between(zaman, simdi).toDays().coerceAtLeast(0)
}

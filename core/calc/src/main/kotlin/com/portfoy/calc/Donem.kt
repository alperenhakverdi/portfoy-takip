package com.portfoy.calc

import java.time.LocalDate

/**
 * Her iki grafikte de aynı olan dönem seçenekleri. Varsayılan dönem [BIR_AY]'dır. [kisaEtiket]
 * (M22) dönem seçici çiplerinde kullanılır — dokuzu da tek ekranda sığsın diye kısaltılmış;
 * [etiket] erişilebilirlik açıklaması (content description) olarak kalır.
 */
enum class Donem(val etiket: String, val kisaEtiket: String) {
    BIR_GUN("1 Gün", "1G"),
    BIR_HAFTA("1 Hafta", "1H"),
    BIR_AY("1 Ay", "1A"),
    UC_AY("3 Ay", "3A"),
    ALTI_AY("6 Ay", "6A"),
    YTD("YTD", "YTD"),
    BIR_YIL("1 Yıl", "1Y"),
    UC_YIL("3 Yıl", "3Y"),
    TUMU("Tümü", "Tümü");

    /**
     * Dönemin başlangıç tarihi.
     * - YTD: içinde bulunulan yılın 1 Ocak'ı.
     * - Tümü: portföydeki en eski işlem kaydının tarihi ([enEskiIslem] yoksa bugün).
     */
    fun baslangic(bugun: LocalDate, enEskiIslem: LocalDate?): LocalDate = when (this) {
        BIR_GUN -> bugun.minusDays(1)
        BIR_HAFTA -> bugun.minusWeeks(1)
        BIR_AY -> bugun.minusMonths(1)
        UC_AY -> bugun.minusMonths(3)
        ALTI_AY -> bugun.minusMonths(6)
        YTD -> LocalDate.of(bugun.year, 1, 1)
        BIR_YIL -> bugun.minusYears(1)
        UC_YIL -> bugun.minusYears(3)
        TUMU -> enEskiIslem ?: bugun
    }

    companion object {
        val VARSAYILAN = BIR_AY
    }
}

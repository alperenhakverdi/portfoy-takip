package com.portfoy.calc

import java.time.LocalDate

/** Her iki grafikte de aynı olan dönem seçenekleri. Varsayılan dönem [BIR_AY]'dır. */
enum class Donem(val etiket: String) {
    BIR_GUN("1 Gün"),
    BIR_HAFTA("1 Hafta"),
    BIR_AY("1 Ay"),
    UC_AY("3 Ay"),
    ALTI_AY("6 Ay"),
    YTD("YTD"),
    BIR_YIL("1 Yıl"),
    UC_YIL("3 Yıl"),
    TUMU("Tümü");

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

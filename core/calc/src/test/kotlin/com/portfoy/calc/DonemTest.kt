package com.portfoy.calc

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DonemTest {

    private val bugun = LocalDate.of(2026, 9, 21)

    @Test
    fun `dokuz secenek dokumandaki sirayla`() {
        assertEquals(
            listOf("1 Gün", "1 Hafta", "1 Ay", "3 Ay", "6 Ay", "YTD", "1 Yıl", "3 Yıl", "Tümü"),
            Donem.entries.map { it.etiket },
        )
    }

    @Test
    fun `varsayilan donem 1 ay`() {
        assertEquals(Donem.BIR_AY, Donem.VARSAYILAN)
    }

    @Test
    fun `sabit donemlerin baslangici`() {
        assertEquals(LocalDate.of(2026, 9, 20), Donem.BIR_GUN.baslangic(bugun, null))
        assertEquals(LocalDate.of(2026, 9, 14), Donem.BIR_HAFTA.baslangic(bugun, null))
        assertEquals(LocalDate.of(2026, 8, 21), Donem.BIR_AY.baslangic(bugun, null))
        assertEquals(LocalDate.of(2026, 6, 21), Donem.UC_AY.baslangic(bugun, null))
        assertEquals(LocalDate.of(2026, 3, 21), Donem.ALTI_AY.baslangic(bugun, null))
        assertEquals(LocalDate.of(2025, 9, 21), Donem.BIR_YIL.baslangic(bugun, null))
        assertEquals(LocalDate.of(2023, 9, 21), Donem.UC_YIL.baslangic(bugun, null))
    }

    @Test
    fun `YTD icinde bulunulan yilin 1 Ocagindan baslar`() {
        assertEquals(LocalDate.of(2026, 1, 1), Donem.YTD.baslangic(bugun, null))
        assertEquals(LocalDate.of(2027, 1, 1), Donem.YTD.baslangic(LocalDate.of(2027, 3, 3), null))
    }

    @Test
    fun `Tumu en eski islem tarihinden baslar, kayit yoksa bugundur`() {
        assertEquals(LocalDate.of(2024, 5, 3), Donem.TUMU.baslangic(bugun, LocalDate.of(2024, 5, 3)))
        assertEquals(bugun, Donem.TUMU.baslangic(bugun, null))
    }

    @Test
    fun `ay sonu tasmasi guvenli`() {
        assertEquals(LocalDate.of(2026, 2, 28), Donem.BIR_AY.baslangic(LocalDate.of(2026, 3, 31), null))
    }
}

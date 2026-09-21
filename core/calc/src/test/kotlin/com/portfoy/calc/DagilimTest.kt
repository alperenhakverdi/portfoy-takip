package com.portfoy.calc

import com.portfoy.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DagilimTest {

    private fun toplam(slices: List<AllocationSlice>): BigDecimal =
        slices.fold(BigDecimal.ZERO) { acc, s -> acc + s.percent }

    @Test
    fun `yuzdeler guncel degerden hesaplanir ve buyukten kucuge siralanir`() {
        val slices = allocate(
            mapOf(
                Category.ABD to bd("77"),
                Category.FON to bd("12"),
                Category.EMTIA to bd("6"),
                Category.BIST to bd("3"),
                Category.NAKIT to bd("2"),
            ),
        )
        assertEquals(
            listOf(Category.ABD, Category.FON, Category.EMTIA, Category.BIST, Category.NAKIT),
            slices.map { it.category },
        )
        assertBd("77", slices[0].percent)
        assertBd("100", toplam(slices))
    }

    @Test
    fun `yuvarlama yuzunden 100'u asan toplam en buyuk dilimden dusulur`() {
        // 33,335 / 33,335 / 33,33 -> yuvarlanınca 33,34 + 33,34 + 33,33 = 100,01
        val slices = allocate(
            mapOf(
                Category.ABD to bd("33335"),
                Category.BIST to bd("33335"),
                Category.FON to bd("33330"),
            ),
        )
        assertBd("100", toplam(slices))
        assertBd("33.33", slices.first { it.category == Category.ABD }.percent) // en büyük (eşitlikte ilk) dilim
        assertBd("33.34", slices.first { it.category == Category.BIST }.percent)
        assertBd("33.33", slices.first { it.category == Category.FON }.percent)
    }

    @Test
    fun `yuvarlama yuzunden 100'un altinda kalan toplam da 100'e sabitlenir`() {
        val slices = allocate(
            mapOf(Category.ABD to bd("1"), Category.BIST to bd("1"), Category.FON to bd("1")),
        )
        assertBd("100", toplam(slices))
    }

    @Test
    fun `portfoyde olmayan kategori hic gorunmez`() {
        val slices = allocate(mapOf(Category.ABD to bd("100"), Category.BIST to bd("0")))
        assertEquals(listOf(Category.ABD), slices.map { it.category })
    }

    @Test
    fun `tek dilim yuzde 100 olur`() {
        val slices = allocate(mapOf(Category.EMTIA to bd("42.5")))
        assertBd("100", slices.single().percent)
    }

    @Test
    fun `bos ya da sifir degerde dilim yok`() {
        assertTrue(allocate(emptyMap()).isEmpty())
        assertTrue(allocate(mapOf(Category.ABD to bd("0"))).isEmpty())
    }
}

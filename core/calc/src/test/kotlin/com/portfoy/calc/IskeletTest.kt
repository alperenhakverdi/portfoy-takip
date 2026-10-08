package com.portfoy.calc

import com.portfoy.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class IskeletTest {
    @Test
    fun `yedi kategori tanimli`() {
        assertEquals(7, Category.entries.size)
    }
}

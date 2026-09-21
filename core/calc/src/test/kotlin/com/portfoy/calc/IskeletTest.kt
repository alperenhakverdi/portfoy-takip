package com.portfoy.calc

import com.portfoy.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class IskeletTest {
    @Test
    fun `alti kategori tanimli`() {
        assertEquals(6, Category.entries.size)
    }
}

package com.portfoy.calc

import com.portfoy.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class IskeletTest {
    @Test
    fun `bes kategori tanimli`() {
        assertEquals(5, Category.entries.size)
    }
}

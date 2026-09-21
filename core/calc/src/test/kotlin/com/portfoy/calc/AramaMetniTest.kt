package com.portfoy.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AramaMetniTest {

    @Test
    fun `turkce karakterler ascii karsiligina iner`() {
        assertEquals("turk hava yollari", normalizeForSearch("Türk Hava Yolları"))
        assertEquals("cigdem sogut", normalizeForSearch("Çiğdem Söğüt"))
    }

    @Test
    fun `buyuk I ve noktali I dogru cevrilir`() {
        assertEquals("igdir", normalizeForSearch("IĞDIR"))
        assertEquals("istanbul", normalizeForSearch("İstanbul"))
    }

    @Test
    fun `turk aramasi Türk sonucunu bulur`() {
        assertTrue(normalizeForSearch("Türk Hava Yolları").contains(normalizeForSearch("turk")))
        assertTrue(normalizeForSearch("THYAO Türk Hava Yolları").contains(normalizeForSearch("TÜRK")))
    }

    @Test
    fun `kod ve isim buyuk kucuk harften bagimsiz`() {
        assertEquals("nvda", normalizeForSearch("NVDA"))
        assertEquals("nvidia", normalizeForSearch("Nvidia"))
    }
}

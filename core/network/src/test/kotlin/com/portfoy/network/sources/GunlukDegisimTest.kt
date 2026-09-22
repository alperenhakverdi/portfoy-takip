package com.portfoy.network.sources

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GunlukDegisimTest {

    @Test
    fun `artis yuzdesi dogru hesaplanir`() {
        assertEquals(0, BigDecimal("10.0000").compareTo(gunlukDegisim(110.0, 100.0)!!))
    }

    @Test
    fun `azalis eksi yuzde verir`() {
        assertEquals(0, BigDecimal("-5.0000").compareTo(gunlukDegisim(95.0, 100.0)!!))
    }

    @Test
    fun `onceki kapanis yoksa null`() {
        assertNull(gunlukDegisim(110.0, null))
    }

    @Test
    fun `onceki kapanis sifirsa null, sifira bolme yok`() {
        assertNull(gunlukDegisim(110.0, 0.0))
    }
}

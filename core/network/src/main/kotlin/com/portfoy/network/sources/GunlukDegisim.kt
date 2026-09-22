package com.portfoy.network.sources

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Günlük değişim yüzdesi: (güncel − önceki kapanış) / önceki kapanış × 100. Para birimi fark etmez
 * (oran birimsizdir), bu yüzden TL çevriminden önce, kaynağın kendi para biriminde hesaplanır.
 * Önceki kapanış yoksa ya da sıfırsa `null` döner (yeni kote edilmiş varlık, bozuk veri...).
 */
internal fun gunlukDegisim(guncel: Double, onceki: Double?): BigDecimal? {
    if (onceki == null || onceki == 0.0) return null
    val g = BigDecimal.valueOf(guncel)
    val o = BigDecimal.valueOf(onceki)
    return (g - o).divide(o, MathContext.DECIMAL64).multiply(BigDecimal(100)).setScale(4, RoundingMode.HALF_UP)
}

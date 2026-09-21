package com.portfoy.calc

import java.math.BigDecimal

/** Güncel değer = güncel fiyat × adet. */
fun currentValue(priceTl: BigDecimal, quantity: BigDecimal): BigDecimal = priceTl * quantity

/** ABD varlıklarının TL değeri: USD fiyat × güncel USD/TRY. */
fun usdToTry(usdPrice: BigDecimal, usdTry: BigDecimal): BigDecimal = usdPrice * usdTry

/** Kâr/Zarar (TL) = güncel değer − toplam maliyet. */
fun profitLoss(value: BigDecimal, cost: BigDecimal): BigDecimal = value - cost

/**
 * Getiri % = (güncel değer − toplam maliyet) / toplam maliyet × 100.
 * Maliyet sıfır ya da negatifse hesaplanamaz; `null` döner (ekranda "—").
 */
fun returnPercent(value: BigDecimal, cost: BigDecimal): BigDecimal? {
    if (cost.signum() <= 0) return null
    return (value - cost).dividedBy(cost) * HUNDRED
}

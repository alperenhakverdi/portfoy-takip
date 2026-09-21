package com.portfoy.calc

import com.portfoy.model.Transaction
import java.math.BigDecimal

/** Bir varlığın alım kayıtlarından türetilen pozisyon. Ayrı tabloda saklanmaz. */
data class Position(
    val quantity: BigDecimal,
    val totalCost: BigDecimal,
) {
    /** Ağırlıklı ortalama birim maliyet. Adet sıfırsa tanımsızdır. */
    val unitCost: BigDecimal?
        get() = if (quantity.signum() == 0) null else totalCost.dividedBy(quantity)
}

/** Toplam maliyet = Σ(alış fiyatı × adet + komisyon). */
fun positionOf(transactions: List<Transaction>): Position {
    var quantity = BigDecimal.ZERO
    var cost = BigDecimal.ZERO
    for (t in transactions) {
        quantity += t.quantity
        cost += t.unitPriceTl * t.quantity + t.commissionTl
    }
    return Position(quantity, cost)
}

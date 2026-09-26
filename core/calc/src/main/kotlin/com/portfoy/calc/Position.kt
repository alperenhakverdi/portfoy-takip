package com.portfoy.calc

import com.portfoy.model.Transaction
import com.portfoy.model.TransactionType
import java.math.BigDecimal

/** Bir varlığın alım/azaltma kayıtlarından türetilen pozisyon. Ayrı tabloda saklanmaz. */
data class Position(
    val quantity: BigDecimal,
    val totalCost: BigDecimal,
) {
    /** Ağırlıklı ortalama birim maliyet. Adet sıfırsa tanımsızdır. */
    val unitCost: BigDecimal?
        get() = if (quantity.signum() == 0) null else totalCost.dividedBy(quantity)
}

/**
 * Toplam maliyet = Σ(alış fiyatı × adet + komisyon) − azaltmalar.
 *
 * Kayıtlar tarihe göre sırayla işlenir (M17): [TransactionType.ALIS] adet ve maliyeti ekler,
 * [TransactionType.AZALTMA] o andaki ağırlıklı ortalama maliyeti **değiştirmeden** adet ve
 * toplam maliyeti orantılı düşürür — gerçekleşen kâr/zarar hesaplanmaz (bilinçli kapsam dışı,
 * bkz. GELISTIRME_PLANI.md M17.4). Adet, elde olandan fazla azaltılmaya çalışılırsa sıfırda durur.
 */
fun positionOf(transactions: List<Transaction>): Position {
    var quantity = BigDecimal.ZERO
    var cost = BigDecimal.ZERO
    for (t in transactions.sortedWith(compareBy({ it.tradeDate }, { it.id }))) {
        when (t.type) {
            TransactionType.ALIS -> {
                quantity += t.quantity
                cost += t.unitPriceTl * t.quantity + t.commissionTl
            }
            TransactionType.AZALTMA -> {
                val ortalamaMaliyet = if (quantity.signum() == 0) BigDecimal.ZERO else cost.dividedBy(quantity)
                val azaltilan = t.quantity.min(quantity)
                quantity -= azaltilan
                cost -= ortalamaMaliyet * azaltilan
                if (quantity.signum() <= 0) {
                    quantity = BigDecimal.ZERO
                    cost = BigDecimal.ZERO
                }
            }
        }
    }
    return Position(quantity, cost)
}

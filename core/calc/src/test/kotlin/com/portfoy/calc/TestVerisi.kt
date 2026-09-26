package com.portfoy.calc

import com.portfoy.model.Asset
import com.portfoy.model.Category
import com.portfoy.model.Transaction
import com.portfoy.model.TransactionType
import com.portfoy.model.UnitType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

fun bd(value: String) = BigDecimal(value)

fun asset(id: Long, code: String, category: Category, unit: UnitType = UnitType.ADET) =
    Asset(id = id, code = code, name = code, category = category, currency = "TRY", unitType = unit)

fun alis(
    assetId: Long,
    quantity: String,
    price: String,
    commission: String = "0",
    date: LocalDate = LocalDate.of(2026, 1, 1),
    id: Long = 0,
) = Transaction(
    id = id,
    assetId = assetId,
    type = TransactionType.ALIS,
    quantity = bd(quantity),
    unitPriceTl = bd(price),
    commissionTl = bd(commission),
    tradeDate = date,
)

/** M17 — azaltma: ağırlıklı ortalama maliyeti değiştirmeden adet/maliyeti orantılı düşürür. */
fun azalt(
    assetId: Long,
    quantity: String,
    price: String = "0",
    date: LocalDate = LocalDate.of(2026, 1, 1),
    id: Long = 0,
) = Transaction(
    id = id,
    assetId = assetId,
    type = TransactionType.AZALTMA,
    quantity = bd(quantity),
    unitPriceTl = bd(price),
    commissionTl = BigDecimal.ZERO,
    tradeDate = date,
)

/** Beklenen değeri belirtilen basamağa yuvarlanmış gerçek değerle karşılaştırır. */
fun assertBd(expected: String, actual: BigDecimal?, scale: Int = 2) {
    if (actual == null) throw AssertionError("Beklenen $expected, gelen null")
    assertEquals(bd(expected).setScale(scale), actual.setScale(scale, RoundingMode.HALF_UP))
}

fun assertNullValue(actual: BigDecimal?) = assertNull(actual)

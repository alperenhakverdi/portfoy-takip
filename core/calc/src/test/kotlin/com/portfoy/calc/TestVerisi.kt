package com.portfoy.calc

import com.portfoy.model.Asset
import com.portfoy.model.Category
import com.portfoy.model.Transaction
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
) = Transaction(
    id = 0,
    assetId = assetId,
    quantity = bd(quantity),
    unitPriceTl = bd(price),
    commissionTl = bd(commission),
    tradeDate = date,
)

/** Beklenen değeri belirtilen basamağa yuvarlanmış gerçek değerle karşılaştırır. */
fun assertBd(expected: String, actual: BigDecimal?, scale: Int = 2) {
    if (actual == null) throw AssertionError("Beklenen $expected, gelen null")
    assertEquals(bd(expected).setScale(scale), actual.setScale(scale, RoundingMode.HALF_UP))
}

fun assertNullValue(actual: BigDecimal?) = assertNull(actual)

package com.portfoy.calc

import com.portfoy.model.Asset
import com.portfoy.model.Category
import com.portfoy.model.Transaction
import java.math.BigDecimal

/** Hesaplamaya giren bir varlık: kayıtları ve TL cinsinden güncel (ya da son bilinen) fiyatı. */
data class Holding(
    val asset: Asset,
    val transactions: List<Transaction>,
    /** `null` ise fiyat hiç alınamamıştır; varlık maliyetiyle değerlenir ve işaretlenir. */
    val currentPriceTl: BigDecimal?,
)

data class AssetResult(
    val asset: Asset,
    val quantity: BigDecimal,
    val unitCost: BigDecimal?,
    val totalCost: BigDecimal,
    val currentPriceTl: BigDecimal,
    val priceMissing: Boolean,
    val currentValue: BigDecimal,
    val profitLoss: BigDecimal,
    val returnPercent: BigDecimal?,
)

data class CategoryResult(
    val category: Category,
    val value: BigDecimal,
    val assets: List<AssetResult>,
)

data class PortfolioSummary(
    val totalValue: BigDecimal,
    val totalCost: BigDecimal,
    val profitLoss: BigDecimal,
    val returnPercent: BigDecimal?,
    /** TL değerine göre büyükten küçüğe sıralı. */
    val categories: List<CategoryResult>,
    val allocation: List<AllocationSlice>,
) {
    val isEmpty: Boolean get() = categories.isEmpty()
}

/** Portföy özetini hesaplar. Nakit TL'nin birim fiyatı her zaman 1,00 ₺'dir. */
fun summarize(holdings: List<Holding>): PortfolioSummary {
    val results = holdings.mapNotNull { holding ->
        val position = positionOf(holding.transactions)
        if (position.quantity.signum() == 0) return@mapNotNull null // tüm kayıtlar silinmiş

        val price = when (holding.asset.category) {
            Category.NAKIT -> BigDecimal.ONE
            else -> holding.currentPriceTl
        }
        val missing = price == null
        val effectivePrice = price ?: position.unitCost ?: BigDecimal.ZERO
        val value = currentValue(effectivePrice, position.quantity)
        AssetResult(
            asset = holding.asset,
            quantity = position.quantity,
            unitCost = position.unitCost,
            totalCost = position.totalCost,
            currentPriceTl = effectivePrice,
            priceMissing = missing,
            currentValue = value,
            profitLoss = profitLoss(value, position.totalCost),
            returnPercent = returnPercent(value, position.totalCost),
        )
    }

    val categories = results
        .groupBy { it.asset.category }
        .map { (category, assets) ->
            CategoryResult(
                category = category,
                value = assets.fold(BigDecimal.ZERO) { acc, a -> acc + a.currentValue },
                assets = assets.sortedByDescending { it.currentValue },
            )
        }
        .sortedByDescending { it.value }

    val totalValue = categories.fold(BigDecimal.ZERO) { acc, c -> acc + c.value }
    val totalCost = results.fold(BigDecimal.ZERO) { acc, a -> acc + a.totalCost }

    return PortfolioSummary(
        totalValue = totalValue,
        totalCost = totalCost,
        profitLoss = profitLoss(totalValue, totalCost),
        returnPercent = returnPercent(totalValue, totalCost),
        categories = categories,
        allocation = allocate(categories.associate { it.category to it.value }),
    )
}

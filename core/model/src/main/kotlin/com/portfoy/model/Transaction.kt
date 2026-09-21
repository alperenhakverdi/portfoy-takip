package com.portfoy.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Tek bir alım kaydı. Maliyet TL olarak tutulur; kur çevrimi maliyet tarafında yapılmaz. */
data class Transaction(
    val id: Long,
    val assetId: Long,
    val type: TransactionType = TransactionType.ALIS,
    val quantity: BigDecimal,
    val unitPriceTl: BigDecimal,
    val commissionTl: BigDecimal = BigDecimal.ZERO,
    /** Alış tarihi. Saat bilgisi ayrıca saklanır ama hesaplar gün bazında yapılır. */
    val tradeDate: LocalDate,
    val note: String? = null,
    val createdAt: Instant = Instant.EPOCH,
)

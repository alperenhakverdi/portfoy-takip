package com.portfoy.model

/** Takip edilen bir varlık (hisse, fon, gram altın, nakit TL...). */
data class Asset(
    val id: Long,
    val code: String,
    val name: String,
    val category: Category,
    /** Fiyatın geldiği para birimi (USD, TRY). */
    val currency: String,
    val unitType: UnitType,
    val exchange: String? = null,
    val active: Boolean = true,
)

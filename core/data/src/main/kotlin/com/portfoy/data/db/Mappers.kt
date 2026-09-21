package com.portfoy.data.db

import com.portfoy.calc.normalizeForSearch
import com.portfoy.model.Asset
import com.portfoy.model.Transaction

fun AssetEntity.toModel() = Asset(
    id = id,
    code = code,
    name = name,
    category = category,
    currency = currency,
    unitType = unitType,
    exchange = exchange,
    active = active,
)

fun Asset.toEntity(fundKind: String? = null) = AssetEntity(
    id = id,
    code = code,
    name = name,
    category = category,
    currency = currency,
    unitType = unitType,
    exchange = exchange,
    active = active,
    fundKind = fundKind,
    searchText = normalizeForSearch("$code $name"),
)

fun TransactionEntity.toModel() = Transaction(
    id = id,
    assetId = assetId,
    type = type,
    quantity = quantity,
    unitPriceTl = unitPriceTl,
    commissionTl = commissionTl,
    tradeDate = tradeDate,
    note = note,
    createdAt = createdAt,
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    assetId = assetId,
    type = type,
    quantity = quantity,
    unitPriceTl = unitPriceTl,
    commissionTl = commissionTl,
    tradeDate = tradeDate,
    note = note,
    createdAt = createdAt,
)

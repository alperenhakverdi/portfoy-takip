package com.portfoy.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Fiyat kaynaklarının kimliği. Fiyat kaydında hangi verinin nereden geldiği bununla izlenir. */
enum class SourceId { FINNHUB, TWELVE_DATA, YAHOO, TEFAS, TCMB_EVDS, TCMB_HOURLY, TRUNCGIL, MANUEL, FAKE }

/**
 * Kaynağa fiyat sorarken varlığı tarif eder. [category] `null` ise varlık değil kurdur (USD/TRY).
 * [fundKind] yalnızca fonlar içindir (YAT, EMK, BYF...): TEFAS geçmiş sorgusu türü ister.
 */
data class AssetRef(val code: String, val category: Category?, val fundKind: String? = null) {
    companion object {
        val USDTRY = AssetRef("USDTRY", null)
    }
}

/** Anlık fiyat. [price] kaynağın para biriminde gelir (ABD için USD); TL'ye çevrim sonradan yapılır. */
data class Quote(
    val code: String,
    val price: BigDecimal,
    val currency: String,
    val timestamp: Instant,
    val source: SourceId,
)

/** Günlük kapanış. */
data class Candle(val date: LocalDate, val close: BigDecimal)

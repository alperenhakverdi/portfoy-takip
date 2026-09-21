package com.portfoy.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Fiyat kaynaklarının kimliği. Fiyat kaydında hangi verinin nereden geldiği bununla izlenir. */
enum class SourceId { FINNHUB, TWELVE_DATA, YAHOO, TEFAS, TCMB_EVDS, TCMB_HOURLY, TCMB_DAILY, TRUNCGIL, MANUEL, FAKE }

/**
 * Kaynağa fiyat sorarken varlığı tarif eder. [category] `null` ise ham semboldür: kur (`USDTRY`) ya da
 * kaynağın kendi kodu (`GC=F`). [fundKind] yalnızca fonlar içindir (YAT, EMK, BYF...): TEFAS geçmiş sorgusu türü ister.
 */
data class AssetRef(val code: String, val category: Category?, val fundKind: String? = null) {
    /**
     * Kur varlığının para birimi: `USDTRY` → "USD", `EURTRY` → "EUR". Kur değilse `null`.
     * Çevrim için kullanılan kur ([USDTRY]) ile portföydeki döviz varlığı aynı kaynaklardan beslenir.
     */
    val fxCurrency: String?
        get() = when {
            category != null && category != Category.DOVIZ -> null
            code.length == 6 && code.endsWith("TRY") -> code.take(3)
            else -> null
        }

    companion object {
        val USDTRY = AssetRef("USDTRY", null)
        val EURTRY = AssetRef("EURTRY", null)
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

/** Arama listesine (katalog) girecek bir enstrüman. */
data class AssetInfo(
    val code: String,
    val name: String,
    val category: Category,
    val currency: String,
    val exchange: String? = null,
    /** Fon türü (YAT, BYF...). Yalnızca fonlar için. */
    val fundKind: String? = null,
)

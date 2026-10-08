package com.portfoy.calc.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId

/**
 * Bir getiri değerinin yönü — hem ▲/▼ işareti hem de ekran rengi (M14.2, kazanç yeşil/kayıp kırmızı)
 * bu tek karardan türer, ikisi asla birbirinden bağımsız hesaplanmaz.
 */
enum class GetiriYonu { ARTI, EKSI, NOTR }

/**
 * Türkçe sayı biçimi: binlik ayıracı nokta, ondalık ayıracı virgül (1.234.567,89 ₺).
 * Tutar ve yüzde 2 hane, adet/gram en fazla 4 hane. Yuvarlama yalnızca burada uygulanır.
 * Artı/eksi yön işaretiyle (▲ / ▼) gösterilir.
 */
object TrFormat {
    const val EMPTY = "—"

    private val symbols = DecimalFormatSymbols().apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private fun format(pattern: String, value: BigDecimal): String =
        DecimalFormat(pattern, symbols).apply { roundingMode = RoundingMode.HALF_UP }.format(value)

    /** [birim] M23'te TL/USD görüntüleme geçişi için — depolama her zaman TL, bu yalnız ekranda yazar. */
    fun money(value: BigDecimal?, birim: String = "₺"): String =
        if (value == null) EMPTY else "${format("#,##0.00", value)} $birim"

    fun percent(value: BigDecimal?): String =
        if (value == null) EMPTY else "%${format("#,##0.00", value)}"

    fun quantity(value: BigDecimal?): String =
        if (value == null) EMPTY else format("#,##0.####", value)

    /** "▲ %12,40" / "▼ %3,10"; sıfır yön işaretsizdir. */
    fun signedPercent(value: BigDecimal?): String {
        if (value == null) return EMPTY
        return direction(value) + percent(value.abs())
    }

    /** "▲ 18.430,50 ₺" / "▼ 250,00 ₺"; sıfır yön işaretsizdir. */
    fun signedMoney(value: BigDecimal?, birim: String = "₺"): String {
        if (value == null) return EMPTY
        return direction(value) + money(value.abs(), birim)
    }

    /** İşaret, ekranda görünecek (yuvarlanmış) değere göre belirlenir: "▲ %0,00" görünmesin. */
    private fun direction(value: BigDecimal): String = when (yon(value)) {
        GetiriYonu.ARTI -> "▲ "
        GetiriYonu.EKSI -> "▼ "
        GetiriYonu.NOTR -> ""
    }

    /**
     * [value]'nun yönü, ekranda görünecek (2 hane yuvarlanmış) değere göre — "▲ %0,00" ya da yeşil
     * bir "%0,00" görünmesin diye ham değer değil, yuvarlanmış değer kullanılır. `null` nötr sayılır.
     */
    fun yon(value: BigDecimal?): GetiriYonu {
        if (value == null) return GetiriYonu.NOTR
        val shown = value.setScale(2, RoundingMode.HALF_UP)
        return when {
            shown.signum() > 0 -> GetiriYonu.ARTI
            shown.signum() < 0 -> GetiriYonu.EKSI
            else -> GetiriYonu.NOTR
        }
    }

    /** "son güncelleme" etiketi: bugüne aitse "SS:DD", değilse "GG.AA SS:DD". */
    fun lastUpdate(timestamp: Instant, now: Instant, zone: ZoneId): String {
        val local = timestamp.atZone(zone)
        val time = "%02d:%02d".format(local.hour, local.minute)
        val today = now.atZone(zone).toLocalDate()
        return if (local.toLocalDate() == today) time
        else "%02d.%02d %s".format(local.dayOfMonth, local.monthValue, time)
    }
}

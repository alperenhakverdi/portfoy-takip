package com.portfoy.calc

import java.math.BigDecimal
import java.math.MathContext

/** Tüm bölmeler bu hassasiyetle yapılır; yuvarlama yalnızca ekrana yazarken uygulanır. */
internal val MC: MathContext = MathContext.DECIMAL128

internal val HUNDRED = BigDecimal(100)

/**
 * DECIMAL128 hassasiyetiyle bölme.
 *
 * Adı bilinçli olarak `divide` değil: Kotlin'de üye fonksiyon uzantıya baskın gelir, `divide` yazılırsa
 * Java'nın tam bölmesi çağrılır ve 1/3 gibi sonsuz sonuçlarda ArithmeticException fırlatır.
 */
internal fun BigDecimal.dividedBy(other: BigDecimal): BigDecimal = this.divide(other, MC)

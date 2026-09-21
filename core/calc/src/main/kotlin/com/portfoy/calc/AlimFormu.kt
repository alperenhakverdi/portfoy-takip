package com.portfoy.calc

import java.math.BigDecimal
import java.time.LocalDate

private val binlikNoktali = Regex("""[1-9]\d{0,2}(\.\d{3})+""")

/**
 * Türkçe yazılmış sayıyı çözer.
 *
 * - Virgül varsa ondalık ayıracıdır, nokta binlik ayıracıdır: "1.234,56" → 1234,56.
 * - Virgül yoksa ve yazı `1.234` / `5.000` / `1.234.567` biçimindeyse (nokta + tam üç hane) nokta
 *   binlik ayıracı sayılır, çünkü Türkçe'de böyle yazılır: "5.000" → 5000.
 * - Diğer noktalı sayılar ondalıktır: "1234.56", "1.5", "0.125".
 *
 * Çözülemezse `null` döner.
 */
fun parseDecimal(text: String): BigDecimal? {
    val cleaned = text.trim().replace(" ", "")
    if (cleaned.isEmpty()) return null
    val normalized = when {
        cleaned.contains(',') -> cleaned.replace(".", "").replace(',', '.')
        binlikNoktali.matches(cleaned) -> cleaned.replace(".", "")
        else -> cleaned
    }
    return runCatching { BigDecimal(normalized) }.getOrNull()
}

/** Toplam maliyet = alış fiyatı × adet + komisyon. Formda anlık gösterilir. */
fun purchaseTotal(unitPriceTl: BigDecimal, quantity: BigDecimal, commissionTl: BigDecimal = BigDecimal.ZERO): BigDecimal =
    unitPriceTl * quantity + commissionTl

/** Alan başına üst sınırlar; aşırı büyük giriş biçimi bozmasın diye. */
object AlimSinirlari {
    val ADET_UST: BigDecimal = BigDecimal("1000000000") // 1 milyar
    val NAKIT_UST: BigDecimal = BigDecimal("1000000000000") // 1 trilyon TL
    val FIYAT_UST: BigDecimal = BigDecimal("1000000000")
    val KOMISYON_UST: BigDecimal = BigDecimal("1000000000")
}

/** Form alanı hataları; ekranda ilgili alanın altında gösterilir. Boşsa kayıt yapılabilir. */
data class AlimHatalari(
    val fiyat: String? = null,
    val adet: String? = null,
    val komisyon: String? = null,
    val tarih: String? = null,
) {
    val gecerli: Boolean get() = fiyat == null && adet == null && komisyon == null && tarih == null
}

/** Doğrulanmış form değerleri. */
data class AlimDegerleri(
    val fiyat: BigDecimal,
    val adet: BigDecimal,
    val komisyon: BigDecimal,
    val tarih: LocalDate,
)

data class AlimSonucu(val hatalar: AlimHatalari, val degerler: AlimDegerleri?)

/**
 * Alım formunu doğrular (doküman bölüm 14): adet ve fiyat sıfır ya da negatif olamaz, tarih en fazla
 * bugün olabilir, aşırı büyük sayılar engellenir. Nakit TL'de yalnızca tutar istenir, fiyat 1,00 ₺'dir.
 */
fun dogrulaAlim(
    fiyatMetni: String,
    adetMetni: String,
    komisyonMetni: String,
    tarih: LocalDate,
    bugun: LocalDate,
    nakit: Boolean = false,
): AlimSonucu {
    var fiyatHatasi: String? = null
    var adetHatasi: String? = null
    var komisyonHatasi: String? = null
    var tarihHatasi: String? = null

    val fiyat = if (nakit) BigDecimal.ONE else parseDecimal(fiyatMetni)
    if (!nakit) {
        fiyatHatasi = when {
            fiyat == null -> "Geçerli bir fiyat gir"
            fiyat.signum() <= 0 -> "Fiyat sıfırdan büyük olmalı"
            fiyat > AlimSinirlari.FIYAT_UST -> "Fiyat çok büyük"
            else -> null
        }
    }

    val adet = parseDecimal(adetMetni)
    val adetUst = if (nakit) AlimSinirlari.NAKIT_UST else AlimSinirlari.ADET_UST
    adetHatasi = when {
        adet == null -> if (nakit) "Geçerli bir tutar gir" else "Geçerli bir adet gir"
        adet.signum() <= 0 -> if (nakit) "Tutar sıfırdan büyük olmalı" else "Adet sıfırdan büyük olmalı"
        adet > adetUst -> if (nakit) "Tutar çok büyük" else "Adet çok büyük"
        else -> null
    }

    val komisyon = if (komisyonMetni.isBlank()) BigDecimal.ZERO else parseDecimal(komisyonMetni)
    komisyonHatasi = when {
        komisyon == null -> "Geçerli bir komisyon gir"
        komisyon.signum() < 0 -> "Komisyon negatif olamaz"
        komisyon > AlimSinirlari.KOMISYON_UST -> "Komisyon çok büyük"
        else -> null
    }

    if (tarih.isAfter(bugun)) tarihHatasi = "Tarih bugünden ileri olamaz"

    val hatalar = AlimHatalari(fiyatHatasi, adetHatasi, komisyonHatasi, tarihHatasi)
    val degerler = if (hatalar.gecerli) AlimDegerleri(fiyat!!, adet!!, komisyon!!, tarih) else null
    return AlimSonucu(hatalar, degerler)
}

package com.portfoy.ui.para

import com.portfoy.calc.tryToUsd
import com.portfoy.model.Category
import java.math.BigDecimal

/** M23 — Portföy/Grafik ekranlarındaki özet rakamların hangi para biriminde gösterileceği. Depolama her zaman TL kalır. */
enum class ParaBirimiTercihi {
    TL, USD;

    fun tersi(): ParaBirimiTercihi = if (this == TL) USD else TL
}

/**
 * [tl] tutarını [tercih] USD ise ve kur varsa USD'ye çevirir; yoksa (kur yok ya da tercih TL) sessizce
 * TL kalır. Dönüş: (gösterilecek tutar, birim etiketi).
 */
fun cevrilmisTutar(tl: BigDecimal, tercih: ParaBirimiTercihi, usdTryKuru: BigDecimal?): Pair<BigDecimal, String> {
    if (tercih == ParaBirimiTercihi.USD && usdTryKuru != null && usdTryKuru.signum() > 0) {
        return tryToUsd(tl, usdTryKuru) to "USD"
    }
    return tl to "₺"
}

/** M25 — doğal para birimi dolar olan kategoriler: satırları TL'ye çevirmek yerine USD gösterilir. */
fun usdDogalMi(kategori: Category): Boolean = kategori == Category.ABD || kategori == Category.KRIPTO

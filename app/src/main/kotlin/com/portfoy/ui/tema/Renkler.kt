package com.portfoy.ui.tema

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.portfoy.calc.format.GetiriYonu
import com.portfoy.calc.format.TrFormat
import java.math.BigDecimal

/**
 * M14.2: getiri renkleri. Artı kazanç yeşil, eksi kayıp kırmızı, sıfır/`null` nötr (ikincil metin).
 * Yön kararı [TrFormat.yon]'dan gelir — ▲/▼ işareti ile renk **aynı** hesaptan türer, asla
 * birbirinden bağımsız yuvarlanmaz (renk körlüğü için işaret her zaman korunur, karar 36).
 */
@Composable
fun getiriRengi(deger: BigDecimal?): Color = when (TrFormat.yon(deger)) {
    GetiriYonu.ARTI -> AppTema.renkler.kazanc
    GetiriYonu.EKSI -> AppTema.renkler.kayip
    GetiriYonu.NOTR -> AppTema.renkler.ikincilMetin
}

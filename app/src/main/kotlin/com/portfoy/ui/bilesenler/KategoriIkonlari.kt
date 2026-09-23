package com.portfoy.ui.bilesenler

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.portfoy.model.Category
import com.portfoy.ui.tema.AppTema

/**
 * Kategori ikonu, sabit boyutlu kutuda: satırlar arasında hizalama bozulmaz. Renk M14.3'ün kategori
 * paletinden gelir (temaya göre açık/koyu); M10'daki "renkli ama minimalist" ilkesi korunur.
 * Fon ve Emtia'nın ikonları M14.5'te değişti: Savings/MonetizationOn anlamı zayıf duruyordu
 * (kumbara "birikim", dolar sikkesi "para" anlatıyordu — ikisi de kategorinin kendisini değil).
 */
@Composable
fun KategoriIkonu(kategori: Category, modifier: Modifier = Modifier) {
    val renk = AppTema.renkler.kategoriRengi(kategori)
    when (kategori) {
        Category.NAKIT -> TurkBayragi(modifier)
        Category.EMTIA -> AltinKulcesi(renk, modifier)
        else -> Icon(kategori.simge(), contentDescription = null, tint = renk, modifier = modifier)
    }
}

private fun Category.simge(): ImageVector = when (this) {
    Category.BIST -> Icons.AutoMirrored.Filled.ShowChart
    Category.ABD -> Icons.Filled.Public
    Category.FON -> Icons.Filled.AccountBalance // "kumbara" yerine banka: fon bir kurumun ürünüdür
    Category.DOVIZ -> Icons.Filled.AttachMoney
    Category.EMTIA, Category.NAKIT -> error("Bu kategorinin kendi çizimi var, simge() çağrılmaz")
}

/**
 * Emtia için sade bir külçe çizimi (tek renk, dolgusuz — diğer ikonlarla aynı çizgi kalınlığında).
 * Hazır bir Material ikonu "gram altın/gümüş"ü doğru anlatmıyordu (dolar sikkesi para birimiydi).
 */
@Composable
private fun AltinKulcesi(renk: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val yol = Path().apply {
            moveTo(w * 0.22f, h * 0.32f)
            lineTo(w * 0.78f, h * 0.32f)
            lineTo(w * 0.92f, h * 0.74f)
            lineTo(w * 0.08f, h * 0.74f)
            close()
        }
        drawPath(
            yol,
            color = renk,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/**
 * Nakit TL için Türk bayrağı, sade çizimle: kırmızı zemin, beyaz hilal (iki dairenin üst üste
 * binmesiyle), yıldız tek noktaya indirgenmiştir. Gerçek bayrağın ayrıntısı yerine tanınabilir
 * en yalın hâli hedeflenir (minimalist istek). Bu iki rengi tema değiştirmez — gerçek bir bayrak,
 * bir rozet değil.
 */
@Composable
private fun TurkBayragi(modifier: Modifier = Modifier) {
    val kirmizi = AppTema.renkler.nakitBayrakKirmizi
    Canvas(modifier.clip(RoundedCornerShape(4.dp))) {
        val w = size.width
        val h = size.height
        drawRect(color = kirmizi)

        val hilalYaricap = h * 0.30f
        val merkez = Offset(w * 0.42f, h * 0.5f)
        drawCircle(color = Color.White, radius = hilalYaricap, center = merkez)
        drawCircle(
            color = kirmizi,
            radius = hilalYaricap * 0.82f,
            center = merkez + Offset(hilalYaricap * 0.38f, 0f),
        )

        drawCircle(color = Color.White, radius = h * 0.07f, center = Offset(w * 0.68f, h * 0.5f))
    }
}

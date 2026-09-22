package com.portfoy.ui.bilesenler

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.portfoy.model.Category

/**
 * Kategori ikonlarının rengi (M10, kullanıcı isteği: "renkli olabilir ama minimalist olsun").
 * Uygulamanın geri kalanı gri tonlarında (wireframe teması); bu tek renkler yalnızca kategori
 * simgesine özgü bilinçli bir istisnadır — arka plan, çerçeve, metin rengi değişmez.
 */
private object KategoriRengi {
    val Bist = Color(0xFFC62828) // Borsa İstanbul: kırmızı
    val Abd = Color(0xFF1565C0) // ABD bayrağı lacivert
    val Fon = Color(0xFF6A1B9A) // mor
    val Emtia = Color(0xFFB8860B) // altın
    val Doviz = Color(0xFF2E7D32) // dolar yeşili
    val Nakit = Color(0xFFE30A17) // Türk bayrağı kırmızısı
}

/** Kategori ikonu, sabit boyutlu kutuda: satırlar arasında hizalama bozulmaz. */
@Composable
fun KategoriIkonu(kategori: Category, modifier: Modifier = Modifier) {
    when (kategori) {
        Category.NAKIT -> TurkBayragi(modifier)
        else -> {
            val (simge, renk) = kategori.simgeVeRenk()
            Icon(simge, contentDescription = null, tint = renk, modifier = modifier)
        }
    }
}

private fun Category.simgeVeRenk(): Pair<ImageVector, Color> = when (this) {
    Category.BIST -> Icons.AutoMirrored.Filled.ShowChart to KategoriRengi.Bist
    Category.ABD -> Icons.Filled.Public to KategoriRengi.Abd
    Category.FON -> Icons.Filled.Savings to KategoriRengi.Fon
    Category.EMTIA -> Icons.Filled.MonetizationOn to KategoriRengi.Emtia
    Category.DOVIZ -> Icons.Filled.AttachMoney to KategoriRengi.Doviz
    Category.NAKIT -> error("Nakit TL için TurkBayragi kullanılır")
}

/**
 * Nakit TL için Türk bayrağı, sade çizimle: kırmızı zemin, beyaz hilal (iki dairenin üst üste
 * binmesiyle), yıldız tek noktaya indirgenmiştir. Gerçek bayrağın ayrıntısı yerine tanınabilir
 * en yalın hâli hedeflenir (minimalist istek).
 */
@Composable
private fun TurkBayragi(modifier: Modifier = Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(4.dp))) {
        val w = size.width
        val h = size.height
        drawRect(color = KategoriRengi.Nakit)

        val hilalYaricap = h * 0.30f
        val merkez = Offset(w * 0.42f, h * 0.5f)
        drawCircle(color = Color.White, radius = hilalYaricap, center = merkez)
        drawCircle(
            color = KategoriRengi.Nakit,
            radius = hilalYaricap * 0.82f,
            center = merkez + Offset(hilalYaricap * 0.38f, 0f),
        )

        drawCircle(color = Color.White, radius = h * 0.07f, center = Offset(w * 0.68f, h * 0.5f))
    }
}

package com.portfoy.ui.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Wireframe teması: renk kararı verilmez; ayrım gri tonu, çerçeve kalınlığı ve boşlukla yapılır.
 * Renk paleti, tipografi ve marka kimliği sonraki fazın işidir.
 */
object Gri {
    val Koyu = Color(0xFF262626)
    val Orta = Color(0xFF5E5E5E)
    val Acik = Color(0xFF9A9A9A)
    val Cerceve = Color(0xFF8A8A8A)
    val InceCerceve = Color(0xFFCFCFCF)
    val Yuzey = Color(0xFFFFFFFF)
    val Zemin = Color(0xFFF3F3F3)
    val Vurgu = Color(0xFFE4E4E4)

    /** Dağılım grafiği dilimleri: koyudan açığa. */
    val Dilimler = listOf(
        Color(0xFF262626),
        Color(0xFF5C5C5C),
        Color(0xFF8A8A8A),
        Color(0xFFB3B3B3),
        Color(0xFFD5D5D5),
    )
}

private val WireframeRenkleri = lightColorScheme(
    primary = Gri.Koyu,
    onPrimary = Gri.Yuzey,
    primaryContainer = Gri.Vurgu,
    onPrimaryContainer = Gri.Koyu,
    secondary = Gri.Orta,
    onSecondary = Gri.Yuzey,
    secondaryContainer = Gri.Vurgu,
    onSecondaryContainer = Gri.Koyu,
    background = Gri.Zemin,
    onBackground = Gri.Koyu,
    surface = Gri.Yuzey,
    onSurface = Gri.Koyu,
    surfaceVariant = Gri.Vurgu,
    onSurfaceVariant = Gri.Orta,
    outline = Gri.Cerceve,
    outlineVariant = Gri.InceCerceve,
    error = Gri.Koyu,
    onError = Gri.Yuzey,
    surfaceContainer = Gri.Yuzey,
    surfaceContainerHigh = Gri.Vurgu,
    surfaceContainerHighest = Gri.Vurgu,
)

@Composable
fun PortfoyTemasi(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WireframeRenkleri, content = content)
}

package com.portfoy.ui.tema

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * M14.4: özel bir yazı tipi dosyası eklenmez (APK boyutu ve lisans yükü minimalizme aykırı);
 * sistem yazı tipi üzerinde bilinçli bir ölçek tanımlanır.
 *
 * Asıl kazanç **hizalı rakamlardır**: finans ekranında sayılar bir sütunda üst üste durur.
 * `fontFeatureSettings = "tnum"` (tabular figures), sayısal karakterleri eşit genişlikte çizer —
 * "1" ile "8" farklı genişlikte olduğunda listedeki TL değerleri kayık görünürdü.
 */
private const val TNUM = "tnum"

private val TabularStyle = TextStyle(fontFeatureSettings = TNUM)

val PortfoyTipografi = Typography().let { varsayilan ->
    Typography(
        // Toplam portföy değeri — ekranın en vurgulu rakamı.
        displaySmall = varsayilan.displaySmall.merge(
            TabularStyle.copy(fontSize = 34.sp, fontWeight = FontWeight.SemiBold, lineHeight = 40.sp),
        ),
        // Dönem getirisi yüzdesi.
        headlineSmall = varsayilan.headlineSmall.merge(
            TabularStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),
        ),
        // Kategori adı (akordeon başlığı).
        titleMedium = varsayilan.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium),
        // Varlık kodu (AAPL, THYAO...).
        titleSmall = varsayilan.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
        // Fiyat, tutar gibi değerler.
        bodyMedium = varsayilan.bodyMedium.merge(TabularStyle.copy(fontSize = 15.sp)),
        bodyLarge = varsayilan.bodyLarge.merge(TabularStyle),
        // Varlık adı, notlar, açıklamalar.
        bodySmall = varsayilan.bodySmall.copy(fontSize = 13.sp),
        // "piyasa kapalı" gibi durum etiketleri.
        labelMedium = varsayilan.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
        labelLarge = varsayilan.labelLarge.merge(TabularStyle),
        labelSmall = varsayilan.labelSmall.merge(TabularStyle),
        titleLarge = varsayilan.titleLarge,
        headlineMedium = varsayilan.headlineMedium,
        headlineLarge = varsayilan.headlineLarge.merge(TabularStyle),
        displayMedium = varsayilan.displayMedium,
        displayLarge = varsayilan.displayLarge,
    )
}

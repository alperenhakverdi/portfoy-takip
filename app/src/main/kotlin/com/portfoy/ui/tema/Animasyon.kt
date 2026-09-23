package com.portfoy.ui.tema

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * M14.6: kısa, amaçlı animasyonlar için ortak süre ve easing sabitleri, artı erişilebilirlik desteği.
 * Sistemde "animasyonları azalt" (Ayarlar → Erişilebilirlik) açıksa [LocalReducedMotion] `true` olur
 * ve tüm süreler 0'a iner — animasyon dekoratif olmalı, hiçbir bilgiyi yalnızca animasyonla taşımamalı.
 */
object Animasyon {
    /** Akordeon aç/kapa, sekme geçişi gibi küçük durum değişimleri. */
    const val KISA_MS = 150

    /** Fiyat tazelenince vurgu, donut ilk çizim gibi biraz daha belirgin geçişler. */
    const val ORTA_MS = 400

    val KolayGecis: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

val LocalReducedMotion = compositionLocalOf { false }

/** `Settings.Global.ANIMATOR_DURATION_SCALE` sıfırsa kullanıcı sistem genelinde animasyonları kapatmıştır. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

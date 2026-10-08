package com.portfoy.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfoy.model.Category

/**
 * M14 teması: wireframe'in gri düzeninden gerçek bir görünüme geçiş (doküman 15/1, karar 36).
 * İlke minimalizm — veri kahraman, renk yalnız anlam taşıdığı yerde kullanılır:
 *
 * - **Nötr** katman (bu dosyadaki [AppRenkler]): yüzeyler, çerçeveler, metin. Marka rengi bilinçli
 *   olarak mürekkep (koyu lacivert-gri) — yeşil ve kırmızıyı tamamen semantik anlama ayırır.
 * - **Semantik** katman: kazanç/kayıp, yalnızca [getiriRengi] üzerinden ([Renkler.kt]).
 * - **Kategori** katmanı: yalnız ikon ve grafik dilimlerinde, aşağıdaki altı renk.
 *
 * Açık ve koyu tema birlikte tanımlanır; sistem ayarına göre otomatik seçilir.
 */
data class AppRenkler(
    val zemin: Color,
    val yuzey: Color,
    val yuzeyVaryant: Color,
    val cerceve: Color,
    val inceCerceve: Color,
    val anaMetin: Color,
    val ikincilMetin: Color,
    val birincil: Color,
    val onBirincil: Color,
    val kazanc: Color,
    val kayip: Color,
    val kategoriAbd: Color,
    val kategoriBist: Color,
    val kategoriFon: Color,
    val kategoriEmtia: Color,
    val kategoriDoviz: Color,
    val kategoriNakit: Color,
    val kategoriKripto: Color,
    val nakitBayrakKirmizi: Color,
) {
    /** Getiri işaretine göre renk: artı kazanç, eksi kayıp, sıfır/`null` ikincil metin (nötr). */
    fun kategoriRengi(kategori: Category): Color = when (kategori) {
        Category.ABD -> kategoriAbd
        Category.BIST -> kategoriBist
        Category.FON -> kategoriFon
        Category.EMTIA -> kategoriEmtia
        Category.DOVIZ -> kategoriDoviz
        Category.NAKIT -> kategoriNakit
        Category.KRIPTO -> kategoriKripto
    }
}

private val AcikRenkler = AppRenkler(
    zemin = Color(0xFFF6F7F9),
    yuzey = Color(0xFFFFFFFF),
    yuzeyVaryant = Color(0xFFEDEFF3),
    cerceve = Color(0xFFD6DAE0),
    inceCerceve = Color(0xFFE7E9ED),
    anaMetin = Color(0xFF131A24),
    ikincilMetin = Color(0xFF5C6672),
    birincil = Color(0xFF1A2332),
    onBirincil = Color(0xFFFFFFFF),
    kazanc = Color(0xFF0E7A55),
    kayip = Color(0xFFB3261E),
    kategoriAbd = Color(0xFF2563EB),
    kategoriBist = Color(0xFFEA580C),
    kategoriFon = Color(0xFF7C3AED),
    kategoriEmtia = Color(0xFFCA8A04),
    kategoriDoviz = Color(0xFF0891B2),
    kategoriNakit = Color(0xFF64748B),
    kategoriKripto = Color(0xFFDB2777),
    nakitBayrakKirmizi = Color(0xFFE30A17),
)

private val KoyuRenkler = AppRenkler(
    zemin = Color(0xFF0E1116),
    yuzey = Color(0xFF161B22),
    yuzeyVaryant = Color(0xFF1F2630),
    cerceve = Color(0xFF2C333D),
    inceCerceve = Color(0xFF232A33),
    anaMetin = Color(0xFFE4E7EB),
    ikincilMetin = Color(0xFF9AA3AE),
    birincil = Color(0xFFE4E7EB),
    onBirincil = Color(0xFF1A2332),
    kazanc = Color(0xFF35C88E),
    kayip = Color(0xFFFF6B61),
    kategoriAbd = Color(0xFF60A5FA),
    kategoriBist = Color(0xFFFB923C),
    kategoriFon = Color(0xFFA78BFA),
    kategoriEmtia = Color(0xFFEAB308),
    kategoriDoviz = Color(0xFF22D3EE),
    kategoriNakit = Color(0xFF94A3B8),
    kategoriKripto = Color(0xFFF472B6),
    nakitBayrakKirmizi = Color(0xFFE30A17),
)

val LocalAppRenkler = staticCompositionLocalOf { AcikRenkler }

/** `AppTema.renkler.kazanc` gibi kısa erişim; `MaterialTheme.colorScheme` ile aynı kullanım kalıbı. */
object AppTema {
    val renkler: AppRenkler
        @Composable get() = LocalAppRenkler.current
}

private fun materialSemasi(r: AppRenkler, koyuMu: Boolean) = if (koyuMu) {
    darkColorScheme(
        primary = r.birincil, onPrimary = r.onBirincil,
        primaryContainer = r.yuzeyVaryant, onPrimaryContainer = r.anaMetin,
        secondary = r.ikincilMetin, onSecondary = r.onBirincil,
        secondaryContainer = r.yuzeyVaryant, onSecondaryContainer = r.anaMetin,
        background = r.zemin, onBackground = r.anaMetin,
        surface = r.yuzey, onSurface = r.anaMetin,
        surfaceVariant = r.yuzeyVaryant, onSurfaceVariant = r.ikincilMetin,
        outline = r.cerceve, outlineVariant = r.inceCerceve,
        error = r.kayip, onError = r.onBirincil,
        surfaceContainer = r.yuzey, surfaceContainerHigh = r.yuzeyVaryant, surfaceContainerHighest = r.yuzeyVaryant,
    )
} else {
    lightColorScheme(
        primary = r.birincil, onPrimary = r.onBirincil,
        primaryContainer = r.yuzeyVaryant, onPrimaryContainer = r.anaMetin,
        secondary = r.ikincilMetin, onSecondary = r.onBirincil,
        secondaryContainer = r.yuzeyVaryant, onSecondaryContainer = r.anaMetin,
        background = r.zemin, onBackground = r.anaMetin,
        surface = r.yuzey, onSurface = r.anaMetin,
        surfaceVariant = r.yuzeyVaryant, onSurfaceVariant = r.ikincilMetin,
        outline = r.cerceve, outlineVariant = r.inceCerceve,
        error = r.kayip, onError = r.onBirincil,
        surfaceContainer = r.yuzey, surfaceContainerHigh = r.yuzeyVaryant, surfaceContainerHighest = r.yuzeyVaryant,
    )
}

@Composable
fun PortfoyTemasi(content: @Composable () -> Unit) {
    // M18: kullanıcı elle Açık/Koyu seçebilir; varsayılan Sistem eski otomatik davranışın aynısı.
    val temaVm: TemaViewModel = hiltViewModel()
    val tercih by temaVm.tercih.collectAsState()
    val koyuMu = when (tercih) {
        TemaTercihi.SISTEM -> isSystemInDarkTheme()
        TemaTercihi.ACIK -> false
        TemaTercihi.KOYU -> true
    }
    val renkler = if (koyuMu) KoyuRenkler else AcikRenkler
    val azaltilmisAnimasyon = rememberReducedMotion()
    CompositionLocalProvider(
        LocalAppRenkler provides renkler,
        LocalReducedMotion provides azaltilmisAnimasyon,
    ) {
        MaterialTheme(
            colorScheme = materialSemasi(renkler, koyuMu),
            typography = PortfoyTipografi,
            content = content,
        )
    }
}

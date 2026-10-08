package com.portfoy.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.portfoy.ui.ekle.EkleEkrani
import com.portfoy.ui.grafik.GrafikEkrani
import com.portfoy.ui.portfoy.PortfoyEkrani
import com.portfoy.ui.tema.Animasyon
import com.portfoy.ui.varlik.VarlikYonetimEkrani
import kotlinx.coroutines.launch

/**
 * Alt bardaki sekmeler, soldan sağa: Ekle, Portföy (M21 — Performans sekmesi kaldırıldı, grafik
 * Portföy'den açılan ayrı bir ekrana taşındı). Yalnızca ikon vardır, metin etiketi yoktur. Aktif
 * sekmenin ikonu dolgulu, pasif sekmelerinki çizgiseldir.
 */
private enum class Sekme(
    val rota: String,
    val aciklama: String,
    val dolu: ImageVector,
    val cizgi: ImageVector,
) {
    EKLE("ekle", "Ekle", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
    PORTFOY("portfoy", "Portföy", Icons.Filled.PieChart, Icons.Outlined.PieChart),
}

@Composable
fun Uygulama() {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val girdi by nav.currentBackStackEntryAsState()
    val mevcut = girdi?.destination?.route
    // Klavye açıkken alt bar gizlenir ve içerik klavyenin üstüne oturur; Kaydet düğmesi görünür kalır.
    val klavyeAcik = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    // Varlık yönetimi (M17) ayrı, tam ekran bir sayfadır — o ekrandayken alt bar gizlenir.
    val altBarGorunur = mevcut == null || Sekme.entries.any { it.rota == mevcut }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            // Ekranı boydan boya kaplar, 3 ikon eşit aralıklıdır; sistem çubuğuyla çakışmaz (Scaffold/NavigationBar inset'leri).
            if (!klavyeAcik && altBarGorunur) NavigationBar {
                Sekme.entries.forEach { sekme ->
                    val secili = mevcut == sekme.rota
                    NavigationBarItem(
                        selected = secili,
                        onClick = { nav.sekmeyeGit(sekme.rota) },
                        icon = { Icon(if (secili) sekme.dolu else sekme.cizgi, contentDescription = sekme.aciklama) },
                        label = null,
                        alwaysShowLabel = false,
                    )
                }
            }
        },
    ) { ic ->
        // Uygulama açıldığında varsayılan sekme 3'tür (Portföy). Sekmeler arası geçmiş tutulmaz: başka sekmede
        // geri tuşu Portföy'e döner, Portföy'de uygulamadan çıkar. Her sekmenin durumu ViewModel'inde korunur.
        // Sekme geçişi kısa bir fade-through ile yapılır (M14.6): kayma/büyüme değil, yalnızca solma —
        // "hangi yöne gidiliyor" hissi vermez çünkü alt bar sekmeleri bir sıra değil, bağımsız üç ekrandır.
        NavHost(
            nav,
            startDestination = Sekme.PORTFOY.rota,
            modifier = Modifier.padding(ic).consumeWindowInsets(ic).imePadding(),
            enterTransition = { fadeIn(tween(Animasyon.KISA_MS)) },
            exitTransition = { fadeOut(tween(Animasyon.KISA_MS)) },
            popEnterTransition = { fadeIn(tween(Animasyon.KISA_MS)) },
            popExitTransition = { fadeOut(tween(Animasyon.KISA_MS)) },
        ) {
            composable(Sekme.EKLE.rota) {
                EkleEkrani(onKaydedildi = {
                    nav.sekmeyeGit(Sekme.PORTFOY.rota)
                    scope.launch { snackbar.showSnackbar("Alım kaydedildi") }
                })
            }
            composable(Sekme.PORTFOY.rota) {
                PortfoyEkrani(
                    onEkleGit = { nav.sekmeyeGit(Sekme.EKLE.rota) },
                    onVarlikTikla = { id -> nav.navigate("varlik/$id") },
                    onGrafikGit = { nav.navigate("grafik") },
                    snackbar = snackbar,
                )
            }
            composable(
                "varlik/{assetId}",
                arguments = listOf(navArgument("assetId") { type = NavType.LongType }),
            ) {
                VarlikYonetimEkrani(onGeri = { nav.popBackStack() }, snackbar = snackbar)
            }
            composable("grafik") {
                GrafikEkrani(onGeri = { nav.popBackStack() }, onVarlikTikla = { id -> nav.navigate("varlik/$id") })
            }
        }
    }
}

private fun NavHostController.sekmeyeGit(rota: String) {
    navigate(rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

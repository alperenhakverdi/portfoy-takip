package com.portfoy.ui

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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.portfoy.ui.ekle.EkleEkrani
import com.portfoy.ui.performans.PerformansEkrani
import com.portfoy.ui.portfoy.PortfoyEkrani
import kotlinx.coroutines.launch

/**
 * Alt bardaki sekmeler, soldan sağa: Ekle, Performans, Portföy. Yalnızca ikon vardır, metin etiketi yoktur.
 * Aktif sekmenin ikonu dolgulu, pasif sekmelerinki çizgiseldir.
 */
private enum class Sekme(
    val rota: String,
    val aciklama: String,
    val dolu: ImageVector,
    val cizgi: ImageVector,
) {
    EKLE("ekle", "Ekle", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
    PERFORMANS("performans", "Performans", Icons.AutoMirrored.Filled.ShowChart, Icons.AutoMirrored.Outlined.ShowChart),
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            // Ekranı boydan boya kaplar, 3 ikon eşit aralıklıdır; sistem çubuğuyla çakışmaz (Scaffold/NavigationBar inset'leri).
            if (!klavyeAcik) NavigationBar {
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
        NavHost(nav, startDestination = Sekme.PORTFOY.rota, modifier = Modifier.padding(ic).consumeWindowInsets(ic).imePadding()) {
            composable(Sekme.EKLE.rota) {
                EkleEkrani(onKaydedildi = {
                    nav.sekmeyeGit(Sekme.PORTFOY.rota)
                    scope.launch { snackbar.showSnackbar("Alım kaydedildi") }
                })
            }
            composable(Sekme.PERFORMANS.rota) { PerformansEkrani() }
            composable(Sekme.PORTFOY.rota) {
                PortfoyEkrani(onEkleGit = { nav.sekmeyeGit(Sekme.EKLE.rota) }, snackbar = snackbar)
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

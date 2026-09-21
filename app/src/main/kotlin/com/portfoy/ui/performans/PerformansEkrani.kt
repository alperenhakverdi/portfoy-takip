package com.portfoy.ui.performans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfoy.calc.chartWindow
import com.portfoy.calc.format.TrFormat
import com.portfoy.di.UygulamaZamanDilimi
import com.portfoy.ui.bilesenler.CizgiGrafik
import com.portfoy.ui.bilesenler.DonemSecici
import com.portfoy.ui.bilesenler.Kutu
import com.portfoy.ui.bilesenler.OrnekSeri
import com.portfoy.ui.bilesenler.tr
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Sekme 2 — Performans. Grafik, yüzdesel getiri ve TL bazlı kâr/zarar birlikte gösterilir.
 * Grafiğin y ekseni getiri yüzdesidir; Portföy sekmesindeki grafik ise TL değerini gösterir.
 */
@Composable
fun PerformansEkrani(vm: PerformansViewModel = hiltViewModel()) {
    val ekran by vm.ekran.collectAsState()
    val ozet = ekran.ozet

    if (ozet == null || ozet.isEmpty) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                if (ozet == null) "Yükleniyor…" else "Performans için önce + sekmesinden varlık ekle.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val bugun = remember { LocalDate.now(UygulamaZamanDilimi) }
    val donem = ekran.secim.donem
    val istenen = donem.baslangic(bugun, ekran.enEskiIslem)
    val pencere = chartWindow(istenen, ekran.enEskiIslem ?: bugun, bugun)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { DonemSecici(donem, vm::donemSec) }

        item {
            Kutu(kalinCerceve = true) {
                Text("Seçilen dönemdeki getiri", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    TrFormat.signedPercent(ekran.toplamYuzde),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(TrFormat.signedMoney(ekran.toplamTl), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                val tumSeri = remember(ekran.toplamYuzde, donem) {
                    OrnekSeri.yuzde("getiri-${donem.name}", ekran.toplamYuzde.toDouble())
                }
                val periyotGun = ChronoUnit.DAYS.between(istenen, bugun).coerceAtLeast(1)
                val gorunenGun = ChronoUnit.DAYS.between(pencere.start, bugun).coerceAtLeast(1)
                val nokta = if (pencere.truncated) (tumSeri.size * gorunenGun / periyotGun).toInt().coerceIn(2, tumSeri.size) else tumSeri.size
                val seri = tumSeri.take(nokta)

                CizgiGrafik(
                    degerler = seri,
                    etiket = { i ->
                        val gun = ChronoUnit.DAYS.between(pencere.start, pencere.end) * i / (seri.size - 1).coerceAtLeast(1)
                        "${pencere.start.plusDays(gun).tr()} • ${TrFormat.signedPercent(java.math.BigDecimal(seri[i]))}"
                    },
                )
                if (pencere.truncated) {
                    Spacer(Modifier.height(6.dp))
                    Text("portföy geçmişi ${pencere.portfolioDays} gün", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Text("Örnek veri (wireframe)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Varlık bazlı performans listesi (W6).
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sırala:", style = MaterialTheme.typography.bodyMedium)
                FilterChip(
                    selected = ekran.secim.siralama == Siralama.YUZDE,
                    onClick = { vm.siralamaSec(Siralama.YUZDE) },
                    label = { Text("Yüzde") },
                )
                FilterChip(
                    selected = ekran.secim.siralama == Siralama.TL,
                    onClick = { vm.siralamaSec(Siralama.TL) },
                    label = { Text("TL") },
                )
            }
        }

        item {
            Kutu {
                Row(Modifier.fillMaxWidth()) {
                    Text("Varlık", Modifier.weight(1.4f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Getiri %", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                    Text("Getiri ₺", Modifier.weight(1.1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                }
                ekran.satirlar.forEach { satir ->
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1.4f)) {
                            Text(satir.varlik.code, fontWeight = FontWeight.Bold)
                            Text(satir.varlik.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            satir.girisTarihi?.let {
                                Text("giriş: ${it.tr()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(TrFormat.signedPercent(satir.yuzde), Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
                        Text(TrFormat.signedMoney(satir.tl), Modifier.weight(1.1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item { Text("Nakit TL her dönemde %0,00 ve 0,00 ₺ görünür.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

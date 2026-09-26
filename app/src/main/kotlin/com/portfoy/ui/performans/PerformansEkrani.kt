package com.portfoy.ui.performans

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import com.portfoy.calc.Donem
import com.portfoy.model.Category
import com.portfoy.ui.bilesenler.KategoriIkonu
import com.portfoy.ui.bilesenler.etiket
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfoy.calc.format.TrFormat
import com.portfoy.ui.bilesenler.CizgiGrafik
import com.portfoy.ui.bilesenler.DonemSecici
import com.portfoy.ui.bilesenler.Kutu
import com.portfoy.ui.bilesenler.tr
import com.portfoy.ui.tema.getiriRengi

/**
 * Sekme 2 — Performans. Grafik, yüzdesel getiri ve TL bazlı kâr/zarar birlikte gösterilir.
 * Grafiğin y ekseni getiri yüzdesidir; Portföy sekmesindeki grafik ise TL değerini gösterir.
 */
@Composable
fun PerformansEkrani(vm: PerformansViewModel = hiltViewModel()) {
    val ekran by vm.ekran.collectAsState()
    val ozet = ekran.ozet

    if (!ekran.yuklendi || ozet == null || ozet.isEmpty) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                if (!ekran.yuklendi) "Yükleniyor…" else "Performans için önce + sekmesinden varlık ekle.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val donem = ekran.secim.donem

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
                    style = MaterialTheme.typography.headlineSmall,
                    color = getiriRengi(ekran.toplamYuzde),
                )
                Text(TrFormat.signedMoney(ekran.toplamTl), style = MaterialTheme.typography.titleMedium, color = getiriRengi(ekran.toplamTl))
                Spacer(Modifier.height(12.dp))

                if (ekran.getiri.size < 2) {
                    Text(
                        if (ekran.gecmisYukleniyor) "Geçmiş fiyatlar yükleniyor…" else "Grafik için en az iki günlük veri gerekir.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    CizgiGrafik(
                        degerler = ekran.getiri.map { it.second.toDouble() },
                        etiket = { i -> "${ekran.getiri[i].first.tr()} • ${TrFormat.signedPercent(ekran.getiri[i].second)}" },
                    )
                }
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
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Text("Kategori", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Değerler altta yüzde/TL olarak alt alta gösterildiği için başlık da tek sütun (Getiri).
                Text("Getiri", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
            }
        }

        // Kategori kırılımı: satırda kategorinin toplam getirisi, dokununca altındaki varlıklar açılır.
        items(ekran.kategoriler, key = { "kategori-${it.kategori}" }) { grup ->
            val acik = grup.kategori in ekran.secim.acik
            Kutu(Modifier.clickable { vm.kategoriAcKapa(grup.kategori) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1.4f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (acik) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (acik) "Kapat" else "Aç",
                        )
                        Spacer(Modifier.width(4.dp))
                        KategoriIkonu(grup.kategori, Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(grup.kategori.etiket(), fontWeight = FontWeight.Bold)
                            Text(
                                TrFormat.signedPercent(grup.yuzde),
                                style = MaterialTheme.typography.bodySmall,
                                color = getiriRengi(grup.yuzde),
                            )
                        }
                    }
                    // Yüzde ve TL yan yana değil alt alta: büyük değerlerde (ör. %12.332,00) satır taşıp
                    // iki metnin üst üste binmesini önler, her biri satırın tamamını kullanabilir.
                    Column(horizontalAlignment = Alignment.End) {
                        Text(TrFormat.signedPercent(grup.yuzde), fontWeight = FontWeight.Bold, maxLines = 1, color = getiriRengi(grup.yuzde))
                        Text(TrFormat.signedMoney(grup.tl), style = MaterialTheme.typography.bodySmall, maxLines = 1, color = getiriRengi(grup.tl))
                    }
                }

                // Fon fiyatı günde bir kez, akşam açıklanır; "1 Gün" getirisi bu yüzden %0 görünebilir (doküman 14).
                if (grup.kategori == Category.FON && donem == Donem.BIR_GUN) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Fon fiyatı günde bir kez, akşam açıklanır; 1 Gün getirisi %0 görünebilir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AnimatedVisibility(acik) {
                    Column {
                        grup.varliklar.forEach { satir ->
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1.4f).padding(start = 30.dp)) {
                                    Text(satir.varlik.code, fontWeight = FontWeight.Bold)
                                    // Dönemin tamamında portföyde olmayan varlıklar listede kalır, yanlarında giriş tarihi yazar.
                                    satir.girisTarihi?.let {
                                        Text("giriş: ${it.tr()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(TrFormat.signedPercent(satir.yuzde), style = MaterialTheme.typography.bodyMedium, maxLines = 1, color = getiriRengi(satir.yuzde))
                                    Text(TrFormat.signedMoney(satir.tl), style = MaterialTheme.typography.bodySmall, maxLines = 1, color = getiriRengi(satir.tl))
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Text("Nakit TL her dönemde %0,00 ve 0,00 ₺ görünür.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

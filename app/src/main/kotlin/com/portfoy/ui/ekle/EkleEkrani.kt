package com.portfoy.ui.ekle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfoy.calc.format.TrFormat
import com.portfoy.calc.parseDecimal
import com.portfoy.data.repository.SearchHit
import com.portfoy.di.UygulamaZamanDilimi
import com.portfoy.model.Asset
import com.portfoy.model.Category
import com.portfoy.model.UnitType
import com.portfoy.ui.bilesenler.AlimFormAlanlari
import com.portfoy.ui.bilesenler.AlimFormDurumu
import com.portfoy.ui.bilesenler.Kutu
import com.portfoy.ui.bilesenler.etiket
import java.time.LocalDate
import java.math.BigDecimal

/**
 * Sekme 1 — Ekle. Tek işi varlık eklemektir; açıldığında doğrudan arama kutusu ve klavye gelir.
 * Akış: Ara → Seç → Fiyat ve adet gir → Kaydet.
 */
@Composable
fun EkleEkrani(onKaydedildi: () -> Unit, vm: EkleViewModel = hiltViewModel()) {
    val ekran by vm.ekran.collectAsState()

    LaunchedEffect(Unit) {
        vm.olaylar.collect { olay -> if (olay is EkleOlayi.Kaydedildi) onKaydedildi() }
    }
    // Sekmeden çıkılınca girilen form değerleri kaybolur; uyarı gösterilmez. Arama metni korunur.
    DisposableEffect(Unit) { onDispose { vm.formuSifirla() } }

    when {
        ekran.secili != null -> FormGorunumu(ekran, vm)
        ekran.manuelAcik -> ManuelGorunumu(ekran.sorgu, vm)
        else -> AramaGorunumu(ekran, vm)
    }
}

@Composable
private fun AramaGorunumu(ekran: EkleEkranVerisi, vm: EkleViewModel) {
    val odak = remember { FocusRequester() }
    LaunchedEffect(Unit) { odak.requestFocus() }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = ekran.sorgu,
            onValueChange = vm::sorguDegistir,
            placeholder = { Text("Kod veya isim ara (NVDA, Türk Hava Yolları)") },
            singleLine = true,
            trailingIcon = {
                if (ekran.sorgu.isNotEmpty()) {
                    IconButton(onClick = { vm.sorguDegistir("") }) { Icon(Icons.Filled.Clear, contentDescription = "Temizle") }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).focusRequester(odak),
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Nakit TL: her zaman erişilebilir sabit kayıt, arama gerektirmez.
            ekran.nakit?.let { nakit ->
                item {
                    Kutu(Modifier.clickable { vm.sec(nakit) }, kalinCerceve = true) {
                        Text("Nakit TL", fontWeight = FontWeight.Bold)
                        Text("Türk Lirası nakit ekle (yalnızca tutar)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (!ekran.aramaAktif) {
                if (ekran.sonEklenenler.isNotEmpty()) {
                    item { BolumBasligi("Son eklenenler") }
                    items(ekran.sonEklenenler, key = { "son-${it.id}" }) { KisayolSatiri(it) { vm.sec(it) } }
                }
                if (ekran.sikAranan.isNotEmpty()) {
                    item { BolumBasligi("Sık aranan") }
                    items(ekran.sikAranan, key = { "sik-${it.id}" }) { KisayolSatiri(it) { vm.sec(it) } }
                }
                item { Text("En az 2 karakter yazınca arama başlar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (ekran.sonuclar.isEmpty()) {
                item {
                    Kutu {
                        Text("Aradığın varlık bulunamadı.", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Kod, ad, kategori ve fiyatı kendin girerek ekleyebilirsin.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = vm::manuelAc) { Text("Manuel ekle") }
                    }
                }
            } else {
                // Sonuçlar kategoriye göre gruplanır; eşleşmesi güçlü olan grup üstte gelir.
                val gruplar = ekran.sonuclar.groupBy { it.asset.category }
                gruplar.forEach { (kategori, sonuclar) ->
                    item(key = "baslik-$kategori") { BolumBasligi("${kategori.etiket()} (${sonuclar.size})") }
                    items(sonuclar, key = { "sonuc-${it.asset.id}" }) { SonucSatiri(it) { vm.sec(it.asset) } }
                }
            }
        }
    }
}

@Composable
private fun BolumBasligi(metin: String) {
    Text(metin, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
}

/** Sonuç satırı: varlık kodu, tam adı, kategori etiketi, güncel (önbellekteki) fiyat. */
@Composable
private fun SonucSatiri(sonuc: SearchHit, sec: () -> Unit) {
    Kutu(Modifier.clickable(onClick = sec)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sonuc.asset.code, fontWeight = FontWeight.Bold)
                Text(sonuc.asset.name, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(sonuc.asset.category.etiket(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(TrFormat.money(sonuc.lastPriceTl), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun KisayolSatiri(varlik: Asset, sec: () -> Unit) {
    Kutu(Modifier.clickable(onClick = sec)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(varlik.code, fontWeight = FontWeight.Bold, modifier = Modifier.width(88.dp))
            Text(varlik.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(varlik.category.etiket(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** W8 — Alış formu. Kaydet ekranın alt kısmındadır (tek elle kullanım). */
@Composable
private fun FormGorunumu(ekran: EkleEkranVerisi, vm: EkleViewModel) {
    val secili = ekran.secili ?: return
    val varlik = secili.varlik
    val bugun = remember { LocalDate.now(UygulamaZamanDilimi) }
    val durum = remember(varlik.id) { AlimFormDurumu(tarih = bugun) }
    val nakit = varlik.category == Category.NAKIT

    // Taze fiyat gelince, kullanıcı henüz yazmadıysa alış fiyatı olarak önerilir.
    LaunchedEffect(secili.onerilenFiyat) {
        val oneri = secili.onerilenFiyat
        if (oneri != null && durum.fiyat.isBlank()) {
            durum.fiyat = oneri.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',')
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::secimiKapat) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
            Column(Modifier.weight(1f)) {
                Text(varlik.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${varlik.name} • ${varlik.category.etiket()}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (!nakit && secili.fiyatYukleniyor) {
            Text("Güncel fiyat alınıyor…", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            AlimFormAlanlari(durum, varlik.unitType, abd = varlik.category == Category.ABD, kur = ekran.kur, bugun = bugun)
        }
        Button(
            onClick = {
                durum.denendi = true
                durum.dogrula(bugun, nakit, ekran.kur).degerler?.let { vm.kaydet(it, durum.not) }
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { Text("Kaydet") }
    }
}

/** W10 — Manuel varlık: aramada bulunamayan varlığı kullanıcı kendisi tanımlar. */
@Composable
private fun ManuelGorunumu(sorgu: String, vm: EkleViewModel) {
    var kod by remember { mutableStateOf(sorgu.trim().uppercase().takeIf { it.length in 2..12 && !it.contains(' ') }.orEmpty()) }
    var ad by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf(Category.BIST) }
    var fiyat by remember { mutableStateOf("") }
    var denendi by remember { mutableStateOf(false) }

    val fiyatDegeri = parseDecimal(fiyat)
    val kodHata = denendi && kod.isBlank()
    val adHata = denendi && ad.isBlank()
    val fiyatHata = denendi && (fiyatDegeri == null || fiyatDegeri.signum() <= 0)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::manuelKapat) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
            Text("Manuel varlık ekle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Bu varlığın fiyatı otomatik güncellenmez; fiyatı sen girersin.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(kod, { kod = it.uppercase() }, label = { Text("Kod") }, isError = kodHata, singleLine = true, modifier = Modifier.fillMaxWidth(),
                supportingText = if (kodHata) ({ Text("Kod gerekli", fontWeight = FontWeight.Bold) }) else null)
            OutlinedTextField(ad, { ad = it }, label = { Text("Ad") }, isError = adHata, singleLine = true, modifier = Modifier.fillMaxWidth(),
                supportingText = if (adHata) ({ Text("Ad gerekli", fontWeight = FontWeight.Bold) }) else null)
            Text("Kategori", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Category.ABD, Category.BIST, Category.FON, Category.EMTIA).forEach { k ->
                    FilterChip(selected = kategori == k, onClick = { kategori = k }, label = { Text(k.etiket()) })
                }
            }
            OutlinedTextField(
                fiyat, { fiyat = it }, label = { Text("Güncel fiyat (₺)") }, isError = fiyatHata, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
                supportingText = if (fiyatHata) ({ Text("Sıfırdan büyük bir fiyat gir", fontWeight = FontWeight.Bold) }) else null,
            )
            Text(
                "Emtia için fiyat gram başına, fon için pay başına TL'dir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = {
                denendi = true
                if (kod.isNotBlank() && ad.isNotBlank() && fiyatDegeri != null && fiyatDegeri.signum() > 0) {
                    vm.manuelKaydet(kod, ad, kategori, fiyatDegeri)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { Text("Devam") }
    }
}

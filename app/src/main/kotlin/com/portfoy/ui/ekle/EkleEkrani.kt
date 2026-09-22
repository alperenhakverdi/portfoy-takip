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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.portfoy.ui.bilesenler.AlimFormAlanlari
import com.portfoy.ui.bilesenler.AlimFormDurumu
import com.portfoy.ui.bilesenler.Kutu
import com.portfoy.ui.bilesenler.KategoriIkonu
import com.portfoy.ui.bilesenler.birimEtiketi
import com.portfoy.ui.bilesenler.etiket
import java.time.LocalDate

/**
 * Sekme 1 — Ekle. Tek işi varlık eklemektir.
 * Akış: Kategori seç → Ara ya da listeden seç → Fiyat ve adet gir → Kaydet.
 */
@Composable
fun EkleEkrani(onKaydedildi: () -> Unit, vm: EkleViewModel = hiltViewModel()) {
    val ekran by vm.ekran.collectAsState()

    LaunchedEffect(Unit) {
        vm.olaylar.collect { olay -> if (olay is EkleOlayi.Kaydedildi) onKaydedildi() }
    }
    // Sekmeden çıkılınca girilen form değerleri kaybolur; uyarı gösterilmez. Ekran kategori listesine döner.
    DisposableEffect(Unit) { onDispose { vm.formuSifirla() } }

    when {
        ekran.secili != null -> FormGorunumu(ekran, vm)
        ekran.manuelAcik -> ManuelGorunumu(ekran.kategori ?: Category.BIST, ekran.sorgu, vm)
        ekran.kategori == null -> KategoriGorunumu(ekran, vm)
        else -> AramaGorunumu(ekran, vm)
    }
}

/** Kategori açıklamaları: hangi kategoride ne bulunacağı tek satırda yazar. */
private fun Category.aciklama(): String = when (this) {
    Category.BIST -> "Borsa İstanbul hisseleri"
    Category.ABD -> "ABD borsası hisseleri"
    Category.FON -> "TEFAS yatırım fonları"
    Category.EMTIA -> "Gram altın, gram gümüş"
    Category.DOVIZ -> "USD/TRY, EUR/TRY"
    Category.NAKIT -> "Türk Lirası nakit (yalnızca tutar)"
}

private val KATEGORILER = listOf(
    Category.BIST,
    Category.ABD,
    Category.FON,
    Category.EMTIA,
    Category.DOVIZ,
    Category.NAKIT,
)

/** Ekle sekmesinin ilk ekranı: önce kategori seçilir, arama o kategorinin içinde yapılır. */
@Composable
private fun KategoriGorunumu(ekran: EkleEkranVerisi, vm: EkleViewModel) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Ne eklemek istiyorsun?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(KATEGORILER, key = { "kategori-$it" }) { kategori ->
            Kutu(
                Modifier.clickable { vm.kategoriSec(kategori) },
                kalinCerceve = kategori == Category.NAKIT,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KategoriIkonu(kategori, Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (kategori == Category.NAKIT) "Nakit TL" else kategori.etiket(),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            kategori.aciklama(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }

        // Sık yapılan iş kısa kalsın: aynı varlığa tekrar alım girmek kategori seçmeden de mümkün.
        if (ekran.sonEklenenler.isNotEmpty()) {
            item { BolumBasligi("Son eklenenler") }
            items(ekran.sonEklenenler, key = { "son-${it.id}" }) { KisayolSatiri(it) { vm.sec(it) } }
        }
    }
}

/** Seçilen kategorinin içinde arama ve liste. Az kayıtlı kategorilerde liste doğrudan gelir. */
@Composable
private fun AramaGorunumu(ekran: EkleEkranVerisi, vm: EkleViewModel) {
    val kategori = ekran.kategori ?: return
    val odak = remember { FocusRequester() }
    // Klavye yalnızca aramanın zorunlu olduğu kategorilerde (ABD, fon) kendiliğinden açılır.
    LaunchedEffect(kategori, ekran.aramaGerekli) { if (ekran.aramaGerekli) odak.requestFocus() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::kategoriyiKapat) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
            KategoriIkonu(kategori, Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(kategori.etiket(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = ekran.sorgu,
            onValueChange = vm::sorguDegistir,
            placeholder = { Text("${kategori.etiket()} içinde ara", maxLines = 1) },
            singleLine = true,
            trailingIcon = {
                if (ekran.sorgu.isNotEmpty()) {
                    IconButton(onClick = { vm.sorguDegistir("") }) { Icon(Icons.Filled.Clear, contentDescription = "Temizle") }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).focusRequester(odak),
        )

        val liste = if (ekran.aramaAktif) ekran.sonuclar else ekran.kategoriListesi
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                liste.isNotEmpty() -> {
                    // Arama zorunluysa (ABD, fon) bu liste öne çıkanlardır; hâlâ arama gerektiği hatırlatılır.
                    if (ekran.aramaGerekli && !ekran.aramaAktif) {
                        item { AramaGerekliKutusu(kategori) }
                        item { BolumBasligi("Öne çıkanlar") }
                    }
                    items(liste, key = { "varlik-${it.asset.id}" }) { SonucSatiri(it) { vm.sec(it.asset) } }
                }

                ekran.aramaAktif -> item {
                    Kutu {
                        Text("Aradığın varlık bulunamadı.", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Kod, ad ve fiyatı kendin girerek ekleyebilirsin.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = vm::manuelAc) { Text("Manuel ekle") }
                    }
                }

                ekran.aramaGerekli -> item { AramaGerekliKutusu(kategori) }

                else -> item { Text("Yükleniyor…", style = MaterialTheme.typography.bodySmall) }
            }

            if (liste.isNotEmpty()) {
                item { TextButton(onClick = vm::manuelAc) { Text("Listede yok mu? Manuel ekle") } }
            }
        }
    }
}

@Composable
private fun BolumBasligi(metin: String) {
    Text(metin, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
}

/** Küçük gri metin gözden kaçıyordu (kullanıcı "hiçbir şey çıkmıyor" sandı); belirgin bir kutuya alındı. */
@Composable
private fun AramaGerekliKutusu(kategori: Category) {
    Kutu {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Text(
                "${kategori.etiket()} kategorisinde binlerce kayıt var, hepsi listelenmez.\nAramak için en az 2 karakter yaz.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Sonuç satırı: varlık kodu, tam adı; sağda güncel (önbellekteki) fiyat, altında çok daha küçük puntoda
 * o günkü değişim yüzdesi (fiyat henüz çekilmediyse ya da kaynak vermediyse gösterilmez).
 */
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
                Text(TrFormat.money(sonuc.lastPriceTl), style = MaterialTheme.typography.bodyMedium)
                sonuc.dailyChangePercent?.let {
                    Text(TrFormat.signedPercent(it), style = MaterialTheme.typography.labelSmall)
                }
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

    // ABD varlığında USD fiyat, alış tarihindeki kurla çevrilir: tarih değişince kur yeniden bulunur.
    LaunchedEffect(durum.tarih, varlik.id) {
        if (varlik.category == Category.ABD) vm.kurGuncelle(durum.tarih)
    }

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
            AlimFormAlanlari(
                durum,
                varlik.unitType,
                abd = varlik.category == Category.ABD,
                kur = ekran.kur,
                bugun = bugun,
                birimAdi = varlik.birimEtiketi(),
            )
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

/** W10 — Manuel varlık: aramada bulunamayan varlığı kullanıcı kendisi tanımlar. Kategori seçilen kategoridir. */
@Composable
private fun ManuelGorunumu(kategori: Category, sorgu: String, vm: EkleViewModel) {
    var kod by remember { mutableStateOf(sorgu.trim().uppercase().takeIf { it.length in 2..12 && !it.contains(' ') }.orEmpty()) }
    var ad by remember { mutableStateOf("") }
    var fiyat by remember { mutableStateOf("") }
    var denendi by remember { mutableStateOf(false) }

    val fiyatDegeri = parseDecimal(fiyat)
    val kodHata = denendi && kod.isBlank()
    val adHata = denendi && ad.isBlank()
    val fiyatHata = denendi && (fiyatDegeri == null || fiyatDegeri.signum() <= 0)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::manuelKapat) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
            Text("Manuel varlık • ${kategori.etiket()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

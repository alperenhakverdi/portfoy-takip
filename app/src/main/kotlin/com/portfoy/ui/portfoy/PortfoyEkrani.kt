package com.portfoy.ui.portfoy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfoy.calc.AssetResult
import com.portfoy.calc.CategoryResult
import com.portfoy.calc.PortfolioSummary
import com.portfoy.calc.Tazelik
import com.portfoy.data.repository.TazelemeZamanlayici
import com.portfoy.calc.format.TrFormat
import com.portfoy.calc.parseDecimal
import com.portfoy.di.UygulamaZamanDilimi
import com.portfoy.model.Category
import com.portfoy.model.Transaction
import com.portfoy.model.UnitType
import com.portfoy.network.market.MarketCalendar
import com.portfoy.ui.bilesenler.AlimFormAlanlari
import com.portfoy.ui.bilesenler.AlimFormDurumu
import com.portfoy.ui.bilesenler.CizgiGrafik
import com.portfoy.ui.bilesenler.DilimIsareti
import com.portfoy.ui.bilesenler.DonemSecici
import com.portfoy.ui.bilesenler.DonutGrafik
import com.portfoy.ui.bilesenler.Kutu
import com.portfoy.ui.bilesenler.birimEtiketi
import com.portfoy.ui.bilesenler.KategoriIkonu
import com.portfoy.ui.bilesenler.etiket
import com.portfoy.ui.bilesenler.tr
import com.portfoy.ui.tema.getiriRengi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val takvim = MarketCalendar()

/** Sekme 3 — Portföy. Uygulamanın ana ekranı. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfoyEkrani(
    onEkleGit: () -> Unit,
    snackbar: SnackbarHostState,
    vm: PortfoyViewModel = hiltViewModel(),
) {
    val ekran by vm.ekran.collectAsState()
    val scope = rememberCoroutineScope()
    val bugun = remember { LocalDate.now(UygulamaZamanDilimi) }

    val sil: (Long) -> Unit = { id ->
        scope.launch {
            val silinen = vm.alimSil(id) ?: return@launch
            // 5 saniyelik geri al bildirimi.
            val kapat = launch {
                delay(5_000)
                snackbar.currentSnackbarData?.dismiss()
            }
            val sonuc = snackbar.showSnackbar("Alım kaydı silindi", "Geri al", duration = SnackbarDuration.Indefinite)
            kapat.cancel()
            if (sonuc == SnackbarResult.ActionPerformed) vm.alimiGeriAl(silinen)
        }
    }

    PullToRefreshBox(
        isRefreshing = ekran.tazeleme.yenileniyor,
        onRefresh = {
            scope.launch {
                if (!vm.yenile()) snackbar.showSnackbar("Fiyatlar az önce güncellendi")
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        val ozet = ekran.ozet
        when {
            !ekran.yuklendi || ozet == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Yükleniyor…")
            }
            ozet.isEmpty -> BosDurum(onEkleGit)
            else -> PortfoyIcerigi(ekran, ozet, bugun, vm, sil)
        }
    }
}

/** Blok 4 — Boş durum: grafikler ve kategori listesi yerine yönlendirme. */
@Composable
private fun BosDurum(onEkleGit: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Portföyün boş — + sekmesinden ilk varlığını ekle.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onEkleGit) { Text("Varlık ekle") }
    }
}

@Composable
private fun PortfoyIcerigi(
    ekran: PortfoyEkranVerisi,
    ozet: PortfolioSummary,
    bugun: LocalDate,
    vm: PortfoyViewModel,
    sil: (Long) -> Unit,
) {
    val veri = ekran.veri!!
    val durum = ekran.durum
    var duzenlenen by remember { mutableStateOf<Pair<Transaction, com.portfoy.model.Asset>?>(null) }
    var fiyatGirilen by remember { mutableStateOf<AssetResult?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Blok 1 — Dağılım grafiği (kategori bazında) ve açıklama listesi.
        item {
            Kutu {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DonutGrafik(ozet.allocation, Modifier.width(190.dp))
                }
                Spacer(Modifier.height(12.dp))
                ozet.allocation.forEach { dilim ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        DilimIsareti(dilim.category)
                        Spacer(Modifier.width(10.dp))
                        Text(dilim.category.etiket(), Modifier.weight(1f))
                        Text(TrFormat.percent(dilim.percent), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Blok 2 — Toplam portföy değeri.
        item {
            Kutu(kalinCerceve = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        TrFormat.money(ozet.totalValue),
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = vm::degerGrafiginiAcKapat) {
                        Icon(Icons.AutoMirrored.Outlined.ShowChart, contentDescription = "Toplam değer grafiği")
                    }
                }
                Text(
                    "${TrFormat.signedPercent(ozet.returnPercent)}   ${TrFormat.signedMoney(ozet.profitLoss)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = getiriRengi(ozet.returnPercent),
                )
                Spacer(Modifier.height(6.dp))
                GuncellemeBilgisi(ekran, veri.lastUpdate, veri.fxTime, ozet)

                AnimatedVisibility(durum.degerGrafigiAcik) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        DegerGrafigi(ekran, vm)
                    }
                }
            }
        }

        // Blok 3 — Kategori kırılımı (akordiyon).
        items(ozet.categories, key = { it.category }) { kategori ->
            KategoriSatiri(
                kategori = kategori,
                acik = kategori.category in durum.acikKategoriler,
                acikVarlik = durum.acikVarlik,
                manuelFiyatli = veri.manualPriceAssetIds,
                manuelZamanlar = veri.manualPriceTimes,
                vm = vm,
                sil = sil,
                duzenle = { islem, varlik -> duzenlenen = islem to varlik },
                fiyatGir = { fiyatGirilen = it },
            )
        }
    }

    duzenlenen?.let { (islem, varlik) ->
        AlimDuzenleDialog(
            islem = islem,
            birim = varlik.unitType,
            birimAdi = varlik.birimEtiketi(),
            bugun = bugun,
            onKaydet = { vm.alimGuncelle(it); duzenlenen = null },
            onIptal = { duzenlenen = null },
        )
    }
    fiyatGirilen?.let { sonuc ->
        ElleFiyatDialog(
            varlik = sonuc,
            onKaydet = { vm.elleFiyatGir(sonuc.asset.id, it); fiyatGirilen = null },
            onIptal = { fiyatGirilen = null },
        )
    }
}

@Composable
private fun GuncellemeBilgisi(ekran: PortfoyEkranVerisi, sonGuncelleme: Instant?, kurZamani: Instant?, ozet: PortfolioSummary) {
    val eski = ekran.tazeleme.sonRapor?.hasProblems == true || ozet.categories.any { k -> k.assets.any { it.priceMissing } }
    Text(
        "son güncelleme: " + (sonGuncelleme?.let { TrFormat.lastUpdate(it, Instant.now(), UygulamaZamanDilimi) } ?: "henüz güncellenmedi"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (eski) {
        Text(
            "veriler güncel değil",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
    }
    // ABD varlıkları son bilinen kurla çevrilir; kur birkaç günden eskiyse bu belirtilir (hafta sonu tatili sayılmaz).
    val abdVar = ozet.categories.any { it.category == Category.ABD }
    if (abdVar && kurZamani != null && Tazelik.kurEskiMi(kurZamani, Instant.now())) {
        Text(
            "ABD değerleri ${Tazelik.gunFarki(kurZamani, Instant.now())} gün önceki kurla hesaplandı",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
    }
    // Büyük portföyde gün içi turlar kapanır; açılış ve kapanış turları sürer (bölüm 2.4).
    val kapali = ozet.categories.filter { TazelemeZamanlayici.gunIciKapali(it.category, it.assets.size) }
    if (kapali.isNotEmpty()) {
        Text(
            kapali.joinToString(", ") { it.category.etiket() } + ": portföy büyük olduğu için gün içi güncelleme kapalı, açılış ve kapanışta güncellenir",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Toplam portföy değerinin zaman grafiği (TL), saklanan geçmiş fiyat serilerinden geriye dönük hesaplanır. Performans
 * sekmesindeki grafik ise getiri yüzdesini gösterir; ikisi farklı şeyleri anlatır ve karıştırılmamalıdır.
 */
@Composable
private fun DegerGrafigi(ekran: PortfoyEkranVerisi, vm: PortfoyViewModel) {
    val grafik by vm.grafik.collectAsState()
    val veri = grafik.veri

    DonemSecici(ekran.durum.donem, vm::donemSec)
    Spacer(Modifier.height(10.dp))

    if (veri == null || veri.noktalar.size < 2) {
        Text(
            if (grafik.gecmisYukleniyor) "Geçmiş fiyatlar yükleniyor…" else "Grafik için en az iki günlük veri gerekir.",
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        CizgiGrafik(
            degerler = veri.noktalar.map { it.valueTl.toDouble() },
            etiket = { i -> "${veri.noktalar[i].date.tr()} • ${TrFormat.money(veri.noktalar[i].valueTl)}" },
        )
    }
    // Portföy seçilen dönemden gençse grafik yalnızca mevcut veri kadar çizilir.
    if (veri?.pencere?.truncated == true) {
        Spacer(Modifier.height(6.dp))
        Text("portföy geçmişi ${veri.pencere.portfolioDays} gün", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
    if (veri?.tahmini == true) {
        Text(
            if (grafik.gecmisYukleniyor) "Bazı fiyat geçmişleri yükleniyor…" else "Bazı günler için fiyat geçmişi yok; o günler maliyetle gösteriliyor.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KategoriSatiri(
    kategori: CategoryResult,
    acik: Boolean,
    acikVarlik: Long?,
    manuelFiyatli: Set<Long>,
    manuelZamanlar: Map<Long, Instant>,
    vm: PortfoyViewModel,
    sil: (Long) -> Unit,
    duzenle: (Transaction, com.portfoy.model.Asset) -> Unit,
    fiyatGir: (AssetResult) -> Unit,
) {
    val piyasaKapali = kategori.category.piyasa?.let { !takvim.isOpen(it, Instant.now()) } == true
    Kutu {
        Row(
            Modifier.fillMaxWidth().clickable { vm.kategoriyiAcKapat(kategori.category) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (acik) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            KategoriIkonu(kategori.category, Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(kategori.category.etiket(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (piyasaKapali) Text("piyasa kapalı", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(TrFormat.money(kategori.value), style = MaterialTheme.typography.titleMedium)
        }
        AnimatedVisibility(acik) {
            Column {
                kategori.assets.forEach { varlik ->
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    VarlikSatiri(
                        sonuc = varlik,
                        acik = acikVarlik == varlik.asset.id,
                        elleFiyat = varlik.asset.id in manuelFiyatli,
                        elleFiyatZamani = manuelZamanlar[varlik.asset.id],
                        vm = vm,
                        sil = sil,
                        duzenle = duzenle,
                        fiyatGir = fiyatGir,
                    )
                }
            }
        }
    }
}

@Composable
private fun VarlikSatiri(
    sonuc: AssetResult,
    acik: Boolean,
    elleFiyat: Boolean,
    elleFiyatZamani: Instant?,
    vm: PortfoyViewModel,
    sil: (Long) -> Unit,
    duzenle: (Transaction, com.portfoy.model.Asset) -> Unit,
    fiyatGir: (AssetResult) -> Unit,
) {
    val nakit = sonuc.asset.category == Category.NAKIT
    Column(Modifier.fillMaxWidth().clickable { vm.varligiAcKapat(sonuc.asset.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sonuc.asset.code, fontWeight = FontWeight.Bold)
                if (!nakit) Text(sonuc.asset.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(TrFormat.money(sonuc.currentValue), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(2.dp))
        val miktar = if (nakit) "" else "${TrFormat.quantity(sonuc.quantity)} ${sonuc.asset.birimEtiketi()}"
        // Miktar nötr, getiri kısmı renkli: tek satırda iki farklı renk gerektiği için AnnotatedString.
        val getiriRenk = getiriRengi(sonuc.returnPercent)
        Text(
            buildAnnotatedString {
                if (miktar.isNotBlank()) append(miktar)
                if (!nakit) {
                    if (miktar.isNotBlank()) append("   •   ")
                    withStyle(SpanStyle(color = getiriRenk)) {
                        append(TrFormat.signedPercent(sonuc.returnPercent))
                        append("  ")
                        append(TrFormat.signedMoney(sonuc.profitLoss))
                    }
                }
            },
            style = MaterialTheme.typography.bodySmall,
        )
        if (sonuc.priceMissing) Text("fiyat alınamadı", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        if (elleFiyat) {
            // Elle girilen fiyat otomatik güncellenmez; 7 günü geçtiyse kullanıcı uyarılır (karar 9).
            val eski = elleFiyatZamani != null && Tazelik.elleFiyatEskiMi(elleFiyatZamani, Instant.now())
            Text(
                if (eski) "elle girilen fiyat • ${Tazelik.gunFarki(elleFiyatZamani, Instant.now())} gün önce girildi, güncel değil" else "elle girilen fiyat",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    AnimatedVisibility(acik) {
        VarlikDetayi(sonuc, elleFiyat, vm, sil, duzenle, fiyatGir)
    }
}

/** Varlık detayı (W9): alım kayıtları, ağırlıklı ortalama maliyet, güncel fiyat, düzenle ve sil. */
@Composable
private fun VarlikDetayi(
    sonuc: AssetResult,
    elleFiyat: Boolean,
    vm: PortfoyViewModel,
    sil: (Long) -> Unit,
    duzenle: (Transaction, com.portfoy.model.Asset) -> Unit,
    fiyatGir: (AssetResult) -> Unit,
) {
    val ekran by vm.ekran.collectAsState()
    val islemler = ekran.veri?.holdings?.firstOrNull { it.asset.id == sonuc.asset.id }?.transactions.orEmpty()
    val nakit = sonuc.asset.category == Category.NAKIT

    Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!nakit) {
            SatirBilgi("Ağırlıklı ortalama maliyet", TrFormat.money(sonuc.unitCost))
            SatirBilgi("Güncel fiyat", TrFormat.money(sonuc.currentPriceTl))
            SatirBilgi("Toplam maliyet", TrFormat.money(sonuc.totalCost))
            if (elleFiyat || sonuc.priceMissing) {
                OutlinedButton(onClick = { fiyatGir(sonuc) }) { Text("Fiyatı elle güncelle") }
            }
        }
        Text(if (nakit) "Nakit girişleri" else "Alım kayıtları", fontWeight = FontWeight.Bold)
        islemler.sortedByDescending { it.tradeDate }.forEach { islem ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val ayrinti = if (nakit) TrFormat.money(islem.quantity)
                    else "${TrFormat.quantity(islem.quantity)} × ${TrFormat.money(islem.unitPriceTl)}" +
                        if (islem.commissionTl.signum() > 0) " (+${TrFormat.money(islem.commissionTl)} komisyon)" else ""
                    Text("${islem.tradeDate.tr()}  •  $ayrinti", style = MaterialTheme.typography.bodySmall)
                    islem.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                IconButton(onClick = { duzenle(islem, sonuc.asset) }) { Icon(Icons.Filled.Edit, contentDescription = "Düzenle") }
                IconButton(onClick = { sil(islem.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Sil") }
            }
        }
    }
}

@Composable
private fun SatirBilgi(etiket: String, deger: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiket, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(deger, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

private fun BigDecimal.metin(): String = stripTrailingZeros().toPlainString().replace('.', ',')

@Composable
private fun AlimDuzenleDialog(
    islem: Transaction,
    birim: UnitType,
    birimAdi: String?,
    bugun: LocalDate,
    onKaydet: (Transaction) -> Unit,
    onIptal: () -> Unit,
) {
    val nakit = birim == UnitType.TL
    val durum = remember(islem.id) {
        AlimFormDurumu(
            fiyat = islem.unitPriceTl.metin(),
            adet = islem.quantity.metin(),
            komisyon = if (islem.commissionTl.signum() > 0) islem.commissionTl.metin() else "",
            not = islem.note.orEmpty(),
            tarih = islem.tradeDate,
        )
    }
    AlertDialog(
        onDismissRequest = onIptal,
        title = { Text("Alım kaydını düzenle") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AlimFormAlanlari(durum, birim, abd = false, kur = null, bugun = bugun, birimAdi = birimAdi)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                durum.denendi = true
                val sonuc = durum.dogrula(bugun, nakit, null)
                sonuc.degerler?.let { d ->
                    onKaydet(
                        islem.copy(
                            quantity = d.adet,
                            unitPriceTl = d.fiyat,
                            commissionTl = d.komisyon,
                            tradeDate = d.tarih,
                            note = durum.not.takeIf { it.isNotBlank() },
                        ),
                    )
                }
            }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onIptal) { Text("Vazgeç") } },
    )
}

/** W11 — Elle fiyat girişi. Kaynağı olmayan ya da fiyatı alınamayan varlıklar için kalıcı yedek. */
@Composable
private fun ElleFiyatDialog(varlik: AssetResult, onKaydet: (BigDecimal) -> Unit, onIptal: () -> Unit) {
    var metin by remember { mutableStateOf(varlik.currentPriceTl.metin()) }
    var denendi by remember { mutableStateOf(false) }
    val deger = parseDecimal(metin)
    val hata = denendi && (deger == null || deger.signum() <= 0)

    AlertDialog(
        onDismissRequest = onIptal,
        title = { Text("${varlik.asset.code} — fiyatı elle güncelle") },
        text = {
            Column {
                Text(
                    "Son girilen fiyat: ${TrFormat.money(varlik.currentPriceTl)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = metin,
                    onValueChange = { metin = it },
                    label = { Text("Güncel fiyat (₺)") },
                    isError = hata,
                    supportingText = if (hata) ({ Text("Sıfırdan büyük bir fiyat gir", fontWeight = FontWeight.Bold) }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                denendi = true
                if (deger != null && deger.signum() > 0) onKaydet(deger)
            }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onIptal) { Text("Vazgeç") } },
    )
}

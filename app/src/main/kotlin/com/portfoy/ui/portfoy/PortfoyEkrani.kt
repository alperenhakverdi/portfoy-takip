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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import com.portfoy.model.Category
import com.portfoy.ui.bilesenler.DonutGrafik
import com.portfoy.ui.bilesenler.Kutu
import com.portfoy.ui.bilesenler.birimEtiketi
import com.portfoy.ui.bilesenler.KategoriIkonu
import com.portfoy.ui.bilesenler.etiket
import com.portfoy.ui.para.ParaBirimiTercihi
import com.portfoy.ui.para.cevrilmisTutar
import com.portfoy.ui.para.usdDogalMi
import com.portfoy.ui.tema.TemaTercihi
import com.portfoy.ui.tema.TemaViewModel
import com.portfoy.ui.tema.getiriRengi
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.launch

/** Sekme 2 — Portföy. Uygulamanın ana ekranı. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfoyEkrani(
    onEkleGit: () -> Unit,
    onVarlikTikla: (Long) -> Unit,
    onGrafikGit: () -> Unit,
    snackbar: SnackbarHostState,
    vm: PortfoyViewModel = hiltViewModel(),
    temaVm: TemaViewModel = hiltViewModel(),
) {
    val ekran by vm.ekran.collectAsState()
    val scope = rememberCoroutineScope()
    var temaDialogAcik by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // M18 — tema tercihi: sağ üstte küçük bir ikon, üç seçenekli diyalog açar.
        Row(Modifier.fillMaxWidth().padding(end = 4.dp, top = 4.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { temaDialogAcik = true }) {
                Icon(Icons.Filled.DarkMode, contentDescription = "Tema tercihi")
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
                else -> PortfoyIcerigi(ekran, ozet, vm, onVarlikTikla, onGrafikGit)
            }
        }
    }

    if (temaDialogAcik) {
        TemaTercihiDialog(temaVm, onKapat = { temaDialogAcik = false })
    }
}

@Composable
private fun TemaTercihiDialog(temaVm: TemaViewModel, onKapat: () -> Unit) {
    val mevcut by temaVm.tercih.collectAsState()
    AlertDialog(
        onDismissRequest = onKapat,
        title = { Text("Tema") },
        text = {
            Column {
                TemaSecenegi("Sistem", TemaTercihi.SISTEM, mevcut) { temaVm.ayarla(it); onKapat() }
                TemaSecenegi("Açık", TemaTercihi.ACIK, mevcut) { temaVm.ayarla(it); onKapat() }
                TemaSecenegi("Koyu", TemaTercihi.KOYU, mevcut) { temaVm.ayarla(it); onKapat() }
            }
        },
        confirmButton = { TextButton(onClick = onKapat) { Text("Kapat") } },
    )
}

@Composable
private fun TemaSecenegi(etiket: String, deger: TemaTercihi, secili: TemaTercihi, sec: (TemaTercihi) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { sec(deger) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = deger == secili, onClick = { sec(deger) })
        Spacer(Modifier.width(4.dp))
        Text(etiket)
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
    vm: PortfoyViewModel,
    onVarlikTikla: (Long) -> Unit,
    onGrafikGit: () -> Unit,
) {
    val veri = ekran.veri!!
    val durum = ekran.durum
    val donemsel by vm.donemselGetiriler.collectAsState()
    val paraBirimi by vm.paraBirimi.collectAsState()
    val (gosterilenDeger, birim) = cevrilmisTutar(ozet.totalValue, paraBirimi, veri.usdTryRate)
    val secilenGetiri = donemsel.toplam[durum.ozetDonemi]
    val gosterilenGetiri = secilenGetiri?.let {
        GetiriDegeri(cevrilmisTutar(it.tl, paraBirimi, veri.usdTryRate).first, it.yuzde)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Blok 1 — Dağılım grafiği. M24: yüzdeler altta liste değil, halkanın kenarında kılavuz çizgileriyle.
        item {
            Kutu {
                DonutGrafik(ozet.allocation, Modifier.fillMaxWidth().height(180.dp))
            }
        }

        // Blok 2 — Toplam portföy değeri + getiri (M21: dönem seçilebilir, grafik ayrı ekranda).
        item {
            Kutu(kalinCerceve = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // M23 — rakama dokununca TL↔USD değişir (ayrı bir düğme yok).
                    Text(
                        TrFormat.money(gosterilenDeger, birim),
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.weight(1f).clickable(onClick = vm::paraBirimiDegistir),
                    )
                    IconButton(onClick = onGrafikGit) {
                        Icon(Icons.AutoMirrored.Outlined.ShowChart, contentDescription = "Grafik")
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    KarZararYazisi(
                        gosterilenGetiri,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f).clickable(onClick = vm::paraBirimiDegistir),
                        birim = birim,
                    )
                    DonemDugmesi(durum.ozetDonemi, onTikla = vm::ozetDonemiDegistir)
                }
                Spacer(Modifier.height(6.dp))
                GuncellemeBilgisi(veri.lastUpdate, veri.fxTime, ozet)
            }
        }

        // Blok 3 — Kategori kırılımı (akordiyon).
        items(ozet.categories, key = { it.category }) { kategori ->
            KategoriSatiri(
                kategori = kategori,
                acik = kategori.category in durum.acikKategoriler,
                donem = durum.kategoriDonemi(kategori.category),
                getiri = donemsel.kategori(durum.kategoriDonemi(kategori.category), kategori.category),
                manuelFiyatli = veri.manualPriceAssetIds,
                manuelZamanlar = veri.manualPriceTimes,
                usdTryKuru = veri.usdTryRate,
                vm = vm,
                onVarlikTikla = onVarlikTikla,
            )
        }
    }
}

@Composable
private fun GuncellemeBilgisi(sonGuncelleme: Instant?, kurZamani: Instant?, ozet: PortfolioSummary) {
    Text(
        "son güncelleme: " + (sonGuncelleme?.let { TrFormat.lastUpdate(it, Instant.now(), com.portfoy.di.UygulamaZamanDilimi) } ?: "henüz güncellenmedi"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
 * M21 — kâr/zarar her yerde aynı biçimde: TL solda, yüzde parantez içinde. Yön oku ve renk TL'nin
 * işaretinden gelir (ikisi aynı kârın iki birimi, ayrı ayrı yön taşımaz). Veri yoksa "—".
 */
@Composable
private fun KarZararYazisi(
    getiri: GetiriDegeri?,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    modifier: Modifier = Modifier,
    birim: String = "₺",
) {
    if (getiri == null) {
        Text(TrFormat.EMPTY, style = style, modifier = modifier, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Text(
        "${TrFormat.signedMoney(getiri.tl, birim)} (${TrFormat.percent(getiri.yuzde?.abs())})",
        style = style,
        modifier = modifier,
        color = getiriRengi(getiri.tl),
        maxLines = 1,
    )
}

/** M21 — dönem düğmesi: dokununca Günlük → Haftalık → Tümü → Günlük sırasıyla değişir. */
@Composable
private fun DonemDugmesi(secim: GetiriDonemi, onTikla: () -> Unit) {
    Surface(
        onClick = onTikla,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            secim.kisaEtiket,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun KategoriSatiri(
    kategori: CategoryResult,
    acik: Boolean,
    donem: GetiriDonemi,
    getiri: com.portfoy.data.repository.KategoriDonemGetirisi?,
    manuelFiyatli: Set<Long>,
    manuelZamanlar: Map<Long, Instant>,
    usdTryKuru: BigDecimal?,
    vm: PortfoyViewModel,
    onVarlikTikla: (Long) -> Unit,
) {
    val usd = usdDogalMi(kategori.category)
    val (gosterilenDeger, birim) = if (usd) cevrilmisTutar(kategori.value, ParaBirimiTercihi.USD, usdTryKuru)
    else kategori.value to "₺"
    val gosterilenGetiri = getiri?.let {
        val tl = if (usd) cevrilmisTutar(it.tl, ParaBirimiTercihi.USD, usdTryKuru).first else it.tl
        GetiriDegeri(tl, it.yuzde)
    }

    Kutu {
        Row(
            Modifier.fillMaxWidth().clickable { vm.kategoriyiAcKapat(kategori.category) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (acik) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            KategoriIkonu(kategori.category, Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(kategori.category.etiket(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(TrFormat.money(gosterilenDeger, birim), style = MaterialTheme.typography.titleMedium)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            KarZararYazisi(
                gosterilenGetiri,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp),
                birim = birim,
            )
            DonemDugmesi(donem, onTikla = { vm.kategoriDonemiDegistir(kategori.category) })
        }
        AnimatedVisibility(acik) {
            Column {
                kategori.assets.forEach { varlik ->
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    VarlikSatiri(
                        sonuc = varlik,
                        getiri = getiri?.varliklar?.firstOrNull { it.varlik.id == varlik.asset.id }
                            ?.let { GetiriDegeri(it.tl, it.yuzde) },
                        usd = usd,
                        usdTryKuru = usdTryKuru,
                        elleFiyat = varlik.asset.id in manuelFiyatli,
                        elleFiyatZamani = manuelZamanlar[varlik.asset.id],
                        onTikla = { onVarlikTikla(varlik.asset.id) },
                    )
                }
            }
        }
    }
}

/** M17 — satıra tıklayınca artık burada açılmaz, ayrı bir varlık yönetimi ekranına gidilir. */
@Composable
private fun VarlikSatiri(
    sonuc: AssetResult,
    getiri: GetiriDegeri?,
    usd: Boolean,
    usdTryKuru: BigDecimal?,
    elleFiyat: Boolean,
    elleFiyatZamani: Instant?,
    onTikla: () -> Unit,
) {
    val nakit = sonuc.asset.category == Category.NAKIT
    val (gosterilenDeger, birim) = if (usd) cevrilmisTutar(sonuc.currentValue, ParaBirimiTercihi.USD, usdTryKuru)
    else sonuc.currentValue to "₺"
    val gosterilenGetiri = if (usd) getiri?.let {
        GetiriDegeri(cevrilmisTutar(it.tl, ParaBirimiTercihi.USD, usdTryKuru).first, it.yuzde)
    } else getiri

    Column(Modifier.fillMaxWidth().clickable(onClick = onTikla)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sonuc.asset.code, fontWeight = FontWeight.Bold)
                if (!nakit) Text(sonuc.asset.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(TrFormat.money(gosterilenDeger, birim), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(2.dp))
        if (!nakit) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${TrFormat.quantity(sonuc.quantity)} ${sonuc.asset.birimEtiketi()}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                KarZararYazisi(gosterilenGetiri, style = MaterialTheme.typography.bodySmall, birim = birim)
            }
        }
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
}

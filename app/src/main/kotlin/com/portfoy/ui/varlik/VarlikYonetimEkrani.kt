package com.portfoy.ui.varlik

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.portfoy.calc.AssetResult
import com.portfoy.calc.Tazelik
import com.portfoy.calc.format.TrFormat
import com.portfoy.calc.parseDecimal
import com.portfoy.di.UygulamaZamanDilimi
import com.portfoy.model.Category
import com.portfoy.model.Transaction
import com.portfoy.model.TransactionType
import com.portfoy.model.UnitType
import com.portfoy.ui.bilesenler.AlimFormAlanlari
import com.portfoy.ui.bilesenler.AlimFormDurumu
import com.portfoy.ui.bilesenler.KategoriIkonu
import com.portfoy.ui.bilesenler.birimEtiketi
import com.portfoy.ui.bilesenler.tr
import com.portfoy.ui.tema.getiriRengi
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * M17 — Varlık yönetimi ekranı. Hem Portföy'den hem Performans'tan bir varlığa tıklayınca açılır:
 * özet bilgiler, hareket listesi (düzenle/sil) ve "+ Ekle" / "− Azalt" hareketleri.
 */
@Composable
fun VarlikYonetimEkrani(
    onGeri: () -> Unit,
    snackbar: SnackbarHostState,
    vm: VarlikYonetimViewModel = hiltViewModel(),
) {
    val ekran by vm.ekran.collectAsState()
    val scope = rememberCoroutineScope()
    val bugun = remember { LocalDate.now(UygulamaZamanDilimi) }

    var duzenlenen by remember { mutableStateOf<Transaction?>(null) }
    var fiyatGirDialogAcik by remember { mutableStateOf(false) }
    var ekleDialogAcik by remember { mutableStateOf(false) }
    var azaltDialogAcik by remember { mutableStateOf(false) }

    val sil: (Long) -> Unit = { id ->
        scope.launch {
            val silinen = vm.alimSil(id) ?: return@launch
            val kapat = launch {
                kotlinx.coroutines.delay(5_000)
                snackbar.currentSnackbarData?.dismiss()
            }
            val sonuc = snackbar.showSnackbar("Hareket silindi", "Geri al", duration = SnackbarDuration.Indefinite)
            kapat.cancel()
            if (sonuc == SnackbarResult.ActionPerformed) vm.alimiGeriAl(silinen)
        }
    }

    Column(Modifier.fillMaxSize()) {
        val sonuc = ekran.sonuc
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onGeri) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
            sonuc?.let { KategoriIkonu(it.asset.category, Modifier.size(24.dp).padding(end = 8.dp)) }
            Column {
                Text(sonuc?.asset?.code ?: "…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                sonuc?.asset?.name?.takeIf { it != sonuc.asset.code }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (!ekran.yuklendi) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Yükleniyor…") }
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            item {
                Ozet(sonuc, ekran.elleFiyat, ekran.elleFiyatZamani) { fiyatGirDialogAcik = true }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { ekleDialogAcik = true }, modifier = Modifier.padding(0.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Ekle")
                    }
                    OutlinedButton(onClick = { azaltDialogAcik = true }) {
                        Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Azalt")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(if (sonuc?.asset?.let { it.category == Category.NAKIT } == true) "Nakit girişleri" else "Hareketler", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
            }
            items(ekran.islemler.sortedByDescending { it.tradeDate }, key = { it.id }) { islem ->
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                HareketSatiri(islem, duzenle = { duzenlenen = it }, sil = sil)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    duzenlenen?.let { islem ->
        val sonuc = ekran.sonuc
        AlimDuzenleDialog(
            islem = islem,
            birim = sonuc?.asset?.unitType,
            birimAdi = sonuc?.asset?.birimEtiketi(),
            bugun = bugun,
            onKaydet = { vm.alimGuncelle(it); duzenlenen = null },
            onIptal = { duzenlenen = null },
        )
    }
    if (fiyatGirDialogAcik) {
        ekran.sonuc?.let { sonuc ->
            ElleFiyatDialog(
                sonuc = sonuc,
                onKaydet = { vm.elleFiyatGir(it); fiyatGirDialogAcik = false },
                onIptal = { fiyatGirDialogAcik = false },
            )
        }
    }
    if (ekleDialogAcik) {
        ekran.sonuc?.let { sonuc ->
            HareketDialog(
                baslik = "Ekle",
                birim = sonuc.asset.unitType,
                birimAdi = sonuc.asset.birimEtiketi(),
                bugun = bugun,
                komisyonGoster = true,
                tur = TransactionType.ALIS,
                onKaydet = { adet, fiyat, komisyon, tarih, not -> vm.ekle(adet, fiyat, komisyon, tarih, not); ekleDialogAcik = false },
                onIptal = { ekleDialogAcik = false },
            )
        }
    }
    if (azaltDialogAcik) {
        ekran.sonuc?.let { sonuc ->
            HareketDialog(
                baslik = "Azalt",
                birim = sonuc.asset.unitType,
                birimAdi = sonuc.asset.birimEtiketi(),
                bugun = bugun,
                komisyonGoster = false,
                tur = TransactionType.AZALTMA,
                mevcutAdet = sonuc.quantity,
                onKaydet = { adet, fiyat, _, tarih, not -> vm.azalt(adet, fiyat, tarih, not); azaltDialogAcik = false },
                onIptal = { azaltDialogAcik = false },
            )
        }
    }
}

@Composable
private fun Ozet(sonuc: AssetResult?, elleFiyat: Boolean, elleFiyatZamani: Instant?, fiyatGir: () -> Unit) {
    if (sonuc == null) {
        Text("Bu varlık artık portföyde yok (adet sıfırlandı). Aşağıdan tekrar ekleyebilirsin.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val nakit = sonuc.asset.category == Category.NAKIT
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(TrFormat.money(sonuc.currentValue), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        if (!nakit) {
            Text(
                "${TrFormat.signedPercent(sonuc.returnPercent)}   ${TrFormat.signedMoney(sonuc.profitLoss)}",
                style = MaterialTheme.typography.titleMedium,
                color = getiriRengi(sonuc.returnPercent),
            )
            Spacer(Modifier.height(6.dp))
            SatirBilgi("Ağırlıklı ortalama maliyet", TrFormat.money(sonuc.unitCost))
            SatirBilgi("Güncel fiyat", TrFormat.money(sonuc.currentPriceTl))
            SatirBilgi("Toplam maliyet", TrFormat.money(sonuc.totalCost))
            if (sonuc.priceMissing) Text("fiyat alınamadı", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            if (elleFiyat) {
                val eski = elleFiyatZamani != null && Tazelik.elleFiyatEskiMi(elleFiyatZamani, Instant.now())
                Text(
                    if (eski) "elle girilen fiyat • ${Tazelik.gunFarki(elleFiyatZamani, Instant.now())} gün önce girildi, güncel değil" else "elle girilen fiyat",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (elleFiyat || sonuc.priceMissing) {
                OutlinedButton(onClick = fiyatGir) { Text("Fiyatı elle güncelle") }
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

@Composable
private fun HareketSatiri(islem: Transaction, duzenle: (Transaction) -> Unit, sil: (Long) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            val azaltma = islem.type == TransactionType.AZALTMA
            val isaret = if (azaltma) "−" else ""
            val ayrinti = "$isaret${TrFormat.quantity(islem.quantity)} × ${TrFormat.money(islem.unitPriceTl)}" +
                if (islem.commissionTl.signum() > 0) " (+${TrFormat.money(islem.commissionTl)} komisyon)" else ""
            Text("${islem.tradeDate.tr()}  •  $ayrinti", style = MaterialTheme.typography.bodySmall, fontWeight = if (azaltma) FontWeight.Bold else FontWeight.Normal)
            islem.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        IconButton(onClick = { duzenle(islem) }) { Icon(Icons.Filled.Edit, contentDescription = "Düzenle") }
        IconButton(onClick = { sil(islem.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Sil") }
    }
}

private fun BigDecimal.metin(): String = stripTrailingZeros().toPlainString().replace('.', ',')

@Composable
private fun AlimDuzenleDialog(
    islem: Transaction,
    birim: UnitType?,
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
        title = { Text(if (islem.type == TransactionType.AZALTMA) "Azaltma kaydını düzenle" else "Alım kaydını düzenle") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AlimFormAlanlari(durum, birim ?: UnitType.ADET, abd = false, kur = null, bugun = bugun, birimAdi = birimAdi, tur = islem.type)
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
private fun ElleFiyatDialog(sonuc: AssetResult, onKaydet: (BigDecimal) -> Unit, onIptal: () -> Unit) {
    var metin by remember { mutableStateOf(sonuc.currentPriceTl.metin()) }
    var denendi by remember { mutableStateOf(false) }
    val deger = parseDecimal(metin)
    val hata = denendi && (deger == null || deger.signum() <= 0)

    AlertDialog(
        onDismissRequest = onIptal,
        title = { Text("${sonuc.asset.code} — fiyatı elle güncelle") },
        text = {
            Column {
                Text("Son girilen fiyat: ${TrFormat.money(sonuc.currentPriceTl)}", style = MaterialTheme.typography.bodySmall)
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

/**
 * "+ Ekle" / "− Azalt" ortak diyaloğu. Aynı [AlimFormAlanlari] iki yönde de kullanılır (M17.2);
 * azaltmada komisyon alanı gösterilmez ve gönderilmez, elindeki adet üstte hatırlatılır.
 */
@Composable
private fun HareketDialog(
    baslik: String,
    birim: UnitType,
    birimAdi: String?,
    bugun: LocalDate,
    komisyonGoster: Boolean,
    tur: TransactionType,
    mevcutAdet: BigDecimal? = null,
    onKaydet: (adet: BigDecimal, fiyat: BigDecimal, komisyon: BigDecimal, tarih: LocalDate, not: String?) -> Unit,
    onIptal: () -> Unit,
) {
    val nakit = birim == UnitType.TL
    val durum = remember { AlimFormDurumu(tarih = bugun) }
    AlertDialog(
        onDismissRequest = onIptal,
        title = { Text(baslik) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                mevcutAdet?.let {
                    Text(
                        "Elindeki: ${TrFormat.quantity(it)} ${birimAdi ?: ""}".trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                AlimFormAlanlari(durum, birim, abd = false, kur = null, bugun = bugun, birimAdi = birimAdi, tur = tur)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                durum.denendi = true
                val sonuc = durum.dogrula(bugun, nakit, null)
                sonuc.degerler?.let { d ->
                    onKaydet(d.adet, d.fiyat, if (komisyonGoster) d.komisyon else BigDecimal.ZERO, d.tarih, durum.not.takeIf { it.isNotBlank() })
                }
            }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onIptal) { Text("Vazgeç") } },
    )
}

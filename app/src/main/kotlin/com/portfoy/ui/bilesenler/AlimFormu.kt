package com.portfoy.ui.bilesenler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.portfoy.calc.AlimSonucu
import com.portfoy.calc.dogrulaAlim
import com.portfoy.calc.format.TrFormat
import com.portfoy.calc.parseDecimal
import com.portfoy.calc.purchaseTotal
import com.portfoy.model.TransactionType
import com.portfoy.model.UnitType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Alım formunun girdileri. Hem yeni alım hem düzenleme aynı alanları kullanır. */
class AlimFormDurumu(
    fiyat: String = "",
    adet: String = "",
    komisyon: String = "",
    not: String = "",
    tarih: LocalDate,
) {
    var fiyat by mutableStateOf(fiyat)
    var adet by mutableStateOf(adet)
    var komisyon by mutableStateOf(komisyon)
    var not by mutableStateOf(not)
    var tarih by mutableStateOf(tarih)

    /** ABD varlıklarında fiyat USD girilip alış tarihindeki kurla TL'ye çevrilebilir. */
    var usdModu by mutableStateOf(false)
    var usdFiyat by mutableStateOf("")

    /** Kaydet'e basıldıktan sonra hatalar gösterilir. */
    var denendi by mutableStateOf(false)

    /** TL alış fiyatı metni: USD modundaysa USD × kur, değilse kullanıcının yazdığı. */
    fun tlFiyatMetni(kur: BigDecimal?): String {
        if (!usdModu) return fiyat
        val usd = parseDecimal(usdFiyat)
        if (usd == null || kur == null) return ""
        return (usd * kur).setScale(4, RoundingMode.HALF_UP).toPlainString().replace('.', ',')
    }

    fun dogrula(bugun: LocalDate, nakit: Boolean, kur: BigDecimal?): AlimSonucu =
        dogrulaAlim(tlFiyatMetni(kur), adet, komisyon, tarih, bugun, nakit)
}

/**
 * Alım formu alanları (bölüm 4.2): alış tarihi (varsayılan bugün), alış fiyatı, adet/gram/pay ya da nakit
 * tutarı, komisyon, not. Alt kısımda toplam maliyet anlık gösterilir. Nakit TL'de yalnızca tutar istenir.
 *
 * [tur] azaltma ise (M17) tüm metinler "alış" yerine "satış" der — kullanıcı elindeki bir varlığı
 * azaltırken girdiği fiyatın satış fiyatı olduğu açık olsun diye (doğrulama mantığı aynı kalır).
 */
@Composable
fun AlimFormAlanlari(
    durum: AlimFormDurumu,
    birim: UnitType,
    abd: Boolean,
    kur: BigDecimal?,
    bugun: LocalDate,
    modifier: Modifier = Modifier,
    /** Dövizde miktar alanının birimi (USD, EUR). Diğer varlıklarda kullanılmaz. */
    birimAdi: String? = null,
    tur: TransactionType = TransactionType.ALIS,
) {
    val nakit = birim == UnitType.TL
    val sonuc = durum.dogrula(bugun, nakit, kur)
    val hatalar = if (durum.denendi) sonuc.hatalar else null
    val azaltma = tur == TransactionType.AZALTMA
    val fiyatSozcugu = if (azaltma) "Satış" else "Alış"

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TarihSecici(durum.tarih, bugun, hatalar?.tarih, etiket = "$fiyatSozcugu tarihi") { durum.tarih = it }

        if (!nakit) {
            if (abd) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Fiyatı USD olarak gir", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (kur == null) "Kur bilgisi yok, önce fiyatlar güncellenmeli"
                            else "$fiyatSozcugu tarihindeki kurla TL'ye çevrilir (kur: ${TrFormat.money(kur)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = durum.usdModu, onCheckedChange = { durum.usdModu = it }, enabled = kur != null)
                }
            }
            if (durum.usdModu) {
                Alan("$fiyatSozcugu fiyatı (USD)", durum.usdFiyat, { durum.usdFiyat = it }, hatalar?.fiyat)
                Text(
                    "$fiyatSozcugu fiyatı (₺): ${durum.tlFiyatMetni(kur).ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Alan("$fiyatSozcugu fiyatı (₺)", durum.fiyat, { durum.fiyat = it }, hatalar?.fiyat)
            }
        }

        val adetEtiketi = when (birim) {
            UnitType.TL -> "Tutar (₺)"
            UnitType.GRAM -> "Miktar (gram)"
            UnitType.PAY -> "Adet (pay)"
            UnitType.BIRIM -> "Miktar (${birimAdi ?: "birim"})"
            UnitType.ADET -> "Adet"
        }
        Alan(adetEtiketi, durum.adet, { durum.adet = it }, hatalar?.adet)

        if (!nakit) {
            Alan("Komisyon / masraf (₺) — isteğe bağlı", durum.komisyon, { durum.komisyon = it }, hatalar?.komisyon)
        }
        OutlinedTextField(
            value = durum.not,
            onValueChange = { durum.not = it },
            label = { Text("Not — isteğe bağlı") },
            modifier = Modifier.fillMaxWidth(),
        )

        val degerler = sonuc.degerler
        val toplam = degerler?.let { purchaseTotal(it.fiyat, it.adet, it.komisyon) }
        val onizleme = if (toplam != null) toplam else {
            // Geçersiz olsa bile fiyat ve adet okunabiliyorsa anlık gösterilir.
            val f = if (nakit) BigDecimal.ONE else parseDecimal(durum.tlFiyatMetni(kur))
            val a = parseDecimal(durum.adet)
            if (f != null && a != null && f.signum() > 0 && a.signum() > 0) purchaseTotal(f, a) else null
        }
        Text(
            "${if (azaltma) "Toplam tutar" else "Toplam maliyet"}: ${TrFormat.money(onizleme)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Alan(etiket: String, deger: String, degisti: (String) -> Unit, hata: String?) {
    OutlinedTextField(
        value = deger,
        onValueChange = degisti,
        label = { Text(etiket) },
        isError = hata != null,
        supportingText = hata?.let { { Text(it, fontWeight = FontWeight.Bold) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarihSecici(tarih: LocalDate, bugun: LocalDate, hata: String?, etiket: String = "Alış tarihi", degisti: (LocalDate) -> Unit) {
    var acik by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { acik = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$etiket: ${tarih.tr()}")
        }
        if (hata != null) {
            Text(hata, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
    }
    if (acik) {
        val bugunMs = bugun.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val durum = rememberDatePickerState(
            initialSelectedDateMillis = tarih.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= bugunMs
            },
        )
        DatePickerDialog(
            onDismissRequest = { acik = false },
            confirmButton = {
                TextButton(onClick = {
                    durum.selectedDateMillis?.let { ms ->
                        degisti(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    acik = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { acik = false }) { Text("Vazgeç") } },
        ) { DatePicker(state = durum) }
    }
}

package com.portfoy.ui.bilesenler

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.portfoy.calc.AllocationSlice
import com.portfoy.calc.format.TrFormat
import com.portfoy.ui.tema.AppTema
import com.portfoy.ui.tema.LocalReducedMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** M24 — halkanın kenarındaki bir etiketin yerleşimi: kılavuz çizgisi nereye gidecek, metin nerede duracak. */
private class EtiketYerlesimi(
    /** Dilimin orta açısı (derece, 0 = sağ, saat yönünde). */
    val ortaAci: Float,
    val sagda: Boolean,
    val olcum: TextLayoutResult,
    /** Kılavuz çizgisinin rengi — çıktığı dilimin kategori rengiyle aynı. */
    val renk: Color,
    /** Çakışma ayıklamasından sonra belirlenen dikey merkez. */
    var merkezY: Float,
)

/**
 * Kategori dağılımı halka grafiği. Her dilim kendi kategori rengini kullanır (M14.3) — gri
 * tonlar/desenler yerine, gerçek ve tutarlı bir renk kimliği. İlk çizimde 500 ms'lik bir sweep
 * animasyonuyla açılır (M14.6); sistemde "animasyonları azalt" açıksa bu animasyon atlanır.
 *
 * M24 — yüzdeler artık altta ayrı bir liste değil, halkanın kendi kenarında: her dilimden dışarı bir
 * kılavuz çizgisi çıkar ve ucunda "%12,34 ABD" yazar. Çizgi, çıktığı dilimin kategori rengini taşır —
 * kaldırılan açıklama listesindeki renkli noktanın işini bu üstlenir. Sayı her zaman halkaya yakın
 * taraftadır (sağda önce yüzde, solda önce kategori adı). Aynı taraftaki etiketler üst üste binmeyecek
 * şekilde dikeyde ayrıştırılır. Ekran okuyucu için tüm dağılım [contentDescription]'da yazılı kalır.
 */
@Composable
fun DonutGrafik(dilimler: List<AllocationSlice>, modifier: Modifier = Modifier) {
    val renkler = AppTema.renkler
    val aciklama = dilimler.joinToString { "${it.category.etiket()} ${TrFormat.percent(it.percent)}" }
    val azaltilmisAnimasyon = LocalReducedMotion.current
    val ilerleme = remember(dilimler) { Animatable(if (azaltilmisAnimasyon) 1f else 0f) }
    LaunchedEffect(dilimler, azaltilmisAnimasyon) {
        if (!azaltilmisAnimasyon) ilerleme.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }

    val olcer = rememberTextMeasurer()
    val stil = MaterialTheme.typography.labelSmall
    val adRengi = MaterialTheme.colorScheme.onSurfaceVariant
    val yuzdeRengi = MaterialTheme.colorScheme.onSurface

    Canvas(modifier.semantics { contentDescription = "Portföy dağılımı: $aciklama" }) {
        if (dilimler.isEmpty()) return@Canvas

        // 1) Etiketler ölçülür. Açılar animasyondan bağımsız, son hâline göre hesaplanır: etiketler
        // yerinde durur, animasyon boyunca yalnızca belirir.
        var aci = -90f
        val yerlesim = dilimler.map { dilim ->
            val tam = dilim.percent.toFloat() / 100f * 360f
            val orta = aci + tam / 2f
            aci += tam
            val sagda = cos(orta * PI.toFloat() / 180f) >= 0f
            val ad = dilim.category.etiket()
            val yuzde = TrFormat.percent(dilim.percent)
            // Sayı her zaman halkaya yakın tarafta: sağda önce yüzde, solda önce kategori adı.
            val metin = buildAnnotatedString {
                if (sagda) {
                    withStyle(SpanStyle(color = yuzdeRengi, fontWeight = FontWeight.Bold)) { append(yuzde) }
                    withStyle(SpanStyle(color = adRengi)) { append(" $ad") }
                } else {
                    withStyle(SpanStyle(color = adRengi)) { append("$ad ") }
                    withStyle(SpanStyle(color = yuzdeRengi, fontWeight = FontWeight.Bold)) { append(yuzde) }
                }
            }
            EtiketYerlesimi(orta, sagda, olcer.measure(metin, stil), renkler.kategoriRengi(dilim.category), 0f)
        }

        // 2) Halka, etiketlerden artan yere sığdırılır (sabit bir oran yerine gerçek metin genişlikleri).
        val dirsek = 12.dp.toPx()
        val kuyruk = 6.dp.toPx()
        val pay = dirsek + kuyruk
        val solGenislik = yerlesim.filter { !it.sagda }.maxOfOrNull { it.olcum.size.width }?.toFloat() ?: 0f
        val sagGenislik = yerlesim.filter { it.sagda }.maxOfOrNull { it.olcum.size.width }?.toFloat() ?: 0f
        val disCap = minOf(size.height, size.width - solGenislik - sagGenislik - 2f * pay).coerceAtLeast(1f)
        val disYaricap = disCap / 2f
        val kalinlik = disCap * 0.22f
        val elipsCap = disCap - kalinlik
        val merkez = Offset(solGenislik + pay + disYaricap, size.height / 2f)

        val boslukAcisi = if (dilimler.size > 1) 1.5f else 0f
        var baslangic = -90f
        dilimler.forEach { dilim ->
            val tam = dilim.percent.toFloat() / 100f * 360f * ilerleme.value
            val tarama = (tam - boslukAcisi).coerceAtLeast(if (tam > 0f) 0.5f else 0f)
            if (tarama > 0f) {
                drawArc(
                    color = renkler.kategoriRengi(dilim.category),
                    startAngle = baslangic + boslukAcisi / 2f,
                    sweepAngle = tarama,
                    useCenter = false,
                    topLeft = Offset(merkez.x - elipsCap / 2f, merkez.y - elipsCap / 2f),
                    size = Size(elipsCap, elipsCap),
                    style = Stroke(width = kalinlik, cap = StrokeCap.Butt),
                )
            }
            baslangic += dilim.percent.toFloat() / 100f * 360f
        }

        // 3) Etiketlerin doğal dikey yeri dilimin orta açısıdır; aynı taraftakiler üst üste binmesin
        // diye önce yukarıdan aşağı itilir, alta taşarsa geri yukarı çekilir.
        yerlesim.forEach { it.merkezY = merkez.y + sin(it.ortaAci * PI.toFloat() / 180f) * disYaricap }
        val aralik = 4.dp.toPx()
        listOf(true, false).forEach { taraf ->
            val sutun = yerlesim.filter { it.sagda == taraf }.sortedBy { it.merkezY }
            var ustSinir = 0f
            sutun.forEach {
                val yarim = it.olcum.size.height / 2f
                it.merkezY = max(it.merkezY, ustSinir + yarim)
                ustSinir = it.merkezY + yarim + aralik
            }
            var altSinir = size.height
            sutun.asReversed().forEach {
                val yarim = it.olcum.size.height / 2f
                it.merkezY = min(it.merkezY, altSinir - yarim)
                altSinir = it.merkezY - yarim - aralik
            }
        }

        // 4) Kılavuz çizgisi: halkanın kenarından dışa, oradan etiketin yanına.
        yerlesim.forEach { e ->
            val radyan = e.ortaAci * PI.toFloat() / 180f
            val kenar = Offset(merkez.x + cos(radyan) * disYaricap, merkez.y + sin(radyan) * disYaricap)
            val kirilma = Offset(
                merkez.x + cos(radyan) * (disYaricap + dirsek),
                merkez.y + sin(radyan) * (disYaricap + dirsek),
            )
            val sutunKenari = if (e.sagda) merkez.x + disYaricap + pay else merkez.x - disYaricap - pay
            val metinX = if (e.sagda) sutunKenari else sutunKenari - e.olcum.size.width
            val cizgiUcu = if (e.sagda) metinX - kuyruk / 2f else metinX + e.olcum.size.width + kuyruk / 2f

            drawPath(
                Path().apply {
                    moveTo(kenar.x, kenar.y)
                    lineTo(kirilma.x, kirilma.y)
                    lineTo(cizgiUcu, e.merkezY)
                },
                color = e.renk,
                alpha = ilerleme.value,
                style = Stroke(width = 2f),
            )
            drawText(
                e.olcum,
                topLeft = Offset(metinX, e.merkezY - e.olcum.size.height / 2f),
                alpha = ilerleme.value,
            )
        }
    }
}

/**
 * Çizgi grafik. Bir noktaya basılı tutulup sürüklendiğinde o noktanın etiketi (tarih ve değer) üstte gösterilir.
 * Wireframe fazında veriler örnektir.
 */
@Composable
fun CizgiGrafik(
    degerler: List<Double>,
    etiket: (Int) -> String,
    modifier: Modifier = Modifier,
    yukseklik: androidx.compose.ui.unit.Dp = 160.dp,
) {
    var secili by remember(degerler) { mutableStateOf<Int?>(null) }
    val cizgi = MaterialTheme.colorScheme.primary
    val izgara = MaterialTheme.colorScheme.outlineVariant

    Column(modifier) {
        Text(
            text = secili?.let(etiket) ?: "Değeri görmek için grafiğe basılı tut",
            style = MaterialTheme.typography.labelMedium,
            color = if (secili != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (secili != null) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(yukseklik)
                .border(1.dp, izgara)
                .pointerInput(degerler) {
                    fun indeks(x: Float): Int? {
                        if (degerler.size < 2) return null
                        return ((x / size.width) * (degerler.size - 1)).roundToInt().coerceIn(0, degerler.size - 1)
                    }
                    detectDragGesturesAfterLongPress(
                        onDragStart = { secili = indeks(it.x) },
                        onDrag = { degisim, _ -> secili = indeks(degisim.position.x) },
                        onDragEnd = { secili = null },
                        onDragCancel = { secili = null },
                    )
                },
        ) {
            if (degerler.size < 2) return@Canvas
            val en = degerler.max()
            val az = degerler.min()
            val aralik = (en - az).takeIf { it > 1e-9 } ?: 1.0
            val kenar = 10f
            val yukseklikPx = size.height - 2 * kenar

            fun nokta(i: Int) = Offset(
                x = i.toFloat() / (degerler.size - 1) * size.width,
                y = kenar + (1f - ((degerler[i] - az) / aralik).toFloat()) * yukseklikPx,
            )

            for (k in 1..3) {
                val y = size.height * k / 4f
                drawLine(izgara, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            val yol = Path().apply {
                moveTo(nokta(0).x, nokta(0).y)
                for (i in 1 until degerler.size) lineTo(nokta(i).x, nokta(i).y)
            }
            drawPath(yol, cizgi, style = Stroke(width = 3f))
            secili?.let { i ->
                val p = nokta(i)
                drawLine(cizgi, Offset(p.x, 0f), Offset(p.x, size.height), strokeWidth = 1.5f)
                drawCircle(cizgi, radius = 7f, center = p)
                drawCircle(Color.White, radius = 3.5f, center = p)
            }
        }
    }
}

/** Açıklama satırı için yatay yerleşim yardımcısı. */
@Composable
fun SatirDegerli(
    sol: @Composable () -> Unit,
    sag: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        sol()
        sag()
    }
}

package com.portfoy.ui.bilesenler

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfoy.calc.AllocationSlice
import com.portfoy.calc.format.TrFormat
import com.portfoy.ui.tema.Gri
import kotlin.math.roundToInt

/**
 * Kategori dağılımı halka grafiği. Wireframe'de dilimler gri tonu ve desenle temsil edilir:
 * tek sıradaki dilimler düz, çift sıradakiler ek olarak kesik çizgilidir. Her dilimin etiketi ayrı listededir.
 */
@Composable
fun DonutGrafik(dilimler: List<AllocationSlice>, modifier: Modifier = Modifier) {
    val aciklama = dilimler.joinToString { "${it.category.etiket()} ${TrFormat.percent(it.percent)}" }
    Canvas(
        modifier
            .aspectRatio(1f)
            .semantics { contentDescription = "Portföy dağılımı: $aciklama" },
    ) {
        val kalinlik = size.minDimension * 0.22f
        val cap = size.minDimension - kalinlik
        val sol = (size.width - cap) / 2f
        val ust = (size.height - cap) / 2f
        val boslukAcisi = if (dilimler.size > 1) 1.5f else 0f
        var baslangic = -90f
        dilimler.forEachIndexed { i, dilim ->
            val tam = dilim.percent.toFloat() / 100f * 360f
            val tarama = (tam - boslukAcisi).coerceAtLeast(0.5f)
            drawArc(
                color = Gri.Dilimler[i % Gri.Dilimler.size],
                startAngle = baslangic + boslukAcisi / 2f,
                sweepAngle = tarama,
                useCenter = false,
                topLeft = Offset(sol, ust),
                size = Size(cap, cap),
                style = Stroke(width = kalinlik, cap = StrokeCap.Butt),
            )
            if (i % 2 == 1) {
                drawArc(
                    color = Color.White,
                    startAngle = baslangic + boslukAcisi / 2f,
                    sweepAngle = tarama,
                    useCenter = false,
                    topLeft = Offset(sol, ust),
                    size = Size(cap, cap),
                    style = Stroke(width = kalinlik * 0.28f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))),
                )
            }
            baslangic += tam
        }
    }
}

/** Açıklama listesindeki dilim işareti: grafikteki dilimle aynı gri ton. */
@Composable
fun DilimIsareti(sira: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(14.dp)
            .border(1.dp, Gri.Koyu)
            .background(Gri.Dilimler[sira % Gri.Dilimler.size]),
    )
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

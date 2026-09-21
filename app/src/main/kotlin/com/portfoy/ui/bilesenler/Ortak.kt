package com.portfoy.ui.bilesenler

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.portfoy.calc.Donem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.sin
import kotlin.random.Random

private val TarihBicimi: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun LocalDate.tr(): String = format(TarihBicimi)

/** Çerçeveli, düz yüzeyli kutu: wireframe'de ayrım çerçeve kalınlığıyla yapılır. */
@Composable
fun Kutu(
    modifier: Modifier = Modifier,
    kalinCerceve: Boolean = false,
    icerik: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (kalinCerceve) 2.dp else 1.dp,
            if (kalinCerceve) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(Modifier.padding(12.dp), content = icerik)
    }
}

/** Yatay kaydırılabilir dönem seçici. Her iki grafikte de aynı seçenekler vardır. */
@Composable
fun DonemSecici(secili: Donem, onSec: (Donem) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        items(Donem.entries) { donem ->
            FilterChip(
                selected = donem == secili,
                onClick = { onSec(donem) },
                label = { Text(donem.etiket) },
            )
        }
    }
}

/**
 * Wireframe için örnek seri üreticisi. Aynı anahtar her zaman aynı seriyi verir; gerçek geçmiş
 * seriler M6'da bağlanana kadar grafikler bununla çizilir.
 */
object OrnekSeri {
    private const val NOKTA = 40

    /** [sonDeger]'de biten, geriye doğru rastgele yürüyen değer serisi. */
    fun deger(anahtar: String, sonDeger: Double, oynaklik: Double = 0.025): List<Double> {
        val r = Random(anahtar.hashCode())
        var d = sonDeger
        val geri = mutableListOf(d)
        repeat(NOKTA - 1) {
            d /= 1.0 + (r.nextDouble() - 0.46) * oynaklik
            geri += d
        }
        return geri.reversed()
    }

    /** 0'dan başlayıp [sonYuzde]'de biten getiri (yüzde) serisi. */
    fun yuzde(anahtar: String, sonYuzde: Double): List<Double> {
        val r = Random(anahtar.hashCode())
        val gurultu = (0 until NOKTA).map { sin(it / 4.0) * 2.0 + (r.nextDouble() - 0.5) * 3.0 }
        return (0 until NOKTA).map { i ->
            val t = i.toDouble() / (NOKTA - 1)
            val kopru = gurultu[i] - t * gurultu.last() - (1 - t) * gurultu.first()
            sonYuzde * t + kopru * 0.6
        }
    }

    /** Serinin [i]. noktasına karşılık gelen tarih. */
    fun tarih(baslangic: LocalDate, bitis: LocalDate, i: Int): LocalDate {
        val gun = java.time.temporal.ChronoUnit.DAYS.between(baslangic, bitis)
        return baslangic.plusDays(gun * i / (NOKTA - 1))
    }
}

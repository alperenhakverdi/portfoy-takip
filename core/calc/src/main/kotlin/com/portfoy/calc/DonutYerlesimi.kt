package com.portfoy.calc

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * M29 — Dağılım grafiğinin (donut) kenar etiketlerinin yerleşim matematiği. Çizimden (Compose
 * `Canvas`, `TextMeasurer`) bilinçli olarak ayrıştırıldı ki yüzdeler değiştikçe (yeni kategori,
 * aşırı eşitsiz dağılım, çok sayıda küçük dilim) doğru kaldığı sade birim testlerle doğrulanabilsin —
 * ekranda "şu anki veriyle iyi görünüyor" demekle yetinmek yerine.
 *
 * Üç adım, M24/M28'de bulunan hatalara karşılık gelir:
 * 1. [donutOrtaAcilari] — her dilimin orta açısı (derece, -90 = tepe, saat yönü).
 * 2. [donutSutunDengele] — hangi etiket sağda/solda duracak; ham açıya göre 5-1 gibi aşırı eşitsiz
 *    bir dağılım çıkarsa (küçük dilimler hep yan yana geldiği için) en yakın tarafa taşınır.
 * 3. [donutCakismaGider] — aynı taraftaki etiketlerin dikey merkezleri üst üste binmeyecek şekilde
 *    ayrıştırılır; kılavuz çizgisinin yüksekliği buradan okunur (dilimin kendi açısından değil) ki
 *    birbirine çok yakın iki dilim (M28'deki Fon/Döviz) aynı satıra düşüp çizgileri üst üste binmesin.
 */

/** Yüzdelerden her dilimin orta açısını hesaplar (derece, -90 = tepe, saat yönünde). */
fun donutOrtaAcilari(yuzdeler: List<Float>): List<Float> {
    var aci = -90f
    return yuzdeler.map { yuzde ->
        val tam = yuzde / 100f * 360f
        val orta = aci + tam / 2f
        aci += tam
        orta
    }
}

/**
 * Hangi etiketlerin sağda/solda duracağı. Ham açıya göre (`cos(açı) >= 0` sağda) başlar; sütunlar
 * ikiden fazla fark atarsa, dikey eksene en yakın (yatayda en az yer kaplayan, yani en "taşınabilir")
 * etiket karşı sütuna geçer — ta ki fark en fazla 1 kalana kadar. Dönüş her zaman sonludur: her taşıma
 * farkı tam 2 azaltır, uygun aday kalmayınca döngü güvenle durur (bozuk/eşit açı girdisinde bile).
 */
fun donutSutunDengele(acilar: List<Float>): List<Boolean> {
    val sagda = BooleanArray(acilar.size) { cos(acilar[it] * Math.PI.toFloat() / 180f) >= 0f }
    while (true) {
        val sag = sagda.count { it }
        val sol = sagda.size - sag
        if (abs(sag - sol) <= 1) break
        val solKalabalik = sol > sag
        val aday = sagda.indices
            .filter { sagda[it] != solKalabalik }
            .minByOrNull { abs(cos(acilar[it] * Math.PI.toFloat() / 180f)) } ?: break
        sagda[aday] = solKalabalik
    }
    return sagda.toList()
}

/**
 * Aynı taraftaki etiketlerin doğal dikey merkezlerini ([dogalY]), üst üste binmeyecek şekilde
 * ayrıştırır. Önce yukarıdan aşağı itilir (üstten taşmasın), sonra aşağıdan yukarı çekilir (alttan
 * taşmasın) — iki geçiş birlikte, sığdığı sürece komşu etiketler arasında en az [aralik] kadar boşluk
 * garanti eder ve göreli sırayı hep korur. Sığmadığı aşırı durumlarda (çok sayıda etiket, dar alan)
 * aralık yine de korunur; etiketler [ustSinir]/[altSinir] dışına taşabilir (çağıran kırpabilir) ama
 * hiçbir zaman birbirine çok yaklaşıp okunaksızlaşmaz.
 */
fun donutCakismaGider(
    dogalY: List<Float>,
    yukseklikler: List<Float>,
    sagda: List<Boolean>,
    aralik: Float,
    ustSinir: Float,
    altSinir: Float,
): List<Float> {
    val sonuc = dogalY.toFloatArray()
    listOf(true, false).forEach { taraf ->
        val sutun = sonuc.indices.filter { sagda[it] == taraf }.sortedBy { sonuc[it] }
        var ust = ustSinir
        sutun.forEach { i ->
            val yarim = yukseklikler[i] / 2f
            sonuc[i] = max(sonuc[i], ust + yarim)
            ust = sonuc[i] + yarim + aralik
        }
        var alt = altSinir
        sutun.asReversed().forEach { i ->
            val yarim = yukseklikler[i] / 2f
            sonuc[i] = min(sonuc[i], alt - yarim)
            alt = sonuc[i] - yarim - aralik
        }
    }
    return sonuc.toList()
}

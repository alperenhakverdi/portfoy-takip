package com.portfoy.calc

import kotlin.math.abs
import kotlin.math.cos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DonutYerlesimiTest {

    // --- donutOrtaAcilari ---

    @Test
    fun `iki esit dilim tam karsi uclarda`() {
        val acilar = donutOrtaAcilari(listOf(50f, 50f))
        assertEquals(0f, acilar[0], 0.01f) // sağ (3 yönü)
        assertEquals(180f, acilar[1], 0.01f) // sol (9 yönü)
    }

    @Test
    fun `dort esit dilim doksaner derece ayrik`() {
        val acilar = donutOrtaAcilari(listOf(25f, 25f, 25f, 25f))
        assertEquals(4, acilar.size)
        for (i in 0 until 3) assertEquals(90f, acilar[i + 1] - acilar[i], 0.01f)
    }

    @Test
    fun `tek dilim yuz yuzde`() {
        assertEquals(1, donutOrtaAcilari(listOf(100f)).size)
    }

    // --- donutSutunDengele: temel özellik, her dağılımda korunmalı ---

    private fun sagSolFarki(acilar: List<Float>): Int {
        val sagda = donutSutunDengele(acilar)
        val sag = sagda.count { it }
        return abs(sag - (sagda.size - sag))
    }

    @Test
    fun `zaten dengeliyse dokunulmaz`() {
        // 4 esit dilim: dogal olarak 2 sag 2 sol, degismemeli.
        val acilar = donutOrtaAcilari(listOf(25f, 25f, 25f, 25f))
        assertEquals(listOf(true, true, false, false), donutSutunDengele(acilar))
    }

    @Test
    fun `gercek portfoy dagilimi uc uce dengelenir`() {
        // M28'de hataya yol acan tam senaryo: ABD buyuk, Fon ve Doviz kucuk ve bitisik, tepeye yakin.
        val acilar = donutOrtaAcilari(listOf(51.12f, 26.18f, 9.95f, 8.48f, 2.95f, 1.32f))
        val sagda = donutSutunDengele(acilar)
        // ABD(0) sag, Emtia(1)/Nakit(2)/BIST(3) sol, Doviz(4)/Fon(5) sag: 3-3.
        assertEquals(listOf(true, false, false, false, true, true), sagda)
    }

    @Test
    fun `bes bir gibi asiri esitsiz dagilim en fazla bir fark kalacak sekilde dengelenir`() {
        // Kucuk dilimler hep yan yana oldugunda (buyukten kucuge siralama) dogal olarak 5-1 cikar.
        val acilar = donutOrtaAcilari(listOf(70f, 10f, 8f, 6f, 4f, 2f))
        assertTrue(sagSolFarki(acilar) <= 1)
    }

    @Test
    fun `yedi kategori (kripto dahil) de dengelenir`() {
        val acilar = donutOrtaAcilari(listOf(40f, 20f, 15f, 10f, 8f, 5f, 2f))
        assertTrue(sagSolFarki(acilar) <= 1)
    }

    @Test
    fun `tek kategoride cokme olmaz`() {
        val acilar = donutOrtaAcilari(listOf(100f))
        assertTrue(sagSolFarki(acilar) <= 1)
    }

    @Test
    fun `iki kategoride dogal simetri korunur`() {
        val acilar = donutOrtaAcilari(listOf(60f, 40f))
        assertEquals(listOf(true, false), donutSutunDengele(acilar))
    }

    @Test
    fun `cok sayida kucuk ve bitisik dilimde donguye girmeden sonlanir`() {
        // Bir buyuk dilim + dokuz neredeyse ozdes acili kucuk dilim: aday tukenmeden once dengelenmeli.
        val acilar = donutOrtaAcilari(listOf(10f) + List(9) { 10f })
        assertTrue(sagSolFarki(acilar) <= 1)
    }

    @Test
    fun `rastgele agirlikli pek cok dagilimda fark hep en fazla bir kalir`() {
        // "Buyuklukler degisince de dogru mu" sorusunun dogrudan cevabi: bir deger degil, bir aile.
        val senaryolar = listOf(
            listOf(100f),
            listOf(50f, 50f),
            listOf(90f, 10f),
            listOf(33.34f, 33.33f, 33.33f),
            listOf(97f, 1f, 1f, 1f),
            listOf(51.12f, 26.18f, 9.95f, 8.48f, 2.95f, 1.32f),
            listOf(40f, 20f, 15f, 10f, 8f, 5f, 2f),
            listOf(14.3f, 14.3f, 14.3f, 14.3f, 14.3f, 14.3f, 14.2f),
            listOf(60f, 30f, 5f, 2f, 1.5f, 1.5f),
        )
        senaryolar.forEach { yuzdeler ->
            val acilar = donutOrtaAcilari(yuzdeler)
            assertTrue("$yuzdeler için fark 1'den büyük", sagSolFarki(acilar) <= 1)
        }
    }

    // --- donutCakismaGider ---

    @Test
    fun `yeterince ayrikken dokunulmaz`() {
        val dogalY = listOf(20f, 100f, 180f)
        val sonuc = donutCakismaGider(dogalY, listOf(16f, 16f, 16f), List(3) { true }, 4f, 0f, 300f)
        sonuc.forEachIndexed { i, y -> assertEquals(dogalY[i], y, 0.01f) }
    }

    @Test
    fun `bitisik iki etiket en az aralik kadar ayrilir (M28 regresyonu)`() {
        // Fon ve Doviz gibi: neredeyse ayni dogal yukseklikte, ikisi de sagda.
        val sonuc = donutCakismaGider(listOf(50f, 51f), listOf(20f, 20f), listOf(true, true), 4f, 0f, 300f)
        assertTrue(abs(sonuc[1] - sonuc[0]) >= 20f + 4f - 0.01f)
    }

    @Test
    fun `sira hep korunur`() {
        val dogalY = listOf(50f, 52f, 54f, 56f)
        val sonuc = donutCakismaGider(dogalY, List(4) { 18f }, List(4) { true }, 4f, 0f, 300f)
        for (i in 0 until 3) assertTrue(sonuc[i] < sonuc[i + 1])
    }

    @Test
    fun `tasan durumda bile aralik hep korunur`() {
        // 6 etiket, her biri 40 yuksekliginde, yalnizca 100 birimlik dar bir alanda: sigmaz ama
        // komsular yine de birbirine yapismaz.
        val dogalY = List(6) { 50f }
        val yukseklikler = List(6) { 40f }
        val sonuc = donutCakismaGider(dogalY, yukseklikler, List(6) { true }, 4f, 0f, 100f)
        val sirali = sonuc.sorted()
        for (i in 0 until 5) assertTrue(sirali[i + 1] - sirali[i] >= 40f - 0.01f)
    }

    @Test
    fun `sag ve sol birbirinden bagimsizdir`() {
        val dogalY = listOf(50f, 50f, 999f)
        val sagda = listOf(true, true, false)
        val sonuc = donutCakismaGider(dogalY, listOf(20f, 20f, 20f), sagda, 4f, 0f, 2000f)
        // Soldaki tek öğe (index 2), sağdaki çakışan çiftten etkilenmemeli.
        assertEquals(999f, sonuc[2], 0.01f)
    }

    @Test
    fun `cos isaretiyle tutarlidir`() {
        // donutSutunDengele'nin varsaydigi "sagda = cos >= 0" kuralinin kendisini de dogrular.
        // -90 ve 270 matematiksel olarak ayni nokta (tepe) ama float temsili tam sifir vermiyor;
        // o yuzden sinirdaki -90/270 bu testte yer almiyor, yalnizca acik sag/sol acilar kontrol edilir.
        val acilar = listOf(-45f, 0f, 45f, 90f, 135f, 180f, 225f)
        val sagda = BooleanArray(acilar.size) { cos(acilar[it] * Math.PI.toFloat() / 180f) >= 0f }
        assertEquals(listOf(true, true, true, false, false, false, false), sagda.toList())
    }
}

package com.portfoy.network.sources

import com.portfoy.model.AssetRef
import com.portfoy.model.Category
import com.portfoy.network.Http
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Properties
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Gerçek servislere karşı çalışan canlı denemeler. Ağa çıktığı, anahtar kullandığı ve oran sınırlarına takıldığı için
 * varsayılan olarak **atlanır**. Çalıştırmak için: `LIVE_TESTS=1 ./gradlew :core:network:test --tests "*CanliKaynakTest"`.
 *
 * Anahtarlar proje kökündeki `local.properties` dosyasından okunur (git'e girmez).
 * Bir kaynak değiştiğinde ya da bozulduğunda bu test, fixture'lar eskimiş mi diye ilk bakılacak yerdir.
 */
class CanliKaynakTest {

    private val http = Http(
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(240, TimeUnit.SECONDS) // Finnhub sembol listesi büyük dosya
            .callTimeout(300, TimeUnit.SECONDS)
            .build(),
    )

    private fun canliMi() = assumeTrue("LIVE_TESTS=1 değil, atlandı", System.getenv("LIVE_TESTS") == "1")

    private fun anahtar(ad: String): String {
        val dosya = generateSequence(File("").absoluteFile) { it.parentFile }.map { File(it, "local.properties") }.firstOrNull { it.exists() }
        assumeTrue("local.properties bulunamadı", dosya != null)
        val deger = Properties().apply { dosya!!.inputStream().use { load(it) } }.getProperty(ad, "")
        assumeTrue("$ad tanımlı değil", deger.isNotBlank())
        return deger
    }

    private fun araliktaMi(deger: BigDecimal, alt: Double, ust: Double) = deger.toDouble() in alt..ust

    private val bugun = LocalDate.now()

    @Test
    fun `canli yahoo BIST ABD kur ve ons`() = runBlocking {
        canliMi()
        val yahoo = YahooSource(http)
        val thyao = yahoo.getQuotes(listOf(AssetRef("THYAO", Category.BIST))).getOrThrow().single()
        val kur = yahoo.getQuotes(listOf(AssetRef.USDTRY)).getOrThrow().single()
        val ons = yahoo.getQuotes(listOf(AssetRef("GC=F", null))).getOrThrow().single()
        val gecmis = yahoo.getHistory(AssetRef("AAPL", Category.ABD), bugun.minusDays(30), bugun).getOrThrow()

        println("THYAO ${thyao.price} ${thyao.currency} | USD/TRY ${kur.price} | ons altın ${ons.price} | AAPL ${gecmis.size} gün, son ${gecmis.last()}")
        assertTrue(araliktaMi(thyao.price, 1.0, 5000.0))
        assertTrue(araliktaMi(kur.price, 10.0, 200.0))
        assertTrue(araliktaMi(ons.price, 500.0, 20000.0))
        assertTrue(gecmis.size >= 10)
    }

    @Test
    fun `canli yahoo gram altin ons ve kurdan turetilir`() = runBlocking {
        canliMi()
        val altin = OnsEmtiaSource(YahooSource(http)).getQuotes(listOf(AssetRef("XAUGR", Category.EMTIA))).getOrThrow().single()
        println("Gram altın (ons×kur÷31,1035): ${altin.price} TRY")
        assertTrue(araliktaMi(altin.price, 1000.0, 50000.0))
    }

    @Test
    fun `canli finnhub anlik fiyat`() = runBlocking {
        canliMi()
        val sonuc = FinnhubSource(http, anahtar("FINNHUB_API_KEY")).getQuotes(listOf(AssetRef("AAPL", Category.ABD))).getOrThrow().single()
        println("Finnhub AAPL ${sonuc.price} USD (${sonuc.timestamp})")
        assertTrue(araliktaMi(sonuc.price, 10.0, 2000.0))
    }

    @Test
    fun `canli finnhub ABD sembol listesi`() = runBlocking {
        canliMi()
        val liste = FinnhubSource(http, anahtar("FINNHUB_API_KEY")).listSymbols().getOrThrow()
        println("Finnhub ABD katalog: ${liste.size} kayıt, örnek ${liste.take(3).map { it.code }}")
        assertTrue(liste.size > 5000)
        assertTrue(liste.any { it.code == "AAPL" })
    }

    @Test
    fun `canli twelve data ABD gecmis ve spot altin`() = runBlocking {
        canliMi()
        val td = TwelveDataSource(http, anahtar("TWELVEDATA_API_KEY"))
        val aapl = td.getHistory(AssetRef("AAPL", Category.ABD), bugun.minusDays(14), bugun).getOrThrow()
        val xau = td.getHistory(AssetRef("XAU/USD", null), bugun.minusDays(14), bugun).getOrThrow()
        println("Twelve Data AAPL ${aapl.size} gün (son ${aapl.last()}) | XAU/USD ${xau.size} gün (son ${xau.last()})")
        assertTrue(aapl.size >= 5)
        assertTrue(xau.size >= 5)
    }

    @Test
    fun `canli tefas guncel fiyat gecmis ve liste`() = runBlocking {
        canliMi()
        val tefas = TefasSource(http)
        val fiyat = tefas.getQuotes(listOf(AssetRef("AAL", Category.FON, "YAT"))).getOrThrow().single()
        val gecmis = tefas.getHistory(AssetRef("AAL", Category.FON, "YAT"), bugun.minusDays(20), bugun).getOrThrow()
        val liste = tefas.listSymbols().getOrThrow()
        println("TEFAS AAL ${fiyat.price} (${fiyat.timestamp}) | geçmiş ${gecmis.size} gün | katalog ${liste.size} fon")
        assertTrue(fiyat.price.signum() > 0)
        assertTrue(gecmis.size >= 5)
        assertTrue(liste.size > 500)
    }

    @Test
    fun `canli truncgil ve tcmb`() = runBlocking {
        canliMi()
        val truncgil = TruncgilSource(http).getQuotes(
            listOf(AssetRef("XAUGR", Category.EMTIA), AssetRef("XAGGR", Category.EMTIA), AssetRef.USDTRY),
        ).getOrThrow()
        val tcmb = TcmbGunlukSource(http).getQuotes(listOf(AssetRef.USDTRY)).getOrThrow().single()
        println("Truncgil gram altın ${truncgil[0].price} | gümüş ${truncgil[1].price} | USD ${truncgil[2].price} | TCMB günlük USD ${tcmb.price}")
        assertTrue(araliktaMi(truncgil[0].price, 1000.0, 50000.0))
        assertTrue(araliktaMi(truncgil[2].price, 10.0, 200.0))
        assertTrue(araliktaMi(tcmb.price, 10.0, 200.0))
    }

    @Test
    fun `canli evds kur gecmisi`() = runBlocking {
        canliMi()
        val gecmis = EvdsSource(http, anahtar("EVDS_API_KEY")).getHistory(AssetRef.USDTRY, bugun.minusDays(14), bugun).getOrThrow()
        println("EVDS USD/TRY ${gecmis.size} gün, son ${gecmis.last()}")
        assertTrue(gecmis.size >= 5)
        assertTrue(araliktaMi(gecmis.last().close, 10.0, 200.0))
    }
}

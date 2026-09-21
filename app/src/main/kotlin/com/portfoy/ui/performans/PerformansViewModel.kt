package com.portfoy.ui.performans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfoy.calc.Donem
import com.portfoy.calc.PortfolioSummary
import com.portfoy.calc.summarize
import com.portfoy.data.repository.PortfolioRepository
import com.portfoy.model.Asset
import com.portfoy.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Sıralama ölçütü: kullanıcı yüzde ile TL arasında değiştirebilir. */
enum class Siralama { YUZDE, TL }

data class PerformansSecimi(val donem: Donem = Donem.VARSAYILAN, val siralama: Siralama = Siralama.YUZDE)

/** Bir varlığın seçilen dönemdeki getirisi. */
data class VarlikGetirisi(
    val varlik: Asset,
    val yuzde: BigDecimal,
    val tl: BigDecimal,
    /** Varlık dönemin başında portföyde değildiyse giriş tarihi; listede yanında yazılır. */
    val girisTarihi: LocalDate?,
)

data class PerformansEkranVerisi(
    val ozet: PortfolioSummary? = null,
    val secim: PerformansSecimi = PerformansSecimi(),
    val toplamYuzde: BigDecimal = BigDecimal.ZERO,
    val toplamTl: BigDecimal = BigDecimal.ZERO,
    val satirlar: List<VarlikGetirisi> = emptyList(),
    val enEskiIslem: LocalDate? = null,
)

/**
 * Wireframe fazında dönemsel getiriler örnektir: aynı varlık ve dönem her zaman aynı örnek değeri verir.
 * Gerçek dönemsel getiri (Dietz, geçmiş fiyat serileri) M6'da bağlanır; ekran bu sınıfın çıktısına
 * bağlı olduğu için ekranda değişiklik gerekmez.
 */
@HiltViewModel
class PerformansViewModel @Inject constructor(
    depo: PortfolioRepository,
) : ViewModel() {

    private val secim = MutableStateFlow(PerformansSecimi())

    val ekran: StateFlow<PerformansEkranVerisi> = combine(depo.observePortfolio(), secim) { veri, secim ->
        val ozet = summarize(veri.holdings)
        val bugun = LocalDate.now(com.portfoy.di.UygulamaZamanDilimi)
        val enEski = veri.holdings.flatMap { it.transactions }.minOfOrNull { it.tradeDate }
        val baslangic = secim.donem.baslangic(bugun, enEski)

        val satirlar = ozet.categories.flatMap { it.assets }.map { sonuc ->
            val yuzde = if (sonuc.asset.category == Category.NAKIT) BigDecimal.ZERO else ornekYuzde(sonuc.asset.code, secim.donem)
            val tl = sonuc.currentValue.multiply(yuzde).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            val ilk = veri.holdings.first { it.asset.id == sonuc.asset.id }.transactions.minOf { it.tradeDate }
            VarlikGetirisi(sonuc.asset, yuzde, tl, ilk.takeIf { it.isAfter(baslangic) })
        }

        val toplamTl = satirlar.fold(BigDecimal.ZERO) { acc, s -> acc + s.tl }
        val taban = ozet.totalValue - toplamTl
        val toplamYuzde = if (taban.signum() > 0) toplamTl.multiply(BigDecimal(100)).divide(taban, 2, RoundingMode.HALF_UP) else BigDecimal.ZERO

        val sirali = when (secim.siralama) {
            Siralama.YUZDE -> satirlar.sortedByDescending { it.yuzde }
            Siralama.TL -> satirlar.sortedByDescending { it.tl }
        }
        PerformansEkranVerisi(ozet, secim, toplamYuzde, toplamTl, sirali, enEski)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerformansEkranVerisi())

    fun donemSec(donem: Donem) = secim.update { it.copy(donem = donem) }

    fun siralamaSec(siralama: Siralama) = secim.update { it.copy(siralama = siralama) }

    private fun ornekYuzde(kod: String, donem: Donem): BigDecimal {
        val tohum = abs((kod + donem.name).hashCode()) % 4200
        return BigDecimal(tohum).divide(BigDecimal(100), 2, RoundingMode.HALF_UP) - BigDecimal(12)
    }
}

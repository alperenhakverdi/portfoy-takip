package com.portfoy.ui.performans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfoy.GecmisYoneticisi
import com.portfoy.calc.ChartWindow
import com.portfoy.calc.Donem
import com.portfoy.calc.PortfolioSummary
import com.portfoy.calc.summarize
import com.portfoy.data.repository.GrafikDeposu
import com.portfoy.data.repository.PortfolioData
import com.portfoy.data.repository.PortfolioRepository
import com.portfoy.data.repository.VarlikDonemGetirisi
import com.portfoy.di.UygulamaZamanDilimi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Sıralama ölçütü: kullanıcı yüzde ile TL arasında değiştirebilir. */
enum class Siralama { YUZDE, TL }

data class PerformansSecimi(val donem: Donem = Donem.VARSAYILAN, val siralama: Siralama = Siralama.YUZDE)

data class PerformansEkranVerisi(
    val yuklendi: Boolean = false,
    val ozet: PortfolioSummary? = null,
    val secim: PerformansSecimi = PerformansSecimi(),
    /** Seçilen dönemdeki getiri; hesaplanamıyorsa `null` (ekranda "—"). */
    val toplamYuzde: BigDecimal? = null,
    val toplamTl: BigDecimal = BigDecimal.ZERO,
    /** Dönem başından her güne birikimli getiri yüzdesi (grafiğin y ekseni). */
    val getiri: List<Pair<LocalDate, BigDecimal>> = emptyList(),
    val pencere: ChartWindow? = null,
    val tahmini: Boolean = false,
    val gecmisYukleniyor: Boolean = false,
    val satirlar: List<VarlikDonemGetirisi> = emptyList(),
)

/**
 * Performans sekmesinin verisi: portföy ve varlık bazında dönemsel getiri, saklanan geçmiş fiyat serilerinden geriye dönük
 * hesaplanır. Geçmiş seriler arka planda tamamlandıkça ekran kendiliğinden yenilenir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PerformansViewModel @Inject constructor(
    depo: PortfolioRepository,
    private val grafik: GrafikDeposu,
    gecmis: GecmisYoneticisi,
    private val saat: Clock,
) : ViewModel() {

    private val secim = MutableStateFlow(PerformansSecimi())

    private data class Girdi(val secim: PerformansSecimi, val veri: PortfolioData, val gecmisYukleniyor: Boolean)

    val ekran: StateFlow<PerformansEkranVerisi> = combine(
        secim,
        depo.observePortfolio(),
        gecmis.guncellendi.onStart { emit(Unit) },
        gecmis.yukleniyor,
    ) { s, veri, _, yukleniyor -> Girdi(s, veri, yukleniyor) }
        .mapLatest { g ->
            val ozet = summarize(g.veri.holdings)
            if (ozet.isEmpty) return@mapLatest PerformansEkranVerisi(yuklendi = true, ozet = ozet, secim = g.secim)

            val bugun = saat.instant().atZone(UygulamaZamanDilimi).toLocalDate()
            val toplam = grafik.hesapla(g.secim.donem, bugun)
            val satirlar = grafik.varlikGetirileri(g.secim.donem, bugun)
            val sirali = when (g.secim.siralama) {
                Siralama.YUZDE -> satirlar.sortedWith(compareByDescending<VarlikDonemGetirisi> { it.yuzde != null }.thenByDescending { it.yuzde })
                Siralama.TL -> satirlar.sortedByDescending { it.tl }
            }
            PerformansEkranVerisi(
                yuklendi = true,
                ozet = ozet,
                secim = g.secim,
                toplamYuzde = toplam?.toplamYuzde,
                toplamTl = toplam?.toplamTl ?: BigDecimal.ZERO,
                getiri = toplam?.getiri.orEmpty(),
                pencere = toplam?.pencere,
                tahmini = toplam?.tahmini == true,
                gecmisYukleniyor = g.gecmisYukleniyor,
                satirlar = sirali,
            )
        }
        .flowOn(Dispatchers.Default) // getiri hesabı ana iş parçacığında yapılmaz
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerformansEkranVerisi())

    fun donemSec(donem: Donem) = secim.update { it.copy(donem = donem) }

    fun siralamaSec(siralama: Siralama) = secim.update { it.copy(siralama = siralama) }
}

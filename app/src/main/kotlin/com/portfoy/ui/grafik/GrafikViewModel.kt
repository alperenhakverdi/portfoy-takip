package com.portfoy.ui.grafik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfoy.GecmisYoneticisi
import com.portfoy.calc.ChartWindow
import com.portfoy.calc.Donem
import com.portfoy.calc.PortfolioSummary
import com.portfoy.calc.summarize
import com.portfoy.data.repository.GrafikDeposu
import com.portfoy.data.repository.KategoriDonemGetirisi
import com.portfoy.data.repository.PortfolioData
import com.portfoy.data.repository.PortfolioRepository
import com.portfoy.data.repository.VarlikDonemGetirisi
import com.portfoy.di.UygulamaZamanDilimi
import com.portfoy.model.Category
import com.portfoy.ui.para.ParaBirimiTercihi
import com.portfoy.ui.para.ParaBirimiTercihiDeposu
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

data class GrafikSecimi(
    val donem: Donem = Donem.VARSAYILAN,
    val siralama: Siralama = Siralama.YUZDE,
    /** Açılmış (içindeki varlıklar görünen) kategoriler. */
    val acik: Set<Category> = emptySet(),
)

data class GrafikEkranVerisi(
    val yuklendi: Boolean = false,
    val ozet: PortfolioSummary? = null,
    val secim: GrafikSecimi = GrafikSecimi(),
    /** Seçilen dönemdeki getiri; hesaplanamıyorsa `null` (ekranda "—"). */
    val toplamYuzde: BigDecimal? = null,
    val toplamTl: BigDecimal = BigDecimal.ZERO,
    /** Dönem başından her güne birikimli getiri yüzdesi (grafiğin y ekseni). */
    val getiri: List<Pair<LocalDate, BigDecimal>> = emptyList(),
    val pencere: ChartWindow? = null,
    val tahmini: Boolean = false,
    val gecmisYukleniyor: Boolean = false,
    /** Kategori kırılımı; her kategorinin altında varlıkları. */
    val kategoriler: List<KategoriDonemGetirisi> = emptyList(),
    /** M23 — TL↔USD görüntüleme çevrimi için son bilinen kur. */
    val usdTryRate: BigDecimal? = null,
)

/**
 * M21 — eskiden Performans sekmesiydi; artık Portföy ekranındaki grafik ikonundan açılan ayrı bir
 * ekran. Portföy, kategori ve varlık bazında dönemsel getiri, saklanan geçmiş fiyat serilerinden
 * geriye dönük hesaplanır. Geçmiş seriler arka planda tamamlandıkça ekran kendiliğinden yenilenir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GrafikViewModel @Inject constructor(
    depo: PortfolioRepository,
    private val grafik: GrafikDeposu,
    private val paraBirimiDeposu: ParaBirimiTercihiDeposu,
    gecmis: GecmisYoneticisi,
    private val saat: Clock,
) : ViewModel() {

    private val secim = MutableStateFlow(GrafikSecimi())

    /** M23 — Portföy ekranıyla aynı, kalıcı tercih (tek bir SharedPreferences kaynağı paylaşılır). */
    val paraBirimi: StateFlow<ParaBirimiTercihi> = paraBirimiDeposu.tercih

    fun paraBirimiDegistir() = paraBirimiDeposu.degistir()

    private data class Girdi(val secim: GrafikSecimi, val veri: PortfolioData, val gecmisYukleniyor: Boolean)

    val ekran: StateFlow<GrafikEkranVerisi> = combine(
        secim,
        depo.observePortfolio(),
        gecmis.guncellendi.onStart { emit(Unit) },
        gecmis.yukleniyor,
    ) { s, veri, _, yukleniyor -> Girdi(s, veri, yukleniyor) }
        .mapLatest { g ->
            val ozet = summarize(g.veri.holdings)
            if (ozet.isEmpty) return@mapLatest GrafikEkranVerisi(yuklendi = true, ozet = ozet, secim = g.secim)

            val bugun = saat.instant().atZone(UygulamaZamanDilimi).toLocalDate()
            val toplam = grafik.hesapla(g.secim.donem, bugun)
            val kategoriler = grafik.kategoriGetirileri(g.secim.donem, bugun)
                .map { it.copy(varliklar = sirala(it.varliklar, g.secim.siralama)) }
                .sortedWith(
                    when (g.secim.siralama) {
                        Siralama.YUZDE -> compareByDescending<KategoriDonemGetirisi> { it.yuzde != null }.thenByDescending { it.yuzde }
                        Siralama.TL -> compareByDescending { it.tl }
                    },
                )
            GrafikEkranVerisi(
                yuklendi = true,
                ozet = ozet,
                secim = g.secim,
                toplamYuzde = toplam?.toplamYuzde,
                toplamTl = toplam?.toplamTl ?: BigDecimal.ZERO,
                getiri = toplam?.getiri.orEmpty(),
                pencere = toplam?.pencere,
                tahmini = toplam?.tahmini == true,
                gecmisYukleniyor = g.gecmisYukleniyor,
                kategoriler = kategoriler,
                usdTryRate = g.veri.usdTryRate,
            )
        }
        .flowOn(Dispatchers.Default) // getiri hesabı ana iş parçacığında yapılmaz
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrafikEkranVerisi())

    private fun sirala(satirlar: List<VarlikDonemGetirisi>, siralama: Siralama): List<VarlikDonemGetirisi> =
        when (siralama) {
            Siralama.YUZDE -> satirlar.sortedWith(compareByDescending<VarlikDonemGetirisi> { it.yuzde != null }.thenByDescending { it.yuzde })
            Siralama.TL -> satirlar.sortedByDescending { it.tl }
        }

    fun donemSec(donem: Donem) = secim.update { it.copy(donem = donem) }

    fun siralamaSec(siralama: Siralama) = secim.update { it.copy(siralama = siralama) }

    /** Kategori satırına dokunulunca altındaki varlıklar açılır ya da kapanır. */
    fun kategoriAcKapa(kategori: Category) = secim.update {
        it.copy(acik = if (kategori in it.acik) it.acik - kategori else it.acik + kategori)
    }
}

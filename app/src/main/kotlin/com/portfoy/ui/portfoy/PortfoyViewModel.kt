package com.portfoy.ui.portfoy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfoy.TazelemeDurumu
import com.portfoy.GecmisYoneticisi
import com.portfoy.TazelemeYoneticisi
import com.portfoy.calc.Donem
import com.portfoy.calc.PortfolioSummary
import com.portfoy.calc.summarize
import com.portfoy.data.repository.GrafikDeposu
import com.portfoy.data.repository.GrafikVerisi
import com.portfoy.data.repository.PortfolioData
import com.portfoy.data.repository.PortfolioRepository
import com.portfoy.model.Category
import com.portfoy.ui.para.ParaBirimiTercihi
import com.portfoy.ui.para.ParaBirimiTercihiDeposu
import dagger.hilt.android.lifecycle.HiltViewModel
import com.portfoy.di.UygulamaZamanDilimi
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
import kotlinx.coroutines.launch

/**
 * Sekmeler arası geçişte korunan ekran durumu: açık kategoriler, toplam değer grafiği ve seçili
 * dönem. ViewModel sekme değişse de yaşadığı için bu durum kaybolmaz. Varlık detayı artık ayrı
 * bir ekranda (M17, [com.portfoy.ui.varlik.VarlikYonetimEkrani]) — burada durumu tutulmaz.
 */
data class PortfoyDurumu(
    val acikKategoriler: Set<Category> = emptySet(),
    val degerGrafigiAcik: Boolean = false,
    val donem: Donem = Donem.VARSAYILAN,
)

data class PortfoyEkranVerisi(
    val yuklendi: Boolean = false,
    val veri: PortfolioData? = null,
    val ozet: PortfolioSummary? = null,
    val enEskiIslem: LocalDate? = null,
    val durum: PortfoyDurumu = PortfoyDurumu(),
    val tazeleme: TazelemeDurumu = TazelemeDurumu(),
)

/** Toplam değer grafiğinin verisi. Grafik kapalıyken hesaplanmaz. */
data class GrafikDurumu(
    val veri: GrafikVerisi? = null,
    /** Geçmiş fiyat serileri arka planda tamamlanıyor. */
    val gecmisYukleniyor: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PortfoyViewModel @Inject constructor(
    private val depo: PortfolioRepository,
    private val yonetici: TazelemeYoneticisi,
    private val grafikDeposu: GrafikDeposu,
    private val paraBirimiDeposu: ParaBirimiTercihiDeposu,
    gecmis: GecmisYoneticisi,
    private val saat: Clock,
) : ViewModel() {

    private val durum = MutableStateFlow(PortfoyDurumu())

    val ekran: StateFlow<PortfoyEkranVerisi> = combine(
        depo.observePortfolio(),
        durum,
        yonetici.durum,
    ) { veri, durum, tazeleme ->
        PortfoyEkranVerisi(
            yuklendi = true,
            veri = veri,
            ozet = summarize(veri.holdings),
            enEskiIslem = veri.holdings.flatMap { it.transactions }.minOfOrNull { it.tradeDate },
            durum = durum,
            tazeleme = tazeleme,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PortfoyEkranVerisi())

    /**
     * Toplam portföy değerinin (TL) zaman grafiği. Fiyat, işlem ya da geçmiş seri değiştikçe yeniden hesaplanır;
     * grafik kapalıyken hesap yapılmaz.
     */
    val grafik: StateFlow<GrafikDurumu> = combine(
        durum,
        depo.observePortfolio(),
        gecmis.guncellendi.onStart { emit(Unit) },
        gecmis.yukleniyor,
    ) { d, _, _, yukleniyor -> Triple(d.degerGrafigiAcik, d.donem, yukleniyor) }
        .mapLatest { (acik, donem, yukleniyor) ->
            if (!acik) GrafikDurumu()
            else GrafikDurumu(grafikDeposu.hesapla(donem, saat.instant().atZone(UygulamaZamanDilimi).toLocalDate()), yukleniyor)
        }
        .flowOn(Dispatchers.Default) // seri hesabı ana iş parçacığında yapılmaz
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrafikDurumu())

    /**
     * M19 — günlük değişim satırı: dönem seçiminden bağımsız, her zaman "1 Gün" (Performans
     * sekmesindeki aynı hesaplama yeniden kullanılır). Grafik kapalıyken de hesaplanır çünkü artık
     * her zaman görünen bir özet satırı.
     */
    val gunlukDegisim: StateFlow<GrafikVerisi?> = combine(
        depo.observePortfolio(),
        gecmis.guncellendi.onStart { emit(Unit) },
    ) { _, _ -> Unit }
        .mapLatest { grafikDeposu.hesapla(Donem.BIR_GUN, saat.instant().atZone(UygulamaZamanDilimi).toLocalDate()) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val paraBirimi: StateFlow<ParaBirimiTercihi> = paraBirimiDeposu.tercih

    fun paraBirimiSec(yeni: ParaBirimiTercihi) = paraBirimiDeposu.ayarla(yeni)

    fun kategoriyiAcKapat(kategori: Category) = durum.update {
        it.copy(acikKategoriler = if (kategori in it.acikKategoriler) it.acikKategoriler - kategori else it.acikKategoriler + kategori)
    }

    fun degerGrafiginiAcKapat() = durum.update { it.copy(degerGrafigiAcik = !it.degerGrafigiAcik) }

    fun donemSec(donem: Donem) = durum.update { it.copy(donem = donem) }

    /** Aşağı çekerek yenileme. */
    suspend fun yenile(): Boolean = yonetici.manuelTazele()

    fun onForeground() = yonetici.onForeground()
}

/** Piyasası olan kategoriler için takvim; "piyasa kapalı" etiketi buna göre gösterilir. */
internal val Category.piyasa: com.portfoy.network.market.Market?
    get() = when (this) {
        Category.ABD -> com.portfoy.network.market.Market.US
        Category.BIST -> com.portfoy.network.market.Market.BIST
        else -> null
    }

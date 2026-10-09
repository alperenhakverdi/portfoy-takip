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
import com.portfoy.data.repository.KategoriDonemGetirisi
import com.portfoy.data.repository.PortfolioData
import com.portfoy.data.repository.PortfolioRepository
import com.portfoy.model.Category
import com.portfoy.ui.para.ParaBirimiTercihi
import com.portfoy.ui.para.ParaBirimiTercihiDeposu
import dagger.hilt.android.lifecycle.HiltViewModel
import com.portfoy.di.UygulamaZamanDilimi
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

/**
 * M21 — getiri her satırda (özet kartı, her kategori) kendi dönemini seçer: Günlük/Haftalık/Tümü.
 * Dokununca sırayla değişir, üçü de önceden hesaplanıp hazır tutulur ki geçiş anında olsun.
 */
enum class GetiriDonemi(val donem: Donem, val kisaEtiket: String) {
    GUNLUK(Donem.BIR_GUN, "G"),
    HAFTALIK(Donem.BIR_HAFTA, "H"),
    TUMU(Donem.TUMU, "TÜM"),
    ;

    fun sonraki(): GetiriDonemi = entries[(ordinal + 1) % entries.size]
}

/** Bir dönemdeki kâr/zarar: TL ve (hesaplanabiliyorsa) yüzde. Ekranda hep `TL (yüzde)` biçiminde yazılır. */
data class GetiriDegeri(val tl: BigDecimal, val yuzde: BigDecimal?)

/**
 * Üç dönemin (Günlük/Haftalık/Tümü) önceden hesaplanmış getirileri — hem toplam portföy hem kategori
 * kırılımı (kategorinin altındaki varlıklarla birlikte). Üçü de hazır tutulduğu için dönem düğmesine
 * basınca yeniden hesaplama beklenmez.
 */
data class DonemselGetiriler(
    val toplam: Map<GetiriDonemi, GetiriDegeri> = emptyMap(),
    val kategoriler: Map<GetiriDonemi, List<KategoriDonemGetirisi>> = emptyMap(),
) {
    fun kategori(donem: GetiriDonemi, kategori: Category): KategoriDonemGetirisi? =
        kategoriler[donem]?.firstOrNull { it.kategori == kategori }
}

/**
 * Sekmeler arası geçişte korunan ekran durumu: açık kategoriler ve her satırın (özet kartı + her
 * kategori) seçili dönemi. ViewModel sekme değişse de yaşadığı için bu durum kaybolmaz. Varlık detayı
 * ayrı bir ekranda (M17), toplam değer grafiği de ayrı bir ekranda (M21) — burada durumu tutulmaz.
 *
 * M27 — özet kartının dönem düğmesi "hepsini birden ayarla" gibi davranır: değiştiğinde tüm
 * kategorilerin kendi seçimi sıfırlanır, hepsi özet kartıyla aynı dönemi gösterir. Bir kategoriye
 * tek tek dokunmak yine onu bağımsızlaştırır (`kategoriDonemleri`'ne o kategori için ayrı bir kayıt
 * düşer) — ta ki özet kartı tekrar değişip haritayı sıfırlayana kadar.
 */
data class PortfoyDurumu(
    val acikKategoriler: Set<Category> = emptySet(),
    /** Özet kartındaki toplam getirinin dönemi; varsayılan Tümü (önce "büyük resim"). */
    val ozetDonemi: GetiriDonemi = GetiriDonemi.TUMU,
    /** Özet kartından ayrılmış (elle değiştirilmiş) kategorilerin kendi dönemi; yoksa özet kartını izler. */
    val kategoriDonemleri: Map<Category, GetiriDonemi> = emptyMap(),
) {
    fun kategoriDonemi(kategori: Category): GetiriDonemi = kategoriDonemleri[kategori] ?: ozetDonemi
}

data class PortfoyEkranVerisi(
    val yuklendi: Boolean = false,
    val veri: PortfolioData? = null,
    val ozet: PortfolioSummary? = null,
    val enEskiIslem: LocalDate? = null,
    val durum: PortfoyDurumu = PortfoyDurumu(),
    val tazeleme: TazelemeDurumu = TazelemeDurumu(),
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

    /** M23 — özet kartındaki rakamlara dokununca TL↔USD değişir; tercih kalıcıdır. */
    val paraBirimi: StateFlow<ParaBirimiTercihi> = paraBirimiDeposu.tercih

    fun paraBirimiDegistir() = paraBirimiDeposu.degistir()

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
     * Günlük/Haftalık/Tümü dönemlerinin tamamı önceden hesaplanır — hem toplam portföy hem kategori
     * kırılımı (varlıklarıyla birlikte). Özet kartı ve her kategori satırı kendi dönem düğmesiyle
     * buradan okur; dönem değişince yeniden hesaplama gerekmez.
     */
    val donemselGetiriler: StateFlow<DonemselGetiriler> = combine(
        depo.observePortfolio(),
        gecmis.guncellendi.onStart { emit(Unit) },
    ) { _, _ -> Unit }
        .mapLatest {
            val bugun = saat.instant().atZone(UygulamaZamanDilimi).toLocalDate()
            val toplam = mutableMapOf<GetiriDonemi, GetiriDegeri>()
            val kategoriler = mutableMapOf<GetiriDonemi, List<KategoriDonemGetirisi>>()
            for (secim in GetiriDonemi.entries) {
                grafikDeposu.hesapla(secim.donem, bugun)?.let { toplam[secim] = GetiriDegeri(it.toplamTl, it.toplamYuzde) }
                kategoriler[secim] = grafikDeposu.kategoriGetirileri(secim.donem, bugun)
            }
            DonemselGetiriler(toplam, kategoriler)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DonemselGetiriler())

    fun kategoriyiAcKapat(kategori: Category) = durum.update {
        it.copy(acikKategoriler = if (kategori in it.acikKategoriler) it.acikKategoriler - kategori else it.acikKategoriler + kategori)
    }

    /** M27 — özet kartının dönemi değişince tüm kategoriler onu izlemeye döner (elle ayrılmış olanlar dahil). */
    fun ozetDonemiDegistir() = durum.update { it.copy(ozetDonemi = it.ozetDonemi.sonraki(), kategoriDonemleri = emptyMap()) }

    fun kategoriDonemiDegistir(kategori: Category) = durum.update {
        it.copy(kategoriDonemleri = it.kategoriDonemleri + (kategori to it.kategoriDonemi(kategori).sonraki()))
    }

    /** Aşağı çekerek yenileme. */
    suspend fun yenile(): Boolean = yonetici.manuelTazele()

    fun onForeground() = yonetici.onForeground()
}

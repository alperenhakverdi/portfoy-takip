package com.portfoy.ui.portfoy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfoy.TazelemeDurumu
import com.portfoy.TazelemeYoneticisi
import com.portfoy.calc.Donem
import com.portfoy.calc.PortfolioSummary
import com.portfoy.calc.summarize
import com.portfoy.data.repository.PortfolioData
import com.portfoy.data.repository.PortfolioRepository
import com.portfoy.data.repository.PriceRepository
import com.portfoy.model.Category
import com.portfoy.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Sekmeler arası geçişte korunan ekran durumu: açık kategoriler, açık varlık detayı, toplam değer grafiği
 * ve seçili dönem. ViewModel sekme değişse de yaşadığı için bu durum kaybolmaz.
 */
data class PortfoyDurumu(
    val acikKategoriler: Set<Category> = emptySet(),
    val acikVarlik: Long? = null,
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

@HiltViewModel
class PortfoyViewModel @Inject constructor(
    private val depo: PortfolioRepository,
    private val fiyatDeposu: PriceRepository,
    private val yonetici: TazelemeYoneticisi,
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

    fun kategoriyiAcKapat(kategori: Category) = durum.update {
        it.copy(acikKategoriler = if (kategori in it.acikKategoriler) it.acikKategoriler - kategori else it.acikKategoriler + kategori)
    }

    fun varligiAcKapat(id: Long) = durum.update { it.copy(acikVarlik = if (it.acikVarlik == id) null else id) }

    fun degerGrafiginiAcKapat() = durum.update { it.copy(degerGrafigiAcik = !it.degerGrafigiAcik) }

    fun donemSec(donem: Donem) = durum.update { it.copy(donem = donem) }

    /** Aşağı çekerek yenileme. */
    suspend fun yenile(): Boolean = yonetici.manuelTazele()

    fun onForeground() = yonetici.onForeground()

    fun alimGuncelle(islem: Transaction) {
        viewModelScope.launch { depo.updatePurchase(islem) }
    }

    /** Kaydı siler ve geri alma için döndürür. */
    suspend fun alimSil(id: Long): Transaction? = depo.deletePurchase(id)

    fun alimiGeriAl(islem: Transaction) {
        viewModelScope.launch { depo.restorePurchase(islem) }
    }

    fun elleFiyatGir(varlikId: Long, fiyat: BigDecimal) {
        viewModelScope.launch { fiyatDeposu.setManualPrice(varlikId, fiyat) }
    }
}

/** Piyasası olan kategoriler için takvim; "piyasa kapalı" etiketi buna göre gösterilir. */
internal val Category.piyasa: com.portfoy.network.market.Market?
    get() = when (this) {
        Category.ABD -> com.portfoy.network.market.Market.US
        Category.BIST -> com.portfoy.network.market.Market.BIST
        else -> null
    }

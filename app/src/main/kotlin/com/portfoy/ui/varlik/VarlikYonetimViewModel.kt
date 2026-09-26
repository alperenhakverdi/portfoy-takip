package com.portfoy.ui.varlik

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfoy.GecmisYoneticisi
import com.portfoy.TazelemeYoneticisi
import com.portfoy.calc.AssetResult
import com.portfoy.calc.summarize
import com.portfoy.data.repository.PortfolioRepository
import com.portfoy.data.repository.PriceRepository
import com.portfoy.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VarlikYonetimVerisi(
    val yuklendi: Boolean = false,
    /** `null` ise varlık bulunamadı (tüm kayıtlar silindi) ya da henüz yüklenmedi. */
    val sonuc: AssetResult? = null,
    val islemler: List<Transaction> = emptyList(),
    val elleFiyat: Boolean = false,
    val elleFiyatZamani: Instant? = null,
)

/**
 * M17 — Varlık yönetimi ekranı. Hem Portföy hem Performans'tan aynı ekrana gidilir; bir varlığın
 * tüm alım/azaltma kayıtlarını, düzenleme/silme işlemlerini ve "+ Ekle" / "− Azalt" hareketlerini
 * tek yerde toplar.
 */
@HiltViewModel
class VarlikYonetimViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val depo: PortfolioRepository,
    private val fiyatDeposu: PriceRepository,
    private val yonetici: TazelemeYoneticisi,
    private val gecmis: GecmisYoneticisi,
) : ViewModel() {

    private val assetId: Long = checkNotNull(savedStateHandle["assetId"])

    val ekran: StateFlow<VarlikYonetimVerisi> = depo.observePortfolio()
        .map { veri ->
            val holding = veri.holdings.firstOrNull { it.asset.id == assetId }
            if (holding == null) {
                VarlikYonetimVerisi(yuklendi = true, sonuc = null)
            } else {
                val ozet = summarize(listOf(holding))
                val sonuc = ozet.categories.firstOrNull()?.assets?.firstOrNull()
                VarlikYonetimVerisi(
                    yuklendi = true,
                    sonuc = sonuc,
                    islemler = holding.transactions,
                    elleFiyat = assetId in veri.manualPriceAssetIds,
                    elleFiyatZamani = veri.manualPriceTimes[assetId],
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VarlikYonetimVerisi())

    fun alimGuncelle(islem: Transaction) {
        viewModelScope.launch { depo.updatePurchase(islem) }
    }

    /** Kaydı siler ve geri alma için döndürür. */
    suspend fun alimSil(id: Long): Transaction? = depo.deletePurchase(id)

    fun alimiGeriAl(islem: Transaction) {
        viewModelScope.launch { depo.restorePurchase(islem) }
    }

    fun elleFiyatGir(fiyat: BigDecimal) {
        viewModelScope.launch { fiyatDeposu.setManualPrice(assetId, fiyat) }
    }

    /** "+ Ekle": yeni bir alım hareketi. */
    fun ekle(adet: BigDecimal, fiyat: BigDecimal, komisyon: BigDecimal, tarih: LocalDate, not: String?) {
        viewModelScope.launch {
            depo.addPurchase(assetId, adet, fiyat, komisyon, tarih, not)
            yonetici.simdiTazele()
            gecmis.tamamla()
        }
    }

    /** "− Azalt": adet düşer, ağırlıklı ortalama maliyet değişmez (karar M17.4, gerçekleşen kâr/zarar yok). */
    fun azalt(adet: BigDecimal, fiyat: BigDecimal, tarih: LocalDate, not: String?) {
        viewModelScope.launch { depo.addReduction(assetId, adet, fiyat, tarih, not) }
    }
}

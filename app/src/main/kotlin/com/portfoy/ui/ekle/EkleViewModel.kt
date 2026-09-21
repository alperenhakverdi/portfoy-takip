package com.portfoy.ui.ekle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfoy.TazelemeYoneticisi
import com.portfoy.calc.AlimDegerleri
import com.portfoy.data.db.AssetDao
import com.portfoy.data.repository.PortfolioRepository
import com.portfoy.data.repository.PriceRepository
import com.portfoy.data.repository.SearchHit
import com.portfoy.model.Asset
import com.portfoy.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Formu açılmış varlık ve kaynaklardan gelen (ya da önbellekteki) önerilen fiyat. */
data class SeciliVarlik(
    val varlik: Asset,
    val onerilenFiyat: BigDecimal? = null,
    val fiyatYukleniyor: Boolean = false,
)

/** Ekle sekmesinin arama dışındaki durumu. Sekmeden çıkılınca form alanları sıfırlanır, arama korunur. */
private data class EkleIc(
    val sorgu: String = "",
    val secili: SeciliVarlik? = null,
    val manuelAcik: Boolean = false,
    val kur: BigDecimal? = null,
    val nakit: Asset? = null,
    val sikAranan: List<Asset> = emptyList(),
)

data class EkleEkranVerisi(
    val sorgu: String = "",
    /** Arama yazıldıkça filtrelenir; en az 2 karakter gerekir. */
    val aramaAktif: Boolean = false,
    val sonuclar: List<SearchHit> = emptyList(),
    val nakit: Asset? = null,
    val sonEklenenler: List<Asset> = emptyList(),
    val sikAranan: List<Asset> = emptyList(),
    val secili: SeciliVarlik? = null,
    val manuelAcik: Boolean = false,
    val kur: BigDecimal? = null,
)

sealed interface EkleOlayi {
    /** Kayıt tamamlandı; kullanıcı 3. sekmeye (Portföy) yönlendirilir. */
    data object Kaydedildi : EkleOlayi
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EkleViewModel @Inject constructor(
    private val depo: PortfolioRepository,
    private val fiyatDeposu: PriceRepository,
    private val assetDao: AssetDao,
    private val yonetici: TazelemeYoneticisi,
) : ViewModel() {

    private val ic = MutableStateFlow(EkleIc())
    private val olayKanali = Channel<EkleOlayi>(Channel.BUFFERED)
    val olaylar: Flow<EkleOlayi> = olayKanali.receiveAsFlow()

    private val sonuclar: Flow<List<SearchHit>> = ic.map { it.sorgu }
        .distinctUntilChanged()
        .mapLatest { sorgu ->
            delay(80) // yazdıkça filtrele, her tuşta sorgu atma
            depo.search(sorgu)
        }

    val ekran: StateFlow<EkleEkranVerisi> = combine(ic, sonuclar, depo.observeRecentAssets()) { ic, sonuclar, son ->
        EkleEkranVerisi(
            sorgu = ic.sorgu,
            aramaAktif = ic.sorgu.trim().length >= com.portfoy.calc.MIN_SEARCH_LENGTH,
            sonuclar = sonuclar,
            nakit = ic.nakit,
            sonEklenenler = son,
            sikAranan = ic.sikAranan,
            secili = ic.secili,
            manuelAcik = ic.manuelAcik,
            kur = ic.kur,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EkleEkranVerisi())

    init {
        viewModelScope.launch {
            // Nakit TL her zaman erişilebilir sabit bir kayıttır; sık aranan kodlar kısayol olarak çözülür.
            // İlk kurulumda katalog arka planda yükleniyor olabilir; hazır olana kadar birkaç kez denenir.
            var nakit = depo.cashAsset()
            var sik = sikAraniyor()
            var deneme = 0
            while ((nakit == null || sik.size < SIK_ARANAN.size) && deneme++ < 100) {
                delay(300)
                nakit = depo.cashAsset()
                sik = sikAraniyor()
            }
            ic.update { it.copy(nakit = nakit, sikAranan = sik) }
        }
    }

    private suspend fun sikAraniyor(): List<Asset> =
        SIK_ARANAN.mapNotNull { kod -> depo.search(kod).firstOrNull { it.asset.code == kod }?.asset }

    fun sorguDegistir(sorgu: String) = ic.update { it.copy(sorgu = sorgu) }

    fun sec(varlik: Asset) {
        viewModelScope.launch {
            val nakit = varlik.category == Category.NAKIT
            ic.update {
                it.copy(secili = SeciliVarlik(varlik, depo.lastPriceTl(varlik.id), fiyatYukleniyor = !nakit), manuelAcik = false)
            }
            ic.update { it.copy(kur = depo.latestUsdTry()) }
            if (!nakit) {
                // Seçim anında tek çağrıyla taze fiyat; arama sırasında hiç çağrı atılmaz.
                assetDao.getById(varlik.id)?.let { fiyatDeposu.refresh(listOf(it), PriceRepository.MANUAL_MIN_AGE) }
                val fiyat = depo.lastPriceTl(varlik.id)
                val kur = depo.latestUsdTry()
                ic.update { durum ->
                    durum.copy(
                        secili = durum.secili?.takeIf { it.varlik.id == varlik.id }?.copy(onerilenFiyat = fiyat, fiyatYukleniyor = false),
                        kur = kur,
                    )
                }
            }
        }
    }

    fun secimiKapat() = ic.update { it.copy(secili = null) }

    fun manuelAc() = ic.update { it.copy(manuelAcik = true) }

    fun manuelKapat() = ic.update { it.copy(manuelAcik = false) }

    /** Sekmeden çıkılınca çağrılır: girilen değerler kaybolur, arama metni korunur. */
    fun formuSifirla() = ic.update { it.copy(secili = null, manuelAcik = false) }

    fun kaydet(degerler: AlimDegerleri, not: String) {
        val secili = ic.value.secili ?: return
        viewModelScope.launch {
            depo.addPurchase(secili.varlik.id, degerler.adet, degerler.fiyat, degerler.komisyon, degerler.tarih, not)
            yonetici.simdiTazele()
            ic.update { it.copy(secili = null, sorgu = "") }
            olayKanali.send(EkleOlayi.Kaydedildi)
        }
    }

    /** Aramada bulunamayan varlığı kullanıcı tanımlar; ardından alım formu açılır. */
    fun manuelKaydet(kod: String, ad: String, kategori: Category, fiyatTl: BigDecimal) {
        viewModelScope.launch {
            val id = depo.addManualAsset(kod, ad, kategori, fiyatTl)
            depo.asset(id)?.let { sec(it) }
        }
    }

    private companion object {
        val SIK_ARANAN = listOf("AAPL", "NVDA", "THYAO", "ASELS", "XAUGR")
    }
}

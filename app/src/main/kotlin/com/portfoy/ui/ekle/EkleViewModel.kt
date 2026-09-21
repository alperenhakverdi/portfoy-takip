package com.portfoy.ui.ekle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfoy.GecmisYoneticisi
import com.portfoy.TazelemeYoneticisi
import com.portfoy.calc.AlimDegerleri
import com.portfoy.data.db.AssetDao
import com.portfoy.data.repository.PortfolioRepository
import com.portfoy.data.repository.PriceRepository
import com.portfoy.data.repository.SearchHit
import com.portfoy.model.Asset
import com.portfoy.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import com.portfoy.di.UygulamaZamanDilimi
import java.math.BigDecimal
import java.time.LocalDate
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

/** Ekle sekmesinin durumu. Sekmeden çıkılınca başa (kategori listesine) döner. */
private data class EkleIc(
    val kategori: Category? = null,
    val sorgu: String = "",
    val secili: SeciliVarlik? = null,
    val manuelAcik: Boolean = false,
    val kur: BigDecimal? = null,
    val nakit: Asset? = null,
)

data class EkleEkranVerisi(
    /** `null` ise kategori listesi gösterilir; doluysa o kategorinin arama/liste ekranı. */
    val kategori: Category? = null,
    val sorgu: String = "",
    /** Arama yazıldıkça filtrelenir; en az 2 karakter gerekir. */
    val aramaAktif: Boolean = false,
    val sonuclar: List<SearchHit> = emptyList(),
    /** Kategori az kayıtlıysa (emtia, döviz, BIST) aramaya gerek kalmadan listelenen varlıklar. */
    val kategoriListesi: List<SearchHit> = emptyList(),
    /** Kategoride çok kayıt var; kullanıcı arama yazmadan liste gösterilmez (ABD, fon). */
    val aramaGerekli: Boolean = false,
    val nakit: Asset? = null,
    val sonEklenenler: List<Asset> = emptyList(),
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
    private val gecmis: GecmisYoneticisi,
) : ViewModel() {

    private val ic = MutableStateFlow(EkleIc())
    private val olayKanali = Channel<EkleOlayi>(Channel.BUFFERED)
    val olaylar: Flow<EkleOlayi> = olayKanali.receiveAsFlow()

    private data class Liste(
        val sonuclar: List<SearchHit> = emptyList(),
        val kategoriListesi: List<SearchHit> = emptyList(),
        val aramaGerekli: Boolean = false,
    )

    private val liste: Flow<Liste> = ic.map { it.kategori to it.sorgu }
        .distinctUntilChanged()
        .mapLatest { (kategori, sorgu) ->
            if (kategori == null) return@mapLatest Liste()
            delay(80) // yazdıkça filtrele, her tuşta sorgu atma
            val aramaAktif = sorgu.trim().length >= com.portfoy.calc.MIN_SEARCH_LENGTH
            if (aramaAktif) return@mapLatest Liste(sonuclar = depo.search(sorgu, category = kategori))
            // Kayıt sayısı azsa liste doğrudan gelir; çoksa (ABD, fon) arama beklenir.
            val tumu = if (depo.kategoriSayisi(kategori) <= LISTE_SINIRI) depo.kategoriListesi(kategori, LISTE_SINIRI) else emptyList()
            Liste(kategoriListesi = tumu, aramaGerekli = tumu.isEmpty())
        }

    val ekran: StateFlow<EkleEkranVerisi> = combine(ic, liste, depo.observeRecentAssets()) { ic, liste, son ->
        EkleEkranVerisi(
            kategori = ic.kategori,
            sorgu = ic.sorgu,
            aramaAktif = ic.sorgu.trim().length >= com.portfoy.calc.MIN_SEARCH_LENGTH,
            sonuclar = liste.sonuclar,
            kategoriListesi = liste.kategoriListesi,
            aramaGerekli = liste.aramaGerekli,
            nakit = ic.nakit,
            sonEklenenler = son,
            secili = ic.secili,
            manuelAcik = ic.manuelAcik,
            kur = ic.kur,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EkleEkranVerisi())

    init {
        viewModelScope.launch {
            // Nakit TL sabit bir kayıttır; ilk kurulumda varsayılan varlıklar yazılana kadar birkaç kez denenir.
            var nakit = depo.cashAsset()
            var deneme = 0
            while (nakit == null && deneme++ < 100) {
                delay(300)
                nakit = depo.cashAsset()
            }
            ic.update { it.copy(nakit = nakit) }
        }
    }

    /** Kategori seçildi: o kategorinin arama/liste ekranı açılır. Nakit TL doğrudan forma gider. */
    fun kategoriSec(kategori: Category) {
        if (kategori == Category.NAKIT) {
            ic.value.nakit?.let { sec(it) }
            return
        }
        ic.update { it.copy(kategori = kategori, sorgu = "") }
    }

    fun kategoriyiKapat() = ic.update { it.copy(kategori = null, sorgu = "") }

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

    /**
     * ABD varlığında fiyat USD girilirken, alış tarihindeki kuru bulur. Tarih birkaç günden eskiyse o güne ait kur serisi
     * önce tamamlanır; ağ yoksa son bilinen kur kullanılır.
     */
    fun kurGuncelle(tarih: LocalDate) {
        viewModelScope.launch {
            val bugun = LocalDate.now(UygulamaZamanDilimi)
            if (tarih.isBefore(bugun.minusDays(3))) gecmis.kurGecmisiniHazirla(tarih, UygulamaZamanDilimi)
            val kur = depo.usdTryOn(tarih)
            ic.update { it.copy(kur = kur) }
        }
    }

    fun secimiKapat() = ic.update { it.copy(secili = null) }

    fun manuelAc() = ic.update { it.copy(manuelAcik = true) }

    fun manuelKapat() = ic.update { it.copy(manuelAcik = false) }

    /** Sekmeden çıkılınca çağrılır: girilen değerler kaybolur, ekran kategori listesine döner. */
    fun formuSifirla() = ic.update { it.copy(kategori = null, sorgu = "", secili = null, manuelAcik = false) }

    fun kaydet(degerler: AlimDegerleri, not: String) {
        val secili = ic.value.secili ?: return
        viewModelScope.launch {
            depo.addPurchase(secili.varlik.id, degerler.adet, degerler.fiyat, degerler.komisyon, degerler.tarih, not)
            yonetici.simdiTazele()
            gecmis.tamamla() // yeni alış için geçmiş fiyat serisi arka planda çekilir
            ic.update { it.copy(kategori = null, secili = null, sorgu = "") }
            olayKanali.send(EkleOlayi.Kaydedildi)
        }
    }

    /** Aramada bulunamayan varlığı kullanıcı tanımlar; kategori zaten seçilidir. */
    fun manuelKaydet(kod: String, ad: String, kategori: Category, fiyatTl: BigDecimal) {
        viewModelScope.launch {
            val id = depo.addManualAsset(kod, ad, kategori, fiyatTl)
            depo.asset(id)?.let { sec(it) }
        }
    }

    private companion object {
        /** Bu sayıya kadar olan kategoriler doğrudan listelenir; üstü aramayla bulunur (ABD 27 bin, fon 2 bin). */
        const val LISTE_SINIRI = 600
    }
}

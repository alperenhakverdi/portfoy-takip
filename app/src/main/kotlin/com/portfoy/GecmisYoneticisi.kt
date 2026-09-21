package com.portfoy

import com.portfoy.data.db.AssetDao
import com.portfoy.data.repository.GecmisSonucu
import com.portfoy.data.repository.HistoryRepository
import com.portfoy.data.repository.PortfolioRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Portföydeki varlıkların geçmiş fiyat serilerini arka planda tamamlar. Yalnızca eksik günler çekilir, bu yüzden
 * bir varlık eklendiğinde ve uygulama öne geldiğinde çağırmak güvenlidir. Bir varlık tamamlandıkça [guncellendi]
 * yayınlanır ve grafikler yeniden hesaplanır; TEFAS gibi yavaş kaynaklar diğer varlıkları bekletmez.
 */
@Singleton
class GecmisYoneticisi @Inject constructor(
    private val gecmis: HistoryRepository,
    private val portfoy: PortfolioRepository,
    private val assetDao: AssetDao,
    private val clock: Clock,
) {
    private val kapsam = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val kilit = Mutex()

    private val _yukleniyor = MutableStateFlow(false)
    val yukleniyor: StateFlow<Boolean> = _yukleniyor.asStateFlow()

    private val _guncellendi = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val guncellendi: SharedFlow<Unit> = _guncellendi.asSharedFlow()

    /** Arka planda başlatır. */
    fun tamamla() {
        kapsam.launch { tamamlaSenkron() }
    }

    /** Tüm varlıkların eksik geçmişini tamamlar; hepsi başarılıysa `true`. Arka plan işleri bunu bekler. */
    suspend fun tamamlaSenkron(): Boolean = kilit.withLock {
        _yukleniyor.value = true
        try {
            var hepsi = true
            for (h in portfoy.observePortfolio().first().holdings) {
                val ilkAlis = h.transactions.minOfOrNull { it.tradeDate } ?: continue
                val varlik = assetDao.getById(h.asset.id) ?: continue
                val sonuc = runCatching { gecmis.ensure(varlik, ilkAlis) }.getOrElse { GecmisSonucu(0, it) }
                if (!sonuc.basarili) hepsi = false
                _guncellendi.tryEmit(Unit)
            }
            hepsi
        } finally {
            _yukleniyor.value = false
        }
    }

    /**
     * Bir alış tarihindeki kuru bulabilmek için USD/TRY serisini o tarihten itibaren tamamlar. ABD varlığı için
     * fiyat USD girilirken çağrılır; ağ yoksa sessizce vazgeçilir ve son bilinen kur kullanılır.
     */
    suspend fun kurGecmisiniHazirla(tarih: LocalDate, zaman: ZoneId) {
        val bugun = clock.instant().atZone(zaman).toLocalDate()
        runCatching { gecmis.ensureFx(tarih.minusDays(10), bugun) }
    }
}

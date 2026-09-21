package com.portfoy

import com.portfoy.data.repository.PortfolioRepository
import com.portfoy.data.repository.PriceRepository
import com.portfoy.data.repository.RefreshReport
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class TazelemeDurumu(
    val yenileniyor: Boolean = false,
    /** Son turun özeti; sorun varsa ekranda "veriler güncel değil" gösterilir. */
    val sonRapor: RefreshReport? = null,
)

/**
 * Fiyat tazelemeyi yönetir:
 * - Uygulama öne geldiğinde son çekimin üzerinden 15 dakika geçmişse tazeler (arka planda çekim yok).
 * - Elle tazeleme (aşağı çekme) dakikada en fazla 1 kez çalışır.
 * Aynı anda tek tur çalışır.
 */
@Singleton
class TazelemeYoneticisi @Inject constructor(
    private val fiyatDeposu: PriceRepository,
    private val portfoyDeposu: PortfolioRepository,
    private val clock: Clock,
) {
    private val kapsam = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val kilit = Mutex()
    private val _durum = MutableStateFlow(TazelemeDurumu())
    val durum: StateFlow<TazelemeDurumu> = _durum.asStateFlow()

    private var sonTur: Instant? = null
    private var sonManuel: Instant? = null

    /** Uygulama öne geldi. */
    fun onForeground() {
        val simdi = clock.instant()
        val son = sonTur
        if (son != null && Duration.between(son, simdi) < PriceRepository.AUTO_MIN_AGE) return
        kapsam.launch { tazele(PriceRepository.AUTO_MIN_AGE) }
    }

    /** Aşağı çekerek yenileme. Bir dakikadan sık istenirse `false` döner ve istek atılmaz. */
    suspend fun manuelTazele(): Boolean {
        val simdi = clock.instant()
        val son = sonManuel
        if (son != null && Duration.between(son, simdi) < MANUEL_ARALIK) return false
        sonManuel = simdi
        tazele(PriceRepository.MANUAL_MIN_AGE)
        return true
    }

    /** Yeni eklenen varlığın fiyatı hemen çekilsin diye. */
    fun simdiTazele() {
        kapsam.launch { tazele(PriceRepository.MANUAL_MIN_AGE) }
    }

    private suspend fun tazele(minAge: Duration) {
        kilit.withLock {
            _durum.value = _durum.value.copy(yenileniyor = true)
            val varliklar = portfoyDeposu.observeHeldAssetEntities().first()
            val rapor = runCatching { fiyatDeposu.refresh(varliklar, minAge) }.getOrNull()
            sonTur = clock.instant()
            _durum.value = TazelemeDurumu(yenileniyor = false, sonRapor = rapor)
        }
    }

    private companion object {
        val MANUEL_ARALIK: Duration = Duration.ofMinutes(1)
    }
}

package com.portfoy.arkaplan

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.portfoy.GecmisYoneticisi
import com.portfoy.data.db.PortfolioSnapshotDao
import com.portfoy.data.db.PriceHistoryDao
import com.portfoy.data.repository.SnapshotDeposu
import com.portfoy.data.repository.SonCalismaDeposu
import com.portfoy.data.repository.TazelemeZamanlayici
import com.portfoy.di.UygulamaZamanDilimi
import com.portfoy.calc.HISTORY_YEARS
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import com.portfoy.network.market.RefreshGroup
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Fiyat tazeleme: her 15 dakikada bir uyanır, vadesi gelen grubu (piyasa saatine göre) çalıştırır. */
@HiltWorker
class FiyatTazelemeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parametreler: WorkerParameters,
    private val zamanlayici: TazelemeZamanlayici,
) : CoroutineWorker(context, parametreler) {
    override suspend fun doWork(): Result {
        runCatching { zamanlayici.calistir() }
        return Result.success() // hata sonraki uyanmada yeniden denenir; iş zinciri bozulmaz
    }
}

/**
 * Günlük iş (00:30 TSİ): eksik geçmiş fiyatları tamamlar, eksik günlük kayıtları yazar ve saklama sınırını uygular.
 * Kayıt **kapsadığı güne** yazılır; iş geciktiyse ya da uygulama günlerce açılmadıysa eksik günler geriye dönük tamamlanır.
 */
@HiltWorker
class GunlukIsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parametreler: WorkerParameters,
    private val gecmis: GecmisYoneticisi,
    private val snapshot: SnapshotDeposu,
    private val gecmisDao: PriceHistoryDao,
    private val saat: Clock,
) : CoroutineWorker(context, parametreler) {
    override suspend fun doWork(): Result {
        val bugun = saat.instant().atZone(UygulamaZamanDilimi).toLocalDate()
        runCatching { gecmis.tamamlaSenkron() }
        runCatching { snapshot.eksikGunleriYaz(bugun) }
        // Günlük kapanış serisi varlık başına en fazla 5 yıl saklanır (doküman 11.3/1).
        runCatching { gecmisDao.deleteOlderThan(bugun.minusYears(HISTORY_YEARS)) }
        return Result.success()
    }
}

/**
 * Arama listesini (ABD, fon) ayda bir canlı kaynaklardan tazeler. Uygulama açılışını yormasın diye ayrı bir arka plan işidir;
 * gömülü liste zaten aramayı çalışır tutar. Başarısızsa WorkManager yeniden dener.
 */
@HiltWorker
class KatalogWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parametreler: WorkerParameters,
    private val guncelleyici: com.portfoy.KatalogGuncelleyici,
) : CoroutineWorker(context, parametreler) {
    override suspend fun doWork(): Result =
        if (runCatching { guncelleyici.gerekirseGuncelle() }.isSuccess) Result.success() else Result.retry()
}

/** Fiyat tazelemenin son çalışma zamanlarını kalıcı tutar. */
@Singleton
class SonCalismaTercihleri @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext context: Context,
) : SonCalismaDeposu {
    private val tercihler = context.getSharedPreferences("tazeleme", Context.MODE_PRIVATE)

    override fun al(grup: RefreshGroup): Instant? =
        tercihler.getLong(grup.name, 0L).takeIf { it > 0 }?.let(Instant::ofEpochMilli)

    override fun koy(grup: RefreshGroup, zaman: Instant) {
        tercihler.edit().putLong(grup.name, zaman.toEpochMilli()).apply()
    }
}

/**
 * İşleri planlar. Kısıtlar: ağ var ve pil düşük değil (doküman 13). Uygulama her açıldığında çağrılır; `KEEP` sayesinde
 * var olan plan bozulmaz.
 */
object IsPlanlayici {
    private const val TAZELEME = "fiyat_tazeleme"
    private const val GUNLUK = "gunluk_is"
    private const val KATALOG = "katalog_tazeleme"

    fun planla(context: Context, saat: Clock) {
        val kisitlar = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val yonetici = WorkManager.getInstance(context)

        // WorkManager'ın en kısa periyodu 15 dakikadır; hangi turun vadesi geldiğine zamanlayıcı karar verir.
        yonetici.enqueueUniquePeriodicWork(
            TAZELEME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<FiyatTazelemeWorker>(15, TimeUnit.MINUTES).setConstraints(kisitlar).build(),
        )
        yonetici.enqueueUniquePeriodicWork(
            GUNLUK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<GunlukIsWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(sonraki0030(saat).toMillis(), TimeUnit.MILLISECONDS)
                .setConstraints(kisitlar)
                .build(),
        )
        // Katalog tazeleme ayda bir; uygulama açıldıktan sonra rahatça çalışsın diye ilk çalışma bir saat geciktirilir.
        yonetici.enqueueUniquePeriodicWork(
            KATALOG,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<KatalogWorker>(30, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .setConstraints(kisitlar)
                .build(),
        )
    }

    /** Bir sonraki 00:30 TSİ'ye kalan süre. Günün son fiyatı geç oluşur: ABD kapanışı kışın 00:00 TSİ'dir. */
    internal fun sonraki0030(saat: Clock): Duration {
        val simdi = ZonedDateTime.ofInstant(saat.instant(), UygulamaZamanDilimi)
        var hedef = simdi.with(LocalTime.of(0, 30))
        if (!hedef.isAfter(simdi)) hedef = hedef.plusDays(1)
        return Duration.between(simdi, hedef)
    }
}

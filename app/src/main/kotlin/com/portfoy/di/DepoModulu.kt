package com.portfoy.di

import com.portfoy.BuildConfig
import com.portfoy.KatalogKaynaklari
import com.portfoy.arkaplan.SonCalismaTercihleri
import com.portfoy.data.db.AssetDao
import com.portfoy.data.db.PortfolioSnapshotDao
import com.portfoy.data.db.PortfoyDatabase
import com.portfoy.data.db.PriceHistoryDao
import com.portfoy.data.db.PriceQuoteDao
import com.portfoy.data.db.TransactionDao
import com.portfoy.data.repository.GrafikDeposu
import com.portfoy.data.repository.HistoryRepository
import com.portfoy.data.repository.KatalogDeposu
import com.portfoy.data.repository.PortfolioRepository
import com.portfoy.data.repository.PriceRepository
import com.portfoy.data.repository.SnapshotDeposu
import com.portfoy.data.repository.SonCalismaDeposu
import com.portfoy.data.repository.TazelemeZamanlayici
import com.portfoy.model.AssetRef
import com.portfoy.network.CallBudget
import com.portfoy.network.GecmisAnahtari
import com.portfoy.network.GecmisKaynagi
import com.portfoy.network.GunlukSayac
import com.portfoy.network.Http
import com.portfoy.network.HistoryRouter
import com.portfoy.network.Route
import com.portfoy.network.RouteKey
import com.portfoy.network.SourceHealth
import com.portfoy.network.SourceRouter
import com.portfoy.network.market.RefreshSchedule
import com.portfoy.network.sources.EvdsSource
import com.portfoy.network.sources.FinnhubSource
import com.portfoy.network.sources.OnsEmtiaSource
import com.portfoy.network.sources.TcmbGunlukSource
import com.portfoy.network.sources.TefasSource
import com.portfoy.network.sources.TruncgilSource
import com.portfoy.network.sources.TwelveDataSource
import com.portfoy.network.sources.YahooSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import javax.inject.Singleton

/** Uygulamanın kullandığı saat dilimi. Piyasa saatleri kendi dilimlerinde ayrıca hesaplanır. */
val UygulamaZamanDilimi: ZoneId = ZoneId.of("Europe/Istanbul")

@Module
@InstallIn(SingletonComponent::class)
object DepoModulu {

    @Provides
    @Singleton
    fun saat(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun http(): Http = Http()

    // Kaynaklar tek örnektir: istek aralığı sınırlayıcıları (TEFAS dakikada 6, Twelve Data dakikada 8) paylaşılır.
    @Provides @Singleton fun finnhub(http: Http) = FinnhubSource(http, BuildConfig.FINNHUB_API_KEY)

    @Provides @Singleton fun tefas(http: Http) = TefasSource(http)

    @Provides @Singleton fun yahoo(http: Http) = YahooSource(http)

    @Provides @Singleton fun truncgil(http: Http) = TruncgilSource(http)

    @Provides @Singleton fun twelveData(http: Http) = TwelveDataSource(http, BuildConfig.TWELVEDATA_API_KEY)

    @Provides @Singleton fun evds(http: Http) = EvdsSource(http, BuildConfig.EVDS_API_KEY)

    @Provides @Singleton fun tcmbGunluk(http: Http) = TcmbGunlukSource(http)

    /**
     * Anlık fiyat kaynakları (doküman 9.1, canlı denemelerle doğrulandı):
     * - ABD: Finnhub, yedek Twelve Data
     * - BIST: Yahoo (resmî ücretsiz kaynak yok; yedek elle fiyat girişidir)
     * - Fon: TEFAS
     * - Gram altın/gümüş: Truncgil, yedek Yahoo ons vadelisi × kur ÷ 31,1035
     * - USD/TRY: Truncgil (gün içi), yedek TCMB günlük kur
     */
    @Provides
    @Singleton
    fun kaynakYonlendirici(
        clock: Clock,
        finnhub: FinnhubSource,
        twelve: TwelveDataSource,
        yahoo: YahooSource,
        tefas: TefasSource,
        truncgil: TruncgilSource,
        tcmb: TcmbGunlukSource,
    ) = SourceRouter(
        routes = mapOf(
            RouteKey.US to Route(finnhub, twelve),
            RouteKey.BIST to Route(yahoo),
            RouteKey.FUND to Route(tefas),
            RouteKey.COMMODITY to Route(truncgil, OnsEmtiaSource(yahoo)),
            RouteKey.FX to Route(truncgil, tcmb),
        ),
        budget = CallBudget(CallBudget.DEFAULT_DAILY_CAPS, clock, UygulamaZamanDilimi),
        health = SourceHealth(),
        hataGunlugu = { android.util.Log.w("Portfoy", it) },
        clock = clock,
    )

    /**
     * Geçmiş seri zincirleri: kur EVDS → Yahoo, ABD Twelve Data → Yahoo, BIST Yahoo, fon TEFAS. Gram altın/gümüş, ons serisi
     * (Twelve Data spot → Yahoo vadeli) ile kur serisinden türetilir; Truncgil geçmiş vermez.
     */
    @Provides
    @Singleton
    fun gecmisYonlendirici(
        clock: Clock,
        evds: EvdsSource,
        twelve: TwelveDataSource,
        yahoo: YahooSource,
        tefas: TefasSource,
    ) = HistoryRouter(
        zincirler = mapOf(
            GecmisAnahtari.KUR to listOf(GecmisKaynagi(evds), GecmisKaynagi(yahoo)),
            GecmisAnahtari.ABD to listOf(GecmisKaynagi(twelve), GecmisKaynagi(yahoo)),
            GecmisAnahtari.BIST to listOf(GecmisKaynagi(yahoo)),
            GecmisAnahtari.FON to listOf(GecmisKaynagi(tefas)),
            GecmisAnahtari.ONS_ALTIN to listOf(
                GecmisKaynagi(twelve, AssetRef("XAU/USD", null)),
                GecmisKaynagi(yahoo, AssetRef("GC=F", null)),
            ),
            GecmisAnahtari.ONS_GUMUS to listOf(
                GecmisKaynagi(twelve, AssetRef("XAG/USD", null)),
                GecmisKaynagi(yahoo, AssetRef("SI=F", null)),
            ),
        ),
        sayac = GunlukSayac(CallBudget.HISTORY_DAILY_CAP, clock, UygulamaZamanDilimi),
    )

    @Provides
    @Singleton
    fun katalogKaynaklari(finnhub: FinnhubSource, tefas: TefasSource) = KatalogKaynaklari(listOf(finnhub, tefas))

    @Provides
    @Singleton
    fun katalogDeposu(db: PortfoyDatabase) = KatalogDeposu(db)

    @Provides
    @Singleton
    fun fiyatDeposu(router: SourceRouter, quoteDao: PriceQuoteDao, clock: Clock) =
        PriceRepository(router, quoteDao, clock)

    @Provides
    @Singleton
    fun portfoyDeposu(
        assetDao: AssetDao,
        transactionDao: TransactionDao,
        quoteDao: PriceQuoteDao,
        historyDao: PriceHistoryDao,
        clock: Clock,
    ) = PortfolioRepository(assetDao, transactionDao, quoteDao, clock, historyDao)

    @Provides
    @Singleton
    fun gecmisDeposu(router: HistoryRouter, historyDao: PriceHistoryDao, clock: Clock) =
        HistoryRepository(router, historyDao, clock, UygulamaZamanDilimi)

    @Provides
    @Singleton
    fun grafikDeposu(transactionDao: TransactionDao, assetDao: AssetDao, historyDao: PriceHistoryDao, quoteDao: PriceQuoteDao) =
        GrafikDeposu(transactionDao, assetDao, historyDao, quoteDao)

    @Provides
    @Singleton
    fun snapshotDeposu(grafik: GrafikDeposu, dao: PortfolioSnapshotDao) = SnapshotDeposu(grafik, dao)

    @Provides
    @Singleton
    fun zamanlayici(
        fiyat: PriceRepository,
        portfoy: PortfolioRepository,
        son: SonCalismaDeposu,
        clock: Clock,
    ) = TazelemeZamanlayici(fiyat, portfoy.observeHeldAssetEntities(), RefreshSchedule(), son, clock)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BaglamaModulu {
    @Binds
    abstract fun sonCalisma(tercihler: SonCalismaTercihleri): SonCalismaDeposu
}

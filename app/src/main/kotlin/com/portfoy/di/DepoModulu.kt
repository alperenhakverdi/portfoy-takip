package com.portfoy.di

import com.portfoy.BuildConfig
import com.portfoy.KatalogKaynaklari
import com.portfoy.data.db.AssetDao
import com.portfoy.data.db.PortfoyDatabase
import com.portfoy.data.db.PriceQuoteDao
import com.portfoy.data.db.TransactionDao
import com.portfoy.data.repository.KatalogDeposu
import com.portfoy.data.repository.PortfolioRepository
import com.portfoy.data.repository.PriceRepository
import com.portfoy.network.CallBudget
import com.portfoy.network.Http
import com.portfoy.network.Route
import com.portfoy.network.RouteKey
import com.portfoy.network.SourceHealth
import com.portfoy.network.SourceRouter
import com.portfoy.network.sources.FinnhubSource
import com.portfoy.network.sources.OnsEmtiaSource
import com.portfoy.network.sources.TcmbGunlukSource
import com.portfoy.network.sources.TefasSource
import com.portfoy.network.sources.TruncgilSource
import com.portfoy.network.sources.TwelveDataSource
import com.portfoy.network.sources.YahooSource
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

    @Provides
    @Singleton
    fun finnhub(http: Http) = FinnhubSource(http, BuildConfig.FINNHUB_API_KEY)

    @Provides
    @Singleton
    fun tefas(http: Http) = TefasSource(http)

    /**
     * Kaynak seçimi (doküman 9.1, canlı denemelerle doğrulandı):
     * - ABD: Finnhub, yedek Twelve Data
     * - BIST: Yahoo (resmî ücretsiz kaynak yok; yedek elle fiyat girişidir)
     * - Fon: TEFAS
     * - Gram altın/gümüş: Truncgil, yedek Yahoo ons vadelisi × kur ÷ 31,1035
     * - USD/TRY: Truncgil (gün içi), yedek TCMB günlük kur
     * Yeni bir kaynak eklemek yalnızca burada yeni bir adaptör bağlamayı gerektirir.
     */
    @Provides
    @Singleton
    fun kaynakYonlendirici(clock: Clock, http: Http, finnhub: FinnhubSource, tefas: TefasSource): SourceRouter {
        val yahoo = YahooSource(http)
        val truncgil = TruncgilSource(http)
        return SourceRouter(
            routes = mapOf(
                RouteKey.US to Route(finnhub, TwelveDataSource(http, BuildConfig.TWELVEDATA_API_KEY)),
                RouteKey.BIST to Route(yahoo),
                RouteKey.FUND to Route(tefas),
                RouteKey.COMMODITY to Route(truncgil, OnsEmtiaSource(yahoo)),
                RouteKey.FX to Route(truncgil, TcmbGunlukSource(http)),
            ),
            budget = CallBudget(CallBudget.DEFAULT_DAILY_CAPS, clock, UygulamaZamanDilimi),
            health = SourceHealth(),
            hataGunlugu = { android.util.Log.w("Portfoy", it) },
            clock = clock,
        )
    }

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
    fun portfoyDeposu(assetDao: AssetDao, transactionDao: TransactionDao, quoteDao: PriceQuoteDao, clock: Clock) =
        PortfolioRepository(assetDao, transactionDao, quoteDao, clock)
}

package com.portfoy.di

import com.portfoy.data.db.AssetDao
import com.portfoy.data.db.PriceQuoteDao
import com.portfoy.data.db.TransactionDao
import com.portfoy.data.repository.PortfolioRepository
import com.portfoy.data.repository.PriceRepository
import com.portfoy.model.SourceId
import com.portfoy.network.CallBudget
import com.portfoy.network.FakePriceSource
import com.portfoy.network.Route
import com.portfoy.network.RouteKey
import com.portfoy.network.SourceHealth
import com.portfoy.network.SourceRouter
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

    /**
     * Wireframe fazında tüm rotalar sahte kaynağa bağlıdır. Gerçek kaynaklar (Finnhub, Yahoo, TEFAS...)
     * M5'te yalnızca burada değiştirilir; ekranlar ve depolar etkilenmez.
     */
    @Provides
    @Singleton
    fun kaynakYonlendirici(clock: Clock): SourceRouter {
        val sahte = { id: SourceId -> FakePriceSource(id, clock) }
        return SourceRouter(
            routes = mapOf(
                RouteKey.US to Route(sahte(SourceId.FINNHUB)),
                RouteKey.BIST to Route(sahte(SourceId.YAHOO)),
                RouteKey.FUND to Route(sahte(SourceId.TEFAS)),
                RouteKey.COMMODITY to Route(sahte(SourceId.TRUNCGIL)),
                RouteKey.FX to Route(sahte(SourceId.TCMB_HOURLY)),
            ),
            budget = CallBudget(CallBudget.DEFAULT_DAILY_CAPS, clock, UygulamaZamanDilimi),
            health = SourceHealth(),
            clock = clock,
        )
    }

    @Provides
    @Singleton
    fun fiyatDeposu(router: SourceRouter, quoteDao: PriceQuoteDao, clock: Clock) =
        PriceRepository(router, quoteDao, clock)

    @Provides
    @Singleton
    fun portfoyDeposu(assetDao: AssetDao, transactionDao: TransactionDao, quoteDao: PriceQuoteDao, clock: Clock) =
        PortfolioRepository(assetDao, transactionDao, quoteDao, clock)
}

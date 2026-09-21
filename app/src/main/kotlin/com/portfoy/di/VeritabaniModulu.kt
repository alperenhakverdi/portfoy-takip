package com.portfoy.di

import android.content.Context
import androidx.room.Room
import com.portfoy.data.db.AssetDao
import com.portfoy.data.db.PortfolioSnapshotDao
import com.portfoy.data.db.PortfoyDatabase
import com.portfoy.data.db.PriceHistoryDao
import com.portfoy.data.db.PriceQuoteDao
import com.portfoy.data.db.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VeritabaniModulu {

    @Provides
    @Singleton
    fun veritabani(@ApplicationContext context: Context): PortfoyDatabase =
        Room.databaseBuilder(context, PortfoyDatabase::class.java, PortfoyDatabase.NAME).build()

    @Provides
    fun assetDao(db: PortfoyDatabase): AssetDao = db.assetDao()

    @Provides
    fun transactionDao(db: PortfoyDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun priceQuoteDao(db: PortfoyDatabase): PriceQuoteDao = db.priceQuoteDao()

    @Provides
    fun priceHistoryDao(db: PortfoyDatabase): PriceHistoryDao = db.priceHistoryDao()

    @Provides
    fun portfolioSnapshotDao(db: PortfoyDatabase): PortfolioSnapshotDao = db.portfolioSnapshotDao()
}

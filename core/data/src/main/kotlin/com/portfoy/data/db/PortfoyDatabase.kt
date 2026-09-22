package com.portfoy.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Yerel veritabanı. Şema sürümlüdür ve `schemas/` klasörüne aktarılır; yeni kategori ya da alan
 * eklenirken bir migration yazılır ve o şemalara karşı test edilir.
 */
@Database(
    entities = [
        AssetEntity::class,
        TransactionEntity::class,
        PriceQuoteEntity::class,
        PriceHistoryEntity::class,
        PortfolioSnapshotEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PortfoyDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun priceQuoteDao(): PriceQuoteDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun portfolioSnapshotDao(): PortfolioSnapshotDao

    companion object {
        const val NAME = "portfoy.db"
    }
}

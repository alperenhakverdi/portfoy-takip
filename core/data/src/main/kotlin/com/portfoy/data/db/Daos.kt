package com.portfoy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    /** Zaten var olan (kod + kategori) kayıtlar atlanır; katalog yeniden yüklenirken güvenlidir. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(assets: List<AssetEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(asset: AssetEntity): Long

    @Update
    suspend fun update(asset: AssetEntity)

    @Query("SELECT * FROM asset WHERE id = :id")
    suspend fun getById(id: Long): AssetEntity?

    @Query("SELECT * FROM asset WHERE code = :code AND category = :category")
    suspend fun getByCode(code: String, category: com.portfoy.model.Category): AssetEntity?

    @Query("SELECT COUNT(*) FROM asset")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM asset WHERE category = :category")
    suspend fun countByCategory(category: com.portfoy.model.Category): Int

    /**
     * Arama: [query] önceden [com.portfoy.calc.normalizeForSearch] ile normalize edilmiş olmalıdır.
     * Kodu tam eşleşenler, sonra kodla başlayanlar, sonra diğerleri gelir.
     */
    @Query(
        """
        SELECT * FROM asset
        WHERE active = 1 AND searchText LIKE '%' || :query || '%'
        ORDER BY CASE
            WHEN LOWER(code) = :query THEN 0
            WHEN LOWER(code) LIKE :query || '%' THEN 1
            ELSE 2
        END, code
        LIMIT :limit
        """,
    )
    suspend fun search(query: String, limit: Int): List<AssetEntity>

    /** Portföydeki varlıklar: işlem kaydı olanlar. */
    @Query("SELECT * FROM asset WHERE id IN (SELECT DISTINCT assetId FROM transactions)")
    fun observeHeldAssets(): Flow<List<AssetEntity>>
}

@Dao
interface TransactionDao {
    /** Aynı kimlikle eklenirse üzerine yazar; silme geri alınırken kayıt eski kimliğiyle döner. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY tradeDate, id")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE assetId = :assetId ORDER BY tradeDate, id")
    fun observeForAsset(assetId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT MIN(tradeDate) FROM transactions")
    suspend fun oldestTradeDate(): LocalDate?
}

@Dao
interface PriceQuoteDao {
    @Insert
    suspend fun insert(quote: PriceQuoteEntity): Long

    @Query("SELECT * FROM price_quote WHERE assetId = :assetId ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun latestFor(assetId: Long): PriceQuoteEntity?

    /** Her varlığın en yeni fiyatı. */
    @Query(
        """
        SELECT q.* FROM price_quote q
        JOIN (SELECT assetId, MAX(timestamp) AS ts FROM price_quote GROUP BY assetId) m
          ON q.assetId = m.assetId AND q.timestamp = m.ts
        GROUP BY q.assetId
        """,
    )
    fun observeLatest(): Flow<List<PriceQuoteEntity>>

    @Query("DELETE FROM price_quote WHERE assetId = :assetId")
    suspend fun deleteForAsset(assetId: Long)
}

@Dao
interface PriceHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<PriceHistoryEntity>)

    @Query("SELECT * FROM price_history WHERE assetId = :assetId AND date BETWEEN :from AND :to ORDER BY date")
    suspend fun range(assetId: Long, from: LocalDate, to: LocalDate): List<PriceHistoryEntity>

    @Query("SELECT MAX(date) FROM price_history WHERE assetId = :assetId")
    suspend fun lastDate(assetId: Long): LocalDate?

    @Query("SELECT MIN(date) FROM price_history WHERE assetId = :assetId")
    suspend fun firstDate(assetId: Long): LocalDate?

    @Query("DELETE FROM price_history WHERE assetId = :assetId")
    suspend fun deleteForAsset(assetId: Long)

    /** Saklama sınırı: bu tarihten eski kayıtlar silinir. */
    @Query("DELETE FROM price_history WHERE date < :before")
    suspend fun deleteOlderThan(before: LocalDate)
}

@Dao
interface PortfolioSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: PortfolioSnapshotEntity)

    @Query("SELECT * FROM portfolio_snapshot WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun range(from: LocalDate, to: LocalDate): List<PortfolioSnapshotEntity>

    @Query("SELECT * FROM portfolio_snapshot ORDER BY date DESC LIMIT 1")
    suspend fun latest(): PortfolioSnapshotEntity?
}

package com.portfoy.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.portfoy.model.Category
import com.portfoy.model.TransactionType
import com.portfoy.model.UnitType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** [PriceHistoryEntity] içinde USD/TRY kur serisini tutan sanal varlık kimliği (tüm varlıklarca paylaşılır). */
const val FX_USDTRY_ID = -1L

/** Arama listesi ve portföy varlıkları. Bir varlık portföyde sayılır ancak işlem kaydı varsa. */
@Entity(
    tableName = "asset",
    indices = [Index(value = ["code", "category"], unique = true), Index("searchText")],
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val category: Category,
    val currency: String,
    val unitType: UnitType,
    val exchange: String? = null,
    val active: Boolean = true,
    /** Fon türü (YAT, EMK, BYF...). TEFAS geçmiş sorgusu için gerekir. */
    val fundKind: String? = null,
    /** Kod ve ad, Türkçe karakterlerden arındırılmış küçük harf. Çevrimdışı arama bunun üzerinde çalışır. */
    val searchText: String,
)

/** Tek bir alım kaydı. Parasal alanlar metin olarak saklanır; kayan nokta hatası oluşmaz. */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("assetId"), Index("tradeDate")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetId: Long,
    val type: TransactionType,
    val quantity: BigDecimal,
    val unitPriceTl: BigDecimal,
    val commissionTl: BigDecimal,
    val tradeDate: LocalDate,
    val note: String?,
    val createdAt: Instant,
)

/** Çekilen (ya da elle girilen) anlık fiyatlar. Her kayıt yeni satırdır; en yenisi geçerlidir. */
@Entity(
    tableName = "price_quote",
    indices = [Index(value = ["assetId", "timestamp"])],
)
data class PriceQuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetId: Long,
    val price: BigDecimal,
    val currency: String,
    /** Çevrim anındaki kurla hesaplanmış TL karşılığı; geçmiş kur değerleri kaybolmaz. */
    val priceTl: BigDecimal,
    val timestamp: Instant,
    /** Fiyatın geldiği kaynak (FINNHUB, YAHOO, TEFAS, MANUEL...). */
    val source: String,
    /** O günkü değişim yüzdesi; kaynak vermiyorsa `null` (v2'de eklendi, bkz. [com.portfoy.data.db.MIGRATION_1_2]). */
    val changePercent: BigDecimal? = null,
)

/** Günlük kapanış serisi. [assetId] gerçek bir varlık ya da [FX_USDTRY_ID] olabilir. */
@Entity(
    tableName = "price_history",
    primaryKeys = ["assetId", "date"],
)
data class PriceHistoryEntity(
    val assetId: Long,
    val date: LocalDate,
    val close: BigDecimal,
    val closeTl: BigDecimal,
    val source: String,
)

/** Günde bir kayıt: portföyün toplam değeri, maliyeti ve kategori dağılımı. */
@Entity(tableName = "portfolio_snapshot")
data class PortfolioSnapshotEntity(
    @PrimaryKey val date: LocalDate,
    val totalValueTl: BigDecimal,
    val totalCostTl: BigDecimal,
    /** Kategori → TL değeri, JSON metni. */
    val categoryDistribution: String,
)

package com.portfoy.data.repository

import com.portfoy.calc.Holding
import com.portfoy.calc.normalizeForSearch
import com.portfoy.data.db.AssetDao
import com.portfoy.data.db.AssetEntity
import com.portfoy.data.db.PriceQuoteDao
import com.portfoy.data.db.TransactionDao
import com.portfoy.data.db.toEntity
import com.portfoy.data.db.toModel
import com.portfoy.model.Asset
import com.portfoy.model.Category
import com.portfoy.model.Transaction
import com.portfoy.model.TransactionType
import com.portfoy.model.UnitType
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Arama sonucu: varlık ve önbellekteki son bilinen TL fiyatı (arama hiç ağ çağrısı yapmaz). */
data class SearchHit(val asset: Asset, val lastPriceTl: BigDecimal?, val priceSource: String?)

/** Fiyat bilgisiyle birlikte portföy verisi. [lastUpdate] tüm fiyatların en yenisidir. */
data class PortfolioData(
    val holdings: List<Holding>,
    val lastUpdate: Instant?,
    /** Elle fiyat girilen varlıkların kimlikleri (ekranda etiketlenir). */
    val manualPriceAssetIds: Set<Long>,
)

class PortfolioRepository(
    private val assetDao: AssetDao,
    private val transactionDao: TransactionDao,
    private val quoteDao: PriceQuoteDao,
    private val clock: Clock,
) {
    /** Portföyde işlem kaydı olan varlıklar, kayıtları ve son bilinen fiyatlarıyla. */
    fun observePortfolio(): Flow<PortfolioData> = combine(
        assetDao.observeHeldAssets(),
        transactionDao.observeAll(),
        quoteDao.observeLatest(),
    ) { assets, transactions, quotes ->
        val quoteByAsset = quotes.associateBy { it.assetId }
        val transactionsByAsset = transactions.groupBy { it.assetId }
        PortfolioData(
            holdings = assets.map { asset ->
                Holding(
                    asset = asset.toModel(),
                    transactions = transactionsByAsset[asset.id].orEmpty().map { it.toModel() },
                    currentPriceTl = quoteByAsset[asset.id]?.priceTl,
                )
            },
            lastUpdate = quotes.filter { it.assetId > 0 }.maxOfOrNull { it.timestamp },
            manualPriceAssetIds = quotes.filter { it.source == "MANUEL" }.map { it.assetId }.toSet(),
        )
    }

    /** Tazelenecek varlıklar: portföydekiler. */
    fun observeHeldAssetEntities(): Flow<List<AssetEntity>> = assetDao.observeHeldAssets()

    suspend fun search(query: String, limit: Int = 30): List<SearchHit> {
        val normalized = normalizeForSearch(query)
        if (normalized.length < com.portfoy.calc.MIN_SEARCH_LENGTH) return emptyList()
        return assetDao.search(normalized, limit).map { entity ->
            val quote = quoteDao.latestFor(entity.id)
            SearchHit(entity.toModel(), quote?.priceTl, quote?.source)
        }
    }

    suspend fun asset(id: Long): Asset? = assetDao.getById(id)?.toModel()

    suspend fun assetByCode(code: String, category: Category): Asset? = assetDao.getByCode(code, category)?.toModel()

    suspend fun lastPriceTl(assetId: Long): BigDecimal? = quoteDao.latestFor(assetId)?.priceTl

    /** Nakit TL kaydı; arama gerektirmeden erişilir. */
    suspend fun cashAsset(): Asset? = assetByCode(com.portfoy.data.db.VarsayilanVarliklar.NAKIT_KODU, Category.NAKIT)

    suspend fun addPurchase(
        assetId: Long,
        quantity: BigDecimal,
        unitPriceTl: BigDecimal,
        commissionTl: BigDecimal,
        date: LocalDate,
        note: String?,
    ): Long = transactionDao.insert(
        Transaction(
            id = 0,
            assetId = assetId,
            type = TransactionType.ALIS,
            quantity = quantity,
            unitPriceTl = unitPriceTl,
            commissionTl = commissionTl,
            tradeDate = date,
            note = note?.takeIf { it.isNotBlank() },
            createdAt = clock.instant(),
        ).toEntity(),
    )

    suspend fun updatePurchase(transaction: Transaction) = transactionDao.update(transaction.toEntity())

    /** Kaydı siler ve geri alma için döndürür. */
    suspend fun deletePurchase(id: Long): Transaction? {
        val existing = transactionDao.getById(id)?.toModel() ?: return null
        transactionDao.delete(id)
        return existing
    }

    /** Silinen kaydı eski kimliğiyle geri getirir (5 saniyelik geri al). */
    suspend fun restorePurchase(transaction: Transaction) {
        transactionDao.insert(transaction.toEntity())
    }

    /** Aramada bulunamayan varlığı kullanıcı kendisi tanımlar; fiyatı elle girilir. */
    suspend fun addManualAsset(
        code: String,
        name: String,
        category: Category,
        priceTl: BigDecimal,
    ): Long {
        val unit = when (category) {
            Category.FON -> UnitType.PAY
            Category.EMTIA -> UnitType.GRAM
            Category.NAKIT -> UnitType.TL
            else -> UnitType.ADET
        }
        val entity = AssetEntity(
            code = code.trim().uppercase(),
            name = name.trim(),
            category = category,
            currency = "TRY",
            unitType = unit,
            searchText = normalizeForSearch("$code $name"),
        )
        val existing = assetDao.getByCode(entity.code, category)
        val id = existing?.id ?: assetDao.insert(entity)
        quoteDao.insert(
            com.portfoy.data.db.PriceQuoteEntity(
                assetId = id,
                price = priceTl,
                currency = "TRY",
                priceTl = priceTl,
                timestamp = clock.instant(),
                source = "MANUEL",
            ),
        )
        return id
    }

    /** Portföydeki en eski işlem tarihi ("Tümü" dönemi ve portföy yaşı için). */
    suspend fun oldestPurchaseDate(): LocalDate? = transactionDao.oldestTradeDate()

    /** Son işlem yapılan varlıklar (arama boşken kısayol). */
    fun observeRecentAssets(limit: Int = 6): Flow<List<Asset>> = combine(
        assetDao.observeHeldAssets(),
        transactionDao.observeAll(),
    ) { assets, transactions ->
        val order = transactions.sortedByDescending { it.createdAt }.map { it.assetId }.distinct()
        val byId = assets.associateBy { it.id }
        order.mapNotNull { byId[it] }.take(limit).map { it.toModel() }
    }
}

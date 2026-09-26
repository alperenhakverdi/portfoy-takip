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
data class SearchHit(
    val asset: Asset,
    val lastPriceTl: BigDecimal?,
    val priceSource: String?,
    /** O günkü değişim yüzdesi; kaynak vermediyse ya da hiç fiyat çekilmediyse `null`. */
    val dailyChangePercent: BigDecimal? = null,
)

/** Fiyat bilgisiyle birlikte portföy verisi. [lastUpdate] tüm fiyatların en yenisidir. */
data class PortfolioData(
    val holdings: List<Holding>,
    val lastUpdate: Instant?,
    /** Elle fiyat girilen varlıkların kimlikleri (ekranda etiketlenir). */
    val manualPriceAssetIds: Set<Long>,
    /** Elle girilen fiyatların girilme zamanı; 7 günden eskiyse "güncel değil" uyarısı çıkar. */
    val manualPriceTimes: Map<Long, Instant> = emptyMap(),
    /** USD/TRY kurunun son güncellenme zamanı; ABD varlıklarının TL değeri bu kurla hesaplanır. */
    val fxTime: Instant? = null,
)

class PortfolioRepository(
    private val assetDao: AssetDao,
    private val transactionDao: TransactionDao,
    private val quoteDao: PriceQuoteDao,
    private val clock: Clock,
    /** Alış tarihindeki kur için; verilmezse son canlı kur kullanılır. */
    private val historyDao: com.portfoy.data.db.PriceHistoryDao? = null,
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
            manualPriceTimes = quotes.filter { it.source == "MANUEL" }.associate { it.assetId to it.timestamp },
            fxTime = quotes.firstOrNull { it.assetId == com.portfoy.data.db.FX_USDTRY_ID }?.timestamp,
        )
    }

    /** Tazelenecek varlıklar: portföydekiler. */
    fun observeHeldAssetEntities(): Flow<List<AssetEntity>> = assetDao.observeHeldAssets()

    /** [category] verilirse arama yalnız o kategoride yapılır (Ekle sekmesi kategoriden başlar). */
    suspend fun search(query: String, limit: Int = 30, category: Category? = null): List<SearchHit> {
        val normalized = normalizeForSearch(query)
        if (normalized.length < com.portfoy.calc.MIN_SEARCH_LENGTH) return emptyList()
        val sonuclar =
            if (category == null) assetDao.search(normalized, limit)
            else assetDao.searchInCategory(normalized, category, limit)
        return sonuclar.map { it.toHit() }
    }

    /**
     * Kategorinin varlıkları, [limit] taneye kadar. Kategoride daha fazlası varsa liste gösterilmez;
     * [kategoriSayisi] ile karşılaştırılıp aramaya yönlendirilir (ABD 27 bin, fon 2 bin kayıt).
     */
    suspend fun kategoriListesi(category: Category, limit: Int = 600): List<SearchHit> =
        assetDao.byCategory(category, limit).map { it.toHit() }

    suspend fun kategoriSayisi(category: Category): Int = assetDao.countByCategory(category)

    private suspend fun AssetEntity.toHit(): SearchHit {
        val quote = quoteDao.latestFor(id)
        return SearchHit(toModel(), quote?.priceTl, quote?.source, quote?.changePercent)
    }

    /** [kodlar] sırasıyla, kategorideki karşılıkları bulunursa döner; katalogda yoksa atlanır (öne çıkanlar listesi). */
    suspend fun kategoriKisayollari(category: Category, kodlar: List<String>): List<SearchHit> =
        kodlar.mapNotNull { kod -> assetDao.getByCode(kod, category)?.toHit() }

    suspend fun asset(id: Long): Asset? = assetDao.getById(id)?.toModel()

    suspend fun assetByCode(code: String, category: Category): Asset? = assetDao.getByCode(code, category)?.toModel()

    suspend fun lastPriceTl(assetId: Long): BigDecimal? = quoteDao.latestFor(assetId)?.priceTl

    /**
     * [tarih] gününe ait USD/TRY kuru: saklanan günlük seride o güne kadarki son değer, seri yoksa son canlı kur.
     * ABD fiyatını USD girmek isteyen form, alış tarihindeki kurla çevirmek için bunu kullanır.
     */
    suspend fun usdTryOn(tarih: LocalDate): BigDecimal? =
        historyDao?.onOrBefore(com.portfoy.data.db.FX_USDTRY_ID, tarih)?.close ?: latestUsdTry()

    /** Son bilinen USD/TRY kuru; yoksa `null`. */
    suspend fun latestUsdTry(): BigDecimal? = quoteDao.latestFor(com.portfoy.data.db.FX_USDTRY_ID)?.priceTl

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

    /** M17 — bir varlığın adedini azaltır (satış gibi ama gerçekleşen kâr/zarar hesaplanmaz). */
    suspend fun addReduction(
        assetId: Long,
        quantity: BigDecimal,
        unitPriceTl: BigDecimal,
        date: LocalDate,
        note: String?,
    ): Long = transactionDao.insert(
        Transaction(
            id = 0,
            assetId = assetId,
            type = TransactionType.AZALTMA,
            quantity = quantity,
            unitPriceTl = unitPriceTl,
            commissionTl = BigDecimal.ZERO,
            tradeDate = date,
            note = note?.takeIf { it.isNotBlank() },
            createdAt = clock.instant(),
        ).toEntity(),
    )

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
            Category.DOVIZ -> UnitType.BIRIM
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
}

package com.portfoy.data.repository

import com.portfoy.data.db.PriceHistoryDao
import com.portfoy.data.db.PriceQuoteDao
import com.portfoy.data.db.TransactionDao
import java.time.LocalDate

/**
 * Portföyden çıkan (artık hiç işlemi olmayan) varlıkların biriktirdiği fiyat serisi 30 gün sonra
 * temizlenir (doküman 11.3/3).
 *
 * Kaldırılma anı ayrı bir sütunda saklanmaz: held olmayan bir varlık artık günlük tazelemeye
 * girmediği için serisinin son günü kendiliğinden eskir. Son günü 30 günden eskiyse temizlenir.
 * Varlık tekrar satın alınırsa held listesine geri döner ve temizlik ona hiç dokunmaz; bu yüzden
 * yanlışlıkla silme riski yoktur.
 */
class TerkedilenSeriTemizleyici(
    private val transactionDao: TransactionDao,
    private val historyDao: PriceHistoryDao,
    private val quoteDao: PriceQuoteDao,
) {
    suspend fun temizle(bugun: LocalDate): Int {
        val esik = bugun.minusDays(GRACE_GUN)
        val tutulanlar = transactionDao.heldAssetIds().toSet()
        val terkedilmisler = historyDao.assetIdsStaleBefore(esik).filterNot { it in tutulanlar }
        terkedilmisler.forEach { id ->
            historyDao.deleteForAsset(id)
            quoteDao.deleteForAsset(id)
        }
        return terkedilmisler.size
    }

    private companion object {
        const val GRACE_GUN = 30L
    }
}

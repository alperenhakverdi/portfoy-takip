package com.portfoy.network

import com.portfoy.model.AssetRef
import com.portfoy.model.Candle
import com.portfoy.model.Quote
import com.portfoy.model.SourceId
import java.time.LocalDate

/**
 * Her fiyat kaynağı bu arayüzün arkasında durur. Ekranlar ve repository hangi API'nin kullanıldığını
 * bilmez; yeni bir kaynak eklemek yalnızca yeni bir adaptör yazmayı gerektirir.
 */
interface PriceSource {
    val id: SourceId

    /**
     * Kaynağa atılacak gerçek istek sayısı. Toplu sorguyu desteklemeyen kaynak (Finnhub `/quote`)
     * isteği kendi içinde böler ve burada varlık sayısını döndürür; çağrı bütçesi buna göre düşülür.
     */
    fun requestCost(assets: List<AssetRef>): Int = 1

    /** Çağıran taraf için toplu: verilen varlıkların anlık fiyatlarını döndürür. */
    suspend fun getQuotes(assets: List<AssetRef>): Result<List<Quote>>

    /** [from]..[to] aralığında günlük kapanışlar. Desteklemeyen kaynak [UnsupportedOperationException] döndürür. */
    suspend fun getHistory(asset: AssetRef, from: LocalDate, to: LocalDate): Result<List<Candle>>
}

/** Günlük ya da tur çağrı bütçesi yetmediği için istek atılmadı. Kullanıcıya hata olarak gösterilmez. */
class BudgetExceededException(message: String) : Exception(message)

/** Birincil ve yedek kaynak denendi, ikisi de başarısız oldu. */
class AllSourcesFailedException(message: String, cause: Throwable?) : Exception(message, cause)

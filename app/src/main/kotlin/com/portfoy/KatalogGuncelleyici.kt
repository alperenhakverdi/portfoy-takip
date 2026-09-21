package com.portfoy

import android.content.Context
import com.portfoy.data.repository.KatalogDeposu
import com.portfoy.network.SymbolCatalogSource
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arama listesini ayda bir canlı kaynaklardan tazeler: ABD (Finnhub) ve fonlar (TEFAS). Yeni halka arzlar ve yeni
 * fonlar böylece listeye girer. Bu çağrılar fiyat çağrı bütçesinin dışındadır.
 *
 * Başarısız olursa (internet yok, kaynak bozuk) zaman damgası güncellenmez ve bir sonraki açılışta yeniden denenir;
 * gömülü liste zaten aramayı çalışır tutar. BIST için ücretsiz toplu liste olmadığından bu listeyi elle güncelleriz.
 */
@Singleton
class KatalogGuncelleyici @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext context: Context,
    private val katalog: KatalogDeposu,
    private val kaynaklar: KatalogKaynaklari,
    private val clock: Clock,
) {
    private val tercihler = context.getSharedPreferences("katalog", Context.MODE_PRIVATE)

    suspend fun gerekirseGuncelle() {
        val son = tercihler.getLong(SON_GUNCELLEME, 0L)
        val gecen = Duration.between(java.time.Instant.ofEpochMilli(son), clock.instant())
        if (son != 0L && gecen < ARALIK) return

        var basarili = true
        for (kaynak in kaynaklar.liste) {
            kaynak.listSymbols()
                .onSuccess { katalog.guncelle(it) }
                .onFailure { basarili = false }
        }
        if (basarili) tercihler.edit().putLong(SON_GUNCELLEME, clock.millis()).apply()
    }

    private companion object {
        const val SON_GUNCELLEME = "son_guncelleme"
        val ARALIK: Duration = Duration.ofDays(30)
    }
}

/** Tazelemeye giren katalog kaynakları. */
class KatalogKaynaklari(val liste: List<SymbolCatalogSource>)

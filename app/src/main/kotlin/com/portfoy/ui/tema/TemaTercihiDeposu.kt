package com.portfoy.ui.tema

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * M18 — tema tercihini kalıcı tutar (`SharedPreferences`, `KatalogGuncelleyici`'deki desenin aynısı).
 * Reaktif: değişiklik anında hem bu ekranı hem [PortfoyTemasi]'nı günceller, uygulama yeniden
 * başlatılmaz.
 */
@Singleton
class TemaTercihiDeposu @Inject constructor(@ApplicationContext context: Context) {
    private val tercihler = context.getSharedPreferences("tema", Context.MODE_PRIVATE)

    private val _tercih = MutableStateFlow(oku())
    val tercih: StateFlow<TemaTercihi> = _tercih.asStateFlow()

    fun ayarla(yeni: TemaTercihi) {
        tercihler.edit().putString(ANAHTAR, yeni.name).apply()
        _tercih.value = yeni
    }

    private fun oku(): TemaTercihi {
        val kayitli = tercihler.getString(ANAHTAR, null) ?: return TemaTercihi.SISTEM
        return runCatching { TemaTercihi.valueOf(kayitli) }.getOrDefault(TemaTercihi.SISTEM)
    }

    private companion object {
        const val ANAHTAR = "tercih"
    }
}

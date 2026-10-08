package com.portfoy.ui.para

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * M19 — para birimi görüntüleme tercihini kalıcı tutar ([com.portfoy.ui.tema.TemaTercihiDeposu] ile
 * birebir aynı desen: `SharedPreferences`, reaktif `StateFlow`). Varsayılan TL.
 */
@Singleton
class ParaBirimiTercihiDeposu @Inject constructor(@ApplicationContext context: Context) {
    private val tercihler = context.getSharedPreferences("para_birimi", Context.MODE_PRIVATE)

    private val _tercih = MutableStateFlow(oku())
    val tercih: StateFlow<ParaBirimiTercihi> = _tercih.asStateFlow()

    fun ayarla(yeni: ParaBirimiTercihi) {
        tercihler.edit().putString(ANAHTAR, yeni.name).apply()
        _tercih.value = yeni
    }

    private fun oku(): ParaBirimiTercihi {
        val kayitli = tercihler.getString(ANAHTAR, null) ?: return ParaBirimiTercihi.TL
        return runCatching { ParaBirimiTercihi.valueOf(kayitli) }.getOrDefault(ParaBirimiTercihi.TL)
    }

    private companion object {
        const val ANAHTAR = "tercih"
    }
}

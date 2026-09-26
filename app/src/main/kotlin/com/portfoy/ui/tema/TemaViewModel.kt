package com.portfoy.ui.tema

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** M18 — hem [PortfoyTemasi] (okur) hem Portföy ekranındaki tercih diyaloğu (yazar) bunu kullanır. */
@HiltViewModel
class TemaViewModel @Inject constructor(private val depo: TemaTercihiDeposu) : ViewModel() {
    val tercih: StateFlow<TemaTercihi> = depo.tercih

    fun ayarla(yeni: TemaTercihi) = depo.ayarla(yeni)
}

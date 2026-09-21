package com.portfoy

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.portfoy.arkaplan.IsPlanlayici
import com.portfoy.data.db.AssetDao
import com.portfoy.data.db.VarsayilanVarliklar
import com.portfoy.data.repository.KatalogDeposu
import dagger.hilt.android.HiltAndroidApp
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class PortfoyApp : Application(), Configuration.Provider {

    @Inject lateinit var assetDao: AssetDao
    @Inject lateinit var katalog: KatalogDeposu
    @Inject lateinit var isFabrikasi: HiltWorkerFactory
    @Inject lateinit var saat: Clock

    private val uygulamaKapsami = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Arka plan işleri Hilt ile üretilir (bağımlılıkları enjekte edilebilsin diye). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(isFabrikasi).build()

    override fun onCreate() {
        super.onCreate()
        IsPlanlayici.planla(this, saat)
        uygulamaKapsami.launch {
            // Nakit TL ve gram altın/gümüş her kurulumda bulunur; kayıtlar varsa dokunulmaz.
            VarsayilanVarliklar.ekle(assetDao)
            // Gömülü arama listeleri: internetsiz ilk açılışta da arama çalışır.
            val baslangic = System.currentTimeMillis()
            KatalogYukleyici.yukle(this@PortfoyApp, katalog)
            android.util.Log.i("Portfoy", "Katalog yüklemesi: ${System.currentTimeMillis() - baslangic} ms")
        }
    }
}

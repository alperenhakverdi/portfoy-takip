package com.portfoy

import android.app.Application
import com.portfoy.data.db.AssetDao
import com.portfoy.data.db.VarsayilanVarliklar
import com.portfoy.data.repository.KatalogDeposu
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class PortfoyApp : Application() {

    @Inject lateinit var assetDao: AssetDao
    @Inject lateinit var katalog: KatalogDeposu
    @Inject lateinit var katalogGuncelleyici: KatalogGuncelleyici

    private val uygulamaKapsami = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        uygulamaKapsami.launch {
            // Nakit TL ve gram altın/gümüş her kurulumda bulunur; kayıtlar varsa dokunulmaz.
            VarsayilanVarliklar.ekle(assetDao)
            // Gömülü arama listeleri: internetsiz ilk açılışta da arama çalışır.
            val baslangic = System.currentTimeMillis()
            KatalogYukleyici.yukle(this@PortfoyApp, katalog)
            android.util.Log.i("Portfoy", "Katalog yüklemesi: ${System.currentTimeMillis() - baslangic} ms")
            // Ayda bir canlı kaynaklardan tazelenir; hata olursa sessizce bir sonraki açılışta denenir.
            runCatching { katalogGuncelleyici.gerekirseGuncelle() }
        }
    }
}

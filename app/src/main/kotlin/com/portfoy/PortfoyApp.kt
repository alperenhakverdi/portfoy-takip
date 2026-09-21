package com.portfoy

import android.app.Application
import com.portfoy.data.db.AssetDao
import com.portfoy.data.db.VarsayilanVarliklar
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class PortfoyApp : Application() {

    @Inject lateinit var assetDao: AssetDao

    private val uygulamaKapsami = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Nakit TL ve gram altın/gümüş her kurulumda bulunur; kayıtlar varsa dokunulmaz.
        uygulamaKapsami.launch { VarsayilanVarliklar.ekle(assetDao) }
    }
}

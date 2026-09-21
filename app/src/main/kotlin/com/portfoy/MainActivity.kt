package com.portfoy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.portfoy.ui.Uygulama
import com.portfoy.ui.tema.PortfoyTemasi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tazeleme: TazelemeYoneticisi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PortfoyTemasi {
                Uygulama()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Uygulama öne geldiğinde son çekimin üzerinden 15 dakika geçmişse fiyatlar tazelenir.
        tazeleme.onForeground()
    }
}

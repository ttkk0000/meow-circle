package com.ttkk0000.meowcircle.kmpapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.kmpapp.theme.MeowStitchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sdk = MeowCircleSdk(BuildConfig.API_BASE_URL)
        setContent {
            MeowStitchTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MeowApp(sdk = sdk)
                }
            }
        }
    }
}

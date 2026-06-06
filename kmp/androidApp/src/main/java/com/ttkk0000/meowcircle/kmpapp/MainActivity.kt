package com.ttkk0000.meowcircle.kmpapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ttkk0000.meowcircle.MeowCircleSdk

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sdk = MeowCircleSdk(BuildConfig.API_BASE_URL)
        setContent {
            MeowApp(sdk = sdk)
        }
    }
}

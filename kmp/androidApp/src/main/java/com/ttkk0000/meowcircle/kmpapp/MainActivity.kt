package com.ttkk0000.meowcircle.kmpapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ttkk0000.meowcircle.MeowCircleSdk

class MainActivity : ComponentActivity() {
    private val sdk by lazy { MeowCircleSdk(BuildConfig.API_BASE_URL) }
    private val appViewModel by viewModels<MeowAppViewModel> {
        MeowAppViewModel.Factory(sdk)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeowApp(
                sdk = sdk,
                viewModel = appViewModel,
            )
        }
    }
}

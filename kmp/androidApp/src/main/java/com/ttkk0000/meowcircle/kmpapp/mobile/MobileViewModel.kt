package com.ttkk0000.meowcircle.kmpapp.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ttkk0000.meowcircle.client.MobileApi
import com.ttkk0000.meowcircle.client.MobileStore

class MobileViewModel(api: MobileApi) : ViewModel() {
    val store = MobileStore(api, ownsApi = false)
    override fun onCleared() { store.close() }
    class Factory(private val api: MobileApi) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MobileViewModel::class.java))
            return MobileViewModel(api) as T
        }
    }
}

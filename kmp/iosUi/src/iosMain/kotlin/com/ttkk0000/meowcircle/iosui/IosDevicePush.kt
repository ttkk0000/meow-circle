package com.ttkk0000.meowcircle.iosui

interface IosPushController {
    fun setEnabled(enabled: Boolean)
    fun setBadge(count: Int)
}
object IosPushPlatform { var controller: IosPushController? = null }

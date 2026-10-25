package com.ttkk0000.meowcircle.kmpapp.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import com.ttkk0000.meowcircle.client.ProviderCredential
import com.ttkk0000.meowcircle.kmpapp.BuildConfig
import com.ttkk0000.meowcircle.kmpapp.mobile.AndroidProviderAuth

/** 微信约定的导出回调Activity；不解析任意深链为应用登录结果。 */
class WXEntryActivity : Activity(), IWXAPIEventHandler {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); handle(intent) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handle(intent) }
    private fun handle(intent: Intent) {
        if(!AndroidProviderAuth.has("wechat")) { finish(); return }
        if(!WXAPIFactory.createWXAPI(this,BuildConfig.WECHAT_APP_ID,true).handleIntent(intent,this)) finish()
    }
    override fun onReq(request: BaseReq) { finish() }
    override fun onResp(response: BaseResp) {
        val auth = response as? SendAuth.Resp
        if(auth != null && !auth.state.isNullOrBlank()) when(auth.errCode) {
            BaseResp.ErrCode.ERR_OK -> if(!auth.code.isNullOrBlank()) AndroidProviderAuth.complete("wechat",auth.state,ProviderCredential(code = auth.code)) else AndroidProviderAuth.fail("wechat",auth.state)
            BaseResp.ErrCode.ERR_USER_CANCEL -> AndroidProviderAuth.complete("wechat",auth.state,null)
            else -> AndroidProviderAuth.fail("wechat",auth.state)
        }
        finish()
    }
}

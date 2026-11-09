package com.ttkk0000.meowcircle.client

data class AuthReturnTask(val title: String, val detail: String)

/** Describes the actual suspended page; it never creates a draft or resumes a command. */
fun authReturnTask(state: MobileState, route: FeatureRoute?, applying: Boolean): AuthReturnTask {
    if (route != null) return when (route.page) {
        FeaturePage.EntryEdit -> AuthReturnTask("继续圈子草稿", "保留宠物、文字和受众；检查后再发布")
        FeaturePage.CaseEdit -> AuthReturnTask("继续走失求助", "状态和公开同意分别确认")
        FeaturePage.Nearby -> AuthReturnTask("继续附近提醒", "登录不会自动开启订阅")
        FeaturePage.Security -> AuthReturnTask("返回账号设置", "继续核对当前账号与登录方式")
        else -> AuthReturnTask("返回${route.page.title}", "返回原页面，按当前权限重新读取")
    }
    return when (state.destination) {
        is MobileDestination.Pet -> if (applying) AuthReturnTask("继续领养申请", "检查申请内容与共享范围，再确认提交")
            else AuthReturnTask("返回宠物详情", "保留当前页面，不自动提交申请或评论")
        is MobileDestination.Chat -> AuthReturnTask(if (state.destination.group) "返回群聊" else "继续私信", "回原会话，不自动发送")
        is MobileDestination.Case -> AuthReturnTask("返回走失求助", "按当前权限读取；状态与公开同意分别确认")
        is MobileDestination.Application, MobileDestination.Applications -> AuthReturnTask("返回领养申请", "按当前权限读取，不自动确认安排")
        is MobileDestination.Entry -> AuthReturnTask("返回社区动态", "可检查原输入，不自动发送评论")
        else -> when (state.tab) {
            MobileTab.Publish -> AuthReturnTask("继续圈子草稿", "保留宠物、文字和受众；检查后再发布")
            MobileTab.Messages -> AuthReturnTask("返回消息", "登录不会自动发送消息或标记全部已读")
            MobileTab.Adoption -> AuthReturnTask("返回领养介绍", "按当前权限读取，不自动提交申请")
            MobileTab.Profile -> AuthReturnTask("返回我的页面", "查看当前账号资料与任务")
            MobileTab.Community -> AuthReturnTask("返回社区", "继续浏览刚才的内容")
        }
    }
}

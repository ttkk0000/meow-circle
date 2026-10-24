package com.ttkk0000.meowcircle.kmpapp.mobile

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ttkk0000.meowcircle.client.*

@Composable internal fun PublicationsContent(store: MobileStore,s: FeatureState,route: FeatureRoute) {
    val f=store.features
    val kind=route.kind.ifBlank {"pets"}
    MndChoiceGroup("发布类型",kind,listOf("pets" to "宠物介绍","entries" to "日常图文","cases" to "走失求助","offers" to "有偿信息"),enabled=!s.busy,onValueChange=f::selectPublications)
    when(kind) {
        "entries" -> {
            s.publishedEntries.items.forEach {row -> FeatureSection(row.data.title.ifBlank {"日常图文"}) {
                Text(row.data.text)
                NavigationRow("查看正文与评论") {f.closeRoute();store.open(MobileDestination.Entry(row.id))}
                NavigationRow("修改图文或可见范围","按当前版本核对") {f.open(FeatureRoute(FeaturePage.EntryEdit,row.id))}
            }}
            PageFooter(s.publishedEntries,{f.reload()},{f.reload(true)},"还没有发布图文",enabled=!s.loading)
        }
        "cases" -> {
            s.publishedCases.items.forEach {row -> FeatureSection(row.data.title) {
                Text("${row.data.city} · ${stateLabel(row.state)}")
                NavigationRow("查看公开求助") {f.closeRoute();store.open(MobileDestination.Case(row.id))}
                NavigationRow("管理线索与找回状态") {f.open(FeatureRoute(FeaturePage.CaseWork,row.id))}
                NavigationRow("修改求助资料") {f.open(FeatureRoute(FeaturePage.CaseEdit,row.id))}
            }}
            PageFooter(s.publishedCases,{f.reload()},{f.reload(true)},"还没有发布求助",enabled=!s.loading)
        }
        "offers" -> {
            s.publishedOffers.items.forEach {row -> NavigationRow("${row.data.city} · ${row.data.nature}",stateLabel(row.state)) {f.open(FeatureRoute(FeaturePage.Offer,row.id))}}
            PageFooter(s.publishedOffers,{f.reload()},{f.reload(true)},"还没有发布有偿信息",enabled=!s.loading)
        }
        else -> {
            s.publishedPets.items.forEach {row ->
                PetCard(store,store.state.value,row) {f.closeRoute();store.open(MobileDestination.Pet(row.id))}
                NavigationRow("维护${row.data.name}的介绍与评论","补充图文、照护协作、持续答疑") {f.closeRoute();store.open(MobileDestination.Pet(row.id))}
                NavigationRow("修改资料或暂停／结束发布","关键变更会生成新版本；不删除必要处理记录") {f.open(FeatureRoute(FeaturePage.PetEdit,row.id))}
                NavigationRow("处理申请","沟通、接受或说明拒绝原因") {f.closeRoute();store.open(MobileDestination.Applications)}
                NavigationRow("管理走失求助","更新线索与找回状态") {f.selectPublications("cases")}
            }
            PageFooter(s.publishedPets,{f.reload()},{f.reload(true)},"还没有宠物介绍",enabled=!s.loading)
        }
    }
    TaskNotice("旧安排不会悄悄继续","地点、宠物、接待人和费用变更后，双方需重新确认。")
    NavigationRow("本机草稿与待核对操作") {f.open(FeatureRoute(FeaturePage.Recovery))}
    FeaturePrimary("新建发布") {f.closeRoute();store.select(MobileTab.Publish)}
}

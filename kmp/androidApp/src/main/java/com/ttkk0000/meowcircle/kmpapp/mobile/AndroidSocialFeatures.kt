package com.ttkk0000.meowcircle.kmpapp.mobile

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.ttkk0000.meowcircle.client.*

@Composable internal fun SocialFeature(store: MobileStore, s: FeatureState, route: FeatureRoute) {
    val f = store.features; val actor = store.state.value.user?.id
    when (route.page) {
        FeaturePage.EntryEdit -> s.entry?.let { record ->
            val old = record.data
            FormField(f,s,"title","标题",old.title)
            FormField(f,s,"text","正文",old.text,6)
            ChoiceField(f,s,"space","发布空间",listOf("community" to "公开社区","circle" to "宠友圈","diary" to "宠物日记"),old.space)
            val space = f.field("space",old.space)
            if(space != "community") AudienceFields(f,s,if(old.audience.mode == "public" && space == "circle") Audience("friends") else old.audience,publicAllowed = space != "circle")
            if(space == "diary") FormField(f,s,"petId","所属宠物编号",old.petId.takeIf { it > 0 }?.toString().orEmpty(),numeric = true)
            ChoiceField(f,s,"origin","当前内容来源",originChoices,old.contentOrigin.ifBlank { "unknown" })
            FeaturePhotos(store,s,record.mediaIds)
            FeaturePrimary("核对并保存动态",!s.busy && !s.loading) {
                val audience = if(space == "community") Audience() else formAudience(f,if(old.audience.mode == "public" && space == "circle") Audience("friends") else old.audience)
                val petId = f.field("petId",old.petId.toString()).toLongOrNull() ?: 0
                if(audience == null || space == "diary" && petId <= 0 || audience.mode == "public" && space == "circle") f.notify("请核对发布空间、受众及宠物归属") else {
                    val input = EntryInput(old.copy(title = f.field("title",old.title),text = f.field("text",old.text),space = space,audience = audience,petId = petId,contentOrigin = ""),f.version(record.version),f.mediaIds(record.mediaIds),newRequestKey(),PublicationDeclaration(origin = f.field("origin",old.contentOrigin.ifBlank { "unknown" })))
                    if(!f.photosReady()) f.notify("请先完成照片上传") else f.runCommand {
                        val declaration = if(audience.mode == "private") PublicationDeclaration() else f.api.previewPublication(input).let { PublicationDeclaration(it.manifestHash,it.origin,true) }
                        f.reviewPrepared("保存当前动态","${input.entry.title}\n${input.entry.text}\n${audienceLabel(audience.mode)} · ${originLabel(declaration.origin)}\n确认当前内容、来源与照片。") { f.api.editEntry(record.id,input.copy(declaration = declaration)); store.reloadDetail(); store.refreshHome(); f.discardAndReload() }
                    }
                }
            }
        }
        FeaturePage.Friends -> {
            FormField(f,s,"user","对方用户编号",numeric = true)
            FormField(f,s,"reason","好友申请说明")
            FeaturePrimary("发送好友申请", !s.busy && actor != null) {
                val user = f.field("user").toLongOrNull()
                if (user == null || user <= 0 || user == actor) f.notify("请填写其他用户的有效编号") else {
                    val input = FriendInput(user,newRequestKey(),reason = f.field("reason"))
                    f.confirm("发送好友申请","发给用户 #$user\n${input.reason}") { f.api.friend("request",input); f.reload() }
                }
            }
            s.friends.items.forEach { row ->
                val peer = if (row.data.requesterId == actor) row.data.recipientId else row.data.requesterId
                FeatureSection("宠友 #$peer · ${stateLabel(row.state)}") {
                    Text(row.data.reason)
                    FeatureButton("查看资料") { f.open(FeatureRoute(FeaturePage.Member,peer)) }
                    val actions = buildList {
                        if (row.state == "pending") { if (row.data.recipientId == actor) { add("accept" to "接受好友申请"); add("decline" to "拒绝申请") } else add("withdraw" to "撤回申请") }
                        if (row.state == "accepted") add("remove" to "解除好友")
                        if (row.state != "blocked") add("block" to "拉黑") else if (row.data.blockedBy == actor) add("unblock" to "解除拉黑")
                        if (row.state in listOf("removed","declined","withdrawn")) add("request" to "重新申请")
                    }
                    actions.forEach { (action,label) -> FeatureButton(label,!s.busy) {
                        val input = FriendInput(peer,newRequestKey(),row.version)
                        f.confirm(label,"用户 #$peer · 当前版本 ${row.version}\n关系变化后将重新校验私密内容访问。") { f.api.friend(action,input); store.refreshHome(); f.reload() }
                    } }
                }
            }
            MoreFeature(s.friends.next,s.loading) { f.reload(true) }
            if (s.friends.loaded && s.friends.items.isEmpty()) Text("还没有好友关系。申请须由对方接受。")
        }
        FeaturePage.Member -> s.member?.let { member ->
            FeatureSection(member.nickname.ifBlank { member.username }) { Text(member.bio); Text("用户 #${member.id}") }
            FeatureButton("申请成为好友", actor != null && member.id != actor && !s.busy) {
                val relationship = s.friends.items.firstOrNull { it.data.requesterId == member.id || it.data.recipientId == member.id }
                val input = FriendInput(member.id,newRequestKey(),relationship?.version ?: 0)
                f.confirm("申请成为好友",member.nickname) { f.api.friend("request",input); f.notify("好友申请已发送，等待对方接受") }
            }
            FeatureButton("发起私信",actor != null && member.id != actor && !s.busy) { f.runCommand { val chat = f.api.startConversation(member.id); f.finishRoute(); store.open(MobileDestination.Chat(chat.id)) } }
            FeatureButton("举报该资料") { f.open(FeatureRoute(FeaturePage.Review)); f.edit("reason","用户 #${member.id}（${member.nickname}）的资料：\n") }
        }
        FeaturePage.ProfileEdit -> s.profile?.let { profile ->
            FormField(f,s,"nickname","昵称",profile.user.nickname)
            FormField(f,s,"bio","个人介绍",profile.user.bio,3)
            ChoiceField(f,s,"background","个人主页背景",listOf("picnic" to "Picnic","desk" to "Desk","arcade" to "Arcade","garden" to "Garden"),profile.background)
            FeaturePhotos(store,s,maxPhotos = 1)
            CheckLine("移除现有头像",f.field("removeAvatar","false").toBoolean(),{ f.edit("removeAvatar",it.toString()) },!s.busy)
            FeaturePrimary("保存资料",!s.busy && !s.loading) {
                val nickname = f.field("nickname",profile.user.nickname).trim(); val bio = f.field("bio",profile.user.bio)
                if (nickname.isBlank() || nickname.codePointCount(0,nickname.length) > 40 || bio.codePointCount(0,bio.length) > 500) f.notify("昵称需为 1–40 字，介绍最多 500 字") else {
                    val input = ProfileEdit(f.version(profile.version),nickname,bio,profile.theme,f.field("background",profile.background),if(f.field("removeAvatar","false").toBoolean()) 0 else f.mediaIds().firstOrNull())
                    f.confirm("保存资料","$nickname\n$bio") { f.api.editProfile(input); store.refreshHome(); f.discardAndReload() }
                }
            }
        }
        FeaturePage.PetEdit -> {
            if (route.id > 0 && s.pet == null) return
            val record = s.pet; val pet = record?.data ?: PetBody("","cat",audience = Audience("private"))
            FormField(f,s,"name","宠物名字",pet.name)
            ChoiceField(f,s,"species","物种",listOf("cat" to "猫咪","dog" to "狗狗"),pet.species)
            FormField(f,s,"breed","品种（未知可留空）",pet.breed)
            FormField(f,s,"birthday","生日（选填，YYYY-MM-DD）",pet.birthday)
            ChoiceField(f,s,"sex","性别",listOf("" to "未知","male" to "公","female" to "母"),pet.sex)
            FormField(f,s,"bio","宠物介绍",pet.bio,4)
            FormField(f,s,"health","私密健康记录",pet.health,3)
            AudienceFields(f,s,pet.audience)
            CheckLine("开放领养介绍（需成年人及有效处置资格）",f.field("adoption",pet.adoptionAvailable.toString()).toBoolean(),{ f.edit("adoption",it.toString()) },!s.busy)
            FeaturePhotos(store,s,record?.mediaIds.orEmpty(),private = f.field("audience",pet.audience.mode) == "private")
            FeaturePrimary("保存宠物档案",!s.busy && !s.loading) {
                val audience = formAudience(f,pet.audience)
                val birthday = f.field("birthday",pet.birthday)
                if (f.field("name",pet.name).isBlank() || audience == null || birthday.isNotBlank() && runCatching { java.time.LocalDate.parse(birthday) }.isFailure) f.notify("请填写名字、有效生日与可见范围") else {
                    val input = PetInput(pet.copy(name = f.field("name",pet.name).trim(),species = f.field("species",pet.species),breed = f.field("breed",pet.breed),birthday = birthday,
                        sex = f.field("sex",pet.sex),bio = f.field("bio",pet.bio),health = f.field("health",pet.health),audience = audience,adoptionAvailable = f.field("adoption",pet.adoptionAvailable.toString()).toBoolean()),f.version(record?.version ?: 0),f.mediaIds(record?.mediaIds.orEmpty()),newRequestKey())
                    f.confirm("保存宠物档案","${input.pet.name}\n${audienceLabel(audience.mode)}\n${if (input.pet.adoptionAvailable) "开放领养介绍" else "普通档案"}") {
                        val result = if (record == null) f.api.savePet(input) else f.api.editPet(record.id,input)
                        store.refreshHome(); f.completeTo(FeatureRoute(FeaturePage.PetEdit,result.id))
                    }
                }
            }
            if (record != null && record.ownerId == actor) FeatureButton("删除宠物档案",!s.busy) {
                f.confirm("删除宠物档案","${pet.name} · 版本 ${record.version}\n提交后公开内容和照片访问将被收起。") { f.api.removeResource("pets",record.id,record.version); f.finishRoute(); store.back(); store.refreshHome() }
            }
        }
        FeaturePage.Collaborators -> {
            Text(if (route.kind == "care") "照护协作者仅访问授权近况，不获得公开档案编辑和领养决定权限。" else "介绍协作者仅维护介绍与照片；领养决定仍由发布人处理。")
            FormField(f,s,"invitee","邀请的用户编号",numeric = true)
            FeaturePrimary("发出协作邀请",!s.busy && actor != null) {
                val user = f.field("invitee").toLongOrNull()
                if (user == null || user <= 0 || user == actor) f.notify("请填写其他用户编号") else {
                    val input = InviteInput(if (route.kind == "care") "caretaker" else "adoption_editor",route.id,user,newRequestKey())
                    f.confirm("发出协作邀请","邀请用户 #$user，仅授予当前事项中的协作范围。") { f.api.invite(input); f.notify("邀请已发出，可在邀请列表中撤销") }
                }
            }
            FeatureButton("查看／撤销邀请") { f.open(FeatureRoute(FeaturePage.Invitations)) }
            s.pet?.let { pet ->
                FormField(f,s,"bio","领养介绍",pet.data.bio,5)
                FeaturePhotos(store,s,pet.mediaIds)
                FeatureButton("仅更新介绍与照片",!s.busy) {
                    val input = IntroductionEdit(f.version(pet.version),f.field("bio",pet.data.bio),f.mediaIds(pet.mediaIds),newRequestKey())
                    f.confirm("更新领养介绍",input.bio) { f.api.introduction(pet.id,input); f.discardAndReload() }
                }
            }
        }
        FeaturePage.Invitations -> {
            s.invitations.items.forEach { row -> FeatureSection("${invitationScope(row.data.scope)} · ${stateLabel(row.state)}") {
                Text("事项 #${row.data.targetId} · 接收人 #${row.data.recipientId}\n有效期至 ${displayTime(row.data.expiresAt)}")
                val actions = if (row.data.recipientId == actor && row.state == "pending") listOf("accept" to "接受","decline" to "拒绝") else if (row.ownerId == actor && row.state in listOf("pending","accepted")) listOf("revoke" to "撤销授权") else emptyList()
                actions.forEach { (action,label) -> FeatureButton(label,!s.busy) { val input = VersionedAction(row.version); f.confirm(label,"${invitationScope(row.data.scope)} #${row.data.targetId}") { f.api.invitationAction(row.id,action,input); f.reload(); store.refreshHome() } } }
                if (row.state == "accepted" && row.data.scope == "adoption_editor") FeatureButton("维护该宠物介绍") { f.open(FeatureRoute(FeaturePage.Collaborators,row.data.targetId)) }
                if (row.state == "accepted" && row.data.scope == "caretaker") FeatureButton("查看授权照护近况") { f.open(FeatureRoute(FeaturePage.AdoptionWork,row.data.targetId,"care")) }
                if (row.state == "accepted" && row.data.scope == "group_member") FeatureButton("进入群聊") { f.finishRoute(); store.open(MobileDestination.Chat(row.data.targetId,true)) }
            } }
            MoreFeature(s.invitations.next,s.loading) { f.reload(true) }
            if (s.invitations.loaded && s.invitations.items.isEmpty()) Text("暂无邀请")
        }
        FeaturePage.GroupCreate -> {
            FormField(f,s,"name","群聊名称")
            FeaturePrimary("创建群聊",!s.busy && f.field("name").isNotBlank()) {
                val input = GroupInput(f.field("name").trim(),newRequestKey())
                f.confirm("创建群聊",input.name) { val group = f.api.createGroup(input); store.refreshHome(); f.completeTo(FeatureRoute(FeaturePage.Group,group.id)) }
            }
        }
        FeaturePage.Group -> s.group?.let { group ->
            val body = group.data; val pendingActive = body.pendingTransferTo > 0 && body.transferExpiresAt?.let { runCatching { java.time.OffsetDateTime.parse(it).isAfter(java.time.OffsetDateTime.now()) }.getOrDefault(false) } == true; val owner = group.ownerId == actor; val member = actor in body.members
            FeatureSection(body.name) { Text("${stateLabel(group.state)} · ${body.members.size} 位成员"); Text(body.notice.ifBlank { "暂无公告" }); Text(if (owner) "你的角色：群主" else "你的角色：成员") }
            if (group.state != "active" || !member) { Text("当前群聊不可继续发送或管理"); return }
            fun action(action: String, label: String, user: Long = 0, notice: String = "") {
                val input = GroupAction(f.version(group.version),newRequestKey(),notice,user)
                f.confirm(label,"${body.name} · 版本 ${group.version}\n${if (user > 0) "目标成员 #$user" else notice}") { f.api.groupAction(group.id,action,input); store.reloadDetail(); store.refreshHome(); f.discardAndReload() }
            }
            FeatureButton(if (actor in body.muted) "取消群免打扰" else "开启群免打扰",!s.busy) { action("mute","更改群免打扰") }
            if (owner) {
                FormField(f,s,"notice","群公告",body.notice,4)
                FeatureButton("保存公告",!s.busy) { action("notice","更新群公告",notice = f.field("notice",body.notice)) }
                FormField(f,s,"invitee","邀请用户编号",numeric = true)
                FeatureButton("邀请成员",!s.busy) { val user = f.field("invitee").toLongOrNull(); if (user == null || user <= 0) f.notify("请输入有效编号") else { val input = InviteInput("group_member",group.id,user,newRequestKey()); f.confirm("邀请成员","用户 #$user") { f.api.invite(input); f.reload() } } }
                body.members.filter { it != actor }.forEach { user -> FeatureSection("成员 #$user") {
                    FeatureButton("查看资料") { f.open(FeatureRoute(FeaturePage.Member,user)) }
                    FeatureButton("移出群聊",!s.busy) { action("remove_member","移出成员",user) }
                    if (!pendingActive) FeatureButton("邀请其接任群主",!s.busy) { action("transfer","发起群主转让（须对方接受）",user) }
                } }
                if (body.pendingTransferTo > 0) { Text("${if(pendingActive) "等待接受" else "转让已到期"} · 用户 #${body.pendingTransferTo} · ${body.transferExpiresAt?.let(::displayTime).orEmpty()}"); FeatureButton("撤回转让",!s.busy) { action("revoke_transfer","撤回群主转让") } }
                FeatureButton("解散群聊",!s.busy) { action("dissolve","解散群聊：全部成员将无法继续发送") }
            } else {
                if (pendingActive && body.pendingTransferTo == actor) {
                    FeatureButton("接受群主转让",!s.busy) { action("accept_transfer","接受群主转让") }
                    FeatureButton("拒绝群主转让",!s.busy) { action("decline_transfer","拒绝群主转让") }
                }
                FeatureButton("退出群聊",!s.busy) { action("leave","退出当前群聊") }
            }
        }
        else -> Unit
    }
}
@Composable internal fun AudienceFields(f: MobileFeatureStore, s: FeatureState, audience: Audience, publicAllowed: Boolean = true) {
    ChoiceField(f,s,"audience","可见范围",buildList { if (publicAllowed) add("public" to "公开"); add("private" to "仅自己"); add("friends" to "好友"); add("selected" to "指定好友"); add("excluded" to "排除指定好友") },audience.mode)
    if (f.field("audience",audience.mode) in listOf("selected","excluded")) FormField(f,s,"audienceIds","好友编号，以逗号分隔",audience.userIds.joinToString(","))
}
internal fun formAudience(f: MobileFeatureStore, original: Audience): Audience? {
    val mode = f.field("audience",original.mode); val selected = ids(f.field("audienceIds",original.userIds.joinToString(","))) ?: return null
    return Audience(mode,if (mode in listOf("selected","excluded")) selected else emptyList()).takeIf { mode != "selected" || selected.isNotEmpty() }
}
internal fun audienceLabel(mode: String) = mapOf("public" to "公开","private" to "仅自己","friends" to "好友可见","selected" to "指定好友可见","excluded" to "排除指定好友")[mode] ?: "未知范围"
internal fun invitationScope(scope: String) = mapOf("adoption_editor" to "领养介绍协作","caretaker" to "私密照护协作","group_member" to "群聊邀请")[scope] ?: scope
@Composable internal fun MoreFeature(cursor: Long, loading: Boolean, more: () -> Unit) { if (cursor != 0L) FeatureButton("继续加载",!loading,more) }

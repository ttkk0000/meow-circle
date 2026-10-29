package com.ttkk0000.meowcircle.clientui

import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.ttkk0000.meowcircle.client.*

@Composable fun SocialFeature(store: MobileStore, s: FeatureState, route: FeatureRoute) {
    val f = store.features; val actor = store.state.value.user?.id
    when (route.page) {
        FeaturePage.EntryEdit -> s.entry?.let { record ->
            val old = record.data
            FormField(f,s,"title","标题",old.title)
            FormField(f,s,"text","正文",old.text,6)
            CityFormField(f,s,old.city,"城市（选填，不含精确位置）")
            ChoiceField(f,s,"space","发布空间",listOf("community" to "公开社区","circle" to "宠友圈","diary" to "宠物日记"),old.space)
            val space = f.field("space",old.space)
            if(space != "community") AudienceFields(f,s,if(old.audience.mode == "public" && space == "circle") Audience("friends") else old.audience,publicAllowed = space != "circle")
            if(space == "diary") PetChoiceField(f,s,"petId",old.petId)
            ChoiceField(f,s,"origin","当前内容来源",originChoices,old.contentOrigin.ifBlank { "unknown" })
            FeaturePhotos(store,s,record.mediaIds)
            FeaturePrimary("核对并保存动态",!s.busy && !s.loading) {
                val audience = if(space == "community") Audience() else formAudience(f,if(old.audience.mode == "public" && space == "circle") Audience("friends") else old.audience)
                val petId = f.field("petId",old.petId.toString()).toLongOrNull() ?: 0
                if(audience == null || space == "diary" && petId <= 0 || audience.mode == "public" && space == "circle") f.notify("请核对发布空间、受众及宠物归属") else {
                    val input = EntryInput(old.copy(city = f.field("city",old.city),title = f.field("title",old.title),text = f.field("text",old.text),space = space,audience = audience,petId = petId,contentOrigin = ""),f.version(record.version),f.mediaIds(record.mediaIds),newRequestKey(),PublicationDeclaration(origin = f.field("origin",old.contentOrigin.ifBlank { "unknown" })))
                    if(!f.photosReady()) f.notify("请先完成照片上传") else f.runCommand {
                        f.requireCurrentAudience(audience)
                        val declaration = if(audience.mode == "private") PublicationDeclaration() else f.api.previewPublication(input).let { PublicationDeclaration(it.manifestHash,it.origin,true) }
                        f.reviewPrepared("保存当前动态","${input.entry.title}\n${input.entry.text}\n${audienceLabel(audience.mode)} · ${originLabel(declaration.origin)}\n确认当前内容、来源与照片。") { f.requireCurrentAudience(audience); f.api.editEntry(record.id,input.copy(declaration = declaration)); store.reloadDetail(); store.refreshHome(); f.discardAndReload() }
                    }
                }
            }
        }
        FeaturePage.Friends -> {
            LaunchedEffect(actor) { if (actor != null && !s.peopleChoices.loaded) f.loadPeopleChoices() }
            Text("在动态、会话或群聊中打开宠友资料后，可向明确的对方发送好友申请。申请须由对方接受。")
            s.friends.items.forEach { row ->
                val peer = if (row.data.requesterId == actor) row.data.recipientId else row.data.requesterId
                val person = s.people[peer]
                val name = person?.nickname?.ifBlank { person.username }?.ifBlank { "宠友" } ?: "宠友资料待核对"
                FeatureSection("$name · ${stateLabel(row.state)}") {
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
                        f.confirm(label,"$name\n关系变化后将重新校验私密内容访问。") { f.api.friend(action,input); store.refreshHome(); f.reload(); f.loadPeopleChoices() }
                    } }
                }
            }
            PageFooter(s.friends, { f.reload() }, { f.reload(true) }, "还没有好友关系，申请须由对方接受。", enabled = !s.loading && !s.busy)
        }
        FeaturePage.Member -> s.member?.let { member ->
            FeatureSection(member.nickname.ifBlank { member.username }) { Text(member.bio) }
            if(member.id != actor) ContentSubscriptionButton(store,member.id)
            if (actor != null && member.id != actor) FormField(f,s,"reason","好友申请说明")
            FeatureButton("申请成为好友", actor != null && member.id != actor && !s.busy) {
                f.runCommand {
                    val input = f.prepareFriendRequest(member.id,f.field("reason"))
                    f.reviewPrepared("申请成为好友","${member.nickname.ifBlank { member.username }}\n${input.reason}") {
                        f.requireOpenedMember(member.id)
                        f.api.friend("request",input); f.notify("好友申请已发送，等待对方接受")
                    }
                }
            }
            FeatureButton("发起私信",actor != null && member.id != actor && !s.busy) { f.runCommand { val chat = f.api.startConversation(member.id); f.finishRoute(); store.open(MobileDestination.Chat(chat.id)) } }
            FeatureButton("举报该资料") { f.open(FeatureRoute(FeaturePage.Review,kind = "users",peer = member.id)); f.edit("reason","用户 #${member.id}（${member.nickname}）的资料：\n") }
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
        FeaturePage.PetDiary -> {
            TaskNotice("这只宠物的独立日记","各篇仍按各自受众读取，不会自动共享或扩大范围。")
            s.diary.items.forEach { row -> FeatureSection(row.data.title.ifBlank { "日记" }) { Text(row.data.text); if(row.mediaIds.isNotEmpty()) MobilePhoto(store,row.mediaIds.first(),actor,"日记照片",androidx.compose.ui.Modifier.fillMaxWidth().heightIn(max = 240.dp)); NavigationRow("查看全文与评论") { f.closeRoute();store.open(MobileDestination.Entry(row.id)) } } }
            PageFooter(s.diary, { f.reload() }, { f.reload(true) }, enabled = !s.loading)
            FeaturePrimary("写这只宠物的日记") { f.closeRoute();store.select(MobileTab.Publish);store.editComposeOptions("diary",Audience("private"),route.id) }
        }
        FeaturePage.PetEdit -> {
            if (route.id > 0 && s.pet == null) return
            val record = s.pet; val pet = record?.data ?: PetBody("","cat",audience = Audience("private"))
            FormField(f,s,"name","宠物名字",pet.name)
            ChoiceField(f,s,"species","物种",listOf("cat" to "猫咪","dog" to "狗狗","rabbit" to "兔兔","other" to "其他宠物"),pet.species)
            CityFormField(f,s,pet.city,"城市（选填，不含家庭地址）")
            FormField(f,s,"breed","品种（未知可留空）",pet.breed)
            DateTimeField(f,s,"birthday","生日（选填）",pet.birthday,dateOnly = true,optional = true)
            ChoiceField(f,s,"sex","性别",listOf("" to "未知","male" to "公","female" to "母"),pet.sex)
            FormField(f,s,"bio","宠物介绍",pet.bio,4)
            FormField(f,s,"health","私密健康记录",pet.health,3)
            AudienceFields(f,s,pet.audience)
            val adoptionSpecies = f.field("species",pet.species) in listOf("cat","dog")
            CheckLine("开放领养介绍（需成年人及有效处置资格）",adoptionSpecies && f.field("adoption",pet.adoptionAvailable.toString()).toBoolean(),{ f.edit("adoption",it.toString()) },!s.busy && adoptionSpecies)
            if(!adoptionSpecies) Text("当前物种支持档案与日记，领养介绍暂未开放。")
            FeaturePhotos(store,s,record?.mediaIds.orEmpty(),private = f.field("audience",pet.audience.mode) == "private")
            FeaturePrimary("保存宠物档案",!s.busy && !s.loading) {
                val audience = formAudience(f,pet.audience)
                val birthday = f.field("birthday",pet.birthday)
                if (f.field("name",pet.name).isBlank() || audience == null || birthday.isNotBlank() && !validDay(birthday)) f.notify("请填写名字、有效生日与可见范围") else {
                    val input = PetInput(pet.copy(name = f.field("name",pet.name).trim(),city = f.field("city",pet.city),species = f.field("species",pet.species),breed = f.field("breed",pet.breed),birthday = birthday,
                        sex = f.field("sex",pet.sex),bio = f.field("bio",pet.bio),health = f.field("health",pet.health),audience = audience,adoptionAvailable = adoptionSpecies && f.field("adoption",pet.adoptionAvailable.toString()).toBoolean()),f.version(record?.version ?: 0),f.mediaIds(record?.mediaIds.orEmpty()),newRequestKey())
                    f.runCommand {
                        f.requireCurrentAudience(audience)
                        f.reviewPrepared("保存宠物档案","${input.pet.name}\n${audienceLabel(audience.mode)}\n${if (input.pet.adoptionAvailable) "开放领养介绍" else "普通档案"}") {
                            f.requireCurrentAudience(audience)
                            val result = if (record == null) f.api.savePet(input) else f.api.editPet(record.id,input)
                            store.refreshHome(); f.completeTo(FeatureRoute(FeaturePage.PetEdit,result.id))
                        }
                    }
                }
            }
            if (record != null && record.ownerId == actor) FeatureButton("删除宠物档案",!s.busy) {
                f.confirm("删除宠物档案","${pet.name} · 版本 ${record.version}\n提交后公开内容和照片访问将被收起。") { f.api.removeResource("pets",record.id,record.version); f.finishRoute(); store.back(); store.refreshHome() }
            }
        }
        FeaturePage.Collaborators -> {
            Text(if (route.kind == "care") "照护协作者仅访问授权近况，不获得公开档案编辑和领养决定权限。" else "介绍协作者仅维护介绍与照片；领养决定仍由发布人处理。")
            val canInvite = actor != null && !s.loading && s.error == null && (if(route.kind == "care") s.adoption?.data?.applicantId == actor && s.careContext?.canPost == true else s.pet?.ownerId == actor)
            if(canInvite) {
            PeopleChoiceField(f,s,"选择协作者",listOfNotNull(f.field("invitee").toLongOrNull()),
                { f.edit("invitee",it.firstOrNull()?.toString().orEmpty()) })
            FeaturePrimary("发出协作邀请",!s.busy && actor != null) {
                val user = f.field("invitee").toLongOrNull()
                if (user == null || user <= 0 || user == actor) f.notify("请选择当前可访问的好友") else {
                    val input = InviteInput(if (route.kind == "care") "caretaker" else "adoption_editor",route.id,user,newRequestKey())
                    f.runCommand {
                        val person = f.requireCurrentPeople(listOf(user)).single()
                        f.reviewPrepared("发出协作邀请","${person.nickname.ifBlank { person.username }}\n${if (route.kind == "care") "仅查看授权的私密照护近况" else "仅维护本宠物的领养介绍与照片，不获得领养决定权限"}；须由对方接受。") {
                            f.requireCurrentPeople(listOf(user)); f.api.invite(input); f.notify("邀请已发出，可在邀请列表中撤销")
                        }
                    }
                }
            }
            }
            FeatureButton(if(canInvite) "查看／撤销邀请" else "查看协作与退出",!s.busy) { f.open(FeatureRoute(FeaturePage.Invitations,kind = if(route.kind == "care") "caretaker" else "adoption_editor",peer = route.id)) }
            s.pet?.let { pet ->
                FormField(f,s,"bio","领养介绍",pet.data.bio,5)
                FeaturePhotos(store,s,pet.mediaIds)
                FeatureButton("仅更新介绍与照片",!s.busy && !s.loading && s.error == null && !s.conflict && !s.unconfirmedResult) {
                    val input = IntroductionEdit(f.version(pet.version),f.field("bio",pet.data.bio),f.mediaIds(pet.mediaIds),newRequestKey())
                    f.confirm("更新领养介绍",input.bio) { f.api.introduction(pet.id,input); f.discardAndReload() }
                }
            }
        }
        FeaturePage.Invitations -> {
            if(route.id > 0) s.invitation?.let { InvitationDetailContent(store,s,it) }
            else {
                val visible = if(route.peer > 0) s.invitations.items.filter { it.data.targetId == route.peer && it.data.scope == route.kind } else s.invitations.items
                if(route.peer > 0) TaskNotice("仅此事项的邀请与协作","事项 #${route.peer} · ${invitationScope(route.kind)}；仅显示当前账号可读取的邀请与授权。")
                visible.forEach { row -> FeatureSection("${invitationScope(row.data.scope)} · ${stateLabel(row.state)}") {
                    if(row.data.note.isNotBlank()) Text(row.data.note)
                    Text("事项 #${row.data.targetId} · 邀请 #${row.id}")
                    if(row.state == "pending") Text("接受邀请截止 ${displayTime(row.data.expiresAt)}")
                    FeatureButton("查看邀请与当前协作",!s.busy && !s.loading) { f.open(FeatureRoute(FeaturePage.Invitations,row.id)) }
                } }
                PageFooter(s.invitations.copy(items = visible), { f.reload() }, { f.reload(true) }, "暂无邀请或授权", enabled = !s.loading && !s.busy)
            }
        }
        FeaturePage.Content -> s.contentSubscriptions?.let { content ->
            FeatureSection("关注的作者") { content.data.followedUsers.forEach { id -> NavigationRow("宠友 #$id") { f.open(FeatureRoute(FeaturePage.Member,id)) } }; if(content.data.followedUsers.isEmpty()) Text("还没有关注的作者，可从动态的发布人资料发起关注。") }
            FeatureSection("关注的求助") { content.data.watchedCases.forEach { id -> NavigationRow("求助 #$id") { f.closeRoute(); store.open(MobileDestination.Case(id)) } }; if(content.data.watchedCases.isEmpty()) Text("暂未关注走失或拾获求助") }
        }
        FeaturePage.GroupDirectory -> {
            CityFormField(f,s)
            FormField(f,s,"q","群名或介绍")
            FeatureButton("按条件查找",!s.loading) { f.reload() }
            TaskNotice("先看群介绍，再决定加入","发现页不展示成员身份、家庭地址、精确距离或聊天内容。入群申请须由当前群主核对。")
            s.directory.items.forEach { row -> NavigationRow(row.data.name,"${row.data.city} · ${row.data.memberCount} 位成员 · 需审核") { f.open(FeatureRoute(FeaturePage.GroupPreview,row.id)) } }
            PageFooter(s.directory, { f.reload() }, { f.reload(true) }, enabled = !s.loading)
            if(s.directory.loaded && s.directory.items.isEmpty()) Text("当前条件暂无可发现的群")
        }
        FeaturePage.GroupPreview -> s.groupPreview?.let { row ->
            FeatureSection(row.data.name) { Text(row.data.summary); Text("${row.data.city} · ${row.data.memberCount} 位成员") }
            if(row.data.member) FeaturePrimary("进入已加入的群聊") { f.closeRoute(); store.open(MobileDestination.Chat(row.id,true)) }
            else { FormField(f,s,"note","入群申请说明",lines = 3); FeaturePrimary("核对并申请加入",!s.busy) { if(actor == null) store.showLogin() else { val input = GroupJoinInput(newRequestKey(),f.field("note"));f.confirm("申请加入群聊",row.data.name+"\n"+input.note) { f.api.requestGroupJoin(row.id,input); f.completeTo(FeatureRoute(FeaturePage.Invitations)) } } } }
        }
        FeaturePage.GroupCreate -> {
            FormField(f,s,"name","群聊名称")
            CheckLine("将群简介放入发现页（不公开群消息）",f.field("discoverable","false").toBoolean(),{ f.edit("discoverable",it.toString()) },!s.busy)
            CityFormField(f,s,label = "城市（公开发现时必填）")
            FormField(f,s,"summary","公开群简介",lines = 3)
            FeaturePrimary("创建群聊",!s.busy && f.field("name").isNotBlank()) {
                val input = GroupInput(f.field("name").trim(),newRequestKey(),f.field("discoverable","false").toBoolean(),f.field("city"),f.field("summary"))
                f.confirm("创建群聊",input.name) { val group = f.api.createGroup(input); store.refreshHome(); f.completeTo(FeatureRoute(FeaturePage.Group,group.id)) }
            }
        }
        FeaturePage.Group -> s.group?.let { group ->
            val body = group.data; val pendingActive = body.pendingTransferTo > 0 && body.transferExpiresAt?.let { futureTime(it) } == true; val owner = group.ownerId == actor; val member = actor in body.members
            FeatureSection(body.name) { Text("${stateLabel(group.state)} · ${body.members.size} 位成员"); Text(body.notice.ifBlank { "暂无公告" }); Text(if (owner) "你的角色：群主" else "你的角色：成员") }
            if (group.state != "active" || !member) { Text("当前群聊不可继续发送或管理"); return }
            fun action(action: String, label: String, user: Long = 0, notice: String = "") {
                val input = GroupAction(f.version(group.version),newRequestKey(),notice,user)
                f.confirm(label,"${body.name} · 版本 ${group.version}\n${if (user > 0) "目标成员 #$user" else notice}") { f.api.groupAction(group.id,action,input); store.reloadDetail(); store.refreshHome(); f.discardAndReload() }
            }
            FeatureButton(if (actor in body.muted) "取消群免打扰" else "开启群免打扰",!s.busy) { action("mute","更改群免打扰") }
            if (owner) {
                CheckLine("允许在发现页展示群简介",f.field("discoverable",body.discoverable.toString()).toBoolean(),{ f.edit("discoverable",it.toString()) },!s.busy)
                CityFormField(f,s,body.city,"公开城市"); FormField(f,s,"summary","公开群简介",body.summary,3)
                FeatureButton("核对并保存发现设置",!s.busy) { val input = GroupAction(f.version(group.version),newRequestKey(),discoverable = f.field("discoverable",body.discoverable.toString()).toBoolean(),city = f.field("city",body.city),summary = f.field("summary",body.summary)); f.confirm("更改群发现设置","仅展示群名、城市、简介与人数，不公开成员身份或群消息。") { f.api.groupAction(group.id,"directory",input); f.discardAndReload() } }
                FormField(f,s,"notice","群公告",body.notice,4)
                FeatureButton("保存公告",!s.busy) { action("notice","更新群公告",notice = f.field("notice",body.notice)) }
                PeopleChoiceField(f,s,"选择邀请入群的好友",listOfNotNull(f.field("invitee").toLongOrNull()),
                    { f.edit("invitee",it.firstOrNull()?.toString().orEmpty()) },excluded = body.members.toSet())
                FeatureButton("邀请成员",!s.busy) {
                    val user = f.field("invitee").toLongOrNull()
                    if (user == null || user <= 0 || user in body.members) f.notify("请选择尚未入群的好友") else {
                        val input = InviteInput("group_member",group.id,user,newRequestKey())
                        f.runCommand {
                            val person = f.requireCurrentPeople(listOf(user)).single()
                            f.reviewPrepared("邀请成员","邀请 ${person.nickname.ifBlank { person.username }} 加入 ${body.name}；对方接受后才能访问群聊。") {
                                f.requireCurrentPeople(listOf(user)); f.api.invite(input); f.reload()
                            }
                        }
                    }
                }
                body.members.filter { it != actor }.forEach { user -> MemberSection(store,user) {
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
@Composable fun AudienceFields(f: MobileFeatureStore, s: FeatureState, audience: Audience, publicAllowed: Boolean = true) {
    ChoiceField(f,s,"audience","可见范围",buildList { if (publicAllowed) add("public" to "公开"); add("private" to "仅自己"); add("friends" to "好友"); add("selected" to "指定好友"); add("excluded" to "排除指定好友") },audience.mode)
    val mode = f.field("audience",audience.mode)
    if (mode in listOf("selected","excluded")) {
        val selected = ids(f.field("audienceIds",audience.userIds.joinToString(","))) ?: audience.userIds
        Text(if (mode == "selected") "仅以下好友可见；未选中的好友无法查看。" else "所有好友中排除以下人员；今后新增的好友也可能可见。")
        PeopleChoiceField(f,s,if (mode == "selected") "指定可见好友" else "排除可见好友",selected,
            { f.edit("audienceIds",it.joinToString(",")) },multiple = true)
        Text("关系失效或读取失败会要求调整或重试，不能自动改成公开。")
    }
}
fun formAudience(f: MobileFeatureStore, original: Audience): Audience? {
    val mode = f.field("audience",original.mode); val selected = ids(f.field("audienceIds",original.userIds.joinToString(","))) ?: return null
    return Audience(mode,if (mode in listOf("selected","excluded")) selected else emptyList()).takeIf { mode != "selected" || selected.isNotEmpty() }
}
fun audienceLabel(mode: String) = mapOf("public" to "公开","private" to "仅自己","friends" to "好友可见","selected" to "指定好友可见","excluded" to "排除指定好友")[mode] ?: "未知范围"
fun invitationScope(scope: String) = mapOf("adoption_editor" to "领养介绍协作","caretaker" to "私密照护协作","group_member" to "群聊邀请","group_request" to "入群申请")[scope] ?: scope

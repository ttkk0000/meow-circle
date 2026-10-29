"""合并本地设计账本并生成逐状态实现索引；不把映射当视觉或运行验收。"""
import argparse
import hashlib
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
design = root / 'docs/design/figma-study'
base = json.loads((design / 'cross-platform-full-state.json').read_text())
families, names, roots, provenance = {}, {}, {}, {}
for name, rows in base.items():
    if not name.endswith('Sources'):
        continue
    for row in rows:
        families[row['key']] = name.removesuffix('Sources')
        names[row['key']] = row['name']


def merge(mapping, source, phone=False):
    for key, value in mapping.items():
        if not isinstance(value, str) or not re.fullmatch(r'\d+:\d+', value):
            continue
        full = 'phone/' + key if phone else key
        if '/' not in full:
            continue
        platform, key = full.split('/', 1)
        if key not in families and phone:
            continue
        roots.setdefault(key, {})[platform] = value
        provenance.setdefault(key, {})[platform] = source


for filename in ['mobile-closure-state.json', 'social-chat-state.json', 'mobile-completion-state.json', 'expression-refinement-state.json']:
    data = json.loads((design / filename).read_text())
    merge(data.get('roots', {}), filename, phone=True)
merge(base['roots'], 'cross-platform-full-state.json')
for filename in ['design-closure-2026-10-21-state.json', 'logic-fix-2026-10-21-state.json', 'legal-design-2026-10-22-state.json']:
    data = json.loads((design / filename).read_text())
    merge(data.get('effectiveCoreRoots', data.get('roots', {})), filename)

# 追加已存在的远端协作稿；旧740状态保留，不用推算节点补齐未核对布局。
baseline_states = len(roots)
collaboration_source = 'collaboration-2026-10-29/state.json'
collaboration = json.loads((design / collaboration_source).read_text())
collaboration_states = {row['key']: row for row in collaboration['states']}
assert len(collaboration_states) == 8
assert not set(collaboration_states).intersection(roots)
for key, row in collaboration_states.items():
    families[key] = 'adoption'
    names[key] = row['name']
merge(collaboration['roots'], collaboration_source)

android = 'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/'
entries = {
 'community': ('ConsumerApp.kt', 'features/Records.tsx', '/'),
 'publishing-media': ('ComposeControls.kt', 'features/WorkflowEditor.tsx', '/publish'),
 'adoption': ('CarePages.kt', 'features/ResourcePage.tsx', '/adoptions'),
 'verification-reports': ('AccountPages.kt', 'features/ResourcePage.tsx', '/reviews'),
 'pet-diary': ('SocialPages.kt', 'features/WorkflowEditor.tsx', '/pets'),
 'lost-found': ('CarePages.kt', 'features/ResourcePage.tsx', '/cases'),
 'messages': ('InboxPages.kt', 'features/chat.tsx', '/messages'),
 'appointments': ('CarePages.kt', 'features/ResourcePage.tsx', '/appointments'),
 'auth': ('AuthPages.kt', 'features/AuthDialog.tsx', '/security'),
 'settings-notifications': ('AccountPages.kt', 'features/Account.tsx', '/settings'),
 'offers-visits': ('OfferPages.kt', 'features/ResourcePage.tsx', '/offers'),
 'global-states': ('FeaturePages.kt', 'components/ui.tsx', '/recovery'),
 'groups': ('SocialPages.kt', 'features/chat.tsx', '/groups'),
 'circle-friends': ('SocialPages.kt', 'components/ContentFields.tsx', '/friendships'),
 'nearby': ('AccountPages.kt', 'features/WorkflowEditor.tsx', '/nearby'),
 'expressions': ('ComposeControls.kt', 'features/chat.tsx', '/messages'),
 'safety-data': ('AccountPages.kt', 'features/Recovery.tsx', '/recovery'),
 'rules': ('ReadingPages.kt', 'features/ReadingPages.tsx', '/rules'),
 'operator-reference': (None, 'operator/OperatorApp.tsx', '/operator'),
}


def family(key):
    if key.startswith('ops'):
        return 'operator-reference'
    if key in families:
        return families[key]
    if re.match(r'(data|guest|reportAccess|reportSupplement|rights|retention|providerData)', key):
        return 'safety-data'
    if re.match(r'(return|returned|trialReserved|placement|candidates|waitlist|contract)', key):
        return 'adoption'
    if re.match(r'(hold|appointment|safetyHold)', key):
        return 'appointments'
    if re.match(r'(rule|legalCenter|serviceTerms|teenPrivacy|sellerNotice)', key):
        return 'rules'
    if re.match(r'(account|adult|age|guardian|auth|email|phone|sms|sdk|link)', key):
        return 'auth'
    if re.match(r'(ai)', key):
        return 'publishing-media'
    if re.match(r'(authority|city)', key):
        return 'offers-visits'
    if re.match(r'(lost)', key):
        return 'lost-found'
    raise ValueError('尚未逐项归类：' + key)


# 明确记录设计与现行服务合同之间的缺口，不能用通用页面归类掩盖。
contract_gaps = {}
# 2026-10-24：原14项已补真实源合同与入口；仅表示源码覆盖，运行与视觉状态仍为false。

external_prefix = ('authProvider_', 'authCallback', 'adultProvider', 'adultPassed', 'adultPending', 'guardianProtection', 'nearbyPermission', 'notificationPermission')
representatives = {'home': ['56:27', '311:23', '311:25', '311:26'], 'composePost': ['56:33', '311:378832'], 'post': ['56:32'], 'adopt': ['56:35'], 'messages': ['56:61', '311:379700'], 'mine': ['56:73'], 'login': ['56:70'], 'appointment': ['56:63', '311:379780'], 'lostList': ['56:54'], 'groupChat': ['106:1439'], 'legalCenter': ['632:357494'], 'following': ['106:1387'], 'groupDiscover': ['106:1436'], 'petOther': ['142:2913'], 'careGuide': ['106:1425', '311:199', '311:201', '311:202'], 'rules': ['460:17463'], 'ruleDetail': ['460:17468'], 'ruleHistory': ['460:17473'], 'ruleChange': ['632:357528'], 'ruleAppeal': ['460:17483'], 'serviceTerms': ['460:17458'], 'teenPrivacy': ['460:17408'], 'sellerNotice': ['632:357529'], 'settings': ['56:75'], 'privacy': ['56:76'], 'profileActions': ['106:1406'], 'dataInventory': ['460:17428'], 'sdkList': ['460:17453'], 'providerData': ['632:357526'], 'retention': ['632:357509'], 'myPublishes':['56:53'], 'adoptionComments':['142:3011','145:8407','220:335999']}
representatives.update({'accountChoice':['460:17333'],'accountConflict':['460:17338'],'authProviderCancelled':['238:371728'],'authProviderUnavailable':['238:371727'],'authCallbackExpired':['238:371729'],'authProvider_wechat':['238:371731'],'authLinkExisting':['238:371737'],'authLinkConfirm':['238:371738'],'ageRange':['460:17378'],'adultNotice':['506:6819'],'adultProvider':['506:6824'],'adultPending':['506:6829']})
representatives.update({'guardianInvite':['460:17383'],'guardianReview':['460:17388'],'guardianWaiting':['460:17393'],'guardianActive':['460:17398'],'guardianRevoked':['460:17403'],'guardianProtection':['632:357527']})
# 2026-10-25本批实际回读截图与高保真上下文；仍不作为客户端运行验收。
representatives.update({'offline':['56:81','311:380192','311:380195'],'noResults':['56:82'],
 'contentUnavailable':['56:83'],'permission':['56:84'],'homeEmpty':['56:28'],
 'notificationPermissionHelp':['240:368353'],'mediaChoose':['238:371770'],'mediaPermission':['106:1424'],
 'mediaUploading':['240:368361'],'mediaPartialFailure':['240:368362'],'mediaReorder':['240:368363']})
representatives.update({'postPreview':['56:34','311:378836','311:378838','311:378839'],'aiChoice':['632:357495'],'aiReview':['632:357496'],'aiClearReview':['632:357497'],'aiChecking':['632:357498'],'aiResult':['632:357499'],'aiRestriction':['632:357500']})
representatives.update({'lostLeadInbox':['142:2952'],'lostLeadEmpty':['142:2953'],'lostLeadDetail':['142:2954','311:379528','311:379531'],'lostLeadHelpful':['142:2955'],'lostLeadNotMatch':['142:2956'],'lostLeadChat':['142:2978','145:7246','220:341399']})
representatives.update({'lostLeadPhoto':['142:2948','311:379504','311:379507'],'lostLeadReview':['142:2949'],'lostLeadFailure':['142:2950'],'lostLeadSent':['142:2951']})
representatives.update({'appointmentConfirmedV3':['252:371393'],'appointmentHistoryV2':['252:371389'],'appointmentRestoreReview':['252:371392'],'appointmentReview':['460:211426'],
 'safetyHold':['460:211421','460:211422','460:211425'],'holdCancel':['506:205509','506:205510','506:205513'],'holdCancelPending':['506:205514'],'holdCancelFailed':['506:205519'],'holdCancelled':['506:205524'],
 'holdReproposal':['506:205529','506:205530','506:205533'],'holdProposalWaiting':['506:205534'],'holdReleased':['506:205539','506:205540','506:205543']})
representatives.update({'adoptionDecision':['240:368311'],'adoptionSupplement':['240:368312'],'trialAgreement':['240:368316','311:379012','311:379015'],'trialWaiting':['246:370291'],'trialOther':['246:370292'],'trialActive':['240:368317'],
 'handoverWaiting':['240:368319'],'handoverOther':['240:368320','311:379028','311:379031'],'handoverComplete':['240:368321'],'waitlist':['460:68717','460:68718','460:68721'],'placementConflict':['460:68722'],
 'returnReviewR2':['506:60825','506:60826','506:60829'],'returnWaitingR2':['506:60820'],'returnConfirmedR2':['506:60830'],'returnConflict':['506:60835'],'returnExpired':['506:60845'],'returnWithdrawR2':['506:60855']})
rows = []
for key in sorted(roots):
    group = family(key)
    a, w, route = entries[group]
    if key in ('lostMap','lostAreaMap'):
        a,w,route='MapPage.kt','features/MapPage.tsx','/map'
    elif key in ('lostLeadInbox','lostLeadEmpty'):
        a,w,route='LeadPages.kt','features/LeadPages.tsx','/leads?ref={caseId}'
    elif key in ('lostLeadPhoto','lostLeadReview','lostLeadFailure'):
        a,w,route='LeadSubmissionPages.kt','features/LeadSubmissionPages.tsx','/edit?flow=lead&kind=cases&id={caseId}'
    elif key == 'lostLeadSent':
        a,w,route='LeadPages.kt','features/LeadPages.tsx','/leads/{id}?view=submitted&version={version}'
    elif key in ('lostLeadDetail','lostLeadHelpful','lostLeadNotMatch'):
        a,w,route='LeadPages.kt','features/LeadPages.tsx','/leads/{id}' + ('?view=reviewed&version={version}' if key in ('lostLeadHelpful','lostLeadNotMatch') else '')
    elif key == 'lostLeadChat':
        a,w,route='ConsumerApp.kt','features/chat.tsx','/conversations/{id}'
    elif key in ('circlePhotoViewer','diaryPhotoViewer'):
        a,w='MediaGallery.kt','components/MediaGallery.tsx'
    elif key == 'following':
        a,w,route='ContentSubscriptions.kt','features/ContentPage.tsx','/content'
    elif key in ('groupDiscover','groupJoinPending','groupJoinReview'):
        a,w,route='SocialPages.kt','features/ResourcePage.tsx','/group-directory'
    if key == 'myPublishes':
        a,w,route='PublicationsPage.kt','features/Publications.tsx','/publications'
    elif key in ('adoptionComments','adoptionReply','adoptionCommentSent'):
        a,w,route='ConsumerApp.kt','features/ResourcePage.tsx','/pets/{id}?view=discussion'
    elif key == 'careGuide':
        a,w,route='ReadingPages.kt','features/ReadingPages.tsx','/guide'
    elif key in ('privacy','dataInventory','sdkList','providerData','retention','settings'):
        a,w='ReadingPages.kt','features/ReadingPages.tsx'
        route={'privacy':'/privacy','dataInventory':'/reading?type=inventory','sdkList':'/reading?type=sdk','providerData':'/reading?type=providers','retention':'/reading?type=retention','settings':'/settings'}[key]
    elif key in ('ruleDetail','serviceTerms','teenPrivacy','sellerNotice'):
        route='/reading?type='+{'ruleDetail':'adoption','serviceTerms':'service','teenPrivacy':'teen','sellerNotice':'seller'}[key]
    elif key in ('ruleHistory','ruleChange','ruleAppeal'):
        route='/rules?view='+{'ruleHistory':'history','ruleChange':'changes','ruleAppeal':'appeal'}[key]
    elif key in ('authorProfile','profileActions','authorFollowing'):
        a,w,route='SocialPages.kt','features/ResourcePage.tsx','/users/{id}'
    elif group == 'settings-notifications':
        route='/notification-settings'
    if key in ('offline','noResults','contentUnavailable'):
        a,w='ReadStatePages.kt','components/ui.tsx'
        route={'offline':'/','noResults':'/pets','contentUnavailable':'/pets/{id}'}[key]
    elif key in ('permission','notificationPermissionHelp','mediaPermission'):
        a,w='ReadStatePages.kt','features/PermissionPages.tsx'
        route={'permission':'/location-help','notificationPermissionHelp':'/notification-help','mediaPermission':'/photo-help'}[key]
    elif key in ('mediaChoose','mediaUploading','mediaPartialFailure','mediaReady','mediaRemoved','mediaReorder'):
        a,w='PageComponents.kt','components/Attachments.tsx'
    if key in ('postPreview','aiChoice','aiReview','aiClearReview','aiChecking','aiResult','aiRestriction'):
        a,w,route='PublicationPages.kt','features/PublicationPreview.tsx','/publish'
    if group == 'auth' and (key.startswith(('authProvider','authCallback','authLink','accountChoice','linkExisting','linkConfirm'))):
        a,w='ProviderAuthPages.kt','features/ProviderSignIn.tsx'
    if key.startswith('adult') or key == 'ageRange':
        a,w,route='AgePages.kt','features/AgePages.tsx','/age' if key == 'ageRange' else '/age-check'
    if key.startswith('guardian'):
        a,w,route='GuardianPages.kt','features/GuardianPages.tsx','/guardian-protection' if key == 'guardianProtection' else '/guardians'
    status = 'source_family_mapped_not_visually_verified'
    note = '使用真实数据／错误／角色／版本驱动状态；本条没有运行或逐像素验收。'
    if group == 'operator-reference':
        status = 'dedicated_operator_source_not_runtime_verified'
        note = '三页在独立Web运营入口实现，手机／平板／桌面经响应式Web使用；消费者导航不暴露，具名凭据与服务端角色逐次校验。'
    if key in contract_gaps:
        status, note = 'contract_gap', contract_gaps[key]
    elif key.startswith(external_prefix):
        status = 'external_configuration_and_runtime_required'
        note = '保留真实不可用／等待状态；不能以本地变量模拟供应商、核验或系统权限。'
    if key in ('offline','noResults','contentUnavailable','permission','homeEmpty','notificationPermissionHelp','mediaPermission','mediaChoose','mediaUploading','mediaPartialFailure','mediaReorder'):
        status = 'dedicated_state_source_not_runtime_verified'
        note = '2026-10-25已回读对应稿并补具体状态／操作接线；运行、平台ABI及逐像素验收仍未完成。定位替代／照片选择说明不以系统权限或供应商配置作为阅读门槛。'
    if key in ('postPreview','aiChoice','aiReview','aiClearReview','aiChecking','aiResult','aiRestriction'):
        status = 'dedicated_state_source_not_runtime_verified'
        note = '2026-10-26回读手机7状态及发布预览Web／平板双向稿；独立声明追加记录、真实回执、原请求恢复、来源限制与最终发布分离，复用公共任务架构。13号迁移及各端运行／类型／视觉仍未验收。'
    if key in ('lostLeadInbox','lostLeadEmpty','lostLeadDetail','lostLeadHelpful','lostLeadNotMatch','lostLeadChat'):
        status = 'dedicated_state_source_not_runtime_verified'
        note = '2026-10-26回读私密线索列表／空态／详情／两类结果、目击者聊天及可见输入条，详情核对Web与横平板；实际标记／独立上下文会话绑定服务端原请求、版本与当前求助权限，关闭后历史只读、移除撤回关联读取。未编译、类型检查、回归执行、设备或像素验收。'
    if key in ('lostLeadPhoto','lostLeadReview','lostLeadFailure','lostLeadSent'):
        status = 'dedicated_state_source_not_runtime_verified'
        note = '2026-10-26回读手机四状态及照片Web／横平板；同一私有草稿、完整检查页、真实失败／未知结果、原请求与求助版本、新旧入口兼容及收到结果最低版本门槛已写；公共入口分派遗漏已修正。未编译、类型检查、回归执行或逐端运行验收。'
    if group == 'appointments':
        a,w,route='AppointmentPages.kt','features/AppointmentPages.tsx','/appointments/{id}'
        if key in ('appointment','appointmentNarrow','change','holdReproposal'):
            a,w,route='CarePages.kt','features/WorkflowEditor.tsx','/workflow/appointment-change'
    if key in ('appointmentConfirmedV3','appointmentHistoryV2','appointmentRestoreReview','appointmentReview','safetyHold','holdCancel','holdCancelPending','holdCancelFailed','holdCancelled','holdReproposal','holdProposalWaiting','holdReleased'):
        status = 'dedicated_state_source_not_runtime_verified'
        note = '2026-10-27回读12份手机状态与4状态Web／横平板共20上下文；预约详情／取消四状态、同暂停版本的新安排／双方具体核对、资格与披露复核、历史已替代版本恢复已接线。复用公共Compose与Web组件；未编译、类型检查、测试执行或逐端视觉／运行验收。'
    if key in ('adoptionDecision','adoptionSupplement','trialAgreement','trialWaiting','trialOther','trialActive','handoverWaiting','handoverOther','handoverComplete','waitlist','placementConflict'):
        a,w,route='AdoptionPages.kt','features/AdoptionPages.tsx','/adoptions/{id}'
        status='dedicated_state_source_not_runtime_verified'
        note='2026-10-27按当前角色／版本、同一生效协议与双方各自确认接入公共进度／完整检查；试养改从协议直接取条款与截止，旧草稿主动采用并保留。试养私密照护与交接完成后30天窗口分开，候补不披露其它家庭。未编译、类型检查、回归执行、设备或逐像素验收。'
    if key in ('returnReviewR2','returnWaitingR2','returnConfirmedR2','returnConflict','returnExpired','returnWithdrawR2'):
        a,w,route='CarePages.kt','features/ReturnPages.tsx','/adoptions/{id}'
        status='dedicated_state_source_not_runtime_verified'
        note='2026-10-27回读手机6状态及R2回应Web／横平板；公共完整方案检查、同版角色／确认截止、未知结果原请求、保留草稿后主动采用版本；Web修正effective交接入口，接受方案不等于实际交接完成。未编译、类型检查、测试执行或逐端视觉／运行验收。'
    if key.startswith(('caretaker','publisherCollab')) or key in ('adoptionCaretakerInvite','adoptionCaretakerPending','adoptionCaretakers','adoptionCaretakersEmpty'):
        a,w,route='SocialPages.kt','features/ResourcePage.tsx','/invitations/{invitationId}'
        if key in ('adoptionCaretakers','adoptionCaretakersEmpty'):
            w,route='features/WorkflowEditor.tsx','/edit?flow=introduction&kind=pets&id={petId}'
        elif key == 'adoptionCaretakerInvite':
            w,route='features/WorkflowEditor.tsx','/edit?flow=invite&kind=pets&id={petId}&scope=adoption_editor'
        elif key == 'caretakerInvite':
            w,route='features/WorkflowEditor.tsx','/edit?flow=invite&kind=adoptions&id={adoptionId}&scope=caretaker'
        elif key in ('adoptionCaretakerPending','caretakerPending'):
            w,route='features/Records.tsx','/invitations'
        elif key in ('caretakerRevoke','publisherCollabRevoke'):
            w,route='features/WorkflowEditor.tsx','/edit?flow=invitation-action&kind=invitations&id={invitationId}&action=revoke'
        note='2026-10-29按当前邀请与协作实际入口更正源码／路由归属；介绍对象为pet_id，照护对象为adoption_id，邀请决定绑定invitation_id。接受截止不等于已接受授权终止日；角色与资源有效性由服务端复核。未编译、测试执行、逐端视觉或运行验收。'
    if key in collaboration_states:
        state = collaboration_states[key]
        a,w,route=state['native_source'],state['web_source'],state['web_route']
        status='dedicated_state_source_not_runtime_verified'
        note='2026-10-29纳入远端2026-10-28既有协作退出与有限授权稿，保留原740状态；仅核对稳定节点与现行源码归属。'+state['implementation_note']+' 未播放原型、编译、测试执行或逐端视觉／运行验收。'
        representatives[key] = state.get('screenshots_inspected', [])
    rows.append({'key':key,'name':names.get(key,key),'family':group,'design_roots':roots[key],
                 'root_sources':provenance[key],'android_source':android+a if a else None,
                 'web_source':'web-client/src/'+w if w else None,'web_module_route':route,
                 'ios_source':android+a if a else None,'desktop_source':android+a if a else None,
                 'source_note':'Android／iOS／Desktop消费者复用clientUi页面；这是源码归属，不代表对应状态已逐一视觉验收。',
                 'implementation_status':status,'note':note,'remote_screenshots_inspected':representatives.get(key,[]),'runtime_verified':False})

result = {'date':'2026-10-24','updated':'2026-10-29','file_key':base['fileKey'],'task':'AI-20261024-ALL-FIGMA',
 'meaning':'此表是全量设计索引与源码归属追踪，不是全部页面完成、视觉对齐或运行通过的证明。',
 'precedence':['baseline','mobile completion','design closure','logic fix effective roots','legal design','collaboration 2026-10-28 existing remote roots'],
 'counts':{'states':len(rows),'baseline_states':baseline_states,'incremental_states':len(collaboration_states),'contract_gaps':sum(r['implementation_status']=='contract_gap' for r in rows),'runtime_verified':0},'states':rows}
path = root / 'docs/client/figma-implementation-trace-2026-10-24.json'
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--write',action='store_true');args=parser.parse_args()
source=json.dumps(result,ensure_ascii=False,indent=2)+'\n'
if args.write: path.write_text(source)
else: assert path.read_text()==source, '设计追踪已过期，请先检查差异再用--write同步'
for row in rows:
    for key in ['android_source','web_source','ios_source','desktop_source']:
        if row[key]: assert (root/row[key]).is_file(), row[key]
print(result['counts'])

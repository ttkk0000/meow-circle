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
rows = []
for key in sorted(roots):
    group = family(key)
    a, w, route = entries[group]
    if key in ('lostMap','lostAreaMap'):
        a,w,route='MapPage.kt','features/MapPage.tsx','/map'
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
    rows.append({'key':key,'name':names.get(key,key),'family':group,'design_roots':roots[key],
                 'root_sources':provenance[key],'android_source':android+a if a else None,
                 'web_source':'web-client/src/'+w if w else None,'web_module_route':route,
                 'ios_source':android+a if a else None,'desktop_source':android+a if a else None,
                 'source_note':'Android／iOS／Desktop消费者复用clientUi页面；这是源码归属，不代表对应状态已逐一视觉验收。',
                 'implementation_status':status,'note':note,'remote_screenshots_inspected':representatives.get(key,[]),'runtime_verified':False})

result = {'date':'2026-10-24','file_key':base['fileKey'],'task':'AI-20261024-ALL-FIGMA',
 'meaning':'此表是全量设计索引与源码归属追踪，不是全部页面完成、视觉对齐或运行通过的证明。',
 'precedence':['baseline','mobile completion','design closure','logic fix effective roots','legal design'],
 'counts':{'states':len(rows),'contract_gaps':sum(r['implementation_status']=='contract_gap' for r in rows),'runtime_verified':0},'states':rows}
path = root / 'docs/client/figma-implementation-trace-2026-10-24.json'
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--write',action='store_true');args=parser.parse_args()
source=json.dumps(result,ensure_ascii=False,indent=2)+'\n'
if args.write: path.write_text(source)
else: assert path.read_text()==source, '设计追踪已过期，请先检查差异再用--write同步'
for row in rows:
    for key in ['android_source','web_source','ios_source','desktop_source']:
        if row[key]: assert (root/row[key]).is_file(), row[key]
print(result['counts'])

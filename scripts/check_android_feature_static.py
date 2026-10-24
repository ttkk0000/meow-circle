"""只读取源码、合同和本地资源；不编译、不执行测试、不启动应用或网络。"""
from pathlib import Path
import json
import re
import struct
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
android = root / 'kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile'
shared = root / 'kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client'
feature = (shared / 'MobileFeatureStore.kt').read_text(encoding="utf-8")
enum = feature.split('enum class FeaturePage', 1)[1].split('data class FeatureRoute', 1)[0]
pages = set(re.findall(r'\b(\w+)\("', enum))
ui = '\n'.join((android / file).read_text(encoding="utf-8") for file in [
    'AndroidSocialFeatures.kt', 'AndroidCareFeatures.kt', 'AndroidOfferFeatures.kt', 'AndroidAccountFeatures.kt'])
handled = set(re.findall(r'FeaturePage\.(\w+)\s*->', ui))
assert pages == handled, ('缺少具体页面实现', pages - handled, handled - pages)
store = (shared / 'MobileStore.kt').read_text(encoding="utf-8")
store_methods = set(re.findall(r'\bfun\s+(\w+)\s*\(', store))
feature_methods = set(re.findall(r'\bfun\s+(\w+)\s*\(', feature))
all_android = '\n'.join(p.read_text(encoding="utf-8") for p in android.glob('*.kt'))
assert set(re.findall(r'\bstore::(\w+)', all_android)) <= store_methods
assert set(re.findall(r'\bf::(\w+)', all_android)) <= feature_methods
assert 'val features = MobileFeatureStore(api, scope, localWorkspaces)' in store
assert 'MobileStore(' not in (android / 'AndroidMobileApp.kt').read_text(encoding="utf-8")
assert 'TextFieldValue.Saver' not in all_android, '私密正文不能复制进系统恢复 Bundle'
assert 'rememberPrivateEditor' in all_android
assert 'unknown' in feature and 'uncertainRoutes' in feature and 'operationResolved' in feature
assert 'if (receipt.state != "committed"' in store
assert 'directFirstUnread' in store and 'firstUnread' in all_android and 'fromBeginning' in all_android
assert 'persistReplies()' in store and 'uncertainReplies' in store and 'localReplies' in store
assert 'persistDrafts()' in feature and 'safetyReferences' in feature and 'localDrafts' in feature
assert 'mnd-workflows-guest' in (android / 'MndApplication.kt').read_text(encoding="utf-8")
assert 'mnd-replies-guest' in (android / 'MndApplication.kt').read_text(encoding="utf-8")
assert 'composeAudienceValid' in store and 'composeAudienceValid' in all_android
assert 'uploadPrivate' in (shared / 'MobileApi.kt').read_text(encoding="utf-8")
assert 'AndroidImageOrigin.read' in (android / 'AndroidPhotoPicker.kt').read_text(encoding="utf-8")
assert 'AndroidImageOrigin.write' in (android / 'AndroidPhotoPicker.kt').read_text(encoding="utf-8")
for name in ('noto_sans_sc', 'manrope'):
    path = root / f'kmp/androidApp/src/main/res/font/{name}.ttf'
    assert path.read_bytes()[:4] in (b'\x00\x01\x00\x00', b'OTTO')
    assert (root / f'kmp/androidApp/src/main/assets/fonts/{name}-OFL.txt').is_file()
assert 'scope = true' not in all_android
manifest = ET.parse(root / 'kmp/androidApp/src/main/AndroidManifest.xml').getroot()
ns = '{http://schemas.android.com/apk/res/android}'
assert any(p.attrib.get(ns + 'name') == 'android.permission.POST_NOTIFICATIONS' for p in manifest.findall('uses-permission'))
activity = manifest.find('application/activity')
assert ns + 'screenOrientation' not in activity.attrib
assert any(e.attrib.get(ns + 'scheme') == 'mnd' for e in activity.findall('intent-filter/data'))
policy = (android / 'AndroidMobileApp.kt').read_text(encoding="utf-8")
for boundary in ('width >= 680f', 'width >= 980f', 'height >= 480f', 'width >= 1100f', 'readableWidth >= 900f', 'readableWidth >= 1020f', 'WindowInsets.ime', 'SaveableStateProvider', 'AndroidFeatureMaster'):
    assert boundary in policy, boundary
assert 'fontScale.coerceAtLeast(1f)' in policy and '!policy.keyboard' in policy
assert 'enabled = !navigationLocked' in policy
assert 'DateTimeField' in all_android and 'FormPair' in all_android and 'PetChoiceField' in all_android
assert 'status.progress()' in all_android and 'DataRequestPhase.NeedsReconciliation' in all_android
assert 'WindowInfoTracker' in (android / 'AndroidAdaptiveWindow.kt').read_text(encoding="utf-8")
assert 'FoldingFeature.OcclusionType.FULL' in (android / 'AndroidAdaptiveWindow.kt').read_text(encoding="utf-8")
atlas = root / 'kmp/androidApp/src/main/res/drawable-nodpi/mnd_cat_stickers.png'
assert atlas.read_bytes() == (root / 'internal/platform/mobileapi/assets/v1/mnd-cat-stickers-atlas-v1.png').read_bytes()
assert struct.unpack('>II', atlas.read_bytes()[16:24]) == (1536, 1024)
catalog = json.loads((root / 'internal/mobile/catalog.json').read_text(encoding="utf-8"))
assert len(catalog['stickers']) == 24
assert {s['id'] for s in catalog['stickers']} == {f'sticker-{n:02}' for n in range(1,25)}
for p in list(android.glob('*.kt')) + [shared / 'MobileFeatureStore.kt', shared / 'WorkspaceApi.kt']:
    source = p.read_text(encoding="utf-8")
    assert not p.read_bytes().startswith(b'\xef\xbb\xbf'), p
    assert not re.search(r'^(<<<<<<<|=======|>>>>>>>)', source, re.M), p
    assert not any(line.rstrip() != line for line in source.splitlines()), p
print(f'Android 源码静态核对：{len(pages)} 个扩展页面有具体分派，方法引用、单 Store、路由、输入隐私、自适应与 24 张贴纸资源通过。未编译／测试／设备验证。')

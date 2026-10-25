"""只读核对阅读／发布／宠物评论的跨端资源、合同和接线；不执行应用、编译或测试。"""
import hashlib
import json
import re
import struct
from pathlib import Path

root = Path(__file__).resolve().parents[1]
android = root / 'kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile'
client_ui = root / 'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui'
shared = root / 'kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client'
web = root / 'web-client'
guide = json.loads((web / 'public/assets/guides/manifest.json').read_text())
photo = (root / guide['file']).read_bytes()
def jpeg_size(raw):
    assert raw[:2] == bytes.fromhex('ffd8')
    pos=2
    while pos<len(raw):
        assert raw[pos]==255
        while raw[pos]==255:pos+=1
        marker=raw[pos];pos+=1
        if marker in range(208,217) or marker==1:continue
        length=struct.unpack('>H',raw[pos:pos+2])[0]
        assert length>=2 and pos+length<=len(raw)
        if marker in [192,193,194,195,197,198,199,201,202,203,205,206,207]:
            height,width=struct.unpack('>HH',raw[pos+3:pos+7]);return [width,height]
        pos+=length
    raise AssertionError('JPEG frame dimensions absent')
assert jpeg_size(photo)==guide['intrinsic_dimensions']
assert guide['format']=='JPEG'
assert hashlib.sha256(photo).hexdigest() == guide['sha256']
assert photo == (root / guide['android_file']).read_bytes()
assert guide['design_slots']['phone']['dimensions'] == [358, 240]
assert all(guide['design_slots'][name].get('height', guide['design_slots'][name].get('dimensions', [0, 0])[1]) == 360 for name in ['web', 'tabletP', 'tabletL'])
for path in guide['callsites']:
    assert 'first-cat.jpg' in (root / path).read_text(), path
native_reader = (client_ui / 'ReadingPages.kt').read_text()
assert 'maxWidth >= 600.dp' in native_reader and 'Modifier.height(360.dp)' in native_reader
assert 'aspectRatio(358f/240f)' in native_reader and 'ContentScale.Crop' in native_reader
assert not re.search(r'kind\s*=\s*"[^"\n]*:', native_reader), '阅读类型不能破坏既有冒号分隔的恢复路由'
css = (web / 'src/styles.css').read_text()
assert re.search(r'\.reading-cover\s*\{[^}]*height:\s*360px', css)
assert re.search(r'\.task-content\s*\{[^}]*max-width:\s*820px', css)
assert re.search(r'\.task-actions > div\s*\{[^}]*max-width:\s*788px', css)
catalog = json.loads((root / 'internal/platform/mobileapi/reading-drafts.json').read_text())
assert all(not catalog[name] for name in ['operator_name', 'operator_address', 'operator_contact'])
assert {document['type'] for document in catalog['documents']} == {'guide', 'service', 'adoption', 'teen', 'seller', 'privacy', 'inventory', 'sdk', 'providers', 'retention'}
assert all(document['status'] in ['draft', 'reference'] and not document.get('effective_from') for document in catalog['documents'])
assert len({document['key'] for document in catalog['documents']}) == len(catalog['documents'])
paths = json.loads((root / 'docs/api/mobile-v2.openapi.json').read_text())['paths']
for path, method in [('reading', 'get'), ('reading/{key}', 'get'), ('reading/{key}/download', 'get'), ('pets/{id}/comments', 'post'), ('pet_comments', 'get'), ('comment-metrics', 'get'), ('pet_comments/{id}/reaction', 'post'), ('entry_comments/{id}/reaction', 'post')]:
    assert method in paths['/api/v2/' + path], (path, method)
assert 'text/plain' in paths['/api/v2/reading/{key}/download']['get']['responses']['200']['content']
store = (shared / 'MobileStore.kt').read_text()
for token in ['api.richPetComment', '"pet:${destination.id}"', '"pet" -> MobileDestination.Pet(id,true)', '"entry_comments", "pet_comments")', 'commentMetrics = emptyMap()']:
    assert token in store, token
assert 'pet-comments' in (android / 'MobileViewModel.kt').read_text()
assert 'private fun PetDiscussion' in (client_ui / 'ConsumerApp.kt').read_text()
feature = (shared / 'MobileFeatureStore.kt').read_text()
assert '"owner_id" to "$account"' in feature and 'fun selectPublications' in feature
assert 'startsWith("type-")' in feature and 'readingDocument = if (denied(e)) null' in feature
composer = (web / 'src/features/chat.tsx').read_text()
assert '!["entries", "pets"].includes(kind) && draft' in composer
assert 'reply_to:reply??integer(current.values.reply_to)' in re.sub(r'\s+', '', composer)
assert 'onReplyChange' in composer and 'useExpressionLibrary(owner)' in composer
make = (root / 'Makefile').read_text()
compose = (root / 'docker-compose.yml').read_text()
migrations = sorted((root / 'migrations').glob('[0-9][0-9][0-9]_*.sql'))
assert len(migrations) >= 9
assert [int(path.name[:3]) for path in migrations] == list(range(1, len(migrations) + 1))
for migration in migrations:
    assert 'migrations/' + migration.name in compose
assert '$(GO) run ./cmd/migrate' in make
runner = (root / 'cmd/migrate/main.go').read_text()
assert 'filepath.Glob' in runner and 'sort.Strings(paths)' in runner and 'mnd_schema_migrations' in runner
docker = (root / 'Dockerfile').read_text()
assert '/app/meow-worker' in docker and '/app/web-client/dist' in docker
print('阅读／发布只读核对：10类材料、4向指南原图／几何、公共正文和下载合同、评论原键恢复／图片限制、本人筛选与全部顺序迁移入口一致；未执行类型检查、编译、测试、迁移或应用。')

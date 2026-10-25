"""只读检查本批跨端与基础设施接线；不执行应用、编译、测试、迁移或网络请求。"""
import ast
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
def read(path): return (root / path).read_text()

contract = json.loads(read('docs/api/mobile-v2.openapi.json'))
paths = contract['paths']
for suffix in ['', '/lookup', '/disable']:
    assert 'post' in paths['/api/v2/push-devices' + suffix]
assert paths['/api/v2/push-devices/disable']['post']['requestBody']['content']['application/json']['schema']['additionalProperties'] is False

jobs = set(re.findall(r'"([a-z_]+)"', read('internal/mobile/jobs.go').split('case ', 1)[1].split(':', 1)[0]))
script = ast.parse(read('scripts/delivery.py'))
configured = next(ast.literal_eval(item.value) for item in script.body if isinstance(item, ast.Assign) and any(isinstance(target, ast.Name) and target.id == 'JOBS' for target in item.targets))
assert jobs == set(configured)
compose = read('deploy/compose.production.yml')
assert jobs == set(re.findall(r'MND_WORKER_JOB: ([a-z_]+)', compose))
assert jobs == {key.removeprefix('/api/v2/ops/jobs/') for key in paths if key.startswith('/api/v2/ops/jobs/')}
sql = read('migrations/012_worker_checkpoints.sql')
assert jobs == set(re.findall(r"'([a-z_]+)'", sql.split('CHECK (job IN (', 1)[1].split('))', 1)[0]))
for name in ['internal/platform/mobileworker/runner.go', 'internal/platform/mobileapi/jobs.go', 'internal/jobcoord/postgres.go']:
    assert 'mobile.ValidJob(job)' in read(name)
coordinator = read('internal/jobcoord/postgres.go')
assert all(text in coordinator for text in ['pg_try_advisory_lock', 'pg_advisory_unlock', 'mnd_worker_checkpoints', 'context.WithCancel', 'conn.Ping', 'pgx.ConnectConfig'])

postgres = read('internal/store/postgres/mobile.go')
assert 'strpos(record_key' not in postgres
assert postgres.count('where, args := mobilePredicates(f)') == 2
assert all(value in postgres for value in ['ANY($%d::text[])', 'ALL($%d::text[])', 'record_key LIKE', '012_worker_checkpoints.sql'])
modules = set(re.findall(r'^\s*\w+\s+Kind = "([a-z_]+)"', read('internal/mobile/repository.go'), re.M))
index_sql = read('migrations/011_mobile_query_indexes.sql')
assert modules - {"source_declarations"} == set(re.findall(r"'([a-z_]+)'", index_sql.split('FOREACH module IN ARRAY ARRAY[', 1)[1].split('] LOOP', 1)[0]))
source_sql = read('migrations/013_source_declarations.sql')
assert 'mobile_source_declarations(owner_id,id)' in source_sql and 'mobile_source_declarations(owner_id,state,id)' in source_sql
assert 'mobile_source_declarations' in postgres and '013_source_declarations.sql' in postgres
assert 'text_pattern_ops' in index_sql and 'media_ids' in index_sql and 'participants' in index_sql

browser = read('internal/platform/mobileapi/browser.go')
assert 'recent' not in browser and 'browserRefreshContext{}' in browser
identity = read('internal/identity/service.go')
assert all(value in identity for value in ['BrowserRefresh', 'BrowserReplayUntil', 'next.State != "active"', 's.sealAuthReplay', 's.openAuthReplay'])
assert 'M&D auth replay v1' in read('internal/identity/auth_replay.go')

shared = 'kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/'
push = read(shared + 'DevicePushCoordinator.kt')
assert all(value in push for value in ['collectLatest', 'push-devices/lookup', 'push-devices/disable', 'preferencesSaved', 'globalTotal', 'TimeoutCancellationException'])
policy = read('internal/mobile/push_policy.go')
assert 'SessionKey(ctx)' in policy and 'DisableCurrentPush' in policy
assert 'DisablePushDevices(tx, actor, "", true)' in read('internal/mobile/social.go')
native = read('kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidDevicePush.kt')
assert 'devicePush.consent.collectLatest' in native and 'FirebaseMessagingService' in native
manifest = ET.fromstring(read('kmp/androidApp/src/main/AndroidManifest.xml'))
namespace = '{http://schemas.android.com/apk/res/android}'
metadata = {node.attrib[namespace+'name']: node.attrib[namespace+'value'] for node in manifest.iter('meta-data')}
assert metadata['firebase_messaging_auto_init_enabled'] == 'false'
assert metadata['firebase_analytics_collection_deactivated'] == 'true'
svg = ET.fromstring(read('kmp/androidApp/src/main/assets/figma/bell.svg'))
vector = ET.fromstring(read('kmp/androidApp/src/main/res/drawable/ic_mnd_notification.xml'))
assert [node.attrib['d'] for node in svg.iter('{http://www.w3.org/2000/svg}path')] == [node.attrib[namespace+'pathData'] for node in vector.iter('path')]
assert 'MND_APNS_ENABLED: "NO"' in read('kmp/iosApp/project.yml')
assert 'aps-environment' in read('scripts/mobile.py')

desktop = read('kmp/desktopApp/src/main/kotlin/com/ttkk0000/meowcircle/desktop/DesktopProviderAuth.kt')
assert all(text in desktop for text in ['127.0.0.1', 'getFirst("Origin") == endpoint.web', 'challenge.state', 'readNBytes(8193)', 'server.stop(0)', 'client = "web"'])
assert 'modules("jdk.httpserver")' in read('kmp/desktopApp/build.gradle.kts')
bridge = read('web-client/src/core/desktopProvider.ts')
assert 'history.replaceState' in bridge and 'url.hostname !== "127.0.0.1"' in bridge and 'form.method = "POST"' in bridge

s3 = read('internal/mediablob/s3.go')
assert all(value in s3 for value in ['AWS4-HMAC-SHA256', 'X-Amz-Content-Sha256', 'unversioned', 'X-Amz-Delete-Marker', 'start-after', 'io.LimitReader', 'CheckRedirect'])
erasure = read('internal/platform/mobileapi/erasure.go')
assert erasure.index('blobs().Remove') < erasure.index('FinishMediaErasure')
projection = read('internal/mobile/projection.go').split('type ProjectionEvent struct {', 1)[1].split('}', 1)[0]
assert not re.search(r'\b(?:Data|Body|Token|Text|Media)\b', projection)

catalog = json.loads(read('internal/platform/mobileapi/reading-drafts.json'))
keys = ['draft-handover-contract','draft-trial-contract','draft-sale-contract','draft-agency-contract','draft-wuhan-shanghai-admission']
for key in keys:
    document = next(document for document in catalog['documents'] if document['key'] == key)
    assert document['status'] == 'draft' and document['paragraphs'] and not document.get('effective_from')
assert all(not catalog.get(name) for name in ['operator_name','operator_address','operator_contact'])
print(f'续作只读核对：{len(jobs)}作业跨合同／部署一致、设备权限／撤回、会话加密回执、Desktop本机回跳、{len(modules)}表索引、私有存储及5份草案接线；未执行类型检查、编译、测试或应用。')

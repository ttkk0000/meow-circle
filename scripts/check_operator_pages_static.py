"""只读核对业务后台页面、权限边界及回读合同；不运行前后端或回归。"""
from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[1]
def read(path): return (root / path).read_text()
directory = root / 'web-client/src/operator'
sources = list(directory.glob('*.ts*'))
text = '\n'.join(path.read_text() for path in sources)
models = read('web-client/src/operator/models.ts')
app = read('web-client/src/operator/OperatorApp.tsx')
for view in ['dashboard','reviews','reviewDetail','rights','rightsDetail','privacy','guardian','readiness','access','confirm','pending','forbidden','disconnected']:
    assert re.search(r'"'+view+r'"', models), view
    assert re.search(r'"'+view+r'"', app), view
for path in ['operator/OperatorApp.tsx','operations/OpsApp.tsx']:
    assert '<WorkspaceShell' in read('web-client/src/'+path), path
assert 'lazy(() => import("./operator/OperatorApp"))' in read('web-client/src/main.tsx')
assert not re.search(r'\b(?:localStorage|sessionStorage|indexedDB|eval)\b',text)
assert 'https://www.figma.com/api/mcp/asset/' not in text
client = read('web-client/src/operator/client.ts')
for guard in ['credentials: "omit"','cache: "no-store"','referrerPolicy: "no-referrer"','"X-Operator-Token": this.token','epoch !== this.generation','controller.abort()','12_000']:
    assert guard in client,guard
store = read('web-client/src/operator/useOperator.ts')
for guard in ['versionOf(latest) !== pending.version','preparedRef.current','writing.current','phase: "unknown"','phase: "accepted"','phase: "rejected"','pending.checked','acknowledged','roleFor(pending.kind)','selection.current','invite_version: row.version','pagehide']:
    assert guard in store,guard
assert 'setQueues(emptyQueues())' in store
schema = json.loads(read('docs/api/mobile-v2.openapi.json'))
for path,name in [('/ops/review-queue','StaffReviewPage'),('/ops/guardian-invites/{id}','StaffGuardianInviteDetails'),('/ops/reviews/{id}','Record')]:
    operation=schema['paths']['/api/v2'+path]['get']
    assert operation['security']==[{'operatorToken':[]}]
    assert name in json.dumps(operation)
summary = schema['components']['schemas']['StaffReviewItem']['properties']
assert set(summary)=={'id','version','state','type','created_at'}
guardian = schema['components']['schemas']['StaffGuardianInviteDetails']['properties']
assert not {'nonce','token','evidence_ref','media_ids'} & guardian.keys()
reads=read('internal/mobile/operator_reads.go')
assert reads.count('OperatorID(ctx) == ""')==3 and reads.count('return audit(')==3
assert 'v.EvidenceExpiresAt.After(s.Now())' in reads
context=schema['components']['schemas']['OperatorContext']['properties']
assert 'documents' in context and not {'token','consumers','environments'} & context.keys()
print('业务后台只读核对：13类设计页面、双后台公共Shell、独立内存凭据／角色／版本／未知结果回读、最小队列与受限详情合同；未类型检查、编译、测试或运行。')

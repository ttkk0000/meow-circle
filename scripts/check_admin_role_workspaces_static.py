"""只读核对后台角色设计、源码路由及当前合同；不执行应用、类型检查或回归测试。"""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(path): return (root / path).read_text()
policy = read('web-client/src/operator/roleWorkspaces.ts')
defined = re.findall(r'role: "([a-z_]+)"', policy)
assert set(defined) == {'moderator','privacy','identity_verifier','authority_verifier','finance','ops_viewer','ops_oncall','ops_release','ops_admin'}
assert len(defined) == 9
app = read('web-client/src/operator/OperatorApp.tsx')
assert 'workspaceAllows(workspace?.role, view, session.prepared?.kind)' in app
assert app.index('workspaceAllows(workspace?.role, view, session.prepared?.kind)') < app.index('session.loadDetail')
assert 'sections\n' in app and 'session.clearWorkspace()' in app and 'roleTasks.locked' in app
assert 'workspaceQueues(workspace)' in read('web-client/src/operator/QueuePages.tsx')
tasks = read('web-client/src/operator/RoleTasks.tsx')
for guard in ['permitted(taskRole(flow))','writing.current','lookupKey','lookupQuery','stage: "sending"','"unknown"','old.prepared.body','epoch === session.client.epoch']:
    assert guard in tasks, guard
assert 'const band = v.age_band ?? ""' in tasks
assert not re.search(r'\b(?:localStorage|sessionStorage|indexedDB|eval)\b',tasks)
assert 'setLostResult' in read('web-client/src/operator/useOperator.ts')
api = json.loads(read('docs/api/mobile-v2.openapi.json'))
op = api['paths']['/api/v2/ops/authority']['get']
assert op['security'] == [{'operatorToken':[]}]
assert 'AuthoritySnapshot' in json.dumps(op)
assert set(api['components']['schemas']['AuthoritySnapshot']['properties']) == {'exists','record'}
source=read('internal/mobile/authority_read.go')
assert 'tx.ByKey(Authorities, key)' in source and 'audit(tx, operator, "authority.read"' in source
assert 'tx.List' not in source
design=json.loads(read('docs/design/figma-study/admin-roles-2026-10-28/nodes-and-validation.json'))
assert len(design['homeFrames'])==9 and len(design['taskFrames'])==9
for i in range(1,19):
    p=root/f'docs/design/figma-study/admin-roles-2026-10-28/{i:02}.png'
    assert p.read_bytes().startswith(b'\x89PNG'), p
print('后台角色只读核对：9个人员岗位、2类非人工身份边界、按岗导航／任务与角色撤回、精确范围版本读取、18份设计截图；未类型检查、编译、测试或运行。')

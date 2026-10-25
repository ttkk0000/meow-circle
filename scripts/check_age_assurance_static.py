"""核对年龄核验边界及跨端接线；仅解析源码，不运行应用、供应商或测试。"""
from pathlib import Path
import json
import re

root=Path(__file__).resolve().parents[1]
server=(root/'internal/identity/age_assurance.go').read_text()
gateway=(root/'internal/platform/mobileauth/age_gateway.go').read_text()
shared=(root/'kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/AgeAssuranceStore.kt').read_text()
native=(root/'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AgePages.kt').read_text()
web=(root/'web-client/src/features/AgePages.tsx').read_text()
for guard in ['!in.Accepted','mobile.CommandIdentity(in)','mobile.CommandID(ctx)','recordAgeOperation','prior.Command != "age.start"','ageActive(prior.State)','result.Reference != v.Request.Reference','result.Nonce != v.Request.Nonce','v.Request.ExpiresAt.After(s.Now())','ResultFingerprint','mobile.RecordTrustedAgeProof','r.OwnerID != actor','age_unknown']:
    assert guard in server,guard
assert server.index('recordAgeOperation(tx, actor, key, hash, record)',server.index('created = true')) < server.index('s.Age.Create(')
assert server.count('s.Age.Create(')==1
assert '"under14"' not in server and '"14_17"' not in server
for guard in ['configuredHTTPS(endpoint, false)','len(token) < 32','configuredHTTPS(origin, true)','decoder.DisallowUnknownFields()','http.ErrUseLastResponse','if path == "/requests"']:
    assert guard in gateway,guard
for name in ['AgeProviderInfo','AgeAssuranceInput','AgeAssuranceStatus']:
    assert name in shared and name in server
schema=json.loads((root/'docs/api/mobile-v2.openapi.json').read_text())
fields=set(schema['components']['schemas']['AgeAssuranceStatus']['properties'])
assert not fields & {'nonce','reference','evidence_ref','token','document','identity_number','biometric'}
assert set(schema['components']['schemas']['AgeProviderInfo']['properties'])=={'available','name','privacy_url'}
assert 'AgeProviderRequest' not in schema['components']['schemas'] and 'AgeProviderResult' not in schema['components']['schemas']
for path in ['config','current']:
    assert f'/api/v2/auth/age-assurance/{path}' in schema['paths']
stages=set(re.findall(r'\b(\w+)\b',shared.split('enum class AgeStage {',1)[1].split('}',1)[0]))
assert stages <= set(re.findall(r'AgeStage\.(\w+)',native))
for guard in ['api.currentAgeAssurance()','api.queryAgeAssurance(current.id)','AgeStage.ResultUnknown','api.eligibility()','if(!accepted','requestKey = newRequestKey()']:
    assert guard in shared,guard
for guard in ['operationOwner.current','popup.opener = null','revision !== generation.current','returnTo(from)','api.request<Eligibility>("eligibility"','current.state']:
    assert guard in web,guard
assert not re.search(r'\b(?:localStorage|sessionStorage|indexedDB)\b',web+shared)
assert 's.adult(' not in server # 本接口只核验成年结果，不能借用业务命令的放行路径。
assert 'setStage("passed")' not in web # 页面状态只能来自真实服务端结果，不由本地按钮宣称通过。
assert 'flow.begin(' in native and 'flow::query' in native and 'flow::review' in native
assert 'await persist()' in (root/'web-client/src/features/WorkflowEditor.tsx').read_text()
print('年龄核验静态核对：3份最小DTO、原编号账本／并发恢复、服务端可信结果／有效期／替换校验、原生与Web分状态和原任务返回；未运行网关、类型检查或测试。')

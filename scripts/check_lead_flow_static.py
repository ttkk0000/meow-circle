"""只读检查私密线索合同、跨端入口与权限接线；不执行产品源码或回归。"""
from pathlib import Path
import hashlib
import json
import re

root = Path(__file__).resolve().parents[1]
def read(path): return (root / path).read_text(encoding="utf-8")
shared = "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/"
ui = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/"
contract = json.loads(read("docs/api/mobile-v2.openapi.json"))
for action, schema in [("assessment", "LeadAssessmentInput"), ("contact", "LeadContactInput")]:
    endpoint = contract["paths"][f"/api/v2/leads/{{id}}/{action}"]
    assert set(endpoint) == {"post"}
    assert schema in json.dumps(endpoint)
    assert "version" in contract["components"]["schemas"][schema]["properties"]
    assert "user_id" not in contract["components"]["schemas"][schema]["properties"]
source = read("internal/mobile/lead_review.go")
for guard in ['s.command(ctx, actor, "lead.assess"', 's.command(ctx, actor, "lead.contact"', "expected(lead, in.Version)", "parent.OwnerID != actor", "s.leadContext(tx, lead, actor)", "s.directContact(tx, actor, peer)", 'fmt.Sprintf("lead:%d:%s"', 'ContextKind: "lead", ContextID: id', 'body.ContextKind != "lead"', 'writing && parent.State != "active" && parent.State != "matched"']:
    assert guard in source, guard
assessment = source.split("func (s *Service) AssessLead", 1)[1].split("func (s *Service) LeadConversation", 1)[0]
assert "save(tx, Cases" not in assessment and "save(tx, Pets" not in assessment
service = read("internal/mobile/service.go")
assert "s.leadContext(tx, r, actor)" in service
assert "s.leadConversationContext(tx, r, c, actor, false)" in service
social = read("internal/mobile/social.go")
for guard in ["s.leadConversationContext(tx, r, c, actor, true)", "s.leadConversationContext(tx, r, c, actor, false)", "s.leadConversationContext(tx, p, c, actor, true)"]: assert guard in social, guard
assert "s.canRead(tx, parent, p, actor)" in service
assert "s.canRead(tx, k, p, actor)" in read("internal/mobile/trust.go")
for path in ["internal/mobile/inbox.go", "internal/mobile/inbox_snapshot.go"]: assert "s.canRead(tx," in read(path)
assert 'case mobile.Leads:' in read("internal/platform/mobileapi/handler.go")
api = read(shared + "FeatureApi.kt")
for endpoint in ['"leads/$id"', '"leads/$id/assessment"', '"leads/$id/contact"']: assert endpoint in api, endpoint
store = read(shared + "MobileFeatureStore.kt")
assert "FeaturePage.LeadInbox ->" in store and "FeaturePage.Lead ->" in store
assert "capture(p.version)" in store and "api.case(p.referenceId)" in store
native = read(ui + "LeadPages.kt")
for guard in ["leadAssessmentRecorded(lead,parent,minimum)", '"reviewed-${recorded.version}"', "canAssessLead(lead,parent,actor)", "canContactLead(lead,parent,actor)", "f.version(lead.version)", "f.confirm(", "f.api.assessLead", "f.api.contactLead", "HonestStateCard(", "state.leads.next == 0L", "maxWidth >= 600.dp", "360.dp else 170.dp"]: assert guard in native, guard
shell = read(ui + "ConsumerApp.kt")
for guard in ['item(key = "lead-context")', "contextWritable", "compact = leadContext", "editor.composition == null"]: assert guard in shell, guard
assert 'DesignIcon("smile"' in shell
web = read("web-client/src/features/LeadPages.tsx")
for guard in ["page.rows.map", "page.rows.length === 0", "canAssessLead(row, c, owner)", "canContactLead(row, c, owner)", "row.version >= minimum", "row.data.reviewed_by === c.owner_id", 'workflowURL("lead-contact"', 'workflowURL("lead-assessment"', '<Empty title="暂无新的目击线索">']: assert guard in web, guard
chat = read("web-client/src/features/chat.tsx")
for guard in ["lead.data.reference_id === leadCase.data.id", "lead.denied || leadCase.denied", "!contextReady", "compact={isLead}", "canContactLead(lead.data, leadCase.data, owner)"]: assert guard in chat, guard
flows = read("web-client/src/features/workflows.ts")
for flow in ["lead-assessment", "lead-contact"]:
    block = flows.split('"' + flow + '":', 1)[1].split("\n  },", 1)[0]
    assert "replayable: true" in block and 'c.kind !== "leads"' in block
assert 'afterView: "reviewed"' in flows
assert "workflow.afterView" in read("web-client/src/features/WorkflowEditor.tsx")
assert '(min-width: 600px)' in read("web-client/src/styles.css")
trace = json.loads(read("docs/client/figma-implementation-trace-2026-10-24.json"))
keys = {"lostLeadInbox", "lostLeadEmpty", "lostLeadDetail", "lostLeadHelpful", "lostLeadNotMatch", "lostLeadChat"}
for row in trace["states"]:
    if row["key"] in keys:
        assert row["remote_screenshots_inspected"] and not row["runtime_verified"]
        assert row["android_source"].endswith("ConsumerApp.kt" if row["key"] == "lostLeadChat" else "LeadPages.kt")
contexts = [json.loads(p.read_text()) for p in (root / "docs/design/figma-study/lead-flow-2026-10-26").glob("*.json")]
assert len(contexts) == 11 and sum(c["high_fidelity"] for c in contexts) == 9
assert all(c["screenshot_inspected"] for c in contexts if c["high_fidelity"])
assert all(c["temporary_asset_urls_redacted"] for c in contexts)
assert "https://www.figma.com/api/mcp/asset/" not in native + web + shell + chat
for name in ["smile", "state-paw-print"]:
    paths = [root / f"web-client/public/assets/figma/{name}.svg", root / f"kmp/clientUi/src/commonMain/composeResources/files/figma/{name}.svg", root / f"kmp/androidApp/src/main/assets/figma/{name}.svg"]
    assert len({hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}) == 1
regressions = re.findall(r"^func (Test\w+)\(", read("internal/mobile/lead_review_test.go"), re.M)
assert len(regressions) == 7
print("私密线索只读核对：2个版本命令、实时父权限／关系／关闭规则、两端6状态接线、11份上下文与原SVG；7项Go回归仅源码，未执行。")

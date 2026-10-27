"""只读核对线索提交四状态、兼容合同及后台设计证据；不编译或执行产品／回归。"""
from pathlib import Path
import hashlib
import json
import re
import struct
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
def read(path): return (root / path).read_text(encoding="utf-8")
shared = "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/"
ui = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/"
contract = json.loads(read("docs/api/mobile-v2.openapi.json"))
assert set(contract["paths"]["/api/v2/cases/{id}/lead-submissions"]) == {"post"}
assert "LeadSubmissionInput" in json.dumps(contract["paths"]["/api/v2/cases/{id}/lead-submissions"])
assert set(contract["components"]["schemas"]["LeadSubmissionInput"]["properties"]) == {"client_request_id","version","text","media_ids"}
assert "/api/v2/cases/{id}/leads" in contract["paths"]
source = read("internal/mobile/lead_submission.go")
for guard in ['s.command(ctx, actor, "lead.submit"', "in.Version <= 0", "s.interaction(tx, actor)", "s.canRead(tx, Cases, parent, actor)", "expected(parent, version)", 'parent.State != "active" && parent.State != "matched"', "s.attachments(tx, actor, media)", "tx.ByKey(Leads, key)", "RefID: caseID", "Participants: []int64{parent.OwnerID}", 'save(tx, Leads, &row', "s.notify(tx, parent.OwnerID"]: assert guard in source, guard
assert "Public: true" not in source and "save(tx, Cases" not in source and "save(tx, Pets" not in source
assert "s.privateLead(tx, actor, caseID, 0, key, text, media)" in read("internal/mobile/rescue.go")
assert 'case "lead-submissions":' in read("internal/platform/mobileapi/handler.go")
assert 'durablePost("cases/$id/lead-submissions",input,input.clientRequestId)' in read(shared+"FeatureApi.kt")
native = read(ui+"LeadSubmissionPages.kt")
for guard in ['f.state.value', 'f.field("lead_stage") == "photos"', 'f.edit("lead_stage","edit")', "f.confirm(", "f.submitConfirmation()", "f.api.submitLead(source.id,input)", '"submitted-${received.version}"', "state.unconfirmedResult", "FeaturePage.Recovery", "f.adoptLeadVersion()", "f.importLegacyLead()", "360.dp else 260.dp"]: assert guard in native, guard
store = read(shared+"MobileFeatureStore.kt")
for guard in ["FeaturePage.LeadSubmit ->", "fun adoptLeadVersion()", "fun importLegacyLead()", "private fun adoptReadVersion", "form.copy(baseVersion=version)", "state.photos[old].orEmpty()", "!it.uploading"]: assert guard in store, guard
root_ui = read(ui+"FeaturePages.kt")
assert "FeaturePage.LeadInbox, FeaturePage.Lead, FeaturePage.LeadSubmit" in root_ui
assert "takeUnless { s.route?.page == FeaturePage.LeadSubmit" in root_ui
assert 's.route?.page == FeaturePage.Appointment && f.field("appointment_stage") == "cancel"' in root_ui
status = read(shared+"LeadStatus.kt")
assert "leadSubmissionRecorded" in status and "lead.ownerId == actor" in status and "lead.version >= minimum" in status
assert "leadSubmissionRecorded(lead,actor,submittedVersion)" in read(ui+"LeadPages.kt")
web = read("web-client/src/features/WorkflowEditor.tsx")
assert web.count("<Attachments") == 1, "照片编辑与恢复逻辑必须复用同一个入口"
compact = re.sub(r"\s+", "", web)
for guard in ['LeadReviewTask','LeadPhotoTask','LeadFailureTask','workflow.preview||flow==="lead"','draft?.uncertain&&!busy&&!prepared','flow==="lead"&&leadConfirmed','fingerprint!==publicationFingerprint(latest.current)','onSkip=','photos:[]']: assert guard in compact, guard
assert 'disabled={busy || !canEdit}' in read("web-client/src/features/LeadSubmissionPages.tsx")
assert 'query.get("view") === "submitted"' in read("web-client/src/features/LeadPages.tsx")
assert 'row.owner_id === owner' in read("web-client/src/features/LeadPages.tsx")
assert 'schema: "LeadSubmissionInput"' in read("web-client/src/features/workflows.ts")
assert 'afterView: "submitted"' in read("web-client/src/features/workflows.ts")
assert "disabled = false" in read("web-client/src/components/ui.tsx")
contexts = [json.loads(p.read_text()) for p in (root/"docs/design/figma-study/lead-submission-2026-10-26").glob("*.json")]
assert len(contexts) == 6 and all(c["high_fidelity"] and c["screenshot_inspected"] and c["temporary_asset_urls_redacted"] for c in contexts)
trace = json.loads(read("docs/client/figma-implementation-trace-2026-10-24.json"))
keys = {"lostLeadPhoto","lostLeadReview","lostLeadFailure","lostLeadSent"}
assert len([r for r in trace["states"] if r["key"] in keys and r["remote_screenshots_inspected"] and not r["runtime_verified"]]) == 4
asset = "web-client/public/assets/figma/chevron-left.svg"
data = (root/asset).read_bytes();svg=ET.fromstring(data)
assert [svg.attrib["width"],svg.attrib["height"],svg.attrib["viewBox"]] == ["24","24","0 0 24 24"]
for folder in ["kmp/clientUi/src/commonMain/composeResources/files/figma", "kmp/androidApp/src/main/assets/figma"]: assert hashlib.sha256(data).digest() == hashlib.sha256((root/folder/"chevron-left.svg").read_bytes()).digest()
admin = root/"docs/design/figma-study/admin-2026-10-26"
proof = json.loads((admin/"nodes-and-validation.json").read_text())
assert proof["screen_count"] == 13 and proof["component_instances"] == 400
assert proof["prototype"]["navMatched"] == 104 and proof["prototype"]["bodyMatched"] == 29 and proof["prototype"]["click_navigation"] == 126
assert not proof["prototype"]["missing"] and not proof["prototype"]["iconMissing"] and not proof["prototype"]["selfInvalid"]
assert not proof["validation"]["text_outside_screen_or_parent"] and proof["validation"]["full_screen_image_fills"] == 0
for screen in proof["screens"]:
    png = (admin/screen["screenshot"]).read_bytes()
    assert png[:8] == b"\x89PNG\r\n\x1a\n" and struct.unpack(">II",png[16:24]) == (1440,1000)
    assert screen["screenshot_inspected"] and not screen["runtime_verified"]
    assert screen["mode"]["VariableCollectionId:14:6"] == "56:0"
assert len(re.findall(r"^func Test",read("internal/mobile/lead_submission_test.go"),re.M)) == 4
assert read("kmp/shared/src/commonTest/kotlin/com/ttkk0000/meowcircle/client/LeadSubmissionStatusTest.kt").count("@Test fun") == 2
print("线索提交／后台设计只读核对：版本化原请求、旧入口兼容、同稿四状态、48根分派、真实结果门槛；13稿／400组件实例／126原型入口／13截图；6回归仅源码，未执行。")

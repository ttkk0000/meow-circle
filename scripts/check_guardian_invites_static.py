"""只读核对监护邀请、关系核实和明确授权的源码边界；不运行测试或应用。"""
from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[1]
def read(path): return (root / path).read_text(encoding="utf-8")
identity = read("internal/identity/guardian_invites.go")
domain = read("internal/mobile/guardian_invites.go")
guardian = read("internal/mobile/guardian.go")
handler = read("internal/platform/mobileapi/guardian_invites.go")
shared = read("kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/GuardianInviteStore.kt")
native = read("kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/GuardianPages.kt")
web = read("web-client/src/features/GuardianPages.tsx")
main = read("web-client/src/main.tsx")
operator = "\n".join(path.read_text() for path in sorted((root / "web-client/src/operator").glob("*.ts*")))
schema = json.loads(read("docs/api/mobile-v2.openapi.json"))
for name in ["GuardianInviteInfo", "GuardianInviteStart", "GuardianInviteClaim", "GuardianInviteStatus", "GuardianInvitePage"]:
    assert name in identity and name in shared and name in schema["components"]["schemas"], name
assert set(schema["components"]["schemas"]["GuardianInviteStatus"]["properties"]) == {
    "id", "version", "state", "policy_version", "expires_at", "guardian_record_id", "share_url", "mine", "can_cancel"
}
for guard in ['hmac.New(sha256.New', 'guardian-invite:', 'guardian-current:', 'guardianReplay', 'op.PayloadHash != hash', 'actorUpdate(ctx, actor', 'mobile.CommandID(ctx)', 'eligibility.RequireAdult(tx, actor)', 'r.OwnerID == actor', 'r.State != "guardian_invite_open"', 'r.Participants = []int64{actor}', 'v.Nonce = ""', '/app/guardian-invite#code=', '15 * time.Minute', '7 * 24 * time.Hour']:
    assert guard in identity, guard
claim = identity.split("func (s *Service) ClaimGuardianInvite", 1)[1].split("func (s *Service) CancelGuardianInvite", 1)[0]
assert 'mobile.Guardians' not in claim and 'Scopes' not in claim
for guard in ['r.OwnerID != in.ChildID', 'expected(r, in.InviteVersion)', 'v.ClaimedBy != in.GuardianID', 'v.PolicyVersion != in.PolicyVersion', 'v.ExpiresAt.After(now)', 'audit(tx, operator']:
    assert guard in domain, guard
for guard in ['pendingGuardianInvite(tx, in', 'State: "verified_pending_consent"', 'Scopes: []string{}', 'invitation.State = "guardian_relation_verified"', 'ConsumePurpose(tx, actor', 'writable(tx, Guardians, id, actor, in.Version)']:
    assert guard in guardian, guard
assert 'doc.Status != "published"' in handler and 'doc.SHA256' in handler
assert 'case "age-evidence", "guardian-evidence", "guardian-invites":' in read("internal/platform/mobileapi/ops_auth.go")
assert 'in.PolicyVersion != info.PolicyVersion' in read("internal/platform/mobileapi/handler.go")
protection = read("internal/mobile/qualification.go").split("func (s *Service) directContact", 1)[1].split("// The identity module", 1)[0]
assert 'proof.AgeBand != "adult"' in protection and 'proof.AgeValidUntil.After(s.Now())' in protection
assert 'if connected' in protection and 'Denied("contact_restricted")' in protection
social = read("internal/mobile/social.go")
assert social.count('s.directContact(tx,') == 3
for method in ["Conversation", "ConversationAction", "Message"]:
    section = social.split(f"func (s *Service) {method}(", 1)[1].split("\nfunc ", 1)[0]
    assert 's.directContact(tx,' in section, method
assert '"block", "unblock", "decline", "withdraw", "remove"' in social
for source in [shared, native, web, operator]:
    assert not re.search(r'\b(?:localStorage|sessionStorage|indexedDB|rememberSaveable)\b', source)
for guard in ['originalKey', 'unknown=true', 'api.operation(key)', 'clientRequestId=it']:
    assert guard in shared, guard
assert 'record.ownerId==account.user?.id' in native and 'SensitiveIntent.Guardian' in native
assert 'scope="transient:guardian"' in native
assert 'record.owner_id === flow.owner' in web and 'PurposeCheck' in web and 'guardian.decision' in web
assert 'pending?.owner === owner' in web and 'operations/${original.key}' in web
assert 'new URLSearchParams(location.hash.slice(1))' in main
assert main.index('history.replaceState(null') < main.index('createRoot(root).render')
assert 'handoff.code = ""' in web
assert '"identity_verifier"' in operator and '"GuardianEvidenceInput"' in operator
assert 'credentials: "omit"' in operator and '"X-Operator-Token": this.token' in operator
routes = schema["paths"]
for path in ["/guardians/invites/config", "/guardians/invites", "/guardians/invites/claim", "/guardians/invites/{id}", "/guardians/invites/{id}/cancel", "/ops/guardian-invites"]:
    assert "/api/v2" + path in routes, path
print("监护流程静态核对：5份最小DTO、短时单次邀请／原编号恢复、独立关系核实、监护人范围授权／撤回、公共原生与Web专用页面、具名运营角色；未运行核验、类型检查或测试。")

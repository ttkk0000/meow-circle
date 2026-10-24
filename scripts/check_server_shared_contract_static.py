"""Read-only cross-language contract checks. No Go/Kotlin execution or type checking."""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
shared = root / "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client"
schemas = json.loads((root / "docs/api/mobile-v2.openapi.json").read_text(encoding="utf-8"))["components"]["schemas"]
classes = {}
for file in shared.glob("*.kt"):
    text = file.read_text(encoding="utf-8")
    for match in re.finditer(r"\bdata class\s+(\w+)\s*\(", text):
        start, pos, depth, quoted, escape = match.end(), match.end(), 1, False, False
        while depth and pos < len(text):
            c = text[pos]
            if quoted:
                if escape: escape = False
                elif c == "\\": escape = True
                elif c == '"': quoted = False
            elif c == '"': quoted = True
            elif c == "(": depth += 1
            elif c == ")": depth -= 1
            pos += 1
        fields = re.findall(r'(?:@SerialName\("([^"]+)"\)\s*)?val\s+(\w+)\s*:', text[start:pos-1])
        classes[match.group(1)] = {wire or name for wire, name in fields}

mapping = {
    "CommentMetrics":"CommentMetrics", "ReadingFact":"ReadingFact", "ReadingDocument":"ReadingDocument", "ReadingCatalog":"ReadingCatalog",
    "Entry":"EntryBody", "Pet":"PetBody", "Preference":"PreferenceBody", "Subscription":"SubscriptionBody",
    "ContentSubscriptions":"ContentSubscriptions", "ContentSubscriptionInput":"ContentSubscriptionInput", "GroupDirectoryBody":"GroupDirectoryBody", "GroupJoinInput":"GroupJoinInput",
    "EntryInput":"EntryInput", "PetInput":"PetInput", "AdoptionInput":"AdoptionInput", "AdoptionAction":"AdoptionActionInput",
    "CommentInput":"CommentInput", "MessageInput":"SendMessageInput", "PasswordInput":"PasswordLoginInput",
    "AccountInput":"AccountInput", "ReauthInput":"ReauthInput", "PurposeScope":"PurposeScope",
    "Eligibility":"Eligibility", "PublicationDeclaration":"PublicationDeclaration", "PublicationPreview":"PublicationPreview",
    "AppointmentInput":"AppointmentInput", "AppointmentAction":"AppointmentAction", "AppointmentTerms":"AppointmentTerms",
    "ContractInput":"ContractInput", "ContractTerms":"ContractTerms", "ReturnInput":"ReturnInput", "ReturnTerms":"ReturnTerms",
    "OfferInput":"OfferInput", "Offer":"OfferBody", "Fee":"Fee", "ClaimInput":"ClaimInput", "CaseInput":"CaseInput",
    "CareInput":"CareInput", "GroupInput":"GroupInput", "GroupAction":"GroupAction", "InviteInput":"InviteInput", "FriendInput":"FriendInput",
    "ReviewInput":"ReviewInput", "GuardianDecision":"GuardianDecision", "DataRequestInput":"DataRequestInput",
    "DataRequestStatus":"DataRequestStatus", "RightsInput":"RightsInput", "RightsReceipt":"RightsReceipt",
    "PushDeviceInput":"PushDeviceInput", "PushDeviceReceipt":"PushDeviceReceipt", "Credential":"ProviderCredential",
    "OAuthStart":"ProviderStart", "OAuthFinish":"ProviderFinish", "OAuthResult":"ProviderResult", "ResolveInput":"ProviderResolve",
    "Unread":"InboxSummary", "EmailInput":"EmailAuthInput", "EmailOTPInput":"EmailOTPInput", "OTPInput":"PhoneOTPInput", "Feedback":"FeedbackInput",
    "StateInput":"VersionedAction",
    "InboxSnapshotInput":"InboxSnapshotInput", "InboxReadAllInput":"InboxReadAllInput",
    "InboxReadSnapshot":"InboxReadSnapshot", "InboxReadAllResult":"InboxReadAllResult",
    "Case":"CaseBody", "Subscription":"SubscriptionBody",
}
for server, native in mapping.items():
    assert native in classes and server in schemas, (server,native)
    assert classes[native] == set(schemas[server]["properties"]), (server,native,classes[native] ^ set(schemas[server]["properties"]))
assert "X-Admin-Key" not in (root / "internal/platform/mobileapi/handler.go").read_text(encoding="utf-8")
api = (shared / "MobileApi.kt").read_text(encoding="utf-8")
assert all(token in api for token in ("result_unknown", "sessionMutex", "reconcileOperation", "NonCancellable", "safetySubmitSecure"))
assert "getValue(\"id\").jsonPrimitive.long" in api
assert "data_request.submit" in (root / "internal/mobile/purpose.go").read_text(encoding="utf-8")
print(f"Static cross-language check: {len(mapping)} DTO pairs match; private commands and recovery guards present. No type checking or tests executed.")

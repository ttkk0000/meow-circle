"""Static documentation exporter. Reads Go DTO declarations; never builds/runs Go.

Run from the repository root. Only writes docs/api/mobile-v2.openapi.json.
Business validations and ACLs remain authoritative in the feature services.
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
definitions = {}
for folder in ("internal/mobile", "internal/identity"):
    for source in sorted((ROOT / folder).glob("*.go")):
        if source.name.endswith("_test.go"):
            continue
        text = source.read_text(encoding="utf-8-sig")
        for match in re.finditer(r"(?ms)^type (\w+) struct\s*\{(.*?)^\}", text):
            # Single-line embedded structs are handled below, not allowed to
            # consume the following declaration's closing brace.
            if "}" in match.group(2).splitlines()[0]:
                continue
            definitions[match.group(1)] = match.group(2)
        for name, embedded in re.findall(r"(?m)^type (\w+) struct\{\s*(\w+)\s*\}", text):
            definitions[name] = embedded


def ref(name):
    return {"$ref": "#/components/schemas/" + name}


def field_schema(go_type):
    go_type = go_type.removeprefix("mobile.")
    if go_type.startswith("*"):
        return {"anyOf": [field_schema(go_type[1:]), {"type": "null"}]}
    if go_type.startswith("[]"):
        return {"type": "array", "items": field_schema(go_type[2:])}
    if go_type.startswith("map["):
        return {"type": "object", "additionalProperties": field_schema(go_type.split("]", 1)[1])}
    return {
        "string": {"type": "string"}, "Kind": {"type": "string"},
        "bool": {"type": "boolean"}, "int": {"type": "integer"},
        "int64": {"type": "integer", "format": "int64"},
        "float64": {"type": "number"}, "time.Time": {"type": "string", "format": "date-time"},
        "json.RawMessage": {"type": "object"}, "domain.User": ref("User"),
    }.get(go_type, ref(go_type))


schemas = {}


def dto(name):
    if name in schemas:
        return ref(name)
    if name not in definitions:
        raise ValueError("Unknown DTO: " + name)
    properties = {}
    for line in definitions[name].splitlines():
        field = re.match(r'\s*\w+\s+(\S+)\s+`json:"([^",]+)(?:,[^"]*)?"`', line)
        if field:
            typ, key = field.groups()
            if key != "-":
                properties[key] = field_schema(typ)
        elif re.fullmatch(r"\s*(?:mobile\.)?(RichContent|CommandMeta)\s*", line):
            embedded = line.strip().removeprefix("mobile.")
            dto(embedded)
            properties.update(schemas[embedded]["properties"])
    schemas[name] = {"type": "object", "additionalProperties": False, "properties": properties}
    return ref(name)


schemas["User"] = {"type": "object", "properties": {
    key: {"type": "integer" if key == "id" else "string"}
    for key in ("id", "username", "nickname", "avatar_url", "bio", "created_at")}}
schemas["Envelope"] = {"type": "object", "properties": {"ok": {"type": "boolean"}, "data": {}, "error": {"type": "string"}, "error_code": {"type": "string"}, "request_id": {"type": "string"}}}
schemas["Record"] = {"type": "object", "properties": {
    "id": {"type": "integer"}, "owner_id": {"type": "integer"}, "version": {"type": "integer"},
    "reference_id": {"type": "integer"}, "state": {"type": "string"}, "data": {"type": "object"},
    "media_ids": {"type": "array", "items": {"type": "integer"}}, "created_at": {"type": "string"}, "updated_at": {"type": "string"}}}
paths = {}


def operation(path, method, summary, body=None, public=False, staff=False, response="Envelope"):
    params = []
    for name in re.findall(r"\{(\w+)\}", path):
        params.append({"name": name, "in": "path", "required": True, "schema": {"type": "integer", "minimum": 1}})
    item = {"summary": summary, "operationId": method + "_" + re.sub(r"\W+", "_", path).strip("_"),
            "security": [{"operatorToken": []}] if staff else ([{}, {"bearerAuth": []}] if public else [{"bearerAuth": []}]),
            "responses": {str(code): {"description": description, "content": {"application/json": {"schema": ref(response if code == 200 else "Envelope")}}}
                          for code, description in ((200, "Success"), (400, "Invalid DTO"), (401, "Authentication required"), (403, "Forbidden"), (404, "Not found"), (409, "Reload current version"), (429, "Rate limited"), (503, "Provider unavailable"))}}
    if params:
        item["parameters"] = params
    if body:
        item["requestBody"] = {"required": True, "content": {"application/json": {"schema": dto(body) if isinstance(body, str) else body}}}
    paths.setdefault("/api/v2" + path, {})[method] = item
    return item


readable = "pets offers entries entry_comments entry_reactions friendships cases claims leads subscriptions appointments adoptions care_updates groups group_messages conversations messages invitations notifications preferences expressions drafts media reviews visit_feedback".split()
for kind in readable:
    public = kind in ("pets", "offers", "entries", "cases", "entry_comments", "entry_reactions")
    op = operation("/" + kind, "get", "List visible " + kind, public=public)
    op["parameters"] = [{"name": name, "in": "query", "schema": {"type": typ}}
                        for name, typ in (("cursor", "integer"), ("limit", "integer"), ("reference_id", "integer"), ("owner_id", "integer"), ("space", "string"), ("state", "string"), ("q", "string"), ("city", "string"), ("species", "string"), ("latitude", "number"), ("longitude", "number"), ("radius_km", "number"))]
    operation("/" + kind + "/{id}", "get", "Read visible " + kind, public=public)
for kind, name in (("pets", "PetInput"), ("entries", "EntryInput"), ("cases", "CaseInput"), ("offers", "OfferInput")):
    operation("/" + kind, "post", "Create " + kind, name)
    operation("/" + kind + "/{id}", "patch", "Replace owned aggregate using current version", name)
for kind in ("pets", "offers", "entries", "entry_comments", "drafts"):
    operation("/" + kind + "/{id}", "delete", "Remove owned aggregate", "StateInput")
for kind, name in (("groups", "GroupInput"), ("conversations", "ConversationInput"), ("invitations", "InviteInput"), ("adoptions", "AdoptionInput"), ("appointments", "AppointmentInput"), ("reviews", "ReviewInput"), ("visit_feedback", "Feedback")):
    operation("/" + kind, "post", "Create " + kind, name)
for kind, name in (("subscriptions", "SubscriptionInput"), ("preferences", "SettingsInput"), ("expressions", "SettingsInput"), ("drafts", "SettingsInput")):
    operation("/" + kind, "put", "Save owned settings with version", name)
for action in ("request", "accept", "decline", "withdraw", "remove", "block", "unblock"):
    operation("/friendships/" + action, "post", "Friend " + action, "FriendInput")
for kind, actions, name in (
    ("claims", "match reject request_supplement supplement resubmit withdraw dispute confirm_handover", "StateInput"),
    ("adoptions", "request_supplement supplement approve reject withdraw resubmit resume_candidate propose_trial confirm_trial decline_trial confirm_handover support request_return", "AdoptionAction"),
    ("appointments", "propose restore confirm decline withdraw_change cancel expire safety_hold", "AppointmentAction"),
    ("groups", "directory notice transfer accept_transfer decline_transfer revoke_transfer dissolve leave remove_member mute", "GroupAction"),
    ("conversations", "accept decline", "StateInput"), ("invitations", "accept decline revoke", "StateInput"),
    ("notifications", "read", "StateInput")):
    for action in actions.split():
        operation("/" + kind + "/{id}/" + action, "post", kind + " " + action, name)
for path, name in (("pets/{id}/introduction", "IntroductionInput"), ("entries/{id}/comments", "CommentInput"), ("cases/{id}/claims", "ClaimInput"), ("cases/{id}/close", "StateInput"), ("adoptions/{id}/care_updates", "CareInput")):
    operation("/" + path, "post", path, name)
for kind in ("groups", "conversations"):
    operation("/" + kind + "/{id}/messages", "post", "Send rich message; retry using the same idempotency key", "MessageInput")
    operation("/" + kind + "/{id}/read", "post", "Explicit read receipt", {"type": "object", "properties": {"through_message_id": {"type": "integer"}}, "required": ["through_message_id"], "additionalProperties": False})
operation("/entries/{id}/reaction", "post", "Set desired reaction state", {"type": "object", "properties": {"liked": {"type": "boolean"}}, "required": ["liked"], "additionalProperties": False})
operation("/cases/{id}/leads", "post", "Submit a private lead", {"type": "object", "properties": {"text": {"type": "string"}, "media_ids": {"type": "array", "items": {"type": "integer"}}}, "additionalProperties": False})
operation("/reviews/{id}/supplement", "post", "Supplement a needs_info review using the original client request ID", "StateInput")
for kind in ("pets", "offers"):
    operation("/" + kind + "/{id}/verifications", "get", "Version-scoped public verification summaries", public=True)
for path, name in (("otp", "OTPInput"), ("phone", "PhoneLogin"), ("password", "PasswordInput"), ("reauth", "ReauthInput"), ("oauth-start", "OAuthStart"), ("oauth-finish", "OAuthFinish"), ("oauth-resolve", "ResolveInput")):
    operation("/auth/" + path, "post", "Authentication: " + path, name, public=path != "reauth")
for action in ("phone", "password", "unlink", "email"):
    operation("/auth/account/" + action, "post", "Account action requires a single-use reauth grant", "AccountInput")
operation("/auth/refresh", "post", "Rotate both tokens; expired access token goes in body", {"type": "object", "properties": {key: {"type": "string"} for key in ("access_token", "refresh_token", "device")}, "additionalProperties": False}, public=True)
operation("/auth/recover", "post", "Recover using the recover-purpose OTP", {"type": "object", "properties": {key: {"type": "string"} for key in ("phone", "code", "password")}, "additionalProperties": False}, public=True)
operation("/auth/logout", "post", "Revoke current or all sessions", {"type": "object", "properties": {"all": {"type": "boolean"}}, "additionalProperties": False})
operation("/auth/capabilities", "get", "Only configured login providers", public=True)
operation("/auth/methods", "get", "Masked phone and linked provider record IDs")
operation("/profile", "get", "Own profile and four-theme settings")
operation("/profile", "patch", "Update own profile and public avatar", "ProfileInput")
inbox = operation("/inbox-summary", "get", "Unread counts, first unread message IDs and stranger requests")
inbox["responses"]["200"]["content"]["application/json"]["schema"] = {"allOf": [ref("Envelope"), {"type": "object", "properties": {"data": dto("Unread")}}]}
operation("/users/{id}", "get", "Public user profile; phone and credentials omitted", public=True)
metrics_op = operation("/entry-metrics", "get", "ACL-aware reaction and comment counts for up to 50 entries", public=True)
metrics_op["parameters"] = [{"name": "ids", "in": "query", "required": True, "schema": {"type": "string"}, "description": "Comma-separated entry IDs"}]
operation("/expression-catalog", "get", "128 Unicode emoji and 24 static cat stickers", public=True)
operation("/ops/reviews", "get", "Staff review queue; cursor parameter", staff=True)
operation("/ops/reviews/{id}", "post", "Staff decision and narrow verification expiry", "ReviewDecision", staff=True)
operation("/ops/claim-disputes/{id}", "post", "End disputed match without declaring ownership", "StateInput", staff=True)
for path, keys in (("nearby-alerts", ("case_id", "cursor")), ("case-watch-alerts", ("case_id", "cursor")), ("provider-revocations", ("cursor",))):
    operation("/ops/" + path, "post", "Process a bounded, retryable operations batch", {"type": "object", "properties": {key: {"type": "integer"} for key in keys}, "additionalProperties": False}, staff=True)
operation("/ops/outbox", "get", "Pending domain events, excluding provider credentials", staff=True)

upload = operation("/media", "post", "One JPEG/PNG, 10 MiB, 20 MP; explicit public/private scope")
upload["parameters"] = [{"name": "Idempotency-Key", "in": "header", "required": True, "schema": {"type": "string", "minLength": 8, "maxLength": 100}}]
upload["requestBody"] = {"required": True, "content": {"multipart/form-data": {"schema": {"type": "object", "required": ["file"], "properties": {"file": {"type": "string", "format": "binary"}, "scope": {"type": "string", "enum": ["private", "public"], "default": "private"}}}}}}
media = operation("/media/{id}/content", "get", "ACL checked on every read; named evidence-reader role only grants non-expired private evidence", public=True)
media["responses"]["200"] = {"description": "Image", "content": {"image/jpeg": {"schema": {"type": "string", "format": "binary"}}, "image/png": {"schema": {"type": "string", "format": "binary"}}}}

# Policy, operation-recovery and restricted-access contracts added 2026-10-22.
def typed(path, method, summary, body=None, result=None, public=False, staff=False, access=None):
    item = operation(path, method, summary, body, public=public, staff=staff)
    if access:
        item["security"] = [{access: []}]
    if result:
        item["responses"]["200"]["content"]["application/json"]["schema"] = {
            "allOf": [ref("Envelope")], "properties": {"data": dto(result)}}
    return item

dto("ContentSubscriptions")
dto("GroupDirectoryBody")
typed("/content-subscriptions", "get", "Private followed authors and watched cases; no audience privileges")
typed("/content-subscriptions", "post", "Versioned durable update of content subscriptions", "ContentSubscriptionInput")
typed("/group-directory", "get", "Public group introduction projection without member identities", public=True)
typed("/group-directory/{id}", "get", "Read the current public group introduction", public=True)
typed("/group-directory/{id}/request", "post", "Apply to the current group owner using an original request ID", "GroupJoinInput")
paths["/api/v2/pets"]["get"].setdefault("parameters", []).append({"name":"adoption","in":"query","schema":{"type":"boolean"},"description":"Only open adoption introductions; original ACL still applies"})

typed("/inbox/read-all/snapshot", "post", "Prepare an immutable 10-minute read-all snapshot without acknowledging tasks", "InboxSnapshotInput", "InboxReadSnapshot")
typed("/inbox/read-all", "post", "Acknowledge only the prepared snapshot; newer messages remain unread", "InboxReadAllInput", "InboxReadAllResult")

typed("/eligibility", "get", "Server qualification; unknown evidence denies outward actions", result="Eligibility")
item = typed("/operations/{key}", "get", "Look up the original command; not-found never authorizes a new key", result="Operation")
item["parameters"][0]["schema"] = {"type":"string","minLength":8,"maxLength":100}
typed("/entries/preview", "post", "Bind declaration to exact content and media versions", "EntryInput", "PublicationPreview")
for path, body in (("email-otp","EmailOTPInput"),("email","EmailInput"),("register","EmailInput"),("email-recover","EmailInput")):
    typed("/auth/"+path,"post","Explicit purpose email authentication: "+path,body,public=True)
typed("/auth/verify-phone","post","Consume proof for the currently bound phone",{"type":"object","additionalProperties":False,"properties":{"code":{"type":"string"}}})
for action in ("phone","password","unlink","email"):
    typed("/auth/account/"+action+"/scope","post","Compute server scope; no credential stored in grant","AccountInput","PurposeScope")
typed("/push-devices","post","Register or disable this account's device; token stays private","PushDeviceInput","PushDeviceReceipt")
typed("/guardians","get","Own verified guardian relationships")
typed("/guardians/{id}","get","Read a participant relationship without verifier evidence")
typed("/guardians/{id}/scope","post","Scope a specific verified relationship",result="PurposeScope")
typed("/guardians/{id}/decision","post","Explicit consent or withdrawal; withdrawal needs no current-phone gate","GuardianDecision")
typed("/capability","post","Issue a server-generated owner-bound data request capability")
typed("/data-requests/scope","post","Scope the selected data and query capability","DataRequestInput","PurposeScope")
typed("/data-requests","post","Erase selected fields, close account, or request matters assistance","DataRequestInput")
typed("/data-requests/{id}","get","Restricted status after account closure",result="DataRequestStatus",access="requestAccess")
item=typed("/data-requests/requests/{key}","get","Recover a lost submission using its original request ID",result="DataRequestStatus",access="requestAccess")
item["parameters"][0]["schema"]={"type":"string","minLength":8,"maxLength":100}
typed("/data-requests/{id}/revoke-access","post","Revoke only this request's query capability",access="requestAccess")
typed("/safety/session","post","Guest safety/rights session; no login or phone required",result="SafetyAccess",public=True)
for path,body in (("contact-otp","EmailOTPInput"),("contact-verify","EmailInput")):
    typed("/safety/"+path,"post","Verify the session's contact for a formal notice",body,access="safetyAccess")
typed("/safety/scope","post","Scope a logged-in formal statement","RightsInput","PurposeScope")
item=typed("/safety/tickets","post","Guest safety/privacy or verified formal notice/counterstatement/supplement","RightsInput","RightsReceipt",access="safetyAccess")
item["security"].append({"safetyAccess":[],"bearerAuth":[]})
typed("/safety/tickets/{id}","get","Minimal receipt, without target existence or private identities",result="RightsReceipt",access="safetyAccess")
for action in ("propose","accept","decline","scope"):
    typed("/adoptions/{id}/contracts/"+action,"post","Versioned contract "+action,"ContractInput","PurposeScope" if action=="scope" else None)
for action in ("propose","accept","decline","withdraw","handover"):
    typed("/adoptions/{id}/returns/"+action,"post","Versioned safe return "+action,"ReturnInput")
for path,body in (("age-evidence","AgeEvidenceInput"),("guardian-evidence","GuardianEvidenceInput"),("authority","AuthorityInput"),("deliveries","DeliveryClaim"),("revenue","RevenueInput")):
    typed("/ops/"+path,"post","Named role-checked operation: "+path,body,staff=True)
typed("/ops/deliveries/{id}/result","post","Per-consumer lease result; no global ACK","DeliveryResult",staff=True)
for job in ("push", "nearby_alerts", "content_updates", "privacy_cleanup", "provider_revocations", "maintenance"):
    typed("/ops/jobs/"+job,"post","Execute one bounded job batch with durable continuation: "+job,"JobInput","JobResult",staff=True)
typed("/ops/data-requests/{id}","get","Audited privacy worker inventory without query capability, materials, contact or hold basis",result="DataRequestWork",staff=True)
for action,body in (("process","StateInput"),("hold","DataHoldInput"),("external","ExternalErasureInput")):
    typed("/ops/data-requests/{id}/"+action,"post","Selective data lifecycle: "+action,body,staff=True)
typed("/ops/legacy-erasure","post","Adapt historical payloads without pretending cleanup completed",{"type":"object","properties":{"cursor":{"type":"integer","format":"int64"}}},staff=True)
typed("/ops/media-erasure/{id}/process","post","Remove only an unheld physical copy after reference checking",staff=True)
typed("/ops/upload-expiry","post","Expire upload leases and sweep only old unreferenced managed files; privacy role and explicit scheduler required","UploadCleanupInput","UploadCleanupResult",staff=True)
typed("/ops/rights-cases/{id}/decision","post","Record rights workflow decision and transfer status","RightsDecision",staff=True)
item=typed("/ops/rights-cases","get","Named moderator queue of minimal receipts and evidence review deadlines",result="RightsQueuePage",staff=True)
item["parameters"]=[{"name":"cursor","in":"query","schema":{"type":"integer","format":"int64","minimum":0}}]
typed("/ops/rights-cases/{id}","get","Audited moderator case details; expired evidence stays redacted until a justified versioned extension",result="RightsCaseDetails",staff=True)
typed("/ops/retention-expiry","post","Privacy-only bounded evidence expiry; closed cases, elapsed deadline and no active scoped hold required","RetentionExpiryInput","RetentionExpiryResult",staff=True)
typed("/ops/push/{id}/dispatch","post","Lookup before provider send; accepted is distinct from delivered",staff=True)
item=typed("/safety/media","post","Private guest evidence JPEG/PNG; same metadata and size limits",access="safetyAccess")
item["requestBody"]=upload["requestBody"]
item["parameters"]=upload["parameters"]
item=typed("/safety/media/{id}/content","get","Private evidence for the same guest session",access="safetyAccess")
item["responses"]["200"]=media["responses"]["200"]

typed("/auth/oauth-link-scope","post","Scope the pending verified identity without account merging","ResolveInput","PurposeScope")
item=typed("/safety/requests/{key}","get","Recover the original guest submission by request ID",result="RightsReceipt",access="safetyAccess")
item["parameters"][0]["schema"]={"type":"string","minLength":8,"maxLength":100}

# Read responses as precise DTOs without exposing persistence/session bodies.
dto("OAuthResult")
dto("OTPResult")
item=typed("/share/{kind}/{id}","get","Guest-safe preview data; relative_url points to the /share/{kind}/{id} HTML landing page",result="ShareCard",public=True)
item["parameters"][0]["schema"]={"type":"string","enum":["entries","pets","cases","offers"]}
typed("/ops/revenue","get","Audited page of actual settlements; no automatic external reporting",staff=True)
for path in ("privacy-expiry","challenge-expiry","token-expiry"):
    typed("/ops/"+path,"post","Bounded privacy retention worker; deployment scheduler calls explicitly",{"type":"object","properties":{"cursor":{"type":"integer","format":"int64"}}},staff=True)

# Existing versioned/desired-state DTOs retain compatibility. This optional
# header opts into a durable receipt; replay must use the original body/key.
receipt_routes = [("/"+kind+"/{id}", "delete") for kind in ("pets","offers","entries","entry_comments","drafts")]
receipt_routes += [("/"+kind,"put") for kind in ("subscriptions","preferences","expressions","drafts")]
receipt_routes += [("/entries/{id}/reaction","post"),("/cases/{id}/leads","post"),("/visit_feedback","post")]
receipt_routes += [("/inbox/read-all/snapshot","post"),("/inbox/read-all","post")]
receipt_routes += [("/"+kind+"/{id}/"+action,"post") for kind,actions in (
    ("conversations",("accept","decline","read")),("invitations",("accept","decline","revoke")),
    ("groups",("read",)),("notifications",("read",))) for action in actions]
for path, method in receipt_routes:
    paths["/api/v2"+path][method].setdefault("parameters",[]).append({
        "name":"Idempotency-Key","in":"header","required":False,
        "schema":{"type":"string","minLength":8,"maxLength":100,"pattern":"^[A-Za-z0-9_-]+$"},
        "description":"Optional operation receipt key. Retain the exact payload for retries; a body client_request_id must match."})

# Recursively add named field DTOs and reject dangling references. No service,
# persistence/session challenge or encrypted provider credential DTO is exported.
while True:
    missing = set(re.findall(r'#/components/schemas/(\w+)', json.dumps(schemas))) - schemas.keys()
    if not missing:
        break
    for name in sorted(missing):
        dto(name)
result = {"openapi": "3.1.0", "info": {"title": "M&D mobile modular monolith API", "version": "2.0.0", "description": "Source contract, 2026-10-23. Not build- or runtime-verified. Currency amounts are integer CNY cents. IDs/cursors are int64; clients must preserve precision. See docs/server/MOBILE_SERVER.md for workflows, privacy projections and deployment limits."},
          "paths": paths, "components": {"securitySchemes": {"bearerAuth": {"type": "http", "scheme": "bearer", "bearerFormat": "opaque revocable session"}, "operatorToken": {"type": "apiKey", "in": "header", "name": "X-Operator-Token"}, "safetyAccess": {"type": "apiKey", "in": "header", "name": "X-Safety-Token"}, "requestAccess": {"type": "apiKey", "in": "header", "name": "X-Request-Access"}}, "schemas": schemas}}
destination = ROOT / "docs/api/mobile-v2.openapi.json"
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"Static contract: {len(paths)} paths, {len(schemas)} DTO schemas")

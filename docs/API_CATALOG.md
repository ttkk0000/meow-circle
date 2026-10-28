# M&D V2 API速查表

更新：2026-10-28。从[OpenAPI](api/mobile-v2.openapi.json)静态提取255路径／288操作；这里只提供入口速查，认证、请求、响应、错误、版本与幂等约束仍以完整OpenAPI和领域源码为准。路径存在不等于运行可用。

| 方法 | 路径 | 操作摘要／ID |
|---|---|---|
| GET | `/api/v2/pets` | List visible pets |
| POST | `/api/v2/pets` | Create pets |
| GET | `/api/v2/pets/{id}` | Read visible pets |
| PATCH | `/api/v2/pets/{id}` | Replace owned aggregate using current version |
| DELETE | `/api/v2/pets/{id}` | Remove owned aggregate |
| GET | `/api/v2/offers` | List visible offers |
| POST | `/api/v2/offers` | Create offers |
| GET | `/api/v2/offers/{id}` | Read visible offers |
| PATCH | `/api/v2/offers/{id}` | Replace owned aggregate using current version |
| DELETE | `/api/v2/offers/{id}` | Remove owned aggregate |
| GET | `/api/v2/entries` | List visible entries |
| POST | `/api/v2/entries` | Create entries |
| GET | `/api/v2/entries/{id}` | Read visible entries |
| PATCH | `/api/v2/entries/{id}` | Replace owned aggregate using current version |
| DELETE | `/api/v2/entries/{id}` | Remove owned aggregate |
| GET | `/api/v2/entry_comments` | List visible entry_comments |
| GET | `/api/v2/entry_comments/{id}` | Read visible entry_comments |
| DELETE | `/api/v2/entry_comments/{id}` | Remove owned aggregate |
| GET | `/api/v2/pet_comments` | List visible pet_comments |
| GET | `/api/v2/pet_comments/{id}` | Read visible pet_comments |
| DELETE | `/api/v2/pet_comments/{id}` | Remove owned aggregate |
| GET | `/api/v2/entry_reactions` | List visible entry_reactions |
| GET | `/api/v2/entry_reactions/{id}` | Read visible entry_reactions |
| GET | `/api/v2/friendships` | List visible friendships |
| GET | `/api/v2/friendships/{id}` | Read visible friendships |
| GET | `/api/v2/cases` | List visible cases |
| POST | `/api/v2/cases` | Create cases |
| GET | `/api/v2/cases/{id}` | Read visible cases |
| PATCH | `/api/v2/cases/{id}` | Replace owned aggregate using current version |
| GET | `/api/v2/claims` | List visible claims |
| GET | `/api/v2/claims/{id}` | Read visible claims |
| GET | `/api/v2/leads` | List visible leads |
| GET | `/api/v2/leads/{id}` | Read visible leads |
| GET | `/api/v2/subscriptions` | List visible subscriptions |
| PUT | `/api/v2/subscriptions` | Save owned settings with version |
| GET | `/api/v2/subscriptions/{id}` | Read visible subscriptions |
| GET | `/api/v2/appointments` | List visible appointments |
| POST | `/api/v2/appointments` | Create appointments |
| GET | `/api/v2/appointments/{id}` | Read visible appointments |
| GET | `/api/v2/adoptions` | List visible adoptions |
| POST | `/api/v2/adoptions` | Create adoptions |
| GET | `/api/v2/adoptions/{id}` | Read visible adoptions |
| GET | `/api/v2/care_updates` | List visible care_updates |
| GET | `/api/v2/care_updates/{id}` | Read visible care_updates |
| GET | `/api/v2/groups` | List visible groups |
| POST | `/api/v2/groups` | Create groups |
| GET | `/api/v2/groups/{id}` | Read visible groups |
| GET | `/api/v2/group_messages` | List visible group_messages |
| GET | `/api/v2/group_messages/{id}` | Read visible group_messages |
| GET | `/api/v2/conversations` | List visible conversations |
| POST | `/api/v2/conversations` | Create conversations |
| GET | `/api/v2/conversations/{id}` | Read visible conversations |
| GET | `/api/v2/messages` | List visible messages |
| GET | `/api/v2/messages/{id}` | Read visible messages |
| GET | `/api/v2/invitations` | List visible invitations |
| POST | `/api/v2/invitations` | Create invitations |
| GET | `/api/v2/invitations/{id}` | Read visible invitations |
| GET | `/api/v2/notifications` | List visible notifications |
| GET | `/api/v2/notifications/{id}` | Read visible notifications |
| GET | `/api/v2/preferences` | List visible preferences |
| PUT | `/api/v2/preferences` | Save owned settings with version |
| GET | `/api/v2/preferences/{id}` | Read visible preferences |
| GET | `/api/v2/expressions` | List visible expressions |
| PUT | `/api/v2/expressions` | Save owned settings with version |
| GET | `/api/v2/expressions/{id}` | Read visible expressions |
| GET | `/api/v2/drafts` | List visible drafts |
| PUT | `/api/v2/drafts` | Save owned settings with version |
| GET | `/api/v2/drafts/{id}` | Read visible drafts |
| DELETE | `/api/v2/drafts/{id}` | Remove owned aggregate |
| GET | `/api/v2/media` | List visible media |
| POST | `/api/v2/media` | One JPEG/PNG, 10 MiB, 20 MP; explicit public/private scope |
| GET | `/api/v2/media/{id}` | Read visible media |
| GET | `/api/v2/reviews` | List visible reviews |
| POST | `/api/v2/reviews` | Create reviews |
| GET | `/api/v2/reviews/{id}` | Read visible reviews |
| GET | `/api/v2/visit_feedback` | List visible visit_feedback |
| POST | `/api/v2/visit_feedback` | Create visit_feedback |
| GET | `/api/v2/visit_feedback/{id}` | Read visible visit_feedback |
| GET | `/api/v2/source_declarations` | List visible source_declarations |
| POST | `/api/v2/source_declarations` | Append an immutable manifest-bound source declaration; never publish content |
| GET | `/api/v2/source_declarations/{id}` | Read visible source_declarations |
| POST | `/api/v2/friendships/request` | Friend request |
| POST | `/api/v2/friendships/accept` | Friend accept |
| POST | `/api/v2/friendships/decline` | Friend decline |
| POST | `/api/v2/friendships/withdraw` | Friend withdraw |
| POST | `/api/v2/friendships/remove` | Friend remove |
| POST | `/api/v2/friendships/block` | Friend block |
| POST | `/api/v2/friendships/unblock` | Friend unblock |
| POST | `/api/v2/claims/{id}/match` | claims match |
| POST | `/api/v2/claims/{id}/reject` | claims reject |
| POST | `/api/v2/claims/{id}/request_supplement` | claims request_supplement |
| POST | `/api/v2/claims/{id}/supplement` | claims supplement |
| POST | `/api/v2/claims/{id}/resubmit` | claims resubmit |
| POST | `/api/v2/claims/{id}/withdraw` | claims withdraw |
| POST | `/api/v2/claims/{id}/dispute` | claims dispute |
| POST | `/api/v2/claims/{id}/confirm_handover` | claims confirm_handover |
| POST | `/api/v2/adoptions/{id}/request_supplement` | adoptions request_supplement |
| POST | `/api/v2/adoptions/{id}/supplement` | adoptions supplement |
| POST | `/api/v2/adoptions/{id}/approve` | adoptions approve |
| POST | `/api/v2/adoptions/{id}/reject` | adoptions reject |
| POST | `/api/v2/adoptions/{id}/withdraw` | adoptions withdraw |
| POST | `/api/v2/adoptions/{id}/resubmit` | adoptions resubmit |
| POST | `/api/v2/adoptions/{id}/resume_candidate` | adoptions resume_candidate |
| POST | `/api/v2/adoptions/{id}/propose_trial` | adoptions propose_trial |
| POST | `/api/v2/adoptions/{id}/confirm_trial` | adoptions confirm_trial |
| POST | `/api/v2/adoptions/{id}/decline_trial` | adoptions decline_trial |
| POST | `/api/v2/adoptions/{id}/confirm_handover` | adoptions confirm_handover |
| POST | `/api/v2/adoptions/{id}/support` | adoptions support |
| POST | `/api/v2/adoptions/{id}/request_return` | adoptions request_return |
| POST | `/api/v2/appointments/{id}/propose` | appointments propose |
| POST | `/api/v2/appointments/{id}/restore` | appointments restore |
| POST | `/api/v2/appointments/{id}/confirm` | appointments confirm |
| POST | `/api/v2/appointments/{id}/decline` | appointments decline |
| POST | `/api/v2/appointments/{id}/withdraw_change` | appointments withdraw_change |
| POST | `/api/v2/appointments/{id}/cancel` | appointments cancel |
| POST | `/api/v2/appointments/{id}/expire` | appointments expire |
| POST | `/api/v2/appointments/{id}/safety_hold` | appointments safety_hold |
| POST | `/api/v2/appointments/{id}/safety_repropose` | appointments safety_repropose |
| POST | `/api/v2/groups/{id}/directory` | groups directory |
| POST | `/api/v2/groups/{id}/notice` | groups notice |
| POST | `/api/v2/groups/{id}/transfer` | groups transfer |
| POST | `/api/v2/groups/{id}/accept_transfer` | groups accept_transfer |
| POST | `/api/v2/groups/{id}/decline_transfer` | groups decline_transfer |
| POST | `/api/v2/groups/{id}/revoke_transfer` | groups revoke_transfer |
| POST | `/api/v2/groups/{id}/dissolve` | groups dissolve |
| POST | `/api/v2/groups/{id}/leave` | groups leave |
| POST | `/api/v2/groups/{id}/remove_member` | groups remove_member |
| POST | `/api/v2/groups/{id}/mute` | groups mute |
| POST | `/api/v2/conversations/{id}/accept` | conversations accept |
| POST | `/api/v2/conversations/{id}/decline` | conversations decline |
| POST | `/api/v2/invitations/{id}/accept` | invitations accept |
| POST | `/api/v2/invitations/{id}/decline` | invitations decline |
| POST | `/api/v2/invitations/{id}/revoke` | invitations revoke |
| POST | `/api/v2/invitations/{id}/leave` | invitations leave |
| POST | `/api/v2/notifications/{id}/read` | notifications read |
| POST | `/api/v2/pets/{id}/introduction` | pets/{id}/introduction |
| POST | `/api/v2/entries/{id}/comments` | entries/{id}/comments |
| POST | `/api/v2/pets/{id}/comments` | pets/{id}/comments |
| POST | `/api/v2/cases/{id}/claims` | cases/{id}/claims |
| POST | `/api/v2/cases/{id}/close` | cases/{id}/close |
| POST | `/api/v2/adoptions/{id}/care_updates` | adoptions/{id}/care_updates |
| POST | `/api/v2/groups/{id}/messages` | Send rich message; retry using the same idempotency key |
| POST | `/api/v2/groups/{id}/read` | Explicit read receipt |
| POST | `/api/v2/conversations/{id}/messages` | Send rich message; retry using the same idempotency key |
| POST | `/api/v2/conversations/{id}/read` | Explicit read receipt |
| POST | `/api/v2/entries/{id}/reaction` | Set desired reaction state |
| POST | `/api/v2/entry_comments/{id}/reaction` | Set desired reaction on a visible comment |
| POST | `/api/v2/pet_comments/{id}/reaction` | Set desired reaction on a visible comment |
| GET | `/api/v2/comment-metrics` | ACL-aware counts for up to 50 visible comments |
| POST | `/api/v2/cases/{id}/leads` | Submit a private lead |
| POST | `/api/v2/cases/{id}/lead-submissions` | Submit a private lead with the original request and current case version |
| POST | `/api/v2/reviews/{id}/supplement` | Supplement a needs_info review using the original client request ID |
| GET | `/api/v2/pets/{id}/verifications` | Version-scoped public verification summaries |
| GET | `/api/v2/offers/{id}/verifications` | Version-scoped public verification summaries |
| POST | `/api/v2/auth/otp` | Authentication: otp |
| POST | `/api/v2/auth/phone` | Authentication: phone |
| POST | `/api/v2/auth/password` | Authentication: password |
| POST | `/api/v2/auth/reauth` | Authentication: reauth |
| POST | `/api/v2/auth/oauth-start` | Authentication: oauth-start |
| POST | `/api/v2/auth/oauth-finish` | Authentication: oauth-finish |
| POST | `/api/v2/auth/oauth-resolve` | Authentication: oauth-resolve |
| POST | `/api/v2/auth/account/phone` | Account action requires a single-use reauth grant |
| POST | `/api/v2/auth/account/password` | Account action requires a single-use reauth grant |
| POST | `/api/v2/auth/account/unlink` | Account action requires a single-use reauth grant |
| POST | `/api/v2/auth/account/email` | Account action requires a single-use reauth grant |
| POST | `/api/v2/auth/refresh` | Rotate both tokens; expired access token goes in body |
| POST | `/api/v2/auth/recover` | Recover using the recover-purpose OTP |
| POST | `/api/v2/auth/logout` | Revoke current or all sessions |
| GET | `/api/v2/auth/capabilities` | Only configured login providers |
| GET | `/api/v2/auth/methods` | Masked phone and linked provider record IDs |
| GET | `/api/v2/profile` | Own profile and four-theme settings |
| PATCH | `/api/v2/profile` | Update own profile and public avatar |
| GET | `/api/v2/inbox-summary` | Unread counts, first unread message IDs and stranger requests |
| GET | `/api/v2/users/{id}` | Public user profile; phone and credentials omitted |
| GET | `/api/v2/entry-metrics` | ACL-aware reaction and comment counts for up to 50 entries |
| GET | `/api/v2/expression-catalog` | 128 Unicode emoji and 24 static cat stickers |
| GET | `/api/v2/ops/reviews` | Staff review queue; cursor parameter |
| POST | `/api/v2/ops/reviews/{id}` | Staff decision and narrow verification expiry |
| GET | `/api/v2/ops/reviews/{id}` | Audited moderator detail reread; expired evidence references are withheld |
| POST | `/api/v2/ops/claim-disputes/{id}` | End disputed match without declaring ownership |
| POST | `/api/v2/ops/nearby-alerts` | Process a bounded, retryable operations batch |
| POST | `/api/v2/ops/case-watch-alerts` | Process a bounded, retryable operations batch |
| POST | `/api/v2/ops/provider-revocations` | Process a bounded, retryable operations batch |
| GET | `/api/v2/ops/outbox` | Pending domain events, excluding provider credentials |
| GET | `/api/v2/media/{id}/content` | ACL checked on every read; named evidence-reader role only grants non-expired private evidence |
| GET | `/api/v2/adoptions/{id}/care-context` | Read current authorized care stage and posting capability without application, contract or party details |
| GET | `/api/v2/ops/context` | Current named operator roles and configured reading material; no credential or blanket readiness claim |
| GET | `/api/v2/ops/authority` | Audited exact-scope authority version lookup; no global user or authority directory |
| POST | `/api/v2/ops/authority` | Named role-checked operation: authority |
| GET | `/api/v2/ops/review-queue` | Audited minimal moderator queue; cursor parameter, no private statements or materials |
| GET | `/api/v2/ops/guardian-invites/{id}` | Audited identity-verifier detail including terminal invite state, without nonce or identity materials |
| GET | `/api/v2/content-subscriptions` | Private followed authors and watched cases; no audience privileges |
| POST | `/api/v2/content-subscriptions` | Versioned durable update of content subscriptions |
| GET | `/api/v2/group-directory` | Public group introduction projection without member identities |
| GET | `/api/v2/group-directory/{id}` | Read the current public group introduction |
| POST | `/api/v2/group-directory/{id}/request` | Apply to the current group owner using an original request ID |
| POST | `/api/v2/inbox/read-all/snapshot` | Prepare an immutable 10-minute read-all snapshot without acknowledging tasks |
| GET | `/api/v2/reading` | Public read-only documents; bundled drafts never imply published terms |
| GET | `/api/v2/reading/{key}` | Read one stable document key without changing consent |
| GET | `/api/v2/reading/{key}/download` | Download the same complete plain text and SHA256 as the document |
| POST | `/api/v2/inbox/read-all` | Acknowledge only the prepared snapshot; newer messages remain unread |
| GET | `/api/v2/eligibility` | Server qualification; unknown evidence denies outward actions |
| GET | `/api/v2/auth/age-assurance/config` | Configured minimal age-verification channel; never a proof |
| GET | `/api/v2/auth/age-assurance/current` | Current owner request, including unknown results; null when none |
| POST | `/api/v2/auth/age-assurance` | Explicit consent, immutable task and original request; never auto-submit business |
| POST | `/api/v2/auth/age-assurance/{id}/query` | Query original provider request and apply only trusted bounded evidence |
| GET | `/api/v2/operations/{key}` | Look up the original command; not-found never authorizes a new key |
| POST | `/api/v2/leads/{id}/assessment` | Versioned private case-owner assessment; never resolve a case |
| POST | `/api/v2/leads/{id}/contact` | Explicit witness conversation derived from the current private lead |
| POST | `/api/v2/entries/preview` | Bind declaration to exact content and media versions |
| POST | `/api/v2/auth/email-otp` | Explicit purpose email authentication: email-otp |
| POST | `/api/v2/auth/email` | Explicit purpose email authentication: email |
| POST | `/api/v2/auth/register` | Explicit purpose email authentication: register |
| POST | `/api/v2/auth/email-recover` | Explicit purpose email authentication: email-recover |
| POST | `/api/v2/auth/verify-phone` | Consume proof for the currently bound phone |
| POST | `/api/v2/auth/account/phone/scope` | Compute server scope; no credential stored in grant |
| POST | `/api/v2/auth/account/password/scope` | Compute server scope; no credential stored in grant |
| POST | `/api/v2/auth/account/unlink/scope` | Compute server scope; no credential stored in grant |
| POST | `/api/v2/auth/account/email/scope` | Compute server scope; no credential stored in grant |
| POST | `/api/v2/push-devices` | Register or disable this account's device; token stays private |
| POST | `/api/v2/push-devices/lookup` | Look up this account device binding version without exposing its token |
| POST | `/api/v2/push-devices/disable` | Disable current session push bindings even when its device token is unavailable |
| GET | `/api/v2/guardians` | Own verified guardian relationships |
| GET | `/api/v2/guardians/invites/config` | Published guardian policy and available private invitation channel |
| GET | `/api/v2/guardians/invites` | Own or claimed invitations without child data or proof |
| POST | `/api/v2/guardians/invites` | Explicit short single-use invitation; no relationship or consent is granted |
| POST | `/api/v2/guardians/invites/claim` | Adult independently claims invitation; relationship verification is still required |
| GET | `/api/v2/guardians/invites/{id}` | Recover an owner/participant invitation without exposing private child data |
| POST | `/api/v2/guardians/invites/{id}/cancel` | Cancel only this original invitation or unverified request |
| GET | `/api/v2/ops/guardian-invites` | Audited identity-verifier-only relationship intake |
| GET | `/api/v2/guardians/{id}` | Read a participant relationship without verifier evidence |
| POST | `/api/v2/guardians/{id}/scope` | Scope a specific verified relationship |
| POST | `/api/v2/guardians/{id}/decision` | Explicit consent or withdrawal; withdrawal needs no current-phone gate |
| POST | `/api/v2/capability` | Issue a server-generated owner-bound data request capability |
| POST | `/api/v2/data-requests/scope` | Scope the selected data and query capability |
| POST | `/api/v2/data-requests` | Erase selected fields, close account, or request matters assistance |
| GET | `/api/v2/data-requests/{id}` | Restricted status after account closure |
| GET | `/api/v2/data-requests/requests/{key}` | Recover a lost submission using its original request ID |
| POST | `/api/v2/data-requests/{id}/revoke-access` | Revoke only this request's query capability |
| POST | `/api/v2/safety/session` | Guest safety/rights session; no login or phone required |
| POST | `/api/v2/safety/contact-otp` | Verify the session's contact for a formal notice |
| POST | `/api/v2/safety/contact-verify` | Verify the session's contact for a formal notice |
| POST | `/api/v2/safety/scope` | Scope a logged-in formal statement |
| POST | `/api/v2/safety/tickets` | Guest safety/privacy or verified formal notice/counterstatement/supplement |
| GET | `/api/v2/safety/tickets/{id}` | Minimal receipt, without target existence or private identities |
| POST | `/api/v2/adoptions/{id}/contracts/propose` | Versioned contract propose |
| POST | `/api/v2/adoptions/{id}/contracts/accept` | Versioned contract accept |
| POST | `/api/v2/adoptions/{id}/contracts/decline` | Versioned contract decline |
| POST | `/api/v2/adoptions/{id}/contracts/scope` | Versioned contract scope |
| POST | `/api/v2/adoptions/{id}/returns/propose` | Versioned safe return propose |
| POST | `/api/v2/adoptions/{id}/returns/accept` | Versioned safe return accept |
| POST | `/api/v2/adoptions/{id}/returns/decline` | Versioned safe return decline |
| POST | `/api/v2/adoptions/{id}/returns/withdraw` | Versioned safe return withdraw |
| POST | `/api/v2/adoptions/{id}/returns/handover` | Versioned safe return handover |
| POST | `/api/v2/ops/age-evidence` | Named role-checked operation: age-evidence |
| POST | `/api/v2/ops/guardian-evidence` | Named role-checked operation: guardian-evidence |
| POST | `/api/v2/ops/deliveries` | Named role-checked operation: deliveries |
| POST | `/api/v2/ops/revenue` | Named role-checked operation: revenue |
| GET | `/api/v2/ops/revenue` | Audited page of actual settlements; no automatic external reporting |
| POST | `/api/v2/ops/deliveries/{id}/result` | Per-consumer lease result; no global ACK |
| POST | `/api/v2/ops/jobs/push` | Execute one bounded job batch with durable continuation: push |
| POST | `/api/v2/ops/jobs/nearby_alerts` | Execute one bounded job batch with durable continuation: nearby_alerts |
| POST | `/api/v2/ops/jobs/content_updates` | Execute one bounded job batch with durable continuation: content_updates |
| POST | `/api/v2/ops/jobs/privacy_cleanup` | Execute one bounded job batch with durable continuation: privacy_cleanup |
| POST | `/api/v2/ops/jobs/provider_revocations` | Execute one bounded job batch with durable continuation: provider_revocations |
| POST | `/api/v2/ops/jobs/maintenance` | Execute one bounded job batch with durable continuation: maintenance |
| POST | `/api/v2/ops/jobs/search_projection` | Execute one bounded job batch with durable continuation: search_projection |
| POST | `/api/v2/ops/jobs/audit_projection` | Execute one bounded job batch with durable continuation: audit_projection |
| GET | `/api/v2/ops/data-requests/{id}` | Audited privacy worker inventory without query capability, materials, contact or hold basis |
| POST | `/api/v2/ops/data-requests/{id}/process` | Selective data lifecycle: process |
| POST | `/api/v2/ops/data-requests/{id}/hold` | Selective data lifecycle: hold |
| POST | `/api/v2/ops/data-requests/{id}/external` | Selective data lifecycle: external |
| POST | `/api/v2/ops/legacy-erasure` | Adapt historical payloads without pretending cleanup completed |
| POST | `/api/v2/ops/media-erasure/{id}/process` | Remove only an unheld physical copy after reference checking |
| POST | `/api/v2/ops/upload-expiry` | Expire upload leases and sweep only old unreferenced managed files; privacy role and explicit scheduler required |
| POST | `/api/v2/ops/rights-cases/{id}/decision` | Record rights workflow decision and transfer status |
| GET | `/api/v2/ops/rights-cases` | Named moderator queue of minimal receipts and evidence review deadlines |
| GET | `/api/v2/ops/rights-cases/{id}` | Audited moderator case details; expired evidence stays redacted until a justified versioned extension |
| POST | `/api/v2/ops/retention-expiry` | Privacy-only bounded evidence expiry; closed cases, elapsed deadline and no active scoped hold required |
| POST | `/api/v2/ops/push/{id}/dispatch` | Lookup before provider send; accepted is distinct from delivered |
| POST | `/api/v2/safety/media` | Private guest evidence JPEG/PNG; same metadata and size limits |
| GET | `/api/v2/safety/media/{id}/content` | Private evidence for the same guest session |
| POST | `/api/v2/auth/oauth-link-scope` | Scope the pending verified identity without account merging |
| GET | `/api/v2/safety/requests/{key}` | Recover the original guest submission by request ID |
| GET | `/api/v2/share/{kind}/{id}` | Guest-safe preview data; relative_url points to the /share/{kind}/{id} HTML landing page |
| POST | `/api/v2/ops/privacy-expiry` | Bounded privacy retention worker; deployment scheduler calls explicitly |
| POST | `/api/v2/ops/challenge-expiry` | Bounded privacy retention worker; deployment scheduler calls explicitly |
| POST | `/api/v2/ops/token-expiry` | Bounded privacy retention worker; deployment scheduler calls explicitly |

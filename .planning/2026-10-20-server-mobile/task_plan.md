# Go server alignment with mobile design

Date: 2026-10-20
Workspace: J:/GolandProjects/kitty-circle
Baseline: main / ebfc469c9707bf0ea4a6b383840546077ae65782

## Goal and authorization
Implement server APIs, persistence and business rules matching current phone design. User explicitly permits architecture changes and replacing old code as appropriate. Do not change client UI. No builds, test runs, service/device startup, dependency installation, commits or pushes are authorized.

## Architecture
Modular monolith: typed domain aggregates, feature services/state transitions, transaction boundary, HTTP transport, PostgreSQL and in-memory adapters. Use existing identity records and server bootstrap; no fabricated third-party success. Complex aggregate details may use JSONB within feature-specific tables, while ownership, unique keys, participants, versions and query indexes remain relational.

## Phases
1. Inspect current contracts and produce design-to-server coverage — complete (docs/server/MOBILE_SERVER.md)
2. Transactional repository, schema, identity/session foundations — source complete; migration not executed
3. Pets, friends/circle, messages/groups, drafts/expressions/preferences — source complete
4. Lost/found/claims, adoption/collaborators, appointment/version/visit, trust cases — source complete
5. Phone/provider auth, account lifecycle, secure media and deployment configuration — adapters/config/operations ports complete; actual credentials, device push delivery and erasure policy remain external integration work
6. Wire server routes, regression test source, static checks and handoff — static checks complete; compile/runtime tests explicitly not authorized

## Verification boundary
Write meaningful regression tests for later authorized execution. Only inspect source, SQL, JSON, links, encoding and diffs now; do not execute Go build/test/run or migrations.

## Existing changes to preserve
Uncommitted design docs/assets/planning and unrelated KMP app/shared/iOS files. No server files were dirty at start. Keep old design approval gate for clients; latest user authorizes server implementation.

# Security Policy

## Supported Versions

Only the `main` branch and the latest tagged release receive security fixes.

## Reporting a Vulnerability

Please **do not** file a public GitHub issue for security problems.

Instead, email the maintainer or open a
[private security advisory](https://github.com/ttkk0000/kitty-circle/security/advisories/new)
on GitHub. Include:

- A clear description of the issue and its impact.
- Steps or a proof-of-concept to reproduce.
- Any suggested mitigation.

You should receive an acknowledgement within **72 hours**. We will work with
you on a coordinated disclosure timeline; expect a fix within 30 days for
critical issues.

## Hardening Already In Place

- JWT secrets and admin keys are read from environment variables
  (`JWT_SECRET`, `ADMIN_KEY`) — never commit real values.
- Login attempts are rate-limited per username (see
  `internal/platform/auth/ratelimit.go`); repeated failures return HTTP 429
  with a `Retry-After` header.
- JSON request bodies are capped at 1 MiB (multipart media upload endpoints
  enforce their own per-kind limits).
- CORS origin is configurable via `CORS_ALLOW_ORIGIN`; prefer an explicit
  origin over `*` in production.
- Passwords are salted + hashed with scrypt; plaintext is never stored or
  logged.
- The production container image is based on
  `gcr.io/distroless/static-debian12:nonroot` — no shell, no package
  manager, runs as a non-root user.

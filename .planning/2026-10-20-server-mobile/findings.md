# Findings

- Existing Go server has legacy post/listing/order, DM, notifications, password auth and simple adoption APIs. No native friends/circle, owned-pet diary, lost/found claims, versioned appointments or role invitation workflows.
- Store has memory/PostgreSQL/Redis adapters but old methods collapse errors into bool/zero values. New transactional repository must preserve errors and request context.
- Phone OTP is currently an in-memory development scaffold, not real delivery. JWT sessions are not revocable. New auth must fail closed without provider configuration.
- Existing public static upload directory cannot store private claim/circle/care media. New private storage must use authenticated/granted content access.
- Current CodeGraph healthy (149 files); context/explore used before edits. Broad explore was output-capped, so exact missing handler/schema details are read from current source.


# Admin Patterns

## Admin key authentication

The `X-Admin-Key` header is validated centrally by `AdminAuthFilter` (registered for
`/api/v1/admin/*` in `AdminAuthConfig`) using a constant-time comparison. Controllers do **not**
validate the key themselves — the `adminKey` parameters on `AdminApi` methods exist only so
Swagger documents the required header.

## Admin service methods

Service methods that bypass user ownership checks are prefixed with `admin` (e.g. `adminGetSubdomain`, `adminUpdateTargetIp`, `adminRelabelSubdomain`). This makes it immediately visible at the call site that the method skips the owner guard, and keeps the admin surface searchable.

## 404 instead of 403 for ownership failures

`getOwnedSubdomain` (and similar ownership checks) throws `404 NOT_FOUND` rather than `403 FORBIDDEN` when a user tries to access a resource they don't own. This avoids leaking whether the resource exists at all. Keep this consistent for any future ownership-guarded lookup.

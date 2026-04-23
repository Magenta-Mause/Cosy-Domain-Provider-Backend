# User Management Spec

Covers user accounts, profile data, and the Cosy+ plan tier.

---

## Current implementation

### Data model

**`UserEntity`** (`user_entity` table):

| Field          | Type          | Notes                                      |
|----------------|---------------|--------------------------------------------|
| `uuid`         | String (UUID) | Primary key, auto-generated                |
| `username`     | String        | Unique (case-insensitive lookup), not null |
| `email`        | String        | Not null                                   |
| `passwordHash` | String        | BCrypt hash (strength 10)                  |

**`UserDto`** (response):

```json
{
  "uuid": "...",
  "username": "janne",
  "email": "janne@example.net"
}
```

### Endpoints

#### `GET /api/v1/user`

Returns all users. Currently public (no auth required).

**Note:** This should be restricted before production — either admin-only or removed.

Response: `UserDto[]`

#### `POST /api/v1/user`

Creates a user directly (no login session). Intended for admin use.

Request (`UserCreationDto`):

```json
{
  "username": "janne",
  "email": "janne@example.net",
  "password": "hunter2"
}
```

Response: `UserDto` (201 Created)

---

## Planned features

### Profile update endpoint

Allow authenticated users to update their own username and email.

```
PATCH /api/v1/user/me
Requires: identity token
Body: { "username"?: "newname", "email"?: "new@example.net" }
Response: UserDto
```

Constraints:

- Username: same validation rules as registration
- Email change: triggers re-verification (set `emailVerified=false`, send new OTP)
- Username change: enforce uniqueness

### Get own profile

```
GET /api/v1/user/me
Requires: identity token
Response: UserDto (extended with emailVerified, plan, createdAt)
```

### Account deletion

```
DELETE /api/v1/user/me
Requires: identity token
Body: { "password": "hunter2" }  # confirm with password
```

Logic:

1. Verify password
2. For each owned subdomain: call `Route53Service.deleteARecord` (best-effort)
3. Delete all `SubdomainEntity` records
4. Delete `UserEntity`
5. Invalidate refresh token cookie

### Restrict `GET /api/v1/user`

Change to require an admin role or remove the public listing. Options:

- Add `role` field to `UserEntity` (ADMIN / USER)
- Require `role=ADMIN` in JWT claims for this endpoint
- Or simply remove the endpoint (user listing is not needed in the domain-provider product)

---

## Cosy+ billing tier

The Cosy+ tier (€1/month) unlocks custom subdomain names, up to 5 domains, CNAME support, and priority TLS renewal.
Revenue supports the Cosy core team.

### Schema changes

Add to `UserEntity`:

| Field              | Type      | Notes                                   |
|--------------------|-----------|-----------------------------------------|
| `plan`             | Plan enum | FREE (default) / PLUS                   |
| `planExpiresAt`    | Instant   | nullable; null = no active subscription |
| `stripeCustomerId` | String    | nullable; Stripe customer ID            |

**`Plan` enum:** `FREE`, `PLUS`

### New endpoints

```
GET  /api/v1/billing/portal
Requires: identity token
→ Creates/returns Stripe billing portal session URL

POST /api/v1/billing/webhook
→ Stripe webhook receiver (no auth, verified by Stripe signature)
   Handles: checkout.session.completed, invoice.payment_succeeded, customer.subscription.deleted
```

### Webhook logic

- `checkout.session.completed` → set `plan=PLUS`, `planExpiresAt=subscriptionPeriodEnd`
- `invoice.payment_succeeded` → extend `planExpiresAt`
- `customer.subscription.deleted` → set `plan=FREE`, clear `planExpiresAt`

### Plan enforcement

`SubdomainService` checks `user.plan` before allowing:

- Custom label choice (Cosy+ only — free users must use auto-generated labels)
- CNAME registration (Cosy+ only)

Quota remains 5 for both tiers (free gets auto-assigned labels up to 5).

### Identity token claims

Add `plan` to the identity token payload so the frontend can gate UI without an extra API call:

```json
{
  ...,
  "plan": "PLUS"
}
```

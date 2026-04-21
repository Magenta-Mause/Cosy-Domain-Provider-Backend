# Subdomain Management Spec

Users claim subdomains on `cosy-hosting.net`. Each subdomain maps to an AWS Route53 A record pointing at the user's Cosy instance IP. The backend tracks lifecycle state and enforces ownership.

---

## Current implementation

### Data model

**`SubdomainEntity`** (`subdomain_entity` table):

| Field | Type | Notes |
|---|---|---|
| `uuid` | String (UUID) | Primary key, auto-generated |
| `label` | String | Unique subdomain label (e.g. `"castle"`) |
| `owner` | UserEntity FK | ManyToOne, `owner_uuid` column |
| `targetIp` | String | IPv4 address of Cosy instance |
| `status` | SubdomainStatus | PENDING / ACTIVE / FAILED |
| `createdAt` | Instant | Set on creation, not updatable |
| `updatedAt` | Instant | Auto-updated on every change |

**`SubdomainStatus` enum:**
- `PENDING` — record created in DB, waiting for Route53 sync
- `ACTIVE` — Route53 A record confirmed
- `FAILED` — Route53 operation failed; record retained for retry

**`SubdomainDto`** (response):
```json
{
  "uuid": "...",
  "label": "castle",
  "fqdn": "castle.cosy-hosting.net",
  "targetIp": "192.0.2.44",
  "status": "ACTIVE",
  "createdAt": "...",
  "updatedAt": "..."
}
```

`fqdn` is computed: `label + "." + aws.route53.domain`

### Endpoints

All endpoints require a valid identity token.

#### `GET /api/v1/subdomain`

Returns all subdomains owned by the authenticated user.

Response: `SubdomainDto[]`

#### `GET /api/v1/subdomain/{uuid}`

Returns a single subdomain. Ownership is enforced — non-owners receive `404` (existence not leaked).

Response: `SubdomainDto`

#### `POST /api/v1/subdomain`

Request (`SubdomainCreationDto`):
```json
{ "label": "castle", "targetIp": "192.0.2.44" }
```

Logic (`SubdomainService.createSubdomain`):
1. Validate label format (regex + reserved list check)
2. Check label uniqueness across all users
3. Check user quota (`countByOwner` ≤ `subdomain.max-per-user`)
4. Persist entity with `status=PENDING`
5. Call `Route53Service.upsertARecord(label, targetIp)`
6. Update status to `ACTIVE` on success, `FAILED` on Route53 error
7. Log result

Response: `SubdomainDto` (201 Created)

Errors:
- `400` — invalid label format
- `409` — label already taken or reserved
- `422` — quota exceeded (max 5 per user)

#### `PUT /api/v1/subdomain/{uuid}`

Request (`SubdomainUpdateDto`):
```json
{ "targetIp": "10.0.0.12" }
```

Logic:
1. Verify ownership
2. Update `targetIp`, set `status=PENDING`
3. Call `Route53Service.upsertARecord(label, newIp)` (UPSERT action)
4. Update status to `ACTIVE` or `FAILED`

Response: `SubdomainDto`

#### `DELETE /api/v1/subdomain/{uuid}`

Logic:
1. Verify ownership
2. Call `Route53Service.deleteARecord(label, targetIp)`
3. On Route53 success: delete entity from DB
4. On Route53 failure: set `status=FAILED`, retain entity (for retry)

Response: `204 No Content`

### Label validation rules

```
- Length: 1–63 characters
- Format: /^[a-z0-9][a-z0-9-]*[a-z0-9]$|^[a-z0-9]$/
- Not in reserved list (see application.yaml: subdomain.reserved-labels)
- Unique across all users (case-insensitive check)
```

Reserved labels include: `www`, `api`, `admin`, `mail`, `ftp`, `smtp`, `pop`, `imap`, `ns`, `ns1`, `ns2`, `dns`, `root`, `localhost`, `auth`, `login`, `register`, `cdn`, `static`, `assets`, `app`, `dev`, `staging`, `prod`, `production`, `test`, `stage`, `status`, `support`, `help`, `docs`, `blog`

### Route53 integration

**`Route53Service`** (in `services/aws/`):
- Uses AWS SDK v2 `Route53Client`
- `upsertARecord(label, ip)` — sends `ChangeResourceRecordSets` with action `UPSERT`
- `deleteARecord(label, ip)` — sends `ChangeResourceRecordSets` with action `DELETE`
- `listAllRecords()` — paginates through all A records in the hosted zone
- Default TTL: 300 seconds (configurable)
- Target: `{label}.{aws.route53.domain}`

**`DnsEntryManager` abstraction** (in `services/dns/`):
- Interface for DNS operations — allows swapping Route53 for another provider
- `AwsDnsEntryManager` is the current implementation

---

## Planned features

### Subdomain availability check endpoint

Needed by the frontend's Cosy+ custom-name picker (live availability feedback).

```
GET /api/v1/subdomain/check?label=castle
Response: { "available": boolean, "reason": "taken" | "reserved" | "invalid" | null }
```

No auth required. Must not leak existing subdomain owners.

### Auto-generated name suggestions

For free-tier users who can't choose their name, the backend should generate suggestions.

```
GET /api/v1/subdomain/suggestions?count=7
Response: { "suggestions": ["cosy-castle-42", "pixel-shire", "willow-cove", ...] }
```

Generation pattern: `{adjective}-{noun}-{number}` from a curated wordlist.
Must verify each suggestion is available before returning it.

### TLS certificate tracking

Add cert lifecycle fields to `SubdomainEntity`:

| Field | Type | Notes |
|---|---|---|
| `tlsStatus` | TlsStatus enum | PENDING / VALID / RENEWING / EXPIRED / FAILED |
| `tlsIssuer` | String | e.g. "Let's Encrypt" |
| `tlsIssuedAt` | Instant | |
| `tlsCertExpiresAt` | Instant | |

**`TlsStatus` enum:** `PENDING`, `VALID`, `RENEWING`, `EXPIRED`, `FAILED`

Add to `SubdomainDto`:
```json
"tls": {
  "status": "VALID",
  "issuer": "Let's Encrypt",
  "issuedAt": "...",
  "expiresAt": "...",
  "expiresDays": 72
}
```

TLS certificate automation (ACME/Let's Encrypt integration) is a separate service concern — tracked separately.

### Dynamic DNS update endpoint

DuckDNS-style endpoint for clients that need to update their IP without a full API login.

```
GET /update?label=castle&token=<per-subdomain-token>&ip=192.0.2.44
Response: 200 OK "good" | 400 Bad Request "badauth"
```

Requires a per-subdomain token stored on `SubdomainEntity`. Token should be rotatable from the domain detail page.

Schema change: add `updateToken: String` (UUID, unique) to `SubdomainEntity`.

### Cosy+ plan: custom name choice

Currently all users can choose their label (there is no plan check). When billing is introduced:
- **Free tier**: label is auto-assigned (pattern: `{word}-{word}-{number}`) — user picks from suggestions
- **Cosy+ tier**: user types their chosen label; live availability check enabled
- Plan check: `SubdomainService.createSubdomain` validates that free-tier users submit a label matching the auto-generated pattern (or just accept the suggestion selection)

### Cosy+ plan: CNAME (bring your own domain)

Allow Cosy+ users to use their own domain as an alias.

Schema change: add `customDomain: String` (nullable) to `SubdomainEntity`.

New endpoint:
```
PUT /api/v1/subdomain/{uuid}/custom-domain
Body: { "customDomain": "myserver.example.com" }
```

Logic: validate that a CNAME record for `customDomain` points to `{label}.cosy-hosting.net`, then mark as verified.

### Subdomain status retry

For `FAILED` subdomains, add a retry endpoint:

```
POST /api/v1/subdomain/{uuid}/retry
```

Re-attempts the Route53 operation and updates status accordingly.

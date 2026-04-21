# Auth System Spec

Handles user registration, login, session management, and identity tokens. Uses a two-token model: a long-lived refresh token (httpOnly cookie) and a short-lived identity token (in-memory on frontend).

---

## Current implementation

### Two-token model

```
refresh token  (1 month,  httpOnly SameSite=Strict cookie, type=REFRESH)
identity token (1 hour,   returned in response body,       type=IDENTITY)
```

Both are HMAC-SHA256 signed JWTs. The `tokenType` claim distinguishes them.
`JwtFilter` validates the token type against the expected type for each endpoint before setting `SecurityContext`.

### Endpoints

#### `POST /api/v1/auth/register`

Request (`UserCreationDto`):
```json
{ "username": "janne", "email": "janne@example.net", "password": "hunter2" }
```

Logic:
1. Validate `@NotBlank` + `@Pattern` on all fields
2. Hash password with BCrypt (strength 10)
3. Persist `UserEntity` (uuid auto-generated)
4. Generate refresh token via `JwtUtils.generateRefreshToken(user)`
5. Return token per `tokenMode` param

Response (`LoginResponseDto`):
```json
{ "username": "janne", "token": "<identity-token-or-null>" }
```

Errors:
- `409` — username already taken (`DataIntegrityViolationException`)
- `400` — validation failure

#### `POST /api/v1/auth/login`

Request (`LoginDto`):
```json
{ "username": "janne", "password": "hunter2" }
```

Logic:
1. Load user by username (case-insensitive)
2. Compare password with `BCryptPasswordEncoder.matches()`
3. Generate refresh token
4. Return per `tokenMode`

Response: same `LoginResponseDto`

Errors:
- `401` — invalid credentials (`ResponseStatusException`)

#### `GET /api/v1/auth/token`

Requires: refresh token cookie (or `authToken` query param)

Logic:
1. `JwtFilter` extracts token from cookie / query param
2. Validates it is type `REFRESH`
3. Loads user from `sub` claim
4. Generates new identity token via `JwtUtils.generateIdentityToken(user)`

Response:
```json
{ "username": "janne", "token": "<identity-token>" }
```

This is the endpoint the frontend calls on every app start (`bootstrapAuth()`).

#### `POST /api/v1/auth/logout`

Requires: identity token (or refresh token)

Logic: clears the `refreshToken` httpOnly cookie by setting `Max-Age=0`.

Response: `200 OK` (empty body)

### `tokenMode` query parameter

Applies to login, register, and token endpoints.

| Value | Behavior |
|---|---|
| `COOKIE` (default) | Refresh token set as httpOnly `SameSite=Strict` cookie |
| `DIRECT` | Refresh token returned in response body (for mobile clients / testing) |

### Token claims

**Refresh token:**
```json
{ "iss": "cosy-domain-provider", "sub": "<user-uuid>", "tokenType": "REFRESH", "username": "janne", "iat": ..., "exp": ... }
```

**Identity token:**
```json
{ "iss": "cosy-domain-provider", "sub": "<user-uuid>", "tokenType": "IDENTITY", "username": "janne", "email": "janne@example.net", "iat": ..., "exp": ... }
```

### Security filter chain

- CSRF disabled (stateless API)
- Session policy: `STATELESS`
- CORS: enabled
- Permit without auth: `/api/v1/auth/**`, `/v3/api-docs/**`, `/actuator/**`, `/swagger-ui/**`
- All other paths: require identity token
- `JwtFilter` runs before `UsernamePasswordAuthenticationFilter`

---

## Planned features

### OAuth sign-in (Google, GitHub, Discord, Microsoft, Apple)

**Approach:** Spring OAuth2 Client / Authorization Code flow with PKCE.

Frontend sends user to provider → provider redirects to `/api/v1/auth/oauth/callback?code=&provider=` → backend exchanges code for profile → find or create `UserEntity` → issue refresh token.

Schema changes needed:
- `UserEntity`: add `oauthProvider` (nullable enum: GOOGLE, GITHUB, DISCORD, MICROSOFT, APPLE) + `oauthSubject` (nullable String, unique per provider)
- `UserEntity`: make `passwordHash` nullable (OAuth users have no password)
- New unique index: `(oauthProvider, oauthSubject)`

New endpoints:
```
GET  /api/v1/auth/oauth/authorize?provider=google   → redirect to provider
GET  /api/v1/auth/oauth/callback?code=&state=       → exchange + login/register
```

### Email verification on registration

After `POST /api/v1/auth/register`, mark user as unverified and send a 6-digit OTP via email.

Schema changes:
- `UserEntity`: add `emailVerified: boolean` (default false), `verificationCode: String` (nullable), `verificationCodeExpiresAt: Instant` (nullable)

New endpoint:
```
POST /api/v1/auth/verify-email
Body: { "code": "123456" }
Requires: identity token (unverified user)
→ marks emailVerified=true, clears code fields
```

Resend endpoint:
```
POST /api/v1/auth/resend-verification
Requires: identity token
→ generates new OTP, sends email
```

Subdomain creation should require `emailVerified=true`.

### Password reset via email

```
POST /api/v1/auth/forgot-password
Body: { "email": "janne@example.net" }
→ generates time-limited (30 min) reset token, sends email
→ always returns 200 (don't leak whether email exists)

POST /api/v1/auth/reset-password
Body: { "token": "...", "newPassword": "hunter2new" }
→ validates token, updates passwordHash, invalidates token
```

Schema changes:
- `UserEntity`: add `passwordResetToken: String` (nullable), `passwordResetExpiresAt: Instant` (nullable)

### Password change (authenticated)

```
POST /api/v1/auth/change-password
Requires: identity token
Body: { "currentPassword": "hunter2", "newPassword": "hunter2new" }
→ verify current password, update hash
```

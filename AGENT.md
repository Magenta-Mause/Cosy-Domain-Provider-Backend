# Cosy Domain Provider — Backend

Spring Boot service that manages user accounts, subdomain records on `cosy-hosting.net`, and AWS Route53 DNS entries. Users register/login, then claim a subdomain that gets an A record in Route53 and (eventually) an auto-renewed TLS certificate.

Frontend counterpart: `../cosy-domain-provider-frontend`

See `specs/` for detailed feature specs:
- `specs/auth.md` — auth flow, tokens, planned OAuth + email verification
- `specs/subdomains.md` — subdomain CRUD, Route53, lifecycle, planned features
- `specs/users.md` — user management, Cosy+ billing tier

---

## Commands

```bash
./mvnw spring-boot:run                              # Run dev server
./mvnw package                                      # Build JAR
./mvnw test                                         # Run tests (uses H2 in-memory DB)
./mvnw spotless:apply                               # Format code before committing
./mvnw spotless:check                               # Check formatting in CI
docker compose -f infrastructure/compose.yaml up -d # Start local PostgreSQL
```

## Required environment variables

| Variable | Purpose |
|---|---|
| `COSY_DOMAIN_PROVIDER_JWT_SECRET_KEY` | HMAC-SHA256 signing key for JWT tokens |
| `AWS_HOSTED_ZONE_ID` | Route53 hosted zone ID |
| `AWS_DOMAIN` | Parent domain (e.g. `cosy-hosting.net`) |
| `AWS_ACCESS_KEY_ID` | AWS credentials |
| `AWS_SECRET_ACCESS_KEY` | AWS credentials |

A default insecure JWT key is set in `application.yaml` for local dev — override in production.

---

## Tech stack

- **Spring Boot 4.0** · Java 21 · Maven
- **Spring Security** — stateless JWT filter chain
- **JPA / Hibernate** — PostgreSQL in production, H2 for tests
- **JJWT 0.13** — token generation and validation
- **AWS SDK v2 Route53** — A record management
- **SpringDoc OpenAPI 3.0** — Swagger UI at `/swagger-ui/`, spec at `/v3/api-docs`
- **Lombok** — `@Builder`, `@Getter`, `@Setter`, `@RequiredArgsConstructor` throughout
- **Spotless** — Google Java Format (AOSP variant), enforced before commit

---

## Project structure

```
src/main/java/com/magentamause/cosydomainprovider/
  controller/
    RouteController.java           # SPA fallback route (serves frontend index.html)
    ExceptionController.java       # Global @RestControllerAdvice
    v1/
      schema/                      # API interface definitions (AuthorizationApi, SubdomainApi, UserApi)
      impl/                        # Controller implementations
  services/
    core/                          # Domain logic (UserService, SubdomainService)
    auth/                          # Auth logic (AuthorizationService, SecurityContextService)
    aws/                           # Route53Service
    dns/                           # DnsEntryManager abstraction + AwsDnsEntryManager impl
  entity/                          # JPA entities (UserEntity, SubdomainEntity)
  repository/                      # Spring Data JPA repositories
  model/
    core/                          # Response DTOs (UserDto, SubdomainDto, SubdomainStatus, LoginResponseDto)
    action/                        # Request DTOs (LoginDto, UserCreationDto, SubdomainCreationDto, SubdomainUpdateDto)
    dns/                           # DnsEntry model
    exception/                     # ApiException error response model
  security/jwtfilter/              # JwtFilter, JwtUtils, JwtTokenBody, AuthenticationToken
  configuration/properties/        # Externalized config (AwsConfig, JwtProperties, SubdomainProperties, SecurityFilterChainConfig)
```

---

## Key patterns

### DTO naming

- `*CreationDto` — POST request body (validation annotations, no ID)
- `*UpdateDto` — PUT request body
- `*Dto` — response body; entities map directly, **no** `ApiResponse<T>` wrapper

### Auth flow (two-token model)

```
POST /api/v1/auth/login or /register
  → validates credentials
  → issues refresh token (long-lived, 1 month)
  → stores as httpOnly SameSite=Strict cookie (default) or returns in body (tokenMode=DIRECT)

GET /api/v1/auth/token
  → reads refresh token from cookie
  → returns short-lived identity token (1 hour) in response body

All authenticated requests:
  → Authorization: Bearer <identity-token> header
  → JwtFilter validates token type + loads user into SecurityContext

POST /api/v1/auth/logout
  → clears refresh cookie
```

`tokenMode` query param: `COOKIE` (default) | `DIRECT` (body, useful for testing/mobile)

### Subdomain lifecycle

```
PENDING  →  ACTIVE   (Route53 upsert succeeded)
         →  FAILED   (Route53 error; entity retained for retry)
```

- Max subdomains per user: 5 (configurable via `subdomain.max-per-user`)
- Reserved labels enforced in `SubdomainService` (www, api, admin, mail, etc.)
- Label validation: 1–63 chars, lowercase alphanumeric + hyphens, no leading/trailing hyphen
- Ownership always checked before any read/write operation (`getOwnedSubdomain` returns 404 to non-owners)

### Exception handling

`ExceptionController` (`@RestControllerAdvice`) returns `ApiException` with:
```json
{ "statusCode": 404, "errorCode": "NOT_FOUND", "message": "...", "path": "/api/...", "timestamp": "..." }
```

### Code style

- Run `./mvnw spotless:apply` before every commit
- JSON responses: snake_case (configured in `application.yaml`)
- Entities: `@Builder` + `@Getter` + `@Setter` (Lombok)
- Spring Security: CSRF disabled, STATELESS sessions, CORS enabled

---

## API endpoints

```
POST   /api/v1/auth/login
POST   /api/v1/auth/register
GET    /api/v1/auth/token
POST   /api/v1/auth/logout

GET    /api/v1/subdomain               # list current user's subdomains (auth required)
GET    /api/v1/subdomain/{uuid}        # get single subdomain (auth + ownership)
POST   /api/v1/subdomain               # create subdomain + Route53 A record (auth required)
PUT    /api/v1/subdomain/{uuid}        # update target IP (auth + ownership)
DELETE /api/v1/subdomain/{uuid}        # delete subdomain + Route53 record (auth + ownership)

GET    /api/v1/user                    # list all users
POST   /api/v1/user                    # create user

GET    /v3/api-docs                    # OpenAPI spec (consumed by frontend Orval codegen)
GET    /swagger-ui/
GET    /actuator/**
```

---

## Database schema

**UserEntity** (`user_entity`)
- `uuid` PK, `username` (unique), `email`, `passwordHash`

**SubdomainEntity** (`subdomain_entity`)
- `uuid` PK, `label` (unique), `owner` FK → UserEntity, `targetIp`, `status` (PENDING/ACTIVE/FAILED), `createdAt`, `updatedAt`

---

## Configuration reference

Key properties in `application.yaml`:

```yaml
aws.route53.hosted-zone-id: ${AWS_HOSTED_ZONE_ID}
aws.route53.domain:          ${AWS_DOMAIN}
aws.route53.default-ttl:     300

jwt.secret-key:                        ${COSY_DOMAIN_PROVIDER_JWT_SECRET_KEY}
jwt.identity-token-expiration-time:    3600000    # 1 hour (ms)
jwt.refresh-token-expiration-time:     2678400000  # 1 month (ms)

subdomain.max-per-user: 5
subdomain.reserved-labels: [www, api, admin, mail, ...]
```

---

## Planned features (not yet built)

- **OAuth sign-in** — Google, GitHub, Discord, Microsoft, Apple via Spring OAuth2 Resource Server
- **Cosy+ billing tier** (€3/month)
  - Enables custom subdomain name choice (instead of auto-generated)
  - Up to 5 domains per account
  - Bring-your-own-domain via CNAME
  - Priority TLS renewal queue
  - Revenue supports the Cosy core team
  - Requires `plan` field on `UserEntity` and Stripe (or similar) integration
- **Email verification** on registration (6-digit code sent to email)
- **Password reset** via email (time-limited token)
- **Dynamic DNS update endpoint** — `GET /update?label=&token=&ip=` using per-subdomain token (DuckDNS-style, TODO comment in `SubdomainService`)
- **TLS certificate automation** — Let's Encrypt integration (ACME client); `SubdomainEntity` needs cert expiry tracking

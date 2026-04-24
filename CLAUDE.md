# Cosy Domain Provider — Backend

Spring Boot REST API for managing `cosy-hosting.net` subdomains. Frontend: `../cosy-domain-provider-frontend`.

## Cross-repo work

If a task touches the frontend (wiring new endpoints into the UI, understanding component conventions, or changing the Orval-generated API client), **read `../cosy-domain-provider-frontend/CLAUDE.md` before starting.** It documents the component structure, styling rules, i18n requirements, and admin API client patterns that must be followed on the frontend side.

## Commands

```bash
mvn spring-boot:run           # Start dev server at localhost:8080 (env vars set via IDE run config)
mvn test                     # Run all tests
mvn verify                   # Full build + tests
mvn compile                  # Compile only (fast sanity check)
mvn spotless:apply            # Format all Java sources (Google Java Format, AOSP style)
mvn spotless:check            # Verify formatting without changing files
```

## Key conventions

- **Formatting: Google Java Format** (enforced via `spotless-maven-plugin`). Run `mvn spotless:apply` before committing, `mvn spotless:check` to verify.
- **Lombok everywhere:** Use `@RequiredArgsConstructor` for DI, `@Builder` on entities/DTOs, `@Slf4j` for logging. No manual constructors or getters/setters.
- **Validation at the boundary:** Use Bean Validation (`@Valid`, `@NotBlank`, `@Size`, etc.) on DTOs. Never validate inside services.
- **Error responses:** Throw `ResponseStatusException` with an appropriate `HttpStatus`. No custom exception hierarchy unless unavoidable.
- **No `@Autowired` on fields** — constructor injection only (Lombok `@RequiredArgsConstructor`).

## Package structure

```
controller/v1/
  schema/          ← API interfaces (routing, Swagger annotations, request mapping)
  implementation/  ← @RestController classes that implement the schema interfaces
entity/        ← JPA entities
model/
  action/      ← Request DTOs (input)
  core/        ← Response DTOs (output)
repository/    ← Spring Data JPA repositories
services/
  auth/        ← Auth & OAuth logic
  core/        ← Business logic per domain
configuration/ ← Spring @Configuration classes, grouped by concern
security/      ← JWT filter, security config
```

## Patterns

### Controller: schema + impl split

Every endpoint lives in two files:

**`schema/FooApi.java`** — interface only. Owns `@RequestMapping`, all HTTP method annotations, Swagger `@Tag`/`@Operation`/`@ApiResponse`, and parameter annotations (`@PathVariable`, `@RequestBody`, etc.). Paths start with `/v1/` — the `/api` prefix is added centrally via `WebMvcConfig`.

```java
@Tag(name = "Foo")
@RequestMapping("/v1/foo")
public interface FooApi {

    @Operation(summary = "...")
    @GetMapping("/{id}")
    ResponseEntity<FooDto> getFoo(@PathVariable String id);
}
```

**`implementation/FooController.java`** — `@RestController` that `implements FooApi`. No routing annotations here — only `@Override` on each method and the actual logic (or delegation to a service).

```java
@RestController
@RequiredArgsConstructor
public class FooController implements FooApi {

    private final FooService fooService;

    @Override
    public ResponseEntity<FooDto> getFoo(String id) {
        return ResponseEntity.ok(fooService.getById(id));
    }
}
```

### DTOs

- **`model/action/`** — input DTOs (request bodies, form params). Carry Bean Validation annotations.
- **`model/core/`** — output DTOs. Typically built with `@Builder`.
- **`model/admin/`** — admin-specific response DTOs (e.g. `AdminSubdomainDto`, `AdminUserDto`). Separate from `model/core/` because they include fields that should never be exposed to regular users (owner info, internal counts, etc.).
- Never expose entities directly in responses — always map to a DTO.

### Layer boundaries: controllers call services, not repositories

Controllers must only inject services (and configuration properties like `AdminProperties`). Repository calls belong exclusively in the service layer. A controller that bypasses a service to call a repository directly is a pattern to reject — it leaks data-access logic into the wrong layer.

```
Controller → Service → Repository   ✓
Controller → Repository             ✗
```

### Admin service methods

Service methods that bypass user ownership checks are prefixed with `admin` (e.g. `adminGetSubdomain`, `adminUpdateTargetIp`, `adminRelabelSubdomain`). This makes it immediately visible at the call site that the method skips the owner guard, and keeps the admin surface searchable.

### 404 instead of 403 for ownership failures

`getOwnedSubdomain` (and similar ownership checks) throws `404 NOT_FOUND` rather than `403 FORBIDDEN` when a user tries to access a resource they don't own. This avoids leaking whether the resource exists at all. Keep this consistent for any future ownership-guarded lookup.

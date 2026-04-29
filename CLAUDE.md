# Cosy Domain Provider — Backend

Spring Boot REST API for managing `cosy-hosting.net` subdomains. Frontend: `../cosy-domain-provider-frontend`.

## Cross-repo work

If a task touches the frontend, **read `../cosy-domain-provider-frontend/CLAUDE.md` before starting.**

## Commands

```bash
mvn spring-boot:run     # Start dev server at localhost:8080 (env vars set via IDE run config)
mvn test                # Run all tests
mvn verify              # Full build + tests
mvn compile             # Compile only (fast sanity check)
mvn spotless:apply      # Format all Java sources (Google Java Format, AOSP style)
mvn spotless:check      # Verify formatting without changing files
```

## Key conventions

- **Formatting:** Google Java Format via `spotless-maven-plugin` — run `mvn spotless:apply` before committing.
- **Lombok:** `@RequiredArgsConstructor` for DI, `@Builder` on entities/DTOs, `@Slf4j` for logging.
- **Validation at the boundary:** Bean Validation on DTOs only — never inside services.
- **Errors:** `ResponseStatusException` with appropriate `HttpStatus`.
- **No `@Autowired`** — constructor injection only.

## Further reading

- [`docs/patterns/conventions.md`](docs/patterns/conventions.md) — package structure & conventions detail
- [`docs/patterns/controller.md`](docs/patterns/controller.md) — schema/impl split, DTOs, layer boundaries
- [`docs/patterns/admin.md`](docs/patterns/admin.md) — admin method naming, 404 vs 403

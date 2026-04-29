# Key Conventions

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
  admin/       ← Admin-only response DTOs (owner info, internal counts)
repository/    ← Spring Data JPA repositories
services/
  auth/        ← Auth & OAuth logic
  core/        ← Business logic per domain
configuration/ ← Spring @Configuration classes, grouped by concern
security/      ← JWT filter, security config
```

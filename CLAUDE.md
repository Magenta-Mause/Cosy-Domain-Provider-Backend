# Cosy Domain Provider — Backend

Spring Boot REST API for managing `cosy-hosting.net` subdomains. Frontend: `../cosy-domain-provider-frontend`.

## Commands

```bash
mvn spring-boot:run -Plocal  # Start dev server at localhost:8080 (loads .env via spring-dotenv)
mvn test                     # Run all tests
mvn verify                   # Full build + tests
mvn compile                  # Compile only (fast sanity check)
```

## Key conventions

- **Formatting: Google Java Format** (enforced via `fmt-maven-plugin`). Run `mvn fmt:format` before committing.
- **Lombok everywhere:** Use `@RequiredArgsConstructor` for DI, `@Builder` on entities/DTOs, `@Slf4j` for logging. No manual constructors or getters/setters.
- **Validation at the boundary:** Use Bean Validation (`@Valid`, `@NotBlank`, `@Size`, etc.) on DTOs. Never validate inside services.
- **Error responses:** Throw `ResponseStatusException` with an appropriate `HttpStatus`. No custom exception hierarchy unless unavoidable.
- **No `@Autowired` on fields** — constructor injection only (Lombok `@RequiredArgsConstructor`).

## Package structure

```
controller/v1/
  schema/      ← API interfaces (routing, Swagger annotations, request mapping)
  impl/        ← @RestController classes that implement the schema interfaces
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

**`schema/FooApi.java`** — interface only. Owns `@RequestMapping`, all HTTP method annotations, Swagger `@Tag`/`@Operation`/`@ApiResponse`, and parameter annotations (`@PathVariable`, `@RequestBody`, etc.).

```java
@Tag(name = "Foo")
@RequestMapping("/api/v1/foo")
public interface FooApi {

    @Operation(summary = "...")
    @GetMapping("/{id}")
    ResponseEntity<FooDto> getFoo(@PathVariable String id);
}
```

**`impl/FooController.java`** — `@RestController` that `implements FooApi`. No routing annotations here — only `@Override` on each method and the actual logic (or delegation to a service).

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
- Never expose entities directly in responses — always map to a DTO.

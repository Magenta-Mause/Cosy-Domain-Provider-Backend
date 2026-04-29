# Controller: Schema + Impl Split

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

## DTOs

- **`model/action/`** — input DTOs (request bodies, form params). Carry Bean Validation annotations.
- **`model/core/`** — output DTOs. Typically built with `@Builder`.
- **`model/admin/`** — admin-specific response DTOs (e.g. `AdminSubdomainDto`, `AdminUserDto`). Separate from `model/core/` because they include fields that should never be exposed to regular users (owner info, internal counts, etc.).
- Never expose entities directly in responses — always map to a DTO.

## Layer boundaries

Controllers must only inject services (and configuration properties like `AdminProperties`). Repository calls belong exclusively in the service layer.

```
Controller → Service → Repository   ✓
Controller → Repository             ✗
```

# TODO — Backlog for agents

Actionable items noticed while working on this codebase. Keep this up to date as items are fixed. The full "Known issues" reference list lives in `AGENTS.md`; this file tracks the work to do about them, in priority order.

## High priority

1. **`POST /api/usuarios/login` is unreachable** (AGENTS.md bug #13). `SecurityConfig` permits `/api/usuarios` (exact path) but not `/api/usuarios/login`, so login falls into `anyRequest().authenticated()` → 403. There is no auth mechanism, so the endpoint can never succeed. Fix: add `/api/usuarios/login` to the matcher or switch to `/api/usuarios/**`.
2. **`codigo_interno_seq` missing from `wms_schema.sql`** (AGENTS.md bug #14). `ProductRepository.obtenerSiguienteSecuencia()` runs `nextval('codigo_interno_seq')`, but the schema script never creates the sequence. Creating a product with a blank barcode fails on a fresh DB, and no test can exercise internal barcode generation against a real schema. Fix: add `CREATE SEQUENCE codigo_interno_seq` to the script (one line).
3. **Orphan Stock rows on product delete** (AGENTS.md bug #1). `ProductService.deleteById` removes the `Product` but leaves the `Stock` row behind. Schema has `ON DELETE CASCADE` on `stock.id_producto`, but the service deletes the product via JPA `deleteById` without a native cascade flush, so orphans persist. Fix deliberately (cascade the delete through the repository/entity graph or delete the stock first).
4. **No global exception handler** (AGENTS.md bug #7). No `@ControllerAdvice`. `IllegalArgumentException` (duplicate CUIL/CUIT/barcode) and `RuntimeException` ("X no encontrado") both surface as raw 500s. Fix: add a `@ControllerAdvice` mapping `IllegalArgumentException` → 400, `InvalidCredentialsException` → 401, `RuntimeException` → 500, so clients get a consistent `{ "error": ... }` shape.

## Medium priority

5. **Duplicate login endpoints** (AGENTS.md bug #2). `AuthController` and `UserController.login` are identical. Pick one canonical URL and remove the other.
6. **N+1 query in `ProductService.findAll`** (AGENTS.md bug #3). Per-product `stockRepository.findById`. Fix with `JOIN FETCH` or `@EntityGraph` on `ProductRepository`.
7. **`PickOrderService.update` stale-data risk** (AGENTS.md bug #11). Lines are fetched twice after stock reversal; the second fetch may return stale rows. Fix by reusing the first fetch (e.g. `pickOrderLineRepository.deleteAll(oldLines)`) instead of re-querying.
8. **No auth flow / tests** (AGENTS.md bugs #9, #10, #12). The kept `WmsApplicationTests.contextLoads()` still hits real PostgreSQL — running the full `./mvnw test` requires a live DB, while the new service/controller tests are DB-free. Swap `contextLoads()` to Testcontainers `@ServiceConnection` (Docker is available) or a test profile so the whole suite runs without local PG.
9. **Hardcoded DB credentials** in `application.properties` (user `postgres` / password `12345`). Move to env vars / placeholders (`${DB_USERNAME}`) so the repo is safe to share and CI can configure its own.

## Low priority / cleanup

10. **Unused repository methods**: `UserRepository.findByRol`, `ProductRepository.findByCodigoBarras`, `PickOrderLineRepository.findByIdProducto`, `PickOrderRepository.findByIdUsuario` — no callers. Remove or implement features that use them.
11. **Dead code** (AGENTS.md bug #6): commented-out `@OneToMany` in `User.java` referencing non-existent `Move` / `PickUpOrder` entities. Also `AuthController`/`UserController`/`ProductController` have Spanish inline comments — decide whether to keep.
12. **Empty `Validations` class** (AGENTS.md bug #5): placeholder with no methods. Fill it or delete it.
13. **Unused deps** (AGENTS.md bug #8): `h2` and `mysql-connector-j` in `pom.xml` — only PostgreSQL is used.
14. **ZXing declared but unused**: `com.google.zxing` 3.5.3 is in `pom.xml` (barcode generation) — no code references it yet.
15. **Schema/entity drift to verify**: `Product.descripcionProducto` is `nullable = false` in the entity but nullable in `wms_schema.sql`; `User.cuil` has no `length` on the column (schema uses `VARCHAR(20)`). Harmless under `ddl-auto=validate` today, but worth aligning.
16. **Validation gap in `PickOrderCreateDTO`**: `lineasRetiro` is `@NotNull` only, so an empty `[]` passes validation despite the message "La orden debe tener al menos una línea". Add `@Size(min = 1)` if that rule is intended.
17. **`PickOrder` has no JPA FK to `User`** (AGENTS.md bug #4): `idUsuario` is a raw `Long`. Schema does have the FK, so this is entity-level only.

## Test infrastructure (new, from the testing pass)

- Service unit tests + `@WebMvcTest` controller tests added under `wms/src/test/java/big_three/wms/`. They run without a DB.
- `@WebMvcTest` tests `@Import(SecurityConfig.class)` explicitly and use `@MockitoBean` (Boot 4 — `@MockBean` is deprecated).
- `UserControllerTest.login_requiresAuthentication` documents bug #13: it asserts the login endpoint is 403-blocked.
- If the schema is ever fixed (item 2), add repository/integration coverage for internal barcode generation and the real stock flow.

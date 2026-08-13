# TODO — Backlog for agents

Actionable items noticed while working on this codebase. Keep this up to date as items are fixed. The full "Known issues" reference list lives in `AGENTS.md`; this file tracks the work to do about them, in priority order.

## High priority

1. [X] **`POST /api/usuarios/login` is unreachable** (AGENTS.md bug #13). `SecurityConfig` permits `/api/usuarios` (exact path) but not `/api/usuarios/login`, so login falls into `anyRequest().authenticated()` → 403. There is no auth mechanism, so the endpoint can never succeed. Fix: add `/api/usuarios/login` to the matcher or switch to `/api/usuarios/**`.
   - **What I did:** the duplicate `/api/usuarios/login` endpoint was removed in item 5 (dedupe), so this bug is moot — login goes through the already-permitted `POST /api/auth/login`. A real security/auth mechanism is still pending (another team member); do not implement it until then.
2. [X] **`codigo_interno_seq` missing from `wms_schema.sql`** (AGENTS.md bug #14). `ProductRepository.obtenerSiguienteSecuencia()` runs `nextval('codigo_interno_seq')`, but the schema script never creates the sequence. Creating a product with a blank barcode fails on a fresh DB, and no test can exercise internal barcode generation against a real schema.
   - **What I did:** added `DROP SEQUENCE IF EXISTS codigo_interno_seq;` to the cleanup block (with the `DROP TABLE ... CASCADE` lines) and `CREATE SEQUENCE codigo_interno_seq;` above the `producto` section in `wms_schema.sql`.
   - **Database change required — how to apply**: the schema script is the source of truth (`ddl-auto=validate`), so the sequence must exist in the running DB.
     - *Fresh DB*: run the whole script as usual — `CREATE DATABASE wms_db; \c wms_db; \i wms_schema.sql`.
     - *Existing DB with data*: do **NOT** re-run the whole script (its `DROP TABLE ... CASCADE` block wipes all tables). Apply just the sequence:
       ```sql
       CREATE SEQUENCE IF NOT EXISTS codigo_interno_seq;
       ```
       (to reset it: `DROP SEQUENCE IF EXISTS codigo_interno_seq; CREATE SEQUENCE codigo_interno_seq;`).
     - No `ddl-auto` change needed: Hibernate validates tables only, not native-query sequences.
   - Verify: start the app and `POST /api/productos` with blank `codigoBarras` → 201 with `codigoBarras: "INT-000001"`. Real-DB integration coverage is still pending (see Test infrastructure note).
3. [X] **Orphan Stock rows on product delete** (AGENTS.md bug #1). `ProductService.deleteById` removed the `Product` but left the `Stock` row behind.
   - **What I did:** `deleteById` is now `@Transactional`, deletes the `Stock` row first (via `stockRepository.findById(id).ifPresent(...)`, so products without a stock row still delete) and then the `Product`. Tests updated: `deleteById_deletesProductAndStock`, `deleteById_noStockRow_stillDeletesProduct`.
4. [X] **No global exception handler** (AGENTS.md bug #7). No `@ControllerAdvice`. `IllegalArgumentException` (duplicate CUIL/CUIT/barcode) and `RuntimeException` ("X no encontrado") both surface as raw 500s.
   - **What I did:** added `exception/GlobalExceptionHandler` (`@ControllerAdvice`) mapping `InvalidCredentialsException` → 401, `IllegalArgumentException` → 400, `RuntimeException` → 500, all as `{ "error": message }`. Removed the redundant try/catch in both login controllers so 401s come from the advice. Added `ProductControllerTest` cases `create_duplicateBarcode_returns400` and `create_productNotFound_returns500`. Validation failures keep Spring's default 400 structure.

## Medium priority

5. [X] **Duplicate login endpoints** (AGENTS.md bug #2). `AuthController` and `UserController.login` are identical. Pick one canonical URL and remove the other.
   - **What I did:** kept `AuthController` (`POST /api/auth/login`, already permitted in `SecurityConfig`) as the single login endpoint and removed `UserController.login` (`POST /api/usuarios/login`) plus its `LoginRequestDTO` import. Deleted the obsolete `UserControllerTest.login_requiresAuthentication` test. This also resolves TODO item 1 / bug #13. **Frontend note:** if the Angular app calls `/api/usuarios/login`, update it to `/api/auth/login`.
6. [X] **N+1 query in `ProductService.findAll`** (AGENTS.md bug #3). Per-product `stockRepository.findById`. Fix with `JOIN FETCH` or `@EntityGraph` on `ProductRepository`.
   - **What I did:** replaced the per-product `stockRepository.findById` with a single `stockRepository.findAll()` mapped by `idProducto` (no Product↔Stock JPA association exists, so an `@EntityGraph` can't fetch Stock). The list endpoint now runs exactly 2 queries (all products + all stock) instead of 1 + N. Updated `ProductServiceTest.findAll_includesStock` to stub `stockRepository.findAll()` and assert `findById` is never called.
7. [X] **`PickOrderService.update` stale-data risk** (AGENTS.md bug #11). Lines are fetched twice after stock reversal; the second fetch may return stale rows. Fix by reusing the first fetch (e.g. `pickOrderLineRepository.deleteAll(oldLines)`) instead of re-querying.
   - **What I did:** `update` and `deleteById` now reuse the already-fetched line list and delete it with `pickOrderLineRepository.deleteAll(lines)` instead of calling `findByIdOrdenRetiro` a second time. Updated `PickOrderServiceTest.update_reversesOldLinesAndAppliesNewDeltas` and `deleteById_revertsStockAndDeletesLinesAndOrder` accordingly.
8. **No auth flow / tests** (AGENTS.md bugs #9, #10, #12). The kept `WmsApplicationTests.contextLoads()` still hits real PostgreSQL — running the full `./mvnw test` requires a live DB, while the new service/controller tests are DB-free. Swap `contextLoads()` to Testcontainers `@ServiceConnection` (Docker is available) or a test profile so the whole suite runs without local PG.
9. **Hardcoded DB credentials** in `application.properties` (user `postgres` / password `12345`). Move to env vars / placeholders (`${DB_USERNAME}`) so the repo is safe to share and CI can configure its own.

## Low priority / cleanup

10. [X] **Unused repository methods**: `UserRepository.findByRol`, `ProductRepository.findByCodigoBarras`, `PickOrderLineRepository.findByIdProducto`, `PickOrderRepository.findByIdUsuario` — no callers. Remove or implement features that use them.
    - **What I did:** deleted all four methods plus the now-unused `java.util.List`/`java.util.Optional` imports. Kept `findByCuil`/`existsByCuil` (used by `UserService.login`/`create`).
11. [X] **Dead code** (AGENTS.md bug #6): commented-out `@OneToMany` in `User.java` referencing non-existent `Move` / `PickUpOrder` entities. Also `AuthController`/`UserController`/`ProductController` have Spanish inline comments — decide whether to keep.
    - **What I did:** removed the commented-out `@OneToMany` blocks and stale header comments from `User.java`; removed redundant/stale inline comments from `AuthController` (`} //constructor`, "delega el manejo de errores al @ControllerAdvice") and `ProductController` ("El service ahora devuelve directamente el DTO limpio..."). Kept meaningful comments (repository naming conventions, SecurityConfig rationale).
12. [X] **Empty `Validations` class** (AGENTS.md bug #5): placeholder with no methods. Fill it or delete it.
    - **What I did:** deleted `util/Validations.java` (no callers) and removed the empty `util` package.
13. [X] **Unused deps** (AGENTS.md bug #8): `h2` and `mysql-connector-j` in `pom.xml` — only PostgreSQL is used.
    - **What I did:** removed both dependencies from `pom.xml`. Note: if item 8 ends up using an H2 test profile (instead of Testcontainers), `h2` must be re-added then.
14. **ZXing declared but unused**: `com.google.zxing` 3.5.3 is in `pom.xml` (barcode generation) — no code references it yet. — **KEPT intentionally**: barcode generation is a planned feature; the deps stay until it's built or dropped for good.
15. [X] **Schema/entity drift to verify**: `Product.descripcionProducto` is `nullable = false` in the entity but nullable in `wms_schema.sql`; `User.cuil` has no `length` on the column (schema uses `VARCHAR(20)`). Harmless under `ddl-auto=validate` today, but worth aligning.
    - **What I did:** aligned the entities to the schema — `Product.descripcionProducto` no longer declares `nullable = false`, and `User.cuil` now declares `length = 20`. No DB change needed.
16. [X] **Validation gap in `PickOrderCreateDTO`**: `lineasRetiro` is `@NotNull` only, so an empty `[]` passes validation despite the message "La orden debe tener al menos una línea". Add `@Size(min = 1)` if that rule is intended.
    - **What I did:** added `@Size(min = 1, message = "La orden debe tener al menos una línea")` to `lineasRetiro` and added `PickOrderControllerTest.create_emptyLineas_returns400`.
17. [X] **`PickOrder` has no JPA FK to `User`** (AGENTS.md bug #4): `idUsuario` is a raw `Long`. Schema does have the FK, so this is entity-level only.
    - **What I did (intentional, not fixed):** kept the raw `Long` and documented the decision in `PickOrder.java`. **Possible conflict:** without a JPA relation there is no entity-level referential integrity (only the DB FK guarantees it); mapping it would force lazy-loading the `User` when building responses, which conflicts with `open-in-view=false` and would ripple into `PickOrderResponseDTO`/`PickOrderService`. Revisit if a user fetch is ever needed in order responses.

## Test infrastructure (new, from the testing pass)

- Service unit tests + `@WebMvcTest` controller tests added under `wms/src/test/java/big_three/wms/`. They run without a DB.
- `@WebMvcTest` tests `@Import(SecurityConfig.class)` explicitly and use `@MockitoBean` (Boot 4 — `@MockBean` is deprecated).
- Schema now creates `codigo_interno_seq` (item 2). Pending integration coverage for internal barcode generation and the real stock flow — add once the suite runs against a DB (item 8 Testcontainers work).

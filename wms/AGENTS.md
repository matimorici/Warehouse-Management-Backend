# Warehouse-Management-Backend (WMS)

Backend REST API for a warehouse management system (products, suppliers, stock, pick orders). Spring Boot 4.1.0, Java 25, Lombok, PostgreSQL.

The Maven module lives in `wms/`. The workspace root also holds `README.md` (user-facing docs), this file, and `TODO.md` (the actionable backlog of things to keep working on — read it before starting a task).

## Commands (run from `wms/`)

| Action | Command |
|--------|---------|
| Build | `./mvnw clean compile` |
| Test | `./mvnw test` |
| Run | `./mvnw spring-boot:run` |
| Package | `./mvnw package` |

No lint, format, typecheck, or CI configured. `javac` is the only typechecker.

## Configuration

- **Package**: `big_three.wms`, entrypoint `WmsApplication.java` (port 8080).
- **DB**: PostgreSQL `wms_db` on `localhost:5432`, user `postgres`, password `12345` (hardcoded in `application.properties`). **Schema is owned by Flyway** (`org.flywaydb:flyway-core` + `flyway-database-postgresql`): migrations live in `src/main/resources/db/migration/` and run automatically at startup, before Hibernate. Current chain: `V1__create_schema.sql` (full schema incl. `codigo_interno_seq` and tables with no JPA entity yet — reserved for planned features), `V2__ensure_codigo_interno_seq.sql` (repair for old baselined DBs missing the sequence — no-op on fresh DBs), `V3__seed_admin_user.sql` (default admin: CUIL `20-00000000-1`, password `Admin1234`, rol `ADMINISTRADOR`). `spring.flyway.baseline-on-migrate=true` — existing dev DBs (created with the old `wms_schema.sql`) are baseline-marked as V1 on first boot, no data loss; fresh DBs build from scratch. The old script survives read-only at `src/main/resources/db/wms_schema.sql` as historical reference only. `ddl-auto=validate` only verifies entities match the migrated schema. `show-sql=true`, `open-in-view=false`.
- **No test profile / H2 override** — `@SpringBootTest` hits real PostgreSQL. `h2`/`mysql-connector-j` were removed from `pom.xml` (PostgreSQL only); only `postgresql` runtime driver remains.
- **Frontend CORS**: `http://localhost:4200` (Angular), set per-controller via `@CrossOrigin`.
- **Libraries**: ZXing 3.5.3 (barcode generation) declared but not used anywhere yet — kept intentionally for a planned feature. Lombok excluded from final artifact by `spring-boot-maven-plugin`.
- `wms/doc/` contains generated Javadoc — ignore it.

## Security

`SecurityConfig.java` defines a `BCryptPasswordEncoder` bean and a filter chain with CSRF disabled. Current `permitAll()` list (single-path matchers, **exact paths only — no trailing `/**`**):

- `POST /api/usuarios` and `GET /api/usuarios` → `permitAll`
- `POST /api/auth/login`, `/api/proveedores/**`, `/api/productos/**`, `/api/ordenes-retiro/**` → `permitAll`
- Everything else → `anyRequest().authenticated()`, including:
  - `GET /api/usuarios/{id}`, `DELETE /api/usuarios/{id}`

There is no `UserDetailsService`, JWT, session management, or token mechanism. The app has no real authentication flow; `authenticated()` paths are effectively unreachable. The only login endpoint is `POST /api/auth/login` (permitted) — the old duplicate `POST /api/usuarios/login` was removed.

## Project structure

```
wms/src/main/java/big_three/wms/
├── WmsApplication.java        # entrypoint
├── config/SecurityConfig.java # PasswordEncoder + SecurityFilterChain
├── controller/                # AuthController, PickOrderController, ProductController, ProveedorController, UserController
├── dto/                       # 14 DTOs, one per request/response (see API section)
├── exception/InvalidCredentialsException.java
├── model/                     # User, Product, Proveedor, Stock, PickOrder, PickOrderLine
├── repository/                # 6 Spring Data JPA repositories
└── service/                   # UserService, ProductService, ProveedorService, PickOrderService
```

## Entities (6)

| Entity | Table | PK | Key relationships |
|--------|-------|----|-------------------|
| `User` | `usuario` | `id_usuario` (Long, IDENTITY) | — |
| `Product` | `producto` | `id_producto` (Long, IDENTITY) | `@ManyToOne` → `Proveedor` |
| `Proveedor` | `proveedor` | `id_proveedor` (Long, IDENTITY) | — |
| `Stock` | `stock` | `id_producto` (Long, no auto-gen) | 1:1 with `Product` (same PK, FK `ON DELETE CASCADE` in schema) |
| `PickOrder` | `orden_retiro` | `id_orden_retiro` (Long, IDENTITY) | `idUsuario` stored as raw `Long` (no `@ManyToOne`, no JPA FK) — **intentional**, see Known issues #4 |
| `PickOrderLine` | `linea_retiro` | `@IdClass(PickOrderLineId)`: `id_orden_retiro` + `id_producto` | Composite PK |

- `Product` has inner enum `OrigenCodigoBarras { FABRICANTE, INTERNO }` (mapped `@Enumerated(EnumType.STRING)`).
- `User` fields: `nombre`, `apellido`, `cuil`, `rol` (default `"OPERARIO"`), `contrasena` (BCrypt hash).
- `Stock`: `cantidadDisponible`, `cantidadPendiente`, `fechaHora`.
- `PickOrderLine`: `cantidad` (Integer).

## Business logic (important)

- **Product create** (`ProductService.create`): if `codigoBarras` is provided and unique → `origen = FABRICANTE`; if blank/null → generates `INT-XXXXXX` via `nextval('codigo_interno_seq')` and `origen = INTERNO`. Also creates a `Stock` row (defaults to 0). Duplicate barcode → `IllegalArgumentException`. The sequence is created by migration `V1__create_schema.sql` (`codigo_interno_seq`).
- **Product update** (`update`): also upserts the `Stock` row (creates with 0s if missing). Existing product + no new barcode keeps its barcode/origen.
- **Product delete** (`deleteById`): deletes the `Stock` row first (if present), then the `Product` — no orphans. Note: the `stock` FK already has `ON DELETE CASCADE` at the DB level, so the manual delete is defensive belt-and-suspenders, not the sole guard — keep it.
- **PickOrder create** (`PickOrderService.create`): validates `idUsuario` and every `idProducto` exist, saves order + lines, then calls `productService.ajustarStock(idProducto, -cantidad, +cantidad)` → `disponible -= cantidad`, `pendiente += cantidad`; throws if `disponible` would go negative.
- **PickOrder update** (`update`): reverses old lines' stock, deletes old lines, saves new lines, applies new stock deltas. Known stale-data risk (see Known issues).
- **PickOrder delete** (`deleteById`): reverts stock (`+cantidad` disponible, `-cantidad` pendiente), deletes lines, deletes order.
- **GET /api/ordenes-retiro** returns summaries with `lineasRetiro: null`; only `GET /api/ordenes-retiro/{id}` includes the lines.
- **Response DTOs never include the password hash.** Login (`UserService.login`) just verifies CUIL+password and returns the user DTO — no token/session.

## API endpoints

All controllers have `@CrossOrigin(origins = "http://localhost:4200")`. All routes under `/api/`. Validation failures return 400 with Spanish messages.

### Usuarios — `UserController`

| Method | Path | Auth | Body | Returns |
|--------|------|------|------|---------|
| POST | `/api/usuarios` | No | `UserCreateDTO`: `nombre`, `apellido`, `cuil` (`20-12345678-9` or 11 digits), `contrasena` (≥8, 1 uppercase, 1 digit) | 201 `UserResponseDTO` |
| GET | `/api/usuarios` | No | — | `List<UserResponseDTO>` |
| GET | `/api/usuarios/{id}` | Yes | — | `UserResponseDTO` |
| DELETE | `/api/usuarios/{id}` | Yes | — | 204 |

`UserResponseDTO`: `idUsuario`, `nombre`, `apellido`, `cuil`, `rol`.

### Auth — `AuthController` (canonical login endpoint)

| Method | Path | Auth | Body | Returns |
|--------|------|------|------|---------|
| POST | `/api/auth/login` | No | `LoginRequestDTO` | 200 `UserResponseDTO` / 401 `{error}` |

### Productos — `ProductController`

| Method | Path | Auth | Body | Returns |
|--------|------|------|------|---------|
| POST | `/api/productos` | No | `ProductCreateDTO`: `nombreProducto`, `descripcionProducto`, `codigoBarras`?, `idProveedor`, `origenCodigoBarras` (`FABRICANTE`/`INTERNO`), `cantidadDisponible`?, `cantidadPendiente`? | 201 `ProductResponseDTO` |
| GET | `/api/productos` | No | — | `List<ProductResponseDTO>` (with stock; N+1) |
| GET | `/api/productos/{id}` | No | — | `ProductResponseDTO` |
| PUT | `/api/productos/{id}` | No | `ProductCreateDTO` | `ProductResponseDTO` |
| DELETE | `/api/productos/{id}` | No | — | 204 |
| GET | `/api/productos/{id}/stock` | No | — | `StockResponseDTO` |
| PUT | `/api/productos/{id}/stock` | No | `StockUpdateDTO`: `cantidadDisponible`?, `cantidadPendiente`? | `StockResponseDTO` |

`ProductResponseDTO`: `idProducto`, `nombreProducto`, `descripcionProducto`, `codigoBarras`, `idProveedor`, `origenCodigoBarras`, `cantidadDisponible`, `cantidadPendiente`, `stockFechaHora`.

### Proveedores — `ProveedorController`

| Method | Path | Auth | Body | Returns |
|--------|------|------|------|---------|
| POST | `/api/proveedores` | No | `ProveedorCreateDTO`: `cuit` (`20-12345678-9` or 11 digits), `razonSocial`, `telefono`?, `mail`?, `direccion`? | 201 `ProveedorResponseDTO` |
| GET | `/api/proveedores` | No | — | `List<ProveedorResponseDTO>` |
| GET | `/api/proveedores/{id}` | No | — | `ProveedorResponseDTO` |
| PUT | `/api/proveedores/{id}` | No | `ProveedorCreateDTO` | `ProveedorResponseDTO` |
| DELETE | `/api/proveedores/{id}` | No | — | 204 |

### Órdenes de retiro — `PickOrderController`

| Method | Path | Auth | Body | Returns |
|--------|------|------|------|---------|
| POST | `/api/ordenes-retiro` | No | `PickOrderCreateDTO`: `idUsuario`, `lineasRetiro[]`: `{idProducto, cantidad ≥ 1}` | 201 `PickOrderResponseDTO` (with lines) |
| GET | `/api/ordenes-retiro` | No | — | `List<PickOrderResponseDTO>` (summaries, `lineasRetiro: null`) |
| GET | `/api/ordenes-retiro/{id}` | No | — | `PickOrderResponseDTO` (with lines) |
| PUT | `/api/ordenes-retiro/{id}` | No | `PickOrderCreateDTO` | `PickOrderResponseDTO` |
| DELETE | `/api/ordenes-retiro/{id}` | No | — | 204 (reverts stock) |

## Error handling

- `GlobalExceptionHandler` (`exception/GlobalExceptionHandler.java`) is the single `@ControllerAdvice` for unhandled exceptions, always responding `{ "error": message }`:
  - `InvalidCredentialsException` → 401
  - `IllegalArgumentException` (duplicate CUIL/CUIT/barcode) → 400
  - `RuntimeException` (e.g. "Producto no encontrado") → 500
- Bean Validation failures → 400 with Spring's default field-error structure.
- `InvalidCredentialsException` exists solely for login; the duplicate-email/duplicate-CUIL/duplicate-barcode checks throw `IllegalArgumentException`.

## Known issues (do not reintroduce)

1. ~~**Orphan Stock on Product delete**~~ **FIXED**: `ProductService.deleteById` deletes the `Stock` row (if present) before the `Product`.
2. ~~**Duplicate login endpoints**~~ **FIXED**: `UserController.login` (`POST /api/usuarios/login`) removed; `AuthController` `POST /api/auth/login` is the single login endpoint.
3. ~~**N+1 query** (`ProductService.findAll`, `ProductService.java:135`)~~ **FIXED**: `findAll` now batch-fetches all `Stock` rows in one query and maps by `idProducto` (2 queries total).
4. **PickOrder.idUsuario is raw Long** (`PickOrder.java:25`): no JPA-level referential integrity with `User`. — **INTENTIONAL** (see TODO item 17): the DB FK is the only guard; mapping a `@ManyToOne` would force lazy-loading `User` when building responses, conflicting with `open-in-view=false` and rippling into `PickOrderResponseDTO`/`PickOrderService`. Revisit if order responses ever need the user object.
5. ~~**Empty Validations class**~~ **FIXED**: `util/Validations.java` deleted (no callers).
6. ~~**Dead code in User.java**~~ **FIXED**: commented-out `@OneToMany` blocks and stale header comments removed from `model/User.java`.
7. ~~**No global exception handler**~~ **FIXED**: `GlobalExceptionHandler` `@ControllerAdvice` maps `InvalidCredentialsException` → 401, `IllegalArgumentException` → 400, `RuntimeException` → 500.
8. ~~**Unused dependencies in pom.xml**~~ **FIXED**: `h2` and `mysql-connector-j` removed — only PostgreSQL is used. (Note: ZXing stays — kept intentionally for planned barcode generation.)
9. **No real authentication**: login returns a DTO but no token/session; `authenticated()` endpoints are unreachable.
10. **Single test** (`WmsApplicationTests.contextLoads()`): no test profile, no integration/unit tests.
11. ~~**PickOrderService.update() stale data risk** (`PickOrderService.java`)~~ **FIXED**: `update`/`deleteById` reuse the already-fetched line list (`deleteAll(lines)`) instead of re-querying after the stock reversal.
12. **Test hits real DB**: `@SpringBootTest` connects to PostgreSQL — no H2/test override.
13. ~~**Login endpoint requires auth**~~ **FIXED**: `SecurityConfig` `permitAll("/api/usuarios")` didn't cover `/api/usuarios/login`, so login fell into `anyRequest().authenticated()`. The duplicate `/api/usuarios/login` endpoint was removed entirely; login lives at the permitted `POST /api/auth/login`.
14. ~~**`codigo_interno_seq` missing from schema**~~ **FIXED**: `V1__create_schema.sql` now creates the sequence (migration-superseded; no manual DB steps needed anymore).

## Work rules

- Stick to the exact task asked. Do not fix other bugs, refactor, or touch unrelated files unless explicitly told to.
- If a task uncovers related issues, ask before fixing them.
- **Schema changes go through Flyway migrations.** Never hand-run SQL against a shared/dev DB, never edit an already-applied migration (they are immutable — checksums in `flyway_schema_history` will fail the boot), and never rely on `ddl-auto=create/update`. New structural change → add `db/migration/V<n+1>__<desc>.sql`; it applies automatically on next boot. Keep the chain sorted; don't renumber existing versions.
- Seed/reference data (e.g. users) also goes in migrations so it exists identically on every DB.
- **V2 baseline-repair verification is pending a teammate to actually run** against a genuinely old pre-sequence DB — `bash scripts/verify-v2-repair.sh` (needs local PostgreSQL; creates/disposes a scratch DB `wms_db_v2_test`, never touches `wms_db`). Don't mark the V2 gap "fully closed" until someone has run it and seen `PASÓ`.

## Code style

- DTO/entity mapping done manually (no MapStruct).
- `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` from Lombok on entities.
- Spanish messages in validation annotations.
- `@CrossOrigin(origins = "http://localhost:4200")` on each controller class.
- Jakarta Bean Validation on DTOs (`@NotBlank`, `@NotNull`, `@Size`, `@Pattern`, `@Min`, `@Email`, `@Valid`).
- DB tables use snake_case; Java fields use camelCase.
- PKs use `GenerationType.IDENTITY` (except `Stock`, which shares Product's PK).
- Composite PKs use `@IdClass`.
- `@Transactional` on service methods that modify multiple entities.

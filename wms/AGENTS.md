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

`SecurityConfig.java` defines:
- `passwordEncoder` bean (`BCryptPasswordEncoder`).
- `authenticationProvider` bean (`DaoAuthenticationProvider`), wired to `CustomUserDetailsService` (`big_three.wms.security`, looks up `User` by CUIL via `UserRepository`, maps to `UserDetails` with `ROLE_<enum name>` authority) and the `PasswordEncoder` above.
- `authenticationManager` bean, obtained from `AuthenticationConfiguration`.
- `filterChain`: CSRF still disabled (pending Fase 4). Session management configured with `sessionFixation().changeSessionId()` (session ID regenerated on login). Session timeout: `server.servlet.session.timeout=90m` (`application.properties`).

`POST /api/auth/login` (`AuthController`) authenticates via `AuthenticationManager.authenticate(...)`, stores the result in the `SecurityContext`, and persists it to the `HttpSession` via `HttpSessionSecurityContextRepository` — this is what generates the `JSESSIONID` cookie. `UserService.login()` still runs afterward to build the response DTO; the CUIL/password check now effectively happens twice (once via `AuthenticationManager`, once inside `UserService.login`) — known duplication, not yet cleaned up (see `TODO.md`).

Current `permitAll()` list (single-path matchers, **exact paths only — no trailing `/**`**):

- `POST /api/usuarios` and `GET /api/usuarios` → `permitAll`
- `POST /api/auth/login`, `/api/proveedores/**`, `/api/productos/**`, `/api/ordenes-retiro/**`, `/api/ordenes-compra/**`, `/api/ubicaciones/**`, `/api/valoraciones-proveedor/**` → `permitAll`
- Everything else → `anyRequest().authenticated()`, including:
  - `GET /api/usuarios/{id}`, `DELETE /api/usuarios/{id}`

Role-based authorization (which authenticated routes require which role) is not yet implemented — see `TODO.md`, security section, Fase 5. Any `@WebMvcTest` that also does `@Import(SecurityConfig.class)` needs a `@MockitoBean` of `UserDetailsService` (stubbed to return a valid `UserDetails`), or context loading fails — `authenticationProvider` requires a real `UserDetailsService` bean to construct, and `@WebMvcTest` doesn't scan `@Service` classes by default.

## Project structure

```
wms/src/main/java/big_three/wms/
├── WmsApplication.java        # entrypoint
├── config/SecurityConfig.java # PasswordEncoder + SecurityFilterChain
├── controller/                # AuthController, LocationController, PickOrderController, ProductController, ProveedorController, PurchaseOrderController, SupplierRatingController, UserController, PhysicalMovementController
├── dto/                       # 23 DTOs, one per request/response (see API section)
├── exception/InvalidCredentialsException.java
├── model/                     # Location, User, Product, Proveedor, Stock, PickOrder, PickOrderLine, PurchaseOrder, PurchaseOrderLine, SupplierRating, PhysicalMovement
├── repository/                # 11 Spring Data JPA repositories
└── service/                   # LocationService, UserService, ProductService, ProveedorService, PickOrderService, PurchaseOrderService, SupplierRatingService, PhysicalMovementService
```

## Entities (11)

| Entity | Table | PK | Key relationships |
|--------|-------|----|-------------------|
| `Location` | `ubicacion` | `id_ubicacion` (Long, IDENTITY) | — |
| `User` | `usuario` | `id_usuario` (Long, IDENTITY) | — |
| `Product` | `producto` | `id_producto` (Long, IDENTITY) | `@ManyToOne` → `Proveedor` |
| `Proveedor` | `proveedor` | `id_proveedor` (Long, IDENTITY) | — |
| `Stock` | `stock` | `id_producto` (Long, no auto-gen) | 1:1 with `Product` (same PK, FK `ON DELETE CASCADE` in schema) |
| `PickOrder` | `orden_retiro` | `id_orden_retiro` (Long, IDENTITY) | `idUsuario` stored as raw `Long` (no `@ManyToOne`, no JPA FK) — **intentional**, see Known issues #4 |
| `PickOrderLine` | `linea_retiro` | `@IdClass(PickOrderLineId)`: `id_orden_retiro` + `id_producto` | Composite PK |
| `PurchaseOrder` | `orden_compra` | `id_orden_compra` (Long, IDENTITY) | `idSupplier` stored as raw `Long` (no JPA FK, DB FK only) — same pattern as `PickOrder.idUsuario` |
| `PurchaseOrderLine` | `linea_compra` | `@IdClass(PurchaseOrderLineId)`: `id_orden_compra` + `id_producto` | Composite PK |
| `SupplierRating` | `valoracion_proveedor` | `id_valoracion` (Long, IDENTITY) | `idSupplier` stored as raw `Long` (no JPA FK, DB FK `ON DELETE CASCADE` only) — same pattern as `PurchaseOrder.idSupplier` |
| `PhysicalMovement` | `movimiento_fisico` | `@IdClass(PhysicalMovementId)`: `id_producto` + `fecha_hora` | Composite PK; `idProduct`, `idUser`, `idLocationFrom`/`idLocationTo` all stored as raw `Long`s (no JPA FKs) |

- `Product` has inner enum `OrigenCodigoBarras { FABRICANTE, INTERNO }` (mapped `@Enumerated(EnumType.STRING)`).
- `Location` fields: `name` (mapped to `nombre_ubicacion`).
- `User` fields: `nombre`, `apellido`, `cuil`, `rol` (default `"OPERARIO"`), `contrasena` (BCrypt hash).
- `Stock`: `cantidadDisponible`, `cantidadPendiente`, `fechaHora`.
- `PickOrderLine`: `cantidad` (Integer).
- `PurchaseOrder`: inner enum `Status { PENDIENTE, RECIBIDA, CANCELADA }` (mapped `@Enumerated(EnumType.STRING)`).
- `SupplierRating`: `id` (mapped to `id_valoracion`), `idSupplier` (mapped to `id_proveedor`), `dateTime` (mapped to `fecha_hora`), `deliveryTime` (mapped to `tiempo_entrega`, nullable), `deliveryMethod` (mapped to `forma_entrega`), `priceQualityRatio` (mapped to `relacion_precio_calidad`).
- `PhysicalMovement`: `idProduct` (mapped to `id_producto`, part of PK), `dateTime` (mapped to `fecha_hora`, part of PK), `idLocationFrom` (mapped to `id_ubicacion_desde`, nullable), `idLocationTo` (mapped to `id_ubicacion_hasta`), `idUser` (mapped to `id_usuario`).

## Business logic (important)

- **Product create** (`ProductService.create`): if `codigoBarras` is provided and unique → `origen = FABRICANTE`; if blank/null → generates `INT-XXXXXX` via `nextval('codigo_interno_seq')` and `origen = INTERNO`. Also creates a `Stock` row (defaults to 0). Duplicate barcode → `IllegalArgumentException`. The sequence is created by migration `V1__create_schema.sql` (`codigo_interno_seq`).
- **Product update** (`update`): also upserts the `Stock` row (creates with 0s if missing). Existing product + no new barcode keeps its barcode/origen.
- **Product delete** (`deleteById`): deletes the `Stock` row first (if present), then the `Product` — no orphans. Note: the `stock` FK already has `ON DELETE CASCADE` at the DB level, so the manual delete is defensive belt-and-suspenders, not the sole guard — keep it.
- **PickOrder create** (`PickOrderService.create`): validates `idUsuario` and every `idProducto` exist, saves order + lines, then calls `productService.ajustarStock(idProducto, -cantidad, +cantidad)` → `disponible -= cantidad`, `pendiente += cantidad`; throws if `disponible` would go negative.
- **PickOrder update** (`update`): reverses old lines' stock, deletes old lines, saves new lines, applies new stock deltas. Known stale-data risk (see Known issues).
- **PickOrder delete** (`deleteById`): reverts stock (`+cantidad` disponible, `-cantidad` pendiente), deletes lines, deletes order.
- **GET /api/ordenes-retiro** returns summaries with `lineasRetiro: null`; only `GET /api/ordenes-retiro/{id}` includes the lines. Same for `GET /api/ordenes-compra` (summaries, `lines: null`) vs `GET /api/ordenes-compra/{id}` (with lines).
- **PurchaseOrder create** (`PurchaseOrderService.create`): validates `idSupplier` and every `idProduct` exist, saves order with `fechaHora`=now and `estado`=`PENDIENTE`, saves lines. No stock adjustment on create.
- **PurchaseOrder receive** (`receive`): only from `PENDIENTE`; calls `ajustarStock(idProduct, +cantidad, 0)` (`disponible += cantidad`) for every line and marks the order `RECIBIDA`. Throws `IllegalArgumentException` if already `RECIBIDA`, `RuntimeException` if `CANCELADA`.
- **PurchaseOrder update** (`update`): replaces supplier + lines; if the order was `RECIBIDA`, it first reverses old lines' stock (`-cantidad` disponible), deletes old lines, saves new lines, then re-applies stock for the new lines.
- **PurchaseOrder delete** (`deleteById`): if `RECIBIDA`, reverts stock (`-cantidad` disponible) before deleting lines + order.
- **Location create** (`LocationService.create`): saves a new `Location` (single `name` field).
- **Location findById/update** (`LocationService.findId`/`update`): throw `RuntimeException("Ubicación no encontrada")` when the location is missing.
- **Location delete** (`LocationService.deleteById`): checks `existsById` first and throws `RuntimeException("Ubicación no encontrada para eliminar")` otherwise.
- **SupplierRating create** (`SupplierRatingService.create`): validates the supplier exists (`ProveedorRepository.findById(...)` → `RuntimeException("Proveedor no encontrado")`), sets `dateTime`=now, and stores the rest of the fields.
- **SupplierRating findBySupplier** (`SupplierRatingService.findBySupplier`): validates the supplier exists too (`existsById`), then returns its ratings (empty list if none).
- **SupplierRating update** (`SupplierRatingService.update`): does **not** change `idSupplier` (the DTO field is ignored on `PUT`) — only `deliveryTime`, `deliveryMethod`, `priceQualityRatio`, and refreshes `dateTime`; throws `RuntimeException("Valoración no encontrada")` if the rating is missing.
- **SupplierRating delete** (`SupplierRatingService.deleteById`): checks `existsById` first and throws `RuntimeException("Valoración no encontrada para eliminar")` otherwise.
- **PhysicalMovement create** (`PhysicalMovementService.create`): **append-only** — only records a movement event; there are no update/delete paths (no `PUT`/`DELETE` endpoints). Validates that the product, source location (only when provided), destination location, and user exist — each failure throws `IllegalArgumentException` (→ 400, unlike the rest of the app's "no encontrado" which is `RuntimeException` → 500) — and stamps `dateTime`=now server-side (`LocalDateTime.now()`). Does **not touch `Stock`**.
- **PhysicalMovement reads** (`findAll`, `findByIdProduct`, `findByIdUser`, `findByDateTimeRange`): list events (newest first) or filtered by product, user, or date range (`from`+`to` both required, else → 400); records are never modified.
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

### Ubicaciones — `LocationController`

| Method | Path | Auth | Body | Returns |
|--------|------|------|------|---------|
| POST | `/api/ubicaciones` | No | `LocationCreateDTO`: `name` (3-100 chars, not blank) | 201 `LocationResponseDTO` |
| GET | `/api/ubicaciones` | No | — | `List<LocationResponseDTO>` |
| GET | `/api/ubicaciones/{id}` | No | — | `LocationResponseDTO` |
| PUT | `/api/ubicaciones/{id}` | No | `LocationCreateDTO` | `LocationResponseDTO` |
| DELETE | `/api/ubicaciones/{id}` | No | — | 204 |

`LocationResponseDTO`: `id`, `name`.

### Órdenes de retiro — `PickOrderController`

| Method | Path | Auth | Body | Returns |
|--------|------|------|------|---------|
| POST | `/api/ordenes-retiro` | No | `PickOrderCreateDTO`: `idUsuario`, `lineasRetiro[]`: `{idProducto, cantidad ≥ 1}` | 201 `PickOrderResponseDTO` (with lines) |
| GET | `/api/ordenes-retiro` | No | — | `List<PickOrderResponseDTO>` (summaries, `lineasRetiro: null`) |
| GET | `/api/ordenes-retiro/{id}` | No | — | `PickOrderResponseDTO` (with lines) |
| PUT | `/api/ordenes-retiro/{id}` | No | `PickOrderCreateDTO` | `PickOrderResponseDTO` |
| DELETE | `/api/ordenes-retiro/{id}` | No | — | 204 (reverts stock) |

### Órdenes de compra — `PurchaseOrderController`

| Method | Path | Auth | Body | Returns |
|--------|------|------|------|---------|
| POST | `/api/ordenes-compra` | No | `PurchaseOrderCreateDTO`: `idSupplier`, `lines[]`: `{idProduct, amount ≥ 1}` | 201 `PurchaseOrderResponseDTO` (with lines) |
| GET | `/api/ordenes-compra` | No | — | `List<PurchaseOrderResponseDTO>` (summaries, `lines: null`) |
| GET | `/api/ordenes-compra/{id}` | No | — | `PurchaseOrderResponseDTO` (with lines) |
| PUT | `/api/ordenes-compra/{id}` | No | `PurchaseOrderCreateDTO` | `PurchaseOrderResponseDTO` |
| PUT | `/api/ordenes-compra/{id}/recibir` | No | — | `PurchaseOrderResponseDTO` (adjusts stock: `disponible += cantidad`; order becomes `RECIBIDA`) |
| DELETE | `/api/ordenes-compra/{id}` | No | — | 204 (reverts stock if `RECIBIDA`) |

`PurchaseOrderResponseDTO`: `idPurchaseOrder`, `dateTime`, `idSupplier`, `status` (`PENDIENTE`/`RECIBIDA`/`CANCELADA`), `lines`. `PurchaseOrderLineResponseDTO`: `idProduct`, `amount`.

### Valoraciones de proveedor — `SupplierRatingController`

| Method | Path | Auth | Body | Returns |
|--------|------|------|------|---------|
| POST | `/api/valoraciones-proveedor` | No | `SupplierRatingCreateDTO`: `idSupplier` (required), `deliveryTime`? (≥ 0), `deliveryMethod`? (≤ 100 chars), `priceQualityRatio`? (≤ 100 chars) | 201 `SupplierRatingResponseDTO` |
| GET | `/api/valoraciones-proveedor` | No | — (optional `?idSupplier=` filter) | `List<SupplierRatingResponseDTO>` (all, or filtered by supplier via `findBySupplier`) |
| GET | `/api/valoraciones-proveedor/{id}` | No | — | `SupplierRatingResponseDTO` |
| PUT | `/api/valoraciones-proveedor/{id}` | No | `SupplierRatingCreateDTO` | `SupplierRatingResponseDTO` |
| DELETE | `/api/valoraciones-proveedor/{id}` | No | — | 204 |

`SupplierRatingResponseDTO`: `id`, `dateTime`, `idSupplier`, `deliveryTime`, `deliveryMethod`, `priceQualityRatio`. When the `?idSupplier=` query param is present, the controller delegates to `findBySupplier` (validates the supplier exists → 500 via `RuntimeException`); otherwise `findAll`. The `GET /{id}` path takes precedence over the `?idSupplier=` filter when both are given on the same URL.

### Movimientos físicos — `PhysicalMovementController`

| Method | Path | Auth | Body | Returns |
|--------|------|------|------|---------|
| POST | `/api/movimientos-fisicos` | No | `PhysicalMovementCreateDTO`: `idProduct` (required), `idLocationTo` (required), `idUser` (required), `idLocationFrom`? | 201 `PhysicalMovementResponseDTO` |
| GET | `/api/movimientos-fisicos` | No | — (optional filters `?idProduct=`, `?idUser=`, `?from=&to=`) | `List<PhysicalMovementResponseDTO>` |

Append-only feature: there is no `PUT` (update) or `DELETE`, and no `GET /{id}` (the composite PK includes a `LocalDateTime`). The `dateTime` is always generated server-side (`LocalDateTime.now()`); it cannot be set by the client.

`PhysicalMovementResponseDTO`: `idProduct`, `dateTime`, `idLocationFrom`, `idLocationTo`, `idUser`. Filter precedence when several params are given on the same URL: `?idProduct=` → `?idUser=` → `?from=` + `?to=` (both required, else `IllegalArgumentException` → 400) → no params = all. Missing referenced records on `create` throw `IllegalArgumentException` → 400.

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
10. ~~**Single test**~~ **UPDATED**: the suite is no longer a lone `contextLoads()`. Every service has a Mockito unit test (`@ExtendWith(MockitoExtension.class)`) and every controller a `@WebMvcTest` (see `src/test/java/big_three/wms/service/` and `controller/`). The only `@SpringBootTest` left is `WmsApplicationTests.contextLoads()` — see #12.
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

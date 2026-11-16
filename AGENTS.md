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
- **DB**: PostgreSQL `wms_db` on `localhost:5432`, user `postgres`, password `12345` (hardcoded in `application.properties`). **Schema is owned by Flyway** (`org.flywaydb:flyway-core` + `flyway-database-postgresql`): migrations live in `src/main/resources/db/migration/` and run automatically at startup, before Hibernate. Current chain: `V1__create_schema.sql` (full schema incl. `codigo_interno_seq` and tables with no JPA entity yet — reserved for planned features), `V2__ensure_codigo_interno_seq.sql` (repair for old baselined DBs missing the sequence — no-op on fresh DBs), `V3__seed_admin_user.sql` (default admin: CUIL `20-00000000-1`, password `Admin1234`, rol `ADMINISTRADOR`), `V4__agregar_fecha_baja_usuario.sql` (soft-delete column `usuario.fecha_baja`, NULL = active, plus partial index `idx_usuario_activos`), `V5__soft_delete_and_traceability.sql` (soft-delete columns `fecha_baja` + `id_usuario_baja` and `chk_<tabla>_baja` constraints for `usuario`, `producto`, `proveedor`, `ubicacion`, `orden_compra`, `orden_retiro`; adds `orden_compra.fecha_recepcion` with no backfill). `spring.flyway.baseline-on-migrate=true` — existing dev DBs (created with the old `wms_schema.sql`) are baseline-marked as V1 on first boot, no data loss; fresh DBs build from scratch. The old script survives read-only at `src/main/resources/db/wms_schema.sql` as historical reference only. `ddl-auto=validate` only verifies entities match the migrated schema. `show-sql=true`, `open-in-view=false`.
- **No test profile / H2 override** — `@SpringBootTest` hits real PostgreSQL. `h2`/`mysql-connector-j` were removed from `pom.xml` (PostgreSQL only); only `postgresql` runtime driver remains.
- **Frontend CORS**: configured globally in `SecurityConfig` (`corsConfigurationSource`), allowed origin from `app.cors.allowed-origin` in `application.properties`, credentials enabled. Do not add `@CrossOrigin` to controllers.
- **Libraries**: ZXing 3.5.3 (`core` + `javase`) used by `BarcodeService` to render product barcodes (`GET /api/productos/{id}/barcode`). Lombok excluded from final artifact by `spring-boot-maven-plugin`.
- `wms/doc/` contains generated Javadoc — ignore it.

## Security

`SecurityConfig.java` defines:
- `passwordEncoder` bean (`BCryptPasswordEncoder`).
- `authenticationProvider` bean (`DaoAuthenticationProvider`), wired to `CustomUserDetailsService` (`big_three.wms.security`, looks up `User` by CUIL via `UserRepository`, maps to `UserDetails` with `ROLE_<enum name>` authority, and marks the account `.disabled(true)` when `fechaBaja != null`) and the `PasswordEncoder` above.
- `authenticationManager` bean, obtained from `AuthenticationConfiguration`.
- - `filterChain`: CSRF enabled via `CookieCsrfTokenRepository.withHttpOnlyFalse()` plus `SpaCsrfTokenRequestHandler` (accepts the raw token from the `X-XSRF-TOKEN` header) and `CsrfCookieFilter` (forces the `XSRF-TOKEN` cookie to be issued on the first request; without it Spring loads the token lazily and never writes the cookie)
`POST /api/auth/login` (`AuthController`) authenticates via `AuthenticationManager.authenticate(...)`, stores the result in the `SecurityContext`, and persists it to the `HttpSession` via `HttpSessionSecurityContextRepository` — this is what generates the `JSESSIONID` cookie. `UserService.findByCuil(...)` then looks up the user to build the response DTO; it does not check the password, because the credentials are verified only once, by `AuthenticationManager`.

`CurrentUserProvider` (`big_three.wms.security`) resolves the authenticated user id for write operations that need an actor (soft deletes). `requireCurrentUserId()` reads `SecurityContextHolder`; if there is no authentication, it is not authenticated, or it is anonymous, it throws `IllegalStateException("No hay un usuario autenticado")`; otherwise it looks up `UserRepository.findByCuil(authentication.getName())` (the CUIL is the `Authentication` name) and returns `idUsuario`, or throws `IllegalStateException("Usuario autenticado no encontrado")` when the user does not exist. It is injected into `PurchaseOrderService` and `PickOrderService`; tests mock it.

Login attempts are rate-limited by `LoginAttemptService` (`big_three.wms.security`, in-memory `ConcurrentHashMap` keyed by CUIL): 5 failed attempts lock the CUIL for 15 minutes. `AuthController.login` checks `isBlocked` before authenticating and throws `TooManyAttemptsException` (mapped to `429` in `GlobalExceptionHandler`); a failed `authenticate` calls `recordFailure` and responds `401`, and a success calls `recordSuccess`. An unknown CUIL counts like a wrong password, with the same error message. Known limits: state is lost on restart, it does not work across multiple instances, and the map is never purged. Any `@WebMvcTest` for `AuthController` needs a `@MockitoBean` of `LoginAttemptService`.  It takes a `Clock`: the package-private constructor is for tests (`MutableClock` in `LoginAttemptServiceTest`), while Spring uses the public no-arg constructor with the system clock (`LoginAttemptServiceWiringTest` guards that). Lock expiry is tested.

`POST /api/auth/logout` invalidates the session via `SecurityContextLogoutHandler`. Not in `permitAll()` — requires an authenticated request. Unauthenticated requests get `403`, consistent with the rest of the app's default Spring Security behavior (no `httpBasic()`/`formLogin()` configured).

Authorization rules in `filterChain` (`authorizeHttpRequests`, evaluated top to bottom, first match wins; `anyRequest().authenticated()` is last):

- `/api/auth/login` → `permitAll`
- `/api/usuarios/**` → `hasRole("ADMINISTRADOR")`
- `/api/reportes/**` → `hasRole("ADMINISTRADOR")` (declared before `anyRequest()`)
- `GET /api/proveedores/**` → `hasAnyRole("ADMINISTRADOR", "OPERARIO")`; any other method on `/api/proveedores/**` → `hasRole("ADMINISTRADOR")` (the `GET` rule must stay declared first)
- `/api/productos/**`, `/api/ordenes-retiro/**`, `/api/ordenes-compra/**`, `/api/ubicaciones/**`, `/api/valoraciones-proveedor/**`, `/api/movimientos-fisicos/**` → `hasAnyRole("ADMINISTRADOR", "OPERARIO")`
- Everything else → `anyRequest().authenticated()`

A new controller's routes get no role rule by default: they only require authentication until a matcher is added above `anyRequest()`.

Any `@WebMvcTest` that also does `@Import(SecurityConfig.class)` needs: a `@MockitoBean` of `UserDetailsService` (or context loading fails — `authenticationProvider` requires a real `UserDetailsService` bean and `@WebMvcTest` doesn't scan `@Service` classes), and `@WithMockUser(roles = "ADMINISTRADOR")` on the class unless the test needs an anonymous or `OPERARIO` request. Also check that `@WebMvcTest(...)` points at the controller the test class is named after.
Any `@WebMvcTest` doing `POST`/`PUT`/`DELETE` needs `.with(csrf())` (import `org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf`) on the `mockMvc.perform(...)` call, or the request is rejected with `403` before reaching the controller.
`.with(csrf())` bypasses the real cookie+header flow; the real flow is covered by `CsrfFlowTest`, which should be extended if the CSRF configuration changes.

Session cookie: `server.servlet.session.cookie.secure=true` and `http-only=true` are set in `application.properties` (secure by default). Developers override `secure=false` in their git-ignored `application-local.properties` for local HTTP. Never set `secure=false` in `application.properties`. Guarded by `CookieSecurityPropertiesTest`.

Security headers are Spring Security defaults (`nosniff`, `X-Frame-Options: DENY`, no-cache `Cache-Control`, HSTS only on HTTPS requests), not configured explicitly in `SecurityConfig`. `SecurityHeadersTest` asserts them, so disabling one by accident fails the suite.

Security events are logged with SLF4J: `AuthController` (login ok/failed/blocked, logout) and `LoggingAccessDeniedHandler` (registered in `filterChain`; denied access and CSRF rejections, then delegates to the default `AccessDeniedHandlerImpl` so the `403` response is unchanged). Never log passwords, session IDs or tokens. The login CUIL is user-controlled text (`LoginRequestDTO` only checks `@NotBlank`), so it goes through `safeCuil(...)` before being logged, to prevent log injection. Tests use `OutputCaptureExtension`.

Authorization rules are covered by `RoleAuthorizationTest`, a parameterized table of (method, path, role, allowed). Whenever a route is added or a rule in `SecurityConfig` changes, add or update rows there. Non-GET cases must keep `.with(csrf())`, or denied cases would pass because of the missing CSRF token instead of the role rule. A denied case expects exactly `403`; an allowed case only expects "not 403".  `SessionFlowTest` covers the end-to-end login → session → logout sequence.

## Project structure

```
wms/src/main/java/big_three/wms/
├── WmsApplication.java        # entrypoint
├── config/SecurityConfig.java # PasswordEncoder + SecurityFilterChain
├── controller/                # AuthController, LocationController, PickOrderController, ProductController, ProveedorController, PurchaseOrderController, ReportController, SupplierRatingController, UserController, PhysicalMovementController
├── dto/                       # request/response DTOs (see API section); report DTOs live in dto/report/
├── exception/                 # InvalidCredentialsException + GlobalExceptionHandler
├── model/                     # Location, User, Product, Proveedor, Stock, PickOrder, PickOrderLine, PurchaseOrder, PurchaseOrderLine, SupplierRating, PhysicalMovement
├── repository/                # Spring Data JPA repositories, including ReportRepository (Repository<Product, Long>)
└── service/                   # LocationService, UserService, ProductService, ProveedorService, PickOrderService, PurchaseOrderService, ReportPeriod, ReportService, SupplierRatingService, PhysicalMovementService, BarcodeService
```

Conteos actuales: 11 entidades, 10 controllers, 23 DTOs de API + 11 DTOs de reportes, 12 repositorios y 11 archivos en `service/` (10 services + `ReportPeriod`).

## Entities (11)

| Entity | Table | PK | Key relationships |
|--------|-------|----|-------------------|
| `Location` | `ubicacion` | `id_ubicacion` (Long, IDENTITY) | —; soft delete: `deletedDateTime` (`fecha_baja`), `idDeletedByUser` (`id_usuario_baja`) |
| `User` | `usuario` | `id_usuario` (Long, IDENTITY) | —; soft delete: `fechaBaja` (`fecha_baja`), `idUsuarioBaja` (`id_usuario_baja`) |
| `Product` | `producto` | `id_producto` (Long, IDENTITY) | `@ManyToOne` → `Proveedor`; soft delete: `fechaBaja`, `idUsuarioBaja` |
| `Proveedor` | `proveedor` | `id_proveedor` (Long, IDENTITY) | —; soft delete: `fechaBaja`, `idUsuarioBaja` |
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
- `PurchaseOrder`: inner enum `Status { PENDIENTE, RECIBIDA, CANCELADA }` (mapped `@Enumerated(EnumType.STRING)`); `receivedDateTime` (`fecha_recepcion`, nullable), `deletedDateTime` (`fecha_baja`), `idDeletedByUser` (`id_usuario_baja`).
- `PickOrder`: `fechaBaja` (`fecha_baja`), `idUsuarioBaja` (`id_usuario_baja`).
- Soft-delete columns (`fecha_baja` + `id_usuario_baja`) exist in `usuario`, `producto`, `proveedor`, `ubicacion`, `orden_compra` and `orden_retiro` since V4/V5. All six entities map them. `Location` uses the names `deletedDateTime`/`idDeletedByUser`; the other three use `fechaBaja`/`idUsuarioBaja`. `usuario.fecha_baja` has existed since V4 and is reused by V5 (the column is not duplicated).
- `SupplierRating`: `id` (mapped to `id_valoracion`), `idSupplier` (mapped to `id_proveedor`), `dateTime` (mapped to `fecha_hora`), `deliveryTime` (mapped to `tiempo_entrega`, nullable), `deliveryMethod` (mapped to `forma_entrega`), `priceQualityRatio` (mapped to `relacion_precio_calidad`).
- `PhysicalMovement`: `idProduct` (mapped to `id_producto`, part of PK), `dateTime` (mapped to `fecha_hora`, part of PK), `idLocationFrom` (mapped to `id_ubicacion_desde`, nullable), `idLocationTo` (mapped to `id_ubicacion_hasta`), `idUser` (mapped to `id_usuario`).

## Business logic (important)

- **Product create** (`ProductService.create`): if `codigoBarras` is provided and unique → `origen = FABRICANTE`; if blank/null → generates `INT-XXXXXX` via `nextval('codigo_interno_seq')` and `origen = INTERNO`. Also creates a `Stock` row (defaults to 0). Duplicate barcode → `IllegalArgumentException`. The sequence is created by migration `V1__create_schema.sql` (`codigo_interno_seq`).
- **Product update** (`update`): also upserts the `Stock` row (creates with 0s if missing). Existing product + no new barcode keeps its barcode/origen. A soft-deleted product is treated as nonexistent (`RuntimeException("Producto no encontrado")`), and the provider must be active.
- **Product delete** (`deleteById`): logical delete. Finds the active product (missing or already deleted → `RuntimeException("Producto no encontrado para eliminar")`), blocks when `Stock.cantidadDisponible > 0` (`IllegalArgumentException("No se puede dar de baja un producto con stock disponible")`; a missing stock row counts as 0), then sets `fechaBaja`/`idUsuarioBaja` and saves. It does **not** delete the product or the `Stock` row. `cantidadPendiente` is not checked.
- **Barcode generation** (`BarcodeService.getBarcodePng`): renders the product's `codigoBarras` as a CODE_128 PNG via ZXing (`MultiFormatWriter` + `MatrixToImageWriter`, 300x120, black-on-white) for `GET /api/productos/{id}/barcode`; throws `RuntimeException("Producto no encontrado")` → 500 if the product is missing **or soft-deleted**, and a `RuntimeException` if encoding/`ImageIO` fails.
- **PickOrder create** (`PickOrderService.create`): validates that `idUsuario` and every `idProducto` are **active** (soft-deleted = nonexistent; `"Usuario no encontrado"` / `"Producto no encontrado: <id>"`), saves order + lines, then calls `productService.ajustarStock(idProducto, -cantidad, +cantidad)` → `disponible -= cantidad`, `pendiente += cantidad`; throws if `disponible` would go negative. `update` applies the same active checks.
- **PickOrder update** (`update`): reverses old lines' stock, deletes old lines, saves new lines, applies new stock deltas. Known stale-data risk (see Known issues).
- **PickOrder delete** (`deleteById`): logical delete. Finds the active order (missing or already deleted → `RuntimeException("Orden de retiro no encontrada")`), reverts stock (`+cantidad` disponible, `-cantidad` pendiente), sets `fechaBaja = now()` and `idUsuarioBaja = currentUserProvider.requireCurrentUserId()`, and saves. It does **not** delete the order or its lines.
- **GET /api/ordenes-retiro** returns summaries with `lineasRetiro: null`; only `GET /api/ordenes-retiro/{id}` includes the lines. Same for `GET /api/ordenes-compra` (summaries, `lines: null`) vs `GET /api/ordenes-compra/{id}` (with lines).
- **PurchaseOrder create** (`PurchaseOrderService.create`): validates that `idSupplier` and every `idProduct` are **active** (soft-deleted = nonexistent; `"Proveedor no encontrado"` / `"Producto no encontrado: <id>"`), saves order with `fechaHora`=now and `estado`=`PENDIENTE`, saves lines. No stock adjustment on create. `update` applies the same active checks.
- **PurchaseOrder receive** (`receive`): only from `PENDIENTE`; calls `ajustarStock(idProduct, +cantidad, 0)` (`disponible += cantidad`) for every line, marks the order `RECIBIDA`, and sets `receivedDateTime = now()`. Throws `IllegalArgumentException` if already `RECIBIDA`, `RuntimeException` if `CANCELADA`.
- **PurchaseOrder update** (`update`): replaces supplier + lines; if the order was `RECIBIDA`, it first reverses old lines' stock (`-cantidad` disponible), deletes old lines, saves new lines, then re-applies stock for the new lines. It never changes `receivedDateTime`.
- **PurchaseOrder delete** (`deleteById`): logical delete. Finds the active order (missing or already deleted → `RuntimeException("Orden de compra no encontrada")`); if it was `RECIBIDA`, reverts stock (`-cantidad` disponible) per line; sets `deletedDateTime = now()` and `idDeletedByUser = currentUserProvider.requireCurrentUserId()`; saves. It does **not** delete the order or its lines.
- **Soft-delete semantics for orders**: an order with `fecha_baja` set is "not found" for `findAllSummaries`, `findById`, `update` and `receive`/`deleteById`. `findAllSummaries` uses `PurchaseOrderRepository.findByDeletedDateTimeIsNull()` / `PickOrderRepository.findByFechaBajaIsNull()`; single-record methods use a private `findActiveOrThrow(id)` built on `findById`. No `@SQLRestriction`/`@Where` is used, so filters are explicit. Because soft-deleted orders keep their lines, a product or supplier referenced by them cannot be physically deleted (FK without cascade); 1B now gives products/providers logical deletion, so the history is preserved without physical deletes.
- **Soft-delete semantics for users, products, providers and locations (1B)**: every `findAll`/`findById`/`update`/`deleteById` only sees rows with `fecha_baja IS NULL` (locations: `deletedDateTime IS NULL`). Deleting sets the date and `id_usuario_baja`; rows are never physically deleted. The filters are explicit repository methods (no `@SQLRestriction`/`@Where`). A soft-deleted row is treated as nonexistent by every service that references it. Deletion rules:
  - User: cannot delete yourself (`"No podés darte de baja a vos mismo"`); cannot delete the last active `ADMINISTRADOR` (`countByRolAndFechaBajaIsNull`; `"No se puede dar de baja al último administrador"`).
  - Product: only when `cantidadDisponible == 0` (missing stock row = 0; `cantidadPendiente` is not checked; `"No se puede dar de baja un producto con stock disponible"`). The `Stock` row is preserved.
  - Provider: only when it has no active products and no active `PENDIENTE` purchase orders (`"No se puede dar de baja un proveedor con productos activos u órdenes pendientes"`).
  - Location: only when no active product currently sits there (`"No se puede dar de baja una ubicación que contiene productos"`).
- **Uniqueness stays global across soft-deleted rows**: `usuario.cuil`, `proveedor.cuit` and `producto.codigo_barras` keep their global uniqueness. Duplicate messages mention that the existing row may be soft-deleted: `"Ya existe un usuario con ese CUIL (puede estar dado de baja): <cuil>"`, `"Ya existe un proveedor con ese CUIT (puede estar dado de baja): <cuit>"`, `"Ya existe un producto con ese código de barras (puede estar dado de baja): <codigo>"`. There is no reactivation flow yet.
- **Login and open sessions**: `CustomUserDetailsService` loads the user by CUIL (including soft-deleted rows) and sets `.disabled(fechaBaja != null)`, so a deleted user cannot log in and the attempt follows the normal generic failure/rate-limit path. Known limitation: a session opened **before** the user was deleted remains valid until it expires (90 minutes); there is no session invalidation on soft delete.
- **Location create** (`LocationService.create`): saves a new `Location` (single `name` field).
- **Location findById/update** (`LocationService.findId`/`update`): only active locations are visible; a missing or soft-deleted location throws `RuntimeException("Ubicación no encontrada")`. `findAll` uses `findByDeletedDateTimeIsNull()`.
- **Location delete** (`LocationService.deleteById`): logical delete. Finds the active location (missing or already deleted → `RuntimeException("Ubicación no encontrada para eliminar")`); blocks when an active product's current location is that location (`LocationRepository.existsActiveProductCurrentlyInLocation`, native query; current location = `id_ubicacion_hasta` of the product's latest `movimiento_fisico`) with `IllegalArgumentException("No se puede dar de baja una ubicación que contiene productos")`; otherwise sets `deletedDateTime`/`idDeletedByUser` and saves.
- **SupplierRating create** (`SupplierRatingService.create`): validates the supplier is **active** (`findByIdProveedorAndFechaBajaIsNull` → `RuntimeException("Proveedor no encontrado")`), sets `dateTime`=now, and stores the rest of the fields.
- **SupplierRating findBySupplier** (`SupplierRatingService.findBySupplier`): validates the supplier is **active** (`existsByIdProveedorAndFechaBajaIsNull`), then returns its ratings (empty list if none).
- **SupplierRating update** (`SupplierRatingService.update`): does **not** change `idSupplier` (the DTO field is ignored on `PUT`) — only `deliveryTime`, `deliveryMethod`, `priceQualityRatio`, and refreshes `dateTime`; throws `RuntimeException("Valoración no encontrada")` if the rating is missing.
- **SupplierRating delete** (`SupplierRatingService.deleteById`): checks `existsById` first and throws `RuntimeException("Valoración no encontrada para eliminar")` otherwise.
- **PhysicalMovement create** (`PhysicalMovementService.create`): **append-only** — only records a movement event; there are no update/delete paths (no `PUT`/`DELETE` endpoints). Validates that the product, source location (only when provided), destination location, and user are **active** (soft-deleted = nonexistent) — each failure throws `IllegalArgumentException` (→ 400, unlike the rest of the app's "no encontrado" which is `RuntimeException` → 500) — and stamps `dateTime`=now server-side (`LocalDateTime.now()`). Does **not touch `Stock`**.
- **PhysicalMovement reads** (`findAll`, `findByIdProduct`, `findByIdUser`, `findByDateTimeRange`): list events (newest first) or filtered by product, user, or date range (`from`+`to` both required, else → 400); records are never modified.
- **Response DTOs never include the password hash.** Login (`AuthController.login`) authenticates through `AuthenticationManager`; `UserService.findByCuil` only builds the user DTO. The session cookie is the only credential issued: no token in the response body.

CORS is global (see `SecurityConfig`), not per controller. All routes under `/api/`. Validation failures return 400 with Spanish messages.

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
| GET | `/api/productos/{id}/barcode` | No | — | `image/png` bytes (ZXing CODE_128 of the product's `codigoBarras`); 500 `{"error"}` if the product is missing |

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

`PurchaseOrderResponseDTO`: `idPurchaseOrder`, `dateTime`, `idSupplier`, `status` (`PENDIENTE`/`RECIBIDA`/`CANCELADA`), `receptionDateTime` (null until received), `lines`. `PurchaseOrderLineResponseDTO`: `idProduct`, `amount`.

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

## Reportes

Módulo de solo lectura para el rol `ADMINISTRADOR`, bajo `/api/reportes` (regla propia en `SecurityConfig`, declarada antes de `anyRequest()`). Estructura:

- `controller/ReportController`: `@RequestMapping("/api/reportes")`, solo `GET`.
- `service/ReportService`: resuelve defaults y validaciones manuales (`IllegalArgumentException` → 400); mapea proyecciones nativas a DTOs.
- `repository/ReportRepository`: extiende `org.springframework.data.repository.Repository<Product, Long>` (no tiene entidad propia). Todas las lecturas son `@Query`; una sola interfaz para todos los prompts de reportes.
- `dto/report/`: `StockBajoDTO`, `StockPorProveedorDTO`, `ProductoUbicacionActualDTO`, `UbicacionOcupacionDTO`, `ProductoMasRetiradoDTO`, `ProductoSinRotacionDTO`, `RetiroPorPeriodoDTO`, `ActividadOperarioDTO`, `OrdenCompraResumenDTO`, `ProveedorRankingDTO`, `BajaAuditoriaDTO`. Nombres de rutas, parámetros y campos en español.
- `service/ReportPeriod`: record compartido `ReportPeriod(LocalDateTime from, LocalDateTime toExclusive)` con factory `of(LocalDate from, LocalDate to)`. `from` null → `1970-01-01T00:00`; `to` null → `9999-01-01T00:00` (exclusivo); si no, `from.atStartOfDay()` y `to.plusDays(1).atStartOfDay()`; `from > to` → `IllegalArgumentException("La fecha 'from' no puede ser posterior a 'to'")`. Centraliza la resolución para que las queries nunca reciban `null`.

Convenciones de consultas (aplican a este y a los próximos prompts de reportes):

- Agregar en SQL/JPQL, nunca iterando entidades en Java; prohibido N+1.
- Preferir JPQL con constructor expression (`SELECT new big_three.wms.dto.report.X(...)`).
- No existe asociación JPA `Product`↔`Stock`: usar entity joins (`LEFT JOIN Stock s ON s.idProducto = p.idProducto`).
- `SUM(Integer)` devuelve `Long`; `AVG` devuelve `Double`. Usar `COALESCE` para que un producto sin fila de `stock` cuente como 0.
- SQL nativo solo cuando haga falta (`LATERAL`, `DISTINCT ON`, `date_trunc`, `FILTER`, CTEs). Las queries nativas devuelven proyecciones de interfaz (anidadas en `ReportRepository`) y `ReportService` las mapea a DTO.
- En SQL nativo, alias entre comillas dobles (`AS "idProducto"`): PostgreSQL pasa a minúsculas los alias sin comillas.
- Nunca pasar parámetros `null` a una query; los defaults se resuelven en `ReportService` y en `ReportPeriod`. Usar parámetros no nulos (por ejemplo los booleanos `soloSinUbicar`/`soloConStock`).
- Ordenamientos deterministas, siempre con desempate por nombre o id.
- Los parámetros de período se filtran siempre con `fecha_hora >= :from AND fecha_hora < :toExclusive`; `to` es inclusivo a nivel de día porque `ReportPeriod` lo traduce al inicio del día siguiente.
- Con `date_trunc` nativo, el valor de agrupación (`day`/`week`/`month`) sale de una lista blanca; nunca se pasa el texto del cliente. Agrupar/ordenar por el ordinal de la expresión (`GROUP BY 1`, `ORDER BY 1`) porque PostgreSQL no reconoce como iguales dos placeholders distintos con la misma expresión.

Criterio de bajas lógicas:

- **Reportes de estado actual** (`stock-bajo`, `stock-por-proveedor`, `productos/ubicacion-actual`, `ubicaciones/ocupacion`) excluyen productos, proveedores y ubicaciones con `fecha_baja` no nulo.
- **Reportes históricos por período** (`productos/mas-retirados`, `productos/sin-rotacion`, `retiros/por-periodo`, `operarios/actividad`, `ordenes-compra/resumen`, `proveedores/ranking`) excluyen las órdenes de retiro y de compra dadas de baja, pero incluyen productos, usuarios y proveedores dados de baja cuando tuvieron actividad; esas filas llevan un campo booleano `activo` (`true` si `fecha_baja IS NULL`). Los proveedores activos aparecen siempre, aunque no tengan actividad; los dados de baja solo cuando la tuvieron en el período.
- **Auditoría de bajas** (`auditoria/bajas`) es la excepción: lista justamente las filas con `fecha_baja IS NOT NULL` de las seis tablas. El período se aplica sobre `fecha_baja` y el usuario que dio de baja se resuelve con `LEFT JOIN usuario` (puede ser `null` si la baja se hizo manualmente sin `id_usuario_baja`). No registra quién creó ni quién modificó los registros.

Endpoints actuales:

- `GET /api/reportes/stock-bajo?umbral=10`: productos activos con `cantidadDisponible <= umbral` (incluye 0). Default 10 resuelto en el service; negativo → `IllegalArgumentException("El umbral no puede ser negativo")`. Orden: disponible asc, nombre asc.
- `GET /api/reportes/stock-por-proveedor`: todos los proveedores activos (también sin productos, con ceros), contando solo productos activos. El schema no tiene precios: mide unidades, no dinero. Orden: unidades disponibles desc, razón social.
- `GET /api/reportes/productos/ubicacion-actual?soloSinUbicar=false`: productos activos; ubicación actual = `id_ubicacion_hasta` del último `movimiento_fisico` (query nativa con `LEFT JOIN LATERAL`). Productos nunca movidos: ubicación y fecha `null`. Si el último movimiento apunta a una ubicación dada de baja, la ubicación queda `null` pero la fecha se conserva; `soloSinUbicar=true` solo devuelve los nunca movidos. Orden: nombre de ubicación (nulls al final), nombre de producto.
- `GET /api/reportes/ubicaciones/ocupacion`: ubicaciones activas (también vacías); `cantidadProductos` = productos activos distintos cuya ubicación actual es esa (CTE con `DISTINCT ON`). Orden: cantidad desc, nombre. Limitación documentada: cuenta productos, no unidades, porque el stock no está dividido por ubicación y los movimientos no mueven cantidades.
- `GET /api/reportes/productos/mas-retirados?from&to&limit=10`: top de productos por unidades retiradas en órdenes de retiro activas; incluye productos dados de baja con `activo`; `limit` default 10, 1–100, fuera de rango → `IllegalArgumentException("El límite debe estar entre 1 y 100")`. Solo productos con al menos un retiro en el período. Orden: unidades desc, nombre asc. SQL nativo con `LIMIT :limit`.
- `GET /api/reportes/productos/sin-rotacion?from&to&soloConStock=true`: productos activos sin retiros activos en el período; `soloConStock` default true y exige `cantidadDisponible > 0`; `ultimoRetiro` = `MAX` del último retiro activo del producto sin limitar por período. JPQL con `NOT EXISTS` y subconsulta. Orden: stock desc, nombre asc.
- `GET /api/reportes/retiros/por-periodo?from&to&agrupacion=DIA`: serie temporal sobre órdenes de retiro activas. `agrupacion` se recibe como `String`, se valida en el service (`DIA`/`SEMANA`/`MES`, case-insensitive; inválida → `IllegalArgumentException("La agrupación debe ser DIA, SEMANA o MES")`) y se traduce a `day`/`week`/`month` para `date_trunc` nativo. `SEMANA` es ISO (lunes). Solo buckets con datos. Orden: período asc.
- `GET /api/reportes/operarios/actividad?from&to`: todos los usuarios (sin actividad con ceros y dados de baja con `activo=false`). `cantidadRetiros`/`unidadesRetiradas` salen de un agregado de órdenes activas por usuario; `cantidadMovimientos` de un agregado de `movimiento_fisico`; `ultimaActividad` es el máximo entre ambos dentro del período. Se combinan en el service por `idUsuario` para evitar el producto cartesiano. Orden: `(cantidadRetiros + cantidadMovimientos)` desc, apellido, nombre. El DTO no incluye contraseña.
- `GET /api/reportes/ordenes-compra/resumen?from&to`: una fila por proveedor activo más los proveedores dados de baja con órdenes en el período. El período filtra por `orden_compra.fecha_hora` y excluye órdenes con `fecha_baja`; `ordenesPendientes`/`ordenesRecibidas`/`ordenesCanceladas` usan `COUNT(DISTINCT o.id_orden_compra) FILTER (WHERE estado = ...)` para no multiplicar por las líneas; `unidadesPedidas` suma `linea_compra.cantidad` de `PENDIENTE`+`RECIBIDA` y `unidadesRecibidas` solo de `RECIBIDA`. Los proveedores sin órdenes aparecen con ceros. Orden: `unidadesPedidas` desc, `razonSocial`. `ordenesCanceladas` queda en 0 hasta que exista un endpoint de cancelación.
- `GET /api/reportes/proveedores/ranking?from&to`: proveedores activos siempre; dados de baja solo si tuvieron valoraciones u órdenes recibidas en el período. `cantidadValoraciones`/`promedioTiempoEntregaValorado` filtran por `valoracion_proveedor.fecha_hora` (AVG ignora `NULL`); `ordenesRecibidas`/`promedioDiasRecepcion` filtran por `orden_compra.fecha_recepcion` dentro del período y excluyen órdenes dadas de baja; `promedioDiasRecepcion` = promedio de `(fecha_recepcion - fecha_hora)` en días, redondeado a 2 decimales; `recibidasSinFecha` cuenta órdenes `RECIBIDA` activas con `fecha_recepcion IS NULL` **sin filtro de período**. Tres agregados separados (proveedores, valoraciones, órdenes) se combinan por `idProveedor` en el service para evitar el producto cartesiano. Orden: `promedioDiasRecepcion` asc con nulls al final, `cantidadValoraciones` desc, `razonSocial`. `relacion_precio_calidad` es texto libre y no se agrega.
- `GET /api/reportes/auditoria/bajas?from&to&entidad&limit=100`: bajas lógicas de `usuario`, `producto`, `proveedor`, `ubicacion`, `orden_compra` y `orden_retiro`, filtradas por `fecha_baja` con `ReportPeriod`. `entidad` se recibe como `String`, se valida en el service contra la lista blanca (`USUARIO`/`PRODUCTO`/`PROVEEDOR`/`UBICACION`/`ORDEN_COMPRA`/`ORDEN_RETIRO`, case-insensitive; inválida → `IllegalArgumentException("La entidad debe ser USUARIO, PRODUCTO, PROVEEDOR, UBICACION, ORDEN_COMPRA u ORDEN_RETIRO")`) y se normaliza a `"TODAS"` cuando no se envía; nunca se pasa `null` a la query. `limit` default 100, 1–500, fuera de rango → `IllegalArgumentException("El límite debe estar entre 1 y 500")`. SQL nativo con `UNION ALL` de las seis ramas (columnas casteadas a `VARCHAR`/`BIGINT`/`TIMESTAMP`), `LEFT JOIN usuario` para `usuarioBaja` y filtro de entidad sobre la unión (`WHERE CAST(:entidad AS VARCHAR) = 'TODAS' OR entidad = CAST(:entidad AS VARCHAR)`). `descripcion` se arma en SQL con los formatos pedidos. Orden: `fecha_baja` desc, `entidad`, `id_entidad`; `LIMIT :limit`.

Seguridad y tests:

- `SecurityConfig`: `/api/reportes/**` → `hasRole("ADMINISTRADOR")`, antes de `anyRequest()`.
- `RoleAuthorizationTest` carga todos los controllers: cada nuevo controller necesita su `@MockitoBean` (hoy `ReportService`). Por cada endpoint nuevo se agregan tres filas: `ADMINISTRADOR→true`, `OPERARIO→false`, `ANONIMO→false`.
- `ReportControllerTest` usa `@WebMvcTest(ReportController.class)`, `@Import(SecurityConfig.class)`, `@MockitoBean ReportService` y `UserDetailsService`, y `@WithMockUser(roles = "ADMINISTRADOR")`.
- `ReportPeriodTest` cubre sin fechas, solo `from`, solo `to`, rango válido (el `to` queda exclusivo en el día siguiente) y `from > to`.
- `ReportServiceTest` cubre el mapeo de cada reporte, los defaults y validaciones de `limit`/`agrupacion`/`entidad`, la combinación de retiros/movimientos por `idUsuario` (usuario con retiros sin movimientos, con movimientos sin retiros, con ambos y sin actividad), la combinación de proveedores por `idProveedor` (solo valoraciones, solo órdenes, ambos, sin actividad) y la auditoría de bajas (mapeo, `entidad` case-insensitive, `"TODAS"` por defecto, `limit` 1–500 y fila sin `id_usuario_baja` con `usuarioBaja = null`), incluidos `activo`, nulls y redondeo de promedios.
- Los mocks no validan las queries: al cambiar JPQL/SQL hay que arrancar la app con el perfil `local` (Hibernate valida los JPQL) y, si hay PostgreSQL, correr las queries nativas contra datos de prueba. Para los reportes históricos, los datos de prueba deben incluir una orden dada de baja, un producto dado de baja con retiros, un producto jamás retirado, un usuario sin actividad, un usuario dado de baja con actividad y órdenes en los bordes de día (23:59 del `to` incluido, 00:00 del día siguiente excluido).

## Error handling

- `GlobalExceptionHandler` (`exception/GlobalExceptionHandler.java`) is the single `@ControllerAdvice` for unhandled exceptions, always responding `{ "error": message }`:
  - `InvalidCredentialsException` → 401
  - `IllegalArgumentException` (duplicate CUIL/CUIT/barcode, soft-delete business rules such as stock/active products/last admin, report validation such as a negative threshold) → 400
  - `MethodArgumentTypeMismatchException` (e.g. `umbral=abc`) → 400 with `{"error": "El parámetro '<nombre>' tiene un valor inválido"}`. This handler exists specifically so type mismatches do not fall into the generic `RuntimeException` → 500 handler.
  - `RuntimeException` (e.g. "Producto no encontrado") → 500
- Bean Validation failures → 400 with Spring's default field-error structure.
- `InvalidCredentialsException` exists solely for login; the duplicate-email/duplicate-CUIL/duplicate-barcode checks throw `IllegalArgumentException`.

## Known issues (do not reintroduce)

1. ~~**Orphan Stock on Product delete**~~ **FIXED then superseded**: product deletion is now logical (1B), so the `Stock` row is intentionally preserved with the product. The previous physical delete removed the stock row first; that path no longer exists.
2. ~~**Duplicate login endpoints**~~ **FIXED**: `UserController.login` (`POST /api/usuarios/login`) removed; `AuthController` `POST /api/auth/login` is the single login endpoint.
3. ~~**N+1 query** (`ProductService.findAll`, `ProductService.java:135`)~~ **FIXED**: `findAll` now batch-fetches all `Stock` rows in one query and maps by `idProducto` (2 queries total).
4. **PickOrder.idUsuario is raw Long** (`PickOrder.java:25`): no JPA-level referential integrity with `User`. — **INTENTIONAL** (see TODO item 17): the DB FK is the only guard; mapping a `@ManyToOne` would force lazy-loading `User` when building responses, conflicting with `open-in-view=false` and rippling into `PickOrderResponseDTO`/`PickOrderService`. Revisit if order responses ever need the user object.
5. ~~**Empty Validations class**~~ **FIXED**: `util/Validations.java` deleted (no callers).
6. ~~**Dead code in User.java**~~ **FIXED**: commented-out `@OneToMany` blocks and stale header comments removed from `model/User.java`.
7. ~~**No global exception handler**~~ **FIXED**: `GlobalExceptionHandler` `@ControllerAdvice` maps `InvalidCredentialsException` → 401, `IllegalArgumentException` → 400, `RuntimeException` → 500.
8. ~~**Unused dependencies in pom.xml**~~ **FIXED**: `h2` and `mysql-connector-j` removed — only PostgreSQL is used. (Note: ZXing stays — kept intentionally for planned barcode generation.)
9. **No real authentication**: login returns a DTO but no token/session; `authenticated()` endpoints are unreachable.
10. ~~**Single test**~~ **UPDATED**: the suite is no longer a lone `contextLoads()`. Every service has a Mockito unit test (`@ExtendWith(MockitoExtension.class)`) and every controller a `@WebMvcTest` (see `src/test/java/big_three/wms/service/` and `controller/`). The only `@SpringBootTest` left is `WmsApplicationTests.contextLoads()` — see #12.
11. ~~**PickOrderService.update() stale data risk** (`PickOrderService.java`)~~ **FIXED**: `update` reuses the already-fetched line list (`deleteAll(lines)`) instead of re-querying after the stock reversal. `deleteById` is now a soft delete and no longer deletes the lines or the order.
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
- Jakarta Bean Validation on DTOs (`@NotBlank`, `@NotNull`, `@Size`, `@Pattern`, `@Min`, `@Email`, `@Valid`).
- DB tables use snake_case; Java fields use camelCase.
- PKs use `GenerationType.IDENTITY` (except `Stock`, which shares Product's PK).
- Composite PKs use `@IdClass`.
- `@Transactional` on service methods that modify multiple entities.

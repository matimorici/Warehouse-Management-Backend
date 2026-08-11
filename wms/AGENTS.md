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
- **DB**: PostgreSQL `wms_db` on `localhost:5432`, user `postgres`, password `12345` (hardcoded in `application.properties`). `ddl-auto=validate` — schema must exist externally; the source of truth is `src/main/resources/sql/wms_schema.sql` (create DB, `\i` the script). `show-sql=true`, `open-in-view=false`.
- **No test profile / H2 override** — `@SpringBootTest` hits real PostgreSQL. `h2` and `mysql-connector-j` are in `pom.xml` but unused.
- **Frontend CORS**: `http://localhost:4200` (Angular), set per-controller via `@CrossOrigin`.
- **Libraries**: ZXing 3.5.3 (barcode generation) declared but not used anywhere yet. Lombok excluded from final artifact by `spring-boot-maven-plugin`.
- `wms/doc/` contains generated Javadoc — ignore it.

## Security

`SecurityConfig.java` defines a `BCryptPasswordEncoder` bean and a filter chain with CSRF disabled. Current `permitAll()` list (single-path matchers, **exact paths only — no trailing `/**`**):

- `POST /api/usuarios` and `GET /api/usuarios` → `permitAll`
- `POST /api/auth/login`, `/api/proveedores/**`, `/api/productos/**`, `/api/ordenes-retiro/**` → `permitAll`
- Everything else → `anyRequest().authenticated()`, including:
  - `POST /api/usuarios/login` — **requires auth even though it's the login endpoint** (bug, see Known issues)
  - `GET /api/usuarios/{id}`, `DELETE /api/usuarios/{id}`

There is no `UserDetailsService`, JWT, session management, or token mechanism. The app has no real authentication flow; `authenticated()` paths are effectively unreachable.

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
├── service/                   # UserService, ProductService, ProveedorService, PickOrderService
└── util/Validations.java      # empty placeholder
```

## Entities (6)

| Entity | Table | PK | Key relationships |
|--------|-------|----|-------------------|
| `User` | `usuario` | `id_usuario` (Long, IDENTITY) | — |
| `Product` | `producto` | `id_producto` (Long, IDENTITY) | `@ManyToOne` → `Proveedor` |
| `Proveedor` | `proveedor` | `id_proveedor` (Long, IDENTITY) | — |
| `Stock` | `stock` | `id_producto` (Long, no auto-gen) | 1:1 with `Product` (same PK, FK `ON DELETE CASCADE` in schema) |
| `PickOrder` | `orden_retiro` | `id_orden_retiro` (Long, IDENTITY) | `idUsuario` stored as raw `Long` (no `@ManyToOne`, no FK) |
| `PickOrderLine` | `linea_retiro` | `@IdClass(PickOrderLineId)`: `id_orden_retiro` + `id_producto` | Composite PK |

- `Product` has inner enum `OrigenCodigoBarras { FABRICANTE, INTERNO }` (mapped `@Enumerated(EnumType.STRING)`).
- `User` fields: `nombre`, `apellido`, `cuil`, `rol` (default `"OPERARIO"`), `contrasena` (BCrypt hash).
- `Stock`: `cantidadDisponible`, `cantidadPendiente`, `fechaHora`.
- `PickOrderLine`: `cantidad` (Integer).

## Business logic (important)

- **Product create** (`ProductService.create`): if `codigoBarras` is provided and unique → `origen = FABRICANTE`; if blank/null → generates `INT-XXXXXX` via `nextval('codigo_interno_seq')` and `origen = INTERNO`. Also creates a `Stock` row (defaults to 0). Duplicate barcode → `IllegalArgumentException`.
  - **Gotcha**: `codigo_interno_seq` is queried in `ProductRepository.obtenerSiguienteSecuencia()` but **is not created by `wms_schema.sql`** — creating a product with no barcode fails until the sequence exists in the DB.
- **Product update** (`update`): also upserts the `Stock` row (creates with 0s if missing). Existing product + no new barcode keeps its barcode/origen.
- **Product delete** (`deleteById`): deletes only the `Product`; the `Stock` row is left behind as an orphan (bug — do not reintroduce; a "fix" must be deliberate).
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
| POST | `/api/usuarios/login` | **Yes (bug)** | `LoginRequestDTO`: `cuil`, `contrasena` | 200 `UserResponseDTO` / 401 `{error}` |
| GET | `/api/usuarios` | No | — | `List<UserResponseDTO>` |
| GET | `/api/usuarios/{id}` | Yes | — | `UserResponseDTO` |
| DELETE | `/api/usuarios/{id}` | Yes | — | 204 |

`UserResponseDTO`: `idUsuario`, `nombre`, `apellido`, `cuil`, `rol`.

### Auth — `AuthController` (duplicate of `/api/usuarios/login`)

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

- **No `@ControllerAdvice`** — no global exception handler.
- `RuntimeException` → 500 with raw message (e.g. "Producto no encontrado").
- `IllegalArgumentException` → 500 too (not 400), except when caught: only `InvalidCredentialsException` is handled, and only in the two login controllers → 401 `{ "error": message }`.
- Bean Validation failures → 400 with Spring's default field-error structure.
- `InvalidCredentialsException` exists solely for login; the duplicate-email/duplicate-CUIL/duplicate-barcode checks throw `IllegalArgumentException`.

## Known issues (do not reintroduce)

1. **Orphan Stock on Product delete** (`ProductService.deleteById`, `ProductService.java:152`): deletes the product but not its `Stock` row — leaves orphans.
2. **Duplicate login endpoints**: `POST /api/usuarios/login` (UserController) and `POST /api/auth/login` (AuthController) do the same thing.
3. **N+1 query** (`ProductService.findAll`, `ProductService.java:135`): per-product `stockRepository.findById()` — no `JOIN FETCH`/`@EntityGraph`.
4. **PickOrder.idUsuario is raw Long** (`PickOrder.java:25`): no JPA-level referential integrity with `User`.
5. **Empty Validations class** (`util/Validations.java`): placeholder with no methods.
6. **Dead code in User.java** (`model/User.java`): commented-out `@OneToMany` referencing non-existent `Move` and `PickUpOrder` entities.
7. **No global exception handler**: no `@ControllerAdvice` — unhandled exceptions produce raw 500s.
8. **Unused dependencies in pom.xml**: `h2` and `mysql-connector-j`.
9. **No real authentication**: login returns a DTO but no token/session; `authenticated()` endpoints are unreachable.
10. **Single test** (`WmsApplicationTests.contextLoads()`): no test profile, no integration/unit tests.
11. **PickOrderService.update() stale data risk** (`PickOrderService.java`): re-fetches lines after reversing stock to delete them — second fetch may return stale data.
12. **Test hits real DB**: `@SpringBootTest` connects to PostgreSQL — no H2/test override.
13. **Login endpoint requires auth**: `SecurityConfig` `permitAll("/api/usuarios")` doesn't cover `/api/usuarios/login`, so it falls into `anyRequest().authenticated()` — the login endpoint is unreachable with no auth mechanism present.
14. **`codigo_interno_seq` missing from schema**: `ProductRepository` uses `nextval('codigo_interno_seq')` but `wms_schema.sql` never creates the sequence — barcode-less product creation fails on a fresh DB.

## Work rules

- Stick to the exact task asked. Do not fix other bugs, refactor, or touch unrelated files unless explicitly told to.
- If a task uncovers related issues, ask before fixing them.

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

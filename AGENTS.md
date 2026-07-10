# Warehouse-Management-Backend

Single-module Maven project (Spring Boot 4.1.0, Java 25, Lombok).

## Commands (run from `wms/`)

| Action | Command |
|--------|---------|
| Build | `./mvnw clean compile` |
| Test | `./mvnw test` |
| Run | `./mvnw spring-boot:run` |
| Package | `./mvnw package` |

No lint, format, typecheck, or CI configured. `javac` is the only typechecker.

## Architecture

- **Package**: `big_three.wms`, entrypoint `WmsApplication.java` (port 8080).
- **DB**: PostgreSQL (`wms_db` on `localhost:5432`, user `postgres`). `ddl-auto=validate` — schema must exist externally. `show-sql=true`, `open-in-view=false`. No test profile or H2 override — `@SpringBootTest` hits real PostgreSQL.
- **Security**: `BCryptPasswordEncoder` bean, CSRF disabled. Almost everything is `permitAll()` — only `GET /api/usuarios`, `GET /api/usuarios/{id}`, and `DELETE /api/usuarios/{id}` require authentication. No `UserDetailsService`, JWT, session management, or login token mechanism. The app has no real authentication flow.
- **Frontend CORS**: `http://localhost:4200` (Angular).
- **Libraries**: ZXing 3.5.3 (barcode generation). H2 and MySQL drivers present in `pom.xml` but unused.
- **Packaging**: Lombok excluded from final artifact by `spring-boot-maven-plugin`.

### Entities (6)

| Entity | Table | PK | Key relationships |
|--------|-------|----|-------------------|
| `User` | `usuario` | `id_usuario` (Long, IDENTITY) | — |
| `Product` | `producto` | `id_producto` (Long, IDENTITY) | `@ManyToOne` → `Proveedor` |
| `Proveedor` | `proveedor` | `id_proveedor` (Long, IDENTITY) | — |
| `Stock` | `stock` | `id_producto` (Long, no auto-gen) | 1:1 with `Product` (same PK) |
| `PickOrder` | `orden_retiro` | `id_orden_retiro` (Long, IDENTITY) | `idUsuario` stored as raw `Long` (no FK) |
| `PickOrderLine` | `linea_retiro` | `@IdClass(PickOrderLineId)`: `id_orden_retiro` + `id_producto` | Composite PK |

`Product` has inner enum `OrigenCodigoBarras { FABRICANTE, INTERNO }`.

### API Endpoints (22)

| Method | Path | Controller | Auth |
|--------|------|------------|------|
| POST | `/api/usuarios` | UserController | No |
| POST | `/api/usuarios/login` | UserController | No |
| GET | `/api/usuarios` | UserController | Yes |
| GET | `/api/usuarios/{id}` | UserController | Yes |
| DELETE | `/api/usuarios/{id}` | UserController | Yes |
| POST | `/api/auth/login` | AuthController | No |
| POST | `/api/productos` | ProductController | No |
| GET | `/api/productos` | ProductController | No |
| GET | `/api/productos/{id}` | ProductController | No |
| PUT | `/api/productos/{id}` | ProductController | No |
| DELETE | `/api/productos/{id}` | ProductController | No |
| GET | `/api/productos/{id}/stock` | ProductController | No |
| PUT | `/api/productos/{id}/stock` | ProductController | No |
| POST | `/api/proveedores` | ProveedorController | No |
| GET | `/api/proveedores` | ProveedorController | No |
| GET | `/api/proveedores/{id}` | ProveedorController | No |
| PUT | `/api/proveedores/{id}` | ProveedorController | No |
| DELETE | `/api/proveedores/{id}` | ProveedorController | No |
| POST | `/api/ordenes-retiro` | PickOrderController | No |
| GET | `/api/ordenes-retiro` | PickOrderController | No |
| GET | `/api/ordenes-retiro/{id}` | PickOrderController | No |
| PUT | `/api/ordenes-retiro/{id}` | PickOrderController | No |
| DELETE | `/api/ordenes-retiro/{id}` | PickOrderController | No |

All controllers have `@CrossOrigin(origins = "http://localhost:4200")`.

## Known issues (do not reintroduce)

1. **Orphan Stock on Product delete** (`ProductService.java:152`): `deleteById` deletes the product but not the associated `Stock` record — leaves orphan rows.
2. **Duplicate login endpoints**: `POST /api/usuarios/login` (UserController) and `POST /api/auth/login` (AuthController) do exactly the same thing.
3. **N+1 query** (`ProductService.java:135`): `findAll()` triggers a separate `StockRepository.findById()` per product — no `JOIN FETCH` or `@EntityGraph`.
4. **PickOrder.idUsuario is raw Long** (`PickOrder.java:14`): Stored as `Long`, not `@ManyToOne` — no JPA-level referential integrity with `User`.
5. **Empty Validations class** (`util/Validations.java`): Placeholder with no methods.
6. **Dead code in User.java** (`model/User.java:7-11, 38-44`): Commented-out `@OneToMany` referencing non-existent `Move` and `PickUpOrder` entities.
7. **No global exception handler**: No `@ControllerAdvice` — unhandled `RuntimeException`/`IllegalArgumentException` produce raw 500 errors.
8. **Unused dependencies in pom.xml**: `h2` and `mysql-connector-j` are included but the app only uses PostgreSQL.
9. **No real authentication**: Login returns a DTO but no token/session — subsequent requests have no way to authenticate.
10. **Single test** (`WmsApplicationTests.contextLoads()`): No `src/test/resources/`, no test profile, no integration/unit tests.
11. **PickOrderService.update() stale data risk** (`PickOrderService.java`): Fetches old lines, reverses stock, then re-fetches the same lines to delete — second fetch may return stale data.
12. **Test hits real DB**: `@SpringBootTest` connects to PostgreSQL — no H2 or test profile override.

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
- PKs use `GenerationType.IDENTITY` (except `Stock` which shares Product's PK).
- Composite PKs use `@IdClass`.
- `@Transactional` on service methods that modify multiple entities.

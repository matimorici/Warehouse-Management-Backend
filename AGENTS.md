# Warehouse-Management-Backend

Single-module Maven project (Spring Boot 4.1.0, Java 25, Lombok 1.18.46).

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
- **DB**: PostgreSQL (`ddl-auto=validate` — schema must exist externally). No test profile or H2 override for tests — `@SpringBootTest` hits real PostgreSQL.
- **Auth**: BCryptPasswordEncoder bean, CSRF disabled, only `POST /api/usuarios` is `permitAll()`. No `UserDetailsService`, JWT, or login endpoint — all other endpoints are unreachable without credentials.
- **Frontend CORS**: `http://localhost:4200` (Angular).
- **Packaging note**: Lombok excluded from final artifact by `spring-boot-maven-plugin`.

## Known bugs (do not reintroduce)

1. **ProductController mapping** (`ProductController.java:14`): `@RequestMapping("/api/usuarios")` collides with `UserController` — should be `/api/productos`.
2. **UserService ID type mismatch** (`UserService.java`): `findById`/`deleteById` expect `UUID` but repo is `JpaRepository<User, Long>` — runtime error.
3. **ProductCreateDTO validation** (`ProductCreateDTO.java`): All fields (`codigoBarras`, `idProveedor`, `origenCodigoBarras`) use CUIL regex/messages copied from `UserCreateDTO` — need proper barcode/supplier/origin validation.
4. **ProductService.java**: Malformed — contains draft pseudo-code above the package declaration. Needs cleanup.
5. **ProductRepository** (`ProductRepository.java`): `findBycodigoBarras` uses lowercase `c` — non-standard camelCase.
6. **Single test**: Only `WmsApplicationTests.contextLoads()` exists. No `src/test/resources/`.

## Work rules

- Stick to the exact task asked. Do not fix other bugs, refactor, or touch unrelated files unless explicitly told to.
- If a task uncovers related issues, ask before fixing them.

## Code style

- DTO/entity mapping done manually (no MapStruct).
- `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` from Lombok on entities.
- Spanish messages in validation annotations.
- `@CrossOrigin(origins = "http://localhost:4200")` on each controller class.

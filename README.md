# Warehouse Management Backend (WMS)

Backend REST API para la gestión de almacén. Sistema diseñado para administrar productos, proveedores, stock, órdenes de retiro y de compra, ubicaciones, valoraciones de proveedores, movimientos físicos y códigos de barras.

## Tecnologías

- **Java 25** + **Spring Boot 4.1.0**
- **PostgreSQL** (base de datos)
- **Spring Data JPA** (acceso a datos)
- **Flyway** (migraciones del schema de base de datos)
- **Spring Security** (autenticación — en fase preliminar)
- **Lombok** (reducción de boilerplate)
- **ZXing 3.5.3** (generación de códigos de barras)
- **Maven** (build tool, con wrapper incluido)
- **Angular** (frontend, separado — `localhost:4200`)

## Requisitos previos

- **Java 25** o superior
- **Maven** (o usar el wrapper `./mvnw` incluido en el proyecto)
- **PostgreSQL** corriendo en `localhost:5432`
- **Angular CLI** (para el frontend, si trabajás en eso)

## Instalación

### 1. Clonar el repositorio

```bash
git clone <url-del-repo>
cd Warehouse-Management-Backend
```

### 2. Configurar la base de datos

El sistema necesita una base de datos PostgreSQL llamada `wms_db`. Solo creala (las tablas las crea Flyway automáticamente):

```sql
CREATE DATABASE wms_db;
```

Eso es todo. Al arrancar la aplicación, **Flyway aplica las migraciones del schema** (`db/migration/`) automáticamente y crea todas las tablas, la secuencia `codigo_interno_seq`, los índices y las constraints. Ver [Migraciones de base de datos](#migraciones-de-base-de-datos) más abajo para entender el porqué y el cómo.

### 3. Configurar credenciales (opcional)

Las credenciales por defecto están en `wms/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/wms_db
spring.datasource.username=postgres
spring.datasource.password=12345
```

Si tu PostgreSQL usa otras credenciales, editá ese archivo.

### 4. Ejecutar

Desde la carpeta `wms/`:

```bash
./mvnw spring-boot:run
```

La aplicación arranca en **http://localhost:8080**.

### Build completo

```bash
./mvnw clean compile    # Compilar
./mvnw package          # Generar JAR
./mvnw test             # Ejecutar tests
```

> **Nota sobre tests:** la suite completa (165 tests) no requiere base de datos salvo `WmsApplicationTests.contextLoads()`, que levanta el contexto completo y necesita PostgreSQL corriendo. Para correr solo los tests sin DB: `./mvnw test -Dtest='!WmsApplicationTests'`.

## Migraciones de base de datos

El schema no se mantiene a mano: lo gobierna **Flyway**, una herramienta de migraciones versionadas. Cada cambio estructural es un script SQL numerado que se aplica **una sola vez, en orden**, y queda registrado en una tabla de control. Todo es automático al arrancar la app.

### Qué es y cómo funciona

- Las migraciones viven en `wms/src/main/resources/db/migration/`, nombradas como `V<n>__<descripcion>.sql` (ej. `V1__create_schema.sql`, `V2__agregar_columna_x.sql`).
- Al arrancar, Spring Boot ejecuta Flyway **antes que Hibernate**: compara el número de versión más alto ya aplicado contra los scripts disponibles y aplica los que faltan, en orden de `V1` en adelante.
- Cada migración corre dentro de una transacción: si falla a mitad de camino, se revierte y el arranque aborta (no se deja la base a medias).
- Una vez aplicado, un script **jamás se vuelve a ejecutar**, aunque el archivo se edite. Eso significa que los scripts ya aplicados son **inmutables**: para cambiar algo, se escribe una migración nueva (`V2`, `V3`, ...) que agregue/altere, nunca se toca la anterior.
- Flyway lleva la cuenta en la tabla `flyway_schema_history` de la propia base.
- **Flyway Community no tiene rollback automático** (down-migrations). Si una migración nueva sale mal, no se "deshace": se escribe una migración compensatoria `V<n+1>` que revierta el cambio. Conviene probar cada migración en una base de desarrollo antes de mergearla cerca de una entrega.

### Migraciones actuales

| Versión | Archivo | Qué hace |
|---------|---------|----------|
| V1 | `V1__create_schema.sql` | Crea todo el schema (tablas, índices, constraints, `codigo_interno_seq`). Incluye tablas del dominio que todavía no tienen entidad JPA. |
| V2 | `V2__ensure_codigo_interno_seq.sql` | Reparación: garantiza que `codigo_interno_seq` exista en bases viejas baselineadas como V1 (armadas antes de que la secuencia existiera). No-op en bases nuevas. |
| V3 | `V3__seed_admin_user.sql` | Inserta el usuario administrador por defecto. |

> **Usuario administrador por defecto (seed V3):** CUIL `20-00000000-1`, contraseña `Admin1234`, rol `ADMINISTRADOR`. Existe en todas las bases (nuevas o baselineadas) tras arrancar la app. Se puede loguear con `POST /api/auth/login` o eliminar/modificar si se prefiere.

#### Verificar la reparación V2 (importante, una vez por equipo)

V2 solo tiene lógica real contra una base *vieja* (sin `codigo_interno_seq`); en bases nuevas es no-op y queda sin probar. Antes de dar por cerrado el desfase del baseline, **alguien del equipo debe correr** (con PostgreSQL local):

```bash
bash scripts/verify-v2-repair.sh
```

El script crea un scratch DB (`wms_db_v2_test`), le arma el schema viejo (V1 sin la secuencia), arranca la app contra él y verifica que Flyway haga baseline V1 y aplique V2 (secuencia creada) + V3 (admin seed). **No toca `wms_db`.** Detalles en el encabezado del propio script.

### Por qué (motivación)

Antes, el schema se creaba con `sql/wms_schema.sql`, un script que **borraba todas las tablas y las recreaba** (`DROP TABLE ... CASCADE`). Eso funcionaba en desarrollo pero tenía problemas graves:

1. **No se podía evolucionar sin perder datos.** Cualquier cambio de schema (agregar una columna, una tabla, una secuencia) exigía volver a correr el script completo, que vaciaba la base.
2. **Cambios "manuales" por máquina.** Cuando se agregó `codigo_interno_seq`, cada base existente tuvo que recibir un `CREATE SEQUENCE` a mano. Dos máquinas de dos integrantes del grupo podían terminar con schemas distintos sin que nadie se diera cuenta.
3. **Sin historial ni trazabilidad.** No había forma de saber en qué versión del schema estaba cada base, ni qué cambio se aplicó cuándo.

Las migraciones resuelven todo eso: cada base arranca desde cero (`V1`) y llega al estado actual aplicando una serie de deltas idempotentes y versionados. Cualquier miembro del equipo —o la futura producción— llega al mismo schema con solo `./mvnw spring-boot:run`.

### Cómo se trabaja con ellas (reglas prácticas)

- **Base nueva:** solo `CREATE DATABASE wms_db;` y a arrancar. Flyway crea todo.
- **Base existente (creada con el viejo script):** no hay que hacer nada manual. `spring.flyway.baseline-on-migrate=true` marca la base con su estado actual como "baseline V1" en el primer arranque, sin borrar datos; de ahí en más, las migraciones nuevas se aplican normalmente. **Única excepción:** si tu base es tan vieja que no tiene `codigo_interno_seq` (se agregó al script en una revisión posterior), la migración V2 la crea automáticamente. Bases anteriores a la migración de PKs UUID→BIGINT no son compatibles: recreala desde cero.
- **Agregar un cambio estructural:** se crea `db/migration/V<n+1>__descripcion.sql` con el SQL del cambio (ej. `ALTER TABLE ...`, `CREATE TABLE ...`, `CREATE SEQUENCE ...`). No editar migraciones ya aplicadas. Al siguiente arranque se aplica solo.
- **Datos de referencia** (ej. un usuario admin inicial): se insertan también en una migración, así existen en todas las bases.
- **`ddl-auto=validate`:** se mantiene como red de seguridad. Flyway es dueño del schema y lo crea/actualiza; Hibernate solo verifica que las entidades coincidan. Si una entidad y una migración se desalinean, la app no arranca y avisa, en vez de corromper la base.
- **El script viejo** `wms_schema.sql` se conserva en `db/wms_schema.sql` solo como referencia histórica de la versión original; no se ejecuta más.

## Estructura del proyecto

```
wms/src/main/java/big_three/wms/
├── WmsApplication.java              # Punto de entrada
├── config/
│   └── SecurityConfig.java          # Configuración de seguridad (placeholder)
├── controller/
│   ├── AuthController.java          # Login
│   ├── MovimientoFisicoController.java   # Historial de movimientos físicos
│   ├── OrdenCompraController.java        # CRUD órdenes de compra
│   ├── PickOrderController.java          # CRUD órdenes de retiro
│   ├── ProductController.java            # CRUD productos + stock + barcode
│   ├── ProveedorController.java          # CRUD proveedores
│   ├── UbicacionController.java          # CRUD ubicaciones
│   ├── UserController.java               # CRUD usuarios
│   └── ValoracionProveedorController.java  # CRUD valoraciones de proveedores
├── dto/
│   ├── LineaCompraCreateDTO.java
│   ├── LineaCompraResponseDTO.java
│   ├── LoginRequestDTO.java
│   ├── MovimientoFisicoCreateDTO.java
│   ├── MovimientoFisicoResponseDTO.java
│   ├── OrdenCompraCreateDTO.java
│   ├── OrdenCompraResponseDTO.java
│   ├── PickOrderCreateDTO.java
│   ├── PickOrderLineCreateDTO.java
│   ├── PickOrderLineResponseDTO.java
│   ├── PickOrderResponseDTO.java
│   ├── ProductCreateDTO.java
│   ├── ProductResponseDTO.java
│   ├── ProveedorCreateDTO.java
│   ├── ProveedorResponseDTO.java
│   ├── StockResponseDTO.java
│   ├── StockUpdateDTO.java
│   ├── UbicacionCreateDTO.java
│   ├── UbicacionResponseDTO.java
│   ├── UserCreateDTO.java
│   ├── UserResponseDTO.java
│   ├── ValoracionProveedorCreateDTO.java
│   └── ValoracionProveedorResponseDTO.java
├── exception/
│   ├── GlobalExceptionHandler.java  # @ControllerAdvice (handler global)
│   └── InvalidCredentialsException.java
├── model/
│   ├── LineaCompra.java             # Línea de orden de compra (composite PK)
│   ├── MovimientoFisico.java        # Movimiento físico (composite PK)
│   ├── OrdenCompra.java             # Orden de compra (enum EstadoOrdenCompra)
│   ├── PickOrder.java               # Orden de retiro
│   ├── PickOrderLine.java           # Línea de orden de retiro (composite PK)
│   ├── Product.java                 # Producto (enum OrigenCodigoBarras)
│   ├── Proveedor.java               # Proveedor
│   ├── Stock.java                   # Stock (1:1 con Product)
│   ├── Ubicacion.java               # Ubicación
│   ├── User.java                    # Usuario
│   └── ValoracionProveedor.java     # Valoración de proveedor
├── repository/
│   ├── LineaCompraRepository.java
│   ├── MovimientoFisicoRepository.java
│   ├── OrdenCompraRepository.java
│   ├── PickOrderLineRepository.java
│   ├── PickOrderRepository.java
│   ├── ProductRepository.java
│   ├── ProveedorRepository.java
│   ├── StockRepository.java
│   ├── UbicacionRepository.java
│   ├── UserRepository.java
│   └── ValoracionProveedorRepository.java
└── service/
    ├── BarcodeService.java
    ├── MovimientoFisicoService.java
    ├── OrdenCompraService.java
    ├── PickOrderService.java
    ├── ProductService.java
    ├── ProveedorService.java
    ├── UbicacionService.java
    ├── UserService.java
    └── ValoracionProveedorService.java
```

## Entidades

| Entidad | Tabla | Descripción |
|---------|-------|-------------|
| **User** | `usuario` | Usuarios del sistema (nombre, apellido, CUIL, rol, contraseña hasheada con BCrypt). Rol por defecto: `OPERARIO`. |
| **Product** | `producto` | Productos del almacén. Tiene código de barras (interno o de fábrica), descripción y relación con un proveedor. |
| **Proveedor** | `proveedor` | Proveedores de productos (CUIT, razón social, teléfono, mail, dirección). |
| **Stock** | `stock` | Stock de cada producto. Relación 1:1 con Product (comparten PK). Tiene cantidad disponible y cantidad pendiente. |
| **PickOrder** | `orden_retiro` | Orden de retiro de productos. Asociada a un usuario por `id_usuario` y compuesta por una o más líneas. |
| **PickOrderLine** | `linea_retiro` | Línea de una orden de retiro. PK compuesta: `id_orden_retiro` + `id_producto`. Cantidad a retirar. |
| **ValoracionProveedor** | `valoracion_proveedor` | Valoración de un proveedor: tiempo de entrega, forma de entrega y relación precio-calidad. `fecha_hora` se setea server-side. |
| **Ubicacion** | `ubicacion` | Ubicaciones del almacén (nombre). |
| **OrdenCompra** | `orden_compra` | Orden de compra a un proveedor. Asociada por `id_proveedor`. Estado: `PENDIENTE`, `RECIBIDA` o `CANCELADA`. |
| **LineaCompra** | `linea_compra` | Línea de una orden de compra. PK compuesta: `id_orden_compra` + `id_producto`. Cantidad a comprar. |
| **MovimientoFisico** | `movimiento_fisico` | Historial de movimientos de productos entre ubicaciones. PK compuesta: `id_producto` + `fecha_hora`. Log append-only (sin update/delete). |

## API REST

Todas las rutas están bajo el prefijo `/api/`. Los controladores permiten CORS desde `http://localhost:4200` (Angular).

### Usuarios

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/usuarios` | Crear usuario | `{ "nombre", "apellido", "cuil", "contrasena" }` |
| `GET` | `/api/usuarios` | Listar todos los usuarios | — |
| `GET` | `/api/usuarios/{id}` | Buscar usuario por ID | — |
| `DELETE` | `/api/usuarios/{id}` | Eliminar usuario | — |

### Autenticación

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/auth/login` | Login | `{ "cuil", "contrasena" }` |

> **Nota:** La autenticación está **delegada** a otro integrante del equipo (no implementada acá). El login retorna un DTO con los datos del usuario pero no genera token ni sesión. El plan acordado usa **Sessions** (no JWT) — ver `wms/TODO.md`. Hasta que se implemente, todos los endpoints quedan con `permitAll` en `SecurityConfig`.

### Productos

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/productos` | Crear producto | `{ "nombreProducto", "descripcionProducto", "codigoBarras"?, "idProveedor", "origenCodigoBarras", "cantidadDisponible"?, "cantidadPendiente"? }` |
| `GET` | `/api/productos` | Listar todos los productos (con stock) | — |
| `GET` | `/api/productos/{id}` | Buscar producto por ID | — |
| `PUT` | `/api/productos/{id}` | Actualizar producto | `{ "nombreProducto", "descripcionProducto", "codigoBarras"?, "idProveedor", "origenCodigoBarras", "cantidadDisponible"?, "cantidadPendiente"? }` |
| `DELETE` | `/api/productos/{id}` | Eliminar producto | — |
| `GET` | `/api/productos/{id}/stock` | Obtener stock de un producto | — |
| `PUT` | `/api/productos/{id}/stock` | Actualizar stock de un producto | `{ "cantidadDisponible"?, "cantidadPendiente"? }` |
| `GET` | `/api/productos/{id}/barcode` | Generar imagen PNG del código de barras del producto | — |

> Si `codigoBarras` se envía vacío o nulo al crear, se genera automáticamente un código interno con formato `INT-XXXXXX`. El endpoint de barcode devuelve `image/png` (código CODE_128) a partir de `codigoBarras`.

### Proveedores

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/proveedores` | Crear proveedor | `{ "cuit", "razonSocial", "telefono"?, "mail"?, "direccion"? }` |
| `GET` | `/api/proveedores` | Listar todos los proveedores | — |
| `GET` | `/api/proveedores/{id}` | Buscar proveedor por ID | — |
| `PUT` | `/api/proveedores/{id}` | Actualizar proveedor | `{ "cuit", "razonSocial", "telefono"?, "mail"?, "direccion"? }` |
| `DELETE` | `/api/proveedores/{id}` | Eliminar proveedor | — |

### Órdenes de Retiro

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/ordenes-retiro` | Crear orden de retiro | `{ "idUsuario", "lineasRetiro": [{ "idProducto", "cantidad" }] }` |
| `GET` | `/api/ordenes-retiro` | Listar órdenes (resumen, sin líneas) | — |
| `GET` | `/api/ordenes-retiro/{id}` | Buscar orden por ID (con líneas) | — |
| `PUT` | `/api/ordenes-retiro/{id}` | Actualizar orden | `{ "idUsuario", "lineasRetiro": [{ "idProducto", "cantidad" }] }` |
| `DELETE` | `/api/ordenes-retiro/{id}` | Eliminar orden (revierte stock) | — |

> Al crear/actualizar una orden de retiro, el stock se ajusta automáticamente: se resta de `cantidadDisponible` y se suma a `cantidadPendiente`. Al eliminar, se revierte el ajuste.

### Valoraciones de Proveedores

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/valoraciones-proveedor` | Crear valoración | `{ "idProveedor", "tiempoEntrega"?, "formaEntrega"?, "relacionPrecioCalidad"? }` |
| `GET` | `/api/valoraciones-proveedor` | Listar valoraciones | — |
| `GET` | `/api/valoraciones-proveedor/{id}` | Buscar valoración por ID | — |
| `GET` | `/api/valoraciones-proveedor/proveedor/{idProveedor}` | Valoraciones de un proveedor | — |
| `DELETE` | `/api/valoraciones-proveedor/{id}` | Eliminar valoración | — |

> `fecha_hora` se setea server-side (`LocalDateTime.now()`); el endpoint de crear no la recibe.

### Ubicaciones

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/ubicaciones` | Crear ubicación | `{ "nombre" }` |
| `GET` | `/api/ubicaciones` | Listar ubicaciones | — |
| `GET` | `/api/ubicaciones/{id}` | Buscar ubicación por ID | — |
| `PUT` | `/api/ubicaciones/{id}` | Actualizar ubicación | `{ "nombre" }` |
| `DELETE` | `/api/ubicaciones/{id}` | Eliminar ubicación | — |

### Órdenes de Compra

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/ordenes-compra` | Crear orden de compra (estado `PENDIENTE`) | `{ "idProveedor", "lineasCompra": [{ "idProducto", "cantidad" }] }` |
| `GET` | `/api/ordenes-compra` | Listar órdenes | — |
| `GET` | `/api/ordenes-compra/{id}` | Buscar orden por ID (con líneas) | — |
| `PUT` | `/api/ordenes-compra/{id}?estado=RECIBIDA` | Actualizar orden o cambiar estado | `{ "idProveedor", "lineasCompra": [{ "idProducto", "cantidad" }] }` |
| `DELETE` | `/api/ordenes-compra/{id}` | Eliminar orden | — |

> Al pasar una orden a `RECIBIDA` se suma `cantidad` de cada línea a `cantidadDisponible` del producto. Las órdenes `RECIBIDA` no se pueden editar ni eliminar. `PENDIENTE` → `CANCELADA` no modifica stock.

### Movimientos Físicos

| Método | Ruta | Descripción | Body |
|--------|------|-------------|------|
| `POST` | `/api/movimientos-fisicos` | Registrar movimiento (log append-only) | `{ "idProducto", "idUbicacionOrigen"?, "idUbicacionDestino", "idUsuario", "comentario"? }` |
| `GET` | `/api/movimientos-fisicos` | Listar todos los movimientos | — |
| `GET` | `/api/movimientos-fisicos/producto/{idProducto}` | Movimientos de un producto | — |
| `GET` | `/api/movimientos-fisicos/producto/{idProducto}/fecha/{fechaHora}` | Movimiento específico (fecha ISO) | — |

> `fecha_hora` (parte de la PK) se setea server-side. No tiene endpoints de update/delete: es un historial de eventos.

## Validaciones

Los DTOs de creación usan Jakarta Bean Validation. Errores de validación retornan 400 Bad Request con los mensajes en español:

- **Usuario**: nombre y apellido (3-150 chars), CUIL (formato XX-XXXXXXXX-X), contraseña (mínimo 8 chars, al menos una mayúscula y un dígito)
- **Producto**: nombre (3-150 chars), descripción (3-500 chars), código de barras (máx 50 chars, opcional), proveedor requerido, origen (`FABRICANTE` o `INTERNO`), cantidades ≥ 0
- **Proveedor**: CUIT (formato XX-XXXXXXXX-X), razón social (3-150 chars), email válido (si se provee)
- **Orden de retiro**: usuario requerido, líneas requeridas (mínimo 1), cantidad por línea ≥ 1
- **Valoración de proveedor**: proveedor requerido, tiempo de entrega ≥ 0, forma de entrega (máx 100 chars), relación precio-calidad (0-5)
- **Ubicación**: nombre (3-100 chars)
- **Orden de compra**: proveedor requerido, líneas requeridas (mínimo 1), cantidad por línea ≥ 1
- **Movimiento físico**: producto, ubicación destino y usuario requeridos; ubicación origen opcional; comentario (máx 255 chars)

## Issues conocidos

1. ~~**Orphan Stock al eliminar producto**~~ — FIXED: `ProductService.deleteById()` ahora elimina también el `Stock` asociado.
2. ~~**Login duplicado**~~ — FIXED: el login es único en `POST /api/auth/login`.
3. ~~**N+1 query en listar productos**~~ — FIXED: se resuelve con 2 queries (JOIN Fetch).
4. ~~**Sin handler global de excepciones**~~ — FIXED: existe `GlobalExceptionHandler` (`@ControllerAdvice`).
5. **Sin autenticación real**: el login devuelve datos pero no genera token ni sesión. **Delegada** a otro integrante — plan **Sessions** en `wms/TODO.md`.
6. **`WmsApplicationTests.contextLoads()` requiere PostgreSQL corriendo**: el resto de la suite (unitarios + controllers) no necesita DB.

> Ver `AGENTS.md` para la lista completa de issues conocidos.
